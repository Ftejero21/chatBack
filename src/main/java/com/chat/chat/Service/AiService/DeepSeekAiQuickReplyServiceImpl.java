package com.chat.chat.Service.AiService;

import com.chat.chat.Configuracion.AiProperties;
import com.chat.chat.Configuracion.DeepSeekProperties;
import com.chat.chat.DTO.AiChatContextMessageDTO;
import com.chat.chat.DTO.AiQuickReplyRequestDTO;
import com.chat.chat.DTO.AiQuickReplyResponseDTO;
import com.chat.chat.Exceptions.RecursoNoEncontradoException;
import com.chat.chat.Exceptions.SemanticApiException;
import com.chat.chat.Repository.ChatGrupalRepository;
import com.chat.chat.Repository.ChatIndividualRepository;
import com.chat.chat.Utils.Constantes;
import com.chat.chat.Utils.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.text.Normalizer;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

@Service
@Validated
public class DeepSeekAiQuickReplyServiceImpl implements AiQuickReplyService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DeepSeekAiQuickReplyServiceImpl.class);
    private static final Pattern LEADING_LIST_PATTERN = Pattern.compile("^(?:[-*•]+|\\d+[.)-]?)\\s*");
    private static final int MAX_MESSAGE_LENGTH = 2000;
    private static final int MAX_CONTEXT_MESSAGES = 8;
    private static final int MAX_CONTEXT_MESSAGE_LENGTH = 280;
    private static final int MAX_CONTEXT_TOTAL_LENGTH = 1600;
    private static final int MAX_SUGGESTION_LENGTH = 140;
    private static final int REQUIRED_SUGGESTIONS = 3;
    private static final long QUICK_REPLY_DAY_WINDOW_MS = Duration.ofDays(1).toMillis();

    private final AiProperties aiProperties;
    private final DeepSeekProperties deepSeekProperties;
    private final DeepSeekApiClient deepSeekApiClient;
    private final AiRateLimitService aiRateLimitService;
    private final SecurityUtils securityUtils;
    private final ChatIndividualRepository chatIndividualRepository;
    private final ChatGrupalRepository chatGrupalRepository;
    private final Map<String, CachedQuickReply> quickReplyCache = new ConcurrentHashMap<>();
    private final Map<String, Long> quickReplyCooldownByChat = new ConcurrentHashMap<>();
    private final Map<Long, WindowCounter> quickReplyDailyUsage = new ConcurrentHashMap<>();

    public DeepSeekAiQuickReplyServiceImpl(AiProperties aiProperties,
                                           DeepSeekProperties deepSeekProperties,
                                           DeepSeekApiClient deepSeekApiClient,
                                           AiRateLimitService aiRateLimitService,
                                           SecurityUtils securityUtils,
                                           ChatIndividualRepository chatIndividualRepository,
                                           ChatGrupalRepository chatGrupalRepository) {
        this.aiProperties = aiProperties;
        this.deepSeekProperties = deepSeekProperties;
        this.deepSeekApiClient = deepSeekApiClient;
        this.aiRateLimitService = aiRateLimitService;
        this.securityUtils = securityUtils;
        this.chatIndividualRepository = chatIndividualRepository;
        this.chatGrupalRepository = chatGrupalRepository;
    }

    @Override
    public AiQuickReplyResponseDTO generarSugerencias(AiQuickReplyRequestDTO request) {
        Long userId = securityUtils.getAuthenticatedUserId();
        String originalMessage = request == null ? null : request.getMensajeRecibido();

        if (!aiProperties.isEnabled()) {
            return failure("AI_DISABLED", "La ayuda de IA no esta habilitada.");
        }
        if (!"deepseek".equalsIgnoreCase(aiProperties.getProvider())) {
            return failure("AI_PROVIDER_NOT_SUPPORTED", "El proveedor de IA configurado no es compatible.");
        }
        if (!hasText(originalMessage)) {
            return failure("AI_QUICK_REPLY_EMPTY", "El mensaje recibido es obligatorio.");
        }
        if (!hasText(deepSeekProperties.getApiKey())) {
            return failure("AI_API_KEY_MISSING", "La API Key de DeepSeek no esta configurada.");
        }

        String normalizedMessage = normalizeInput(originalMessage);
        if (!hasText(normalizedMessage)) {
            return failure("AI_QUICK_REPLY_EMPTY", "El mensaje recibido es obligatorio.");
        }
        if (normalizedMessage.length() > MAX_MESSAGE_LENGTH) {
            return failure("AI_QUICK_REPLY_TOO_LONG", "El mensaje recibido supera la longitud maxima permitida.");
        }

        String chatType = normalizeChatType(request == null ? null : request.getTipoChat());
        if (chatType == null) {
            return failure("AI_QUICK_REPLY_CHAT_TYPE_INVALID", "El tipo de chat no es valido.");
        }

        Long messageId = request == null ? null : request.getMessageId();
        String cacheKey = buildCacheKey(userId, messageId);
        if (cacheKey != null) {
            CachedQuickReply cachedQuickReply = quickReplyCache.get(cacheKey);
            if (cachedQuickReply != null) {
                LOGGER.info("[AI][QUICK_REPLY] cache-hit userId={} messageId={}", userId, messageId);
                return success(cachedQuickReply.sugerencias(), "Sugerencias obtenidas de cache");
            }
        }

        try {
            validateChatAccess(request, userId, chatType);
        } catch (IllegalArgumentException ex) {
            return failure("AI_QUICK_REPLY_INVALID_REQUEST", ex.getMessage());
        } catch (RecursoNoEncontradoException ex) {
            return failure("AI_QUICK_REPLY_CHAT_NOT_FOUND", ex.getMessage());
        }

        String chatScopeKey = resolveChatScopeKey(request, chatType);
        QuickReplyLimitCheck quickReplyLimitCheck = checkQuickReplySpecificLimits(userId, chatScopeKey);
        if (!quickReplyLimitCheck.allowed()) {
            return failure(quickReplyLimitCheck.code(), quickReplyLimitCheck.message());
        }

        AiRateLimitCheck rateLimitCheck = aiRateLimitService.checkUsage(userId);
        if (!rateLimitCheck.isAllowed()) {
            return failure(rateLimitCheck.getCode(), rateLimitCheck.getMessage());
        }

        List<AiChatContextMessageDTO> sanitizedContext = sanitizeContext(request == null ? null : request.getContexto());
        String promptContext = buildUserContent(normalizedMessage, chatType, sanitizedContext);

        try {
            LOGGER.info("[AI][QUICK_REPLY] request userId={} tipoChat={} messageLength={} contextSize={}",
                    userId,
                    chatType,
                    normalizedMessage.length(),
                    sanitizedContext.size());
            String rawOutput = deepSeekApiClient.completarTexto(buildSystemPrompt(), promptContext);
            aiRateLimitService.registrarUso(userId);
            List<String> suggestions = sanitizeSuggestions(rawOutput, normalizedMessage, chatType, sanitizedContext);
            registerQuickReplyGeneration(userId, chatScopeKey, cacheKey, suggestions);
            return success(suggestions, "Sugerencias generadas correctamente");
        } catch (SemanticApiException ex) {
            LOGGER.warn("[AI][QUICK_REPLY] provider-error userId={} tipoChat={} code={} status={}",
                    userId,
                    chatType,
                    ex.getCode(),
                    ex.getStatus().value());
            return failure(ex.getCode(), ex.getMessage());
        }
    }

    private QuickReplyLimitCheck checkQuickReplySpecificLimits(Long userId, String chatScopeKey) {
        long now = System.currentTimeMillis();
        if (chatScopeKey != null) {
            Long lastGeneratedAt = quickReplyCooldownByChat.get(buildCooldownKey(userId, chatScopeKey));
            long cooldownMs = Math.max(1, aiProperties.getQuickReplies().getCooldownSeconds()) * 1000L;
            if (lastGeneratedAt != null && now - lastGeneratedAt < cooldownMs) {
                return new QuickReplyLimitCheck(false,
                        "AI_QUICK_REPLIES_COOLDOWN",
                        "Espera un momento antes de generar nuevas respuestas rapidas.");
            }
        }

        WindowCounter usageCounter = quickReplyDailyUsage.computeIfAbsent(safeUserId(userId), ignored -> new WindowCounter());
        synchronized (usageCounter) {
            usageCounter.cleanup(now, QUICK_REPLY_DAY_WINDOW_MS);
            if (usageCounter.size() >= aiProperties.getQuickReplies().getMaxPerUserDay()) {
                return new QuickReplyLimitCheck(false,
                        "AI_QUICK_REPLIES_DAILY_LIMIT",
                        "Has alcanzado el limite diario de respuestas rapidas.");
            }
        }
        return QuickReplyLimitCheck.ok();
    }

    private void registerQuickReplyGeneration(Long userId,
                                              String chatScopeKey,
                                              String cacheKey,
                                              List<String> suggestions) {
        long now = System.currentTimeMillis();
        WindowCounter usageCounter = quickReplyDailyUsage.computeIfAbsent(safeUserId(userId), ignored -> new WindowCounter());
        synchronized (usageCounter) {
            usageCounter.add(now, QUICK_REPLY_DAY_WINDOW_MS);
        }
        if (chatScopeKey != null) {
            quickReplyCooldownByChat.put(buildCooldownKey(userId, chatScopeKey), now);
        }
        if (cacheKey != null && suggestions != null && !suggestions.isEmpty()) {
            quickReplyCache.put(cacheKey, new CachedQuickReply(List.copyOf(suggestions), now));
        }
    }

    private void validateChatAccess(AiQuickReplyRequestDTO request, Long userId, String chatType) {
        Long chatId = request == null ? null : request.getChatId();
        Long chatGrupalId = request == null ? null : request.getChatGrupalId();
        if (Constantes.CHAT_TIPO_INDIVIDUAL.equals(chatType)) {
            if (chatGrupalId != null) {
                throw new IllegalArgumentException("chatGrupalId no aplica a chats individuales.");
            }
            if (chatId != null && !chatIndividualRepository.existsMemberByChatIdAndUserId(chatId, userId)) {
                if (chatIndividualRepository.existsById(chatId)) {
                    throw new IllegalArgumentException(Constantes.MSG_NO_PERTENECE_CHAT);
                }
                throw new RecursoNoEncontradoException(Constantes.MSG_CHAT_NO_ENCONTRADO_ID + chatId);
            }
            return;
        }

        if (chatId != null) {
            throw new IllegalArgumentException("chatId no aplica a chats grupales.");
        }
        if (chatGrupalId != null && !chatGrupalRepository.existsActiveMemberByChatIdAndUserId(chatGrupalId, userId)) {
            if (chatGrupalRepository.existsById(chatGrupalId)) {
                throw new IllegalArgumentException(Constantes.MSG_NO_PERTENECE_GRUPO);
            }
            throw new RecursoNoEncontradoException(Constantes.MSG_CHAT_GRUPAL_NO_ENCONTRADO_ID + chatGrupalId);
        }
    }

    private List<AiChatContextMessageDTO> sanitizeContext(List<AiChatContextMessageDTO> context) {
        if (context == null || context.isEmpty()) {
            return List.of();
        }
        List<AiChatContextMessageDTO> trimmed = new ArrayList<>();
        int totalLength = 0;
        int start = Math.max(0, context.size() - MAX_CONTEXT_MESSAGES);
        for (int i = start; i < context.size(); i++) {
            AiChatContextMessageDTO message = context.get(i);
            if (message == null) {
                continue;
            }
            String autor = normalizeInput(message.getAutor());
            String contenido = normalizeInput(message.getContenido());
            if (!hasText(autor) || !hasText(contenido)) {
                continue;
            }
            autor = truncate(autor, 80);
            contenido = truncate(contenido, MAX_CONTEXT_MESSAGE_LENGTH);
            if (totalLength + contenido.length() > MAX_CONTEXT_TOTAL_LENGTH) {
                int remaining = MAX_CONTEXT_TOTAL_LENGTH - totalLength;
                if (remaining <= 0) {
                    break;
                }
                contenido = truncate(contenido, remaining);
            }
            AiChatContextMessageDTO sanitized = new AiChatContextMessageDTO();
            sanitized.setAutor(autor);
            sanitized.setContenido(contenido);
            sanitized.setEsUsuarioActual(message.isEsUsuarioActual());
            trimmed.add(sanitized);
            totalLength += contenido.length();
            if (totalLength >= MAX_CONTEXT_TOTAL_LENGTH) {
                break;
            }
        }
        return trimmed;
    }

    private String buildSystemPrompt() {
        return "Genera exactamente 3 respuestas rapidas que el usuario pueda enviar directamente al ultimo mensaje recibido. "
                + "Usa un tono natural, breve y adecuado al contexto. "
                + "Si hay contexto reciente, imita ligeramente el estilo del usuario actual, pero no copies mensajes anteriores literalmente. "
                + "No inventes datos importantes. "
                + "No generes respuestas ofensivas, comprometidas, manipuladoras ni demasiado largas. "
                + "Cada respuesta debe tener maximo 1 linea y ser segura para envio directo. "
                + "Devuelve solo las 3 respuestas, separadas por salto de linea, sin numeracion ni explicacion.";
    }

    private String buildUserContent(String normalizedMessage, String chatType, List<AiChatContextMessageDTO> context) {
        StringBuilder builder = new StringBuilder();
        builder.append("Tipo de chat: ").append(chatType).append('\n');
        builder.append("Ultimo mensaje recibido:\n").append(normalizedMessage).append('\n');
        if (!context.isEmpty()) {
            builder.append("Contexto reciente:\n");
            for (AiChatContextMessageDTO item : context) {
                builder.append(item.isEsUsuarioActual() ? "[YO] " : "[OTRO] ");
                builder.append(item.getAutor()).append(": ");
                builder.append(item.getContenido()).append('\n');
            }
        }
        return builder.toString().trim();
    }

    private List<String> sanitizeSuggestions(String rawOutput,
                                             String normalizedMessage,
                                             String chatType,
                                             List<AiChatContextMessageDTO> context) {
        Set<String> cleaned = new LinkedHashSet<>();
        if (hasText(rawOutput)) {
            String normalizedOutput = rawOutput.replace("\r\n", "\n").replace('\r', '\n');
            String[] parts = normalizedOutput.split("\n+");
            for (String part : parts) {
                String candidate = cleanupSuggestion(part);
                if (!hasText(candidate)) {
                    continue;
                }
                cleaned.add(candidate);
                if (cleaned.size() == REQUIRED_SUGGESTIONS) {
                    break;
                }
            }
            if (cleaned.size() < REQUIRED_SUGGESTIONS) {
                String compact = cleanupSuggestion(normalizedOutput.replace('\n', ' '));
                if (hasText(compact)) {
                    String[] fragments = compact.split("\\s{2,}|\\s*\\|\\s*|\\s*;\\s*");
                    for (String fragment : fragments) {
                        String candidate = cleanupSuggestion(fragment);
                        if (!hasText(candidate)) {
                            continue;
                        }
                        cleaned.add(candidate);
                        if (cleaned.size() == REQUIRED_SUGGESTIONS) {
                            break;
                        }
                    }
                }
            }
        }

        for (String fallback : fallbackSuggestions(normalizedMessage, chatType, context)) {
            String candidate = cleanupSuggestion(fallback);
            if (!hasText(candidate)) {
                continue;
            }
            cleaned.add(candidate);
            if (cleaned.size() == REQUIRED_SUGGESTIONS) {
                break;
            }
        }
        return new ArrayList<>(cleaned).subList(0, REQUIRED_SUGGESTIONS);
    }

    private List<String> fallbackSuggestions(String normalizedMessage,
                                             String chatType,
                                             List<AiChatContextMessageDTO> context) {
        String normalized = normalizeForMatching(normalizedMessage);
        boolean support = containsAny(normalized, "mal", "peor", "jodid", "lo siento", "triste", "problema", "ahi ahi");
        boolean question = normalizedMessage.contains("?");
        boolean thanks = containsAny(normalized, "gracias", "thanks");
        List<String> styleHints = extractUserStyleHints(context);

        if (support) {
            return applyStyle(List.of(
                    "jo, lo siento mucho, si quieres hablar aqui estoy",
                    "vaya, espero que mejore pronto, mucho animo",
                    "si te apetece desahogarte, te escucho"
            ), styleHints, chatType);
        }
        if (thanks) {
            return applyStyle(List.of(
                    "nada, para eso estamos",
                    "no hay de que, aqui estoy",
                    "cuando quieras"
            ), styleHints, chatType);
        }
        if (question) {
            return applyStyle(List.of(
                    "si, claro",
                    "puede ser, te digo ahora",
                    "dale, luego te cuento mejor"
            ), styleHints, chatType);
        }
        return applyStyle(List.of(
                "vale, te leo",
                "si, totalmente",
                "dale, luego hablamos bien"
        ), styleHints, chatType);
    }

    private List<String> applyStyle(List<String> base, List<String> styleHints, String chatType) {
        if (styleHints.isEmpty() || Constantes.CHAT_TIPO_GRUPAL.equals(chatType)) {
            return base;
        }
        List<String> styled = new ArrayList<>();
        for (int i = 0; i < base.size(); i++) {
            String prefix = styleHints.get(Math.min(i, styleHints.size() - 1));
            styled.add(prefix + " " + base.get(i));
        }
        return styled;
    }

    private List<String> extractUserStyleHints(List<AiChatContextMessageDTO> context) {
        List<String> hints = new ArrayList<>();
        for (AiChatContextMessageDTO item : context) {
            if (!item.isEsUsuarioActual()) {
                continue;
            }
            String normalized = normalizeForMatching(item.getContenido());
            if (normalized.contains("tio")) {
                hints.add("tio,");
            } else if (normalized.contains("bro") || normalized.contains("hermano")) {
                hints.add("bro,");
            } else if (normalized.contains("joder") || normalized.contains("ostia")) {
                hints.add("jo,");
            }
            if (hints.size() == REQUIRED_SUGGESTIONS) {
                break;
            }
        }
        return hints;
    }

    private String cleanupSuggestion(String value) {
        String normalized = normalizeInput(value);
        if (!hasText(normalized)) {
            return null;
        }
        normalized = LEADING_LIST_PATTERN.matcher(normalized).replaceFirst("");
        normalized = normalized.replaceAll("^['\"`]+|['\"`]+$", "");
        normalized = normalized.replaceAll("\\s{2,}", " ").trim();
        if (!hasText(normalized)) {
            return null;
        }
        return truncate(normalized, MAX_SUGGESTION_LENGTH);
    }

    private String normalizeChatType(String chatType) {
        if (!hasText(chatType)) {
            return null;
        }
        String normalized = chatType.trim().toUpperCase(Locale.ROOT);
        if (Constantes.CHAT_TIPO_INDIVIDUAL.equals(normalized) || Constantes.CHAT_TIPO_GRUPAL.equals(normalized)) {
            return normalized;
        }
        return null;
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private String resolveChatScopeKey(AiQuickReplyRequestDTO request, String chatType) {
        if (request == null) {
            return null;
        }
        if (Constantes.CHAT_TIPO_INDIVIDUAL.equals(chatType) && request.getChatId() != null) {
            return chatType + ":" + request.getChatId();
        }
        if (Constantes.CHAT_TIPO_GRUPAL.equals(chatType) && request.getChatGrupalId() != null) {
            return chatType + ":" + request.getChatGrupalId();
        }
        return null;
    }

    private String buildCacheKey(Long userId, Long messageId) {
        if (messageId == null) {
            return null;
        }
        return safeUserId(userId) + ":" + messageId;
    }

    private String buildCooldownKey(Long userId, String chatScopeKey) {
        return safeUserId(userId) + ":" + chatScopeKey;
    }

    private long safeUserId(Long userId) {
        return userId == null ? -1L : userId;
    }

    private String normalizeInput(String text) {
        if (text == null) {
            return null;
        }
        String compact = text.replace("\r\n", "\n").replace('\r', '\n');
        compact = compact.replaceAll("[\\t\\f\\x0B]+", " ");
        compact = compact.replaceAll(" {2,}", " ");
        compact = compact.replaceAll("\\n{3,}", "\n\n");
        return compact.trim();
    }

    private String truncate(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength).trim();
    }

    private boolean containsAny(String normalized, String... needles) {
        if (normalized == null) {
            return false;
        }
        for (String needle : needles) {
            if (normalized.contains(needle)) {
                return true;
            }
        }
        return false;
    }

    private String normalizeForMatching(String text) {
        String normalized = normalizeInput(text);
        if (normalized == null) {
            return "";
        }
        String ascii = Normalizer.normalize(normalized, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
        return ascii.toLowerCase(Locale.ROOT);
    }

    private AiQuickReplyResponseDTO success(List<String> suggestions, String message) {
        AiQuickReplyResponseDTO response = new AiQuickReplyResponseDTO();
        response.setSuccess(true);
        response.setCodigo("OK");
        response.setMensaje(message);
        response.setSugerencias(suggestions);
        return response;
    }

    private AiQuickReplyResponseDTO failure(String code, String message) {
        AiQuickReplyResponseDTO response = new AiQuickReplyResponseDTO();
        response.setSuccess(false);
        response.setCodigo(code);
        response.setMensaje(message);
        response.setSugerencias(List.of());
        return response;
    }

    private record CachedQuickReply(List<String> sugerencias, long createdAt) {
    }

    private record QuickReplyLimitCheck(boolean allowed, String code, String message) {
        private static QuickReplyLimitCheck ok() {
            return new QuickReplyLimitCheck(true, "OK", "OK");
        }
    }

    private static final class WindowCounter {
        private final Deque<Long> timestamps = new ArrayDeque<>();

        private void add(long now, long windowMs) {
            cleanup(now, windowMs);
            timestamps.addLast(now);
        }

        private void cleanup(long now, long windowMs) {
            while (!timestamps.isEmpty() && now - timestamps.peekFirst() >= windowMs) {
                timestamps.pollFirst();
            }
        }

        private int size() {
            return timestamps.size();
        }
    }
}
