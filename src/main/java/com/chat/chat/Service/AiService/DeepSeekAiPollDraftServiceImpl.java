package com.chat.chat.Service.AiService;

import com.chat.chat.Configuracion.AiProperties;
import com.chat.chat.Configuracion.DeepSeekProperties;
import com.chat.chat.DTO.AiPollDraftContextMessageDTO;
import com.chat.chat.DTO.AiPollDraftRequestDTO;
import com.chat.chat.DTO.AiPollDraftResponseDTO;
import com.chat.chat.Exceptions.RecursoNoEncontradoException;
import com.chat.chat.Exceptions.SemanticApiException;
import com.chat.chat.Repository.ChatGrupalRepository;
import com.chat.chat.Utils.Constantes;
import com.chat.chat.Utils.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

@Service
@Validated
public class DeepSeekAiPollDraftServiceImpl implements AiPollDraftService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DeepSeekAiPollDraftServiceImpl.class);
    private static final Pattern OPTION_PREFIX_PATTERN = Pattern.compile("^(?:OPCION\\s*:\\s*|[-*•]+\\s*|\\d+[.)-]?\\s*)", Pattern.CASE_INSENSITIVE);
    private static final Pattern QUESTION_PREFIX_PATTERN = Pattern.compile("^PREGUNTA\\s*:\\s*", Pattern.CASE_INSENSITIVE);
    private static final Pattern MULTIPLE_PREFIX_PATTERN = Pattern.compile("^MULTIPLE_RESPUESTAS\\s*:\\s*", Pattern.CASE_INSENSITIVE);
    private static final int MAX_MESSAGE_LENGTH = 200;
    private static final int MAX_AUTHOR_LENGTH = 80;
    private static final int MAX_DATE_LENGTH = 40;
    private static final int MAX_QUESTION_LENGTH = 300;
    private static final int MAX_OPTION_LENGTH = 120;

    private final AiProperties aiProperties;
    private final DeepSeekProperties deepSeekProperties;
    private final DeepSeekApiClient deepSeekApiClient;
    private final AiRateLimitService aiRateLimitService;
    private final SecurityUtils securityUtils;
    private final ChatGrupalRepository chatGrupalRepository;

    public DeepSeekAiPollDraftServiceImpl(AiProperties aiProperties,
                                          DeepSeekProperties deepSeekProperties,
                                          DeepSeekApiClient deepSeekApiClient,
                                          AiRateLimitService aiRateLimitService,
                                          SecurityUtils securityUtils,
                                          ChatGrupalRepository chatGrupalRepository) {
        this.aiProperties = aiProperties;
        this.deepSeekProperties = deepSeekProperties;
        this.deepSeekApiClient = deepSeekApiClient;
        this.aiRateLimitService = aiRateLimitService;
        this.securityUtils = securityUtils;
        this.chatGrupalRepository = chatGrupalRepository;
    }

    @Override
    public AiPollDraftResponseDTO generarBorrador(AiPollDraftRequestDTO request) {
        Long userId = securityUtils.getAuthenticatedUserId();

        if (!aiProperties.isEnabled()) {
            return failure("AI_DISABLED", "La ayuda de IA no esta habilitada.");
        }
        if (!"deepseek".equalsIgnoreCase(aiProperties.getProvider())) {
            return failure("AI_PROVIDER_NOT_SUPPORTED", "El proveedor de IA configurado no es compatible.");
        }
        if (!hasText(deepSeekProperties.getApiKey())) {
            return failure("AI_API_KEY_MISSING", "La API Key de DeepSeek no esta configurada.");
        }

        try {
            validateChatAccess(request, userId);
        } catch (IllegalArgumentException ex) {
            return failure("AI_POLL_DRAFT_INVALID_REQUEST", ex.getMessage());
        } catch (RecursoNoEncontradoException ex) {
            return failure("AI_POLL_DRAFT_CHAT_NOT_FOUND", ex.getMessage());
        }

        int maxOptions = resolveMaxOptions(request == null ? null : request.getMaxOpciones());
        List<AiPollDraftContextMessageDTO> sanitizedMessages = sanitizeMessages(request == null ? null : request.getMensajes());
        if (sanitizedMessages.isEmpty()) {
            return failure("AI_POLL_DRAFT_EMPTY_CONTEXT", "No hay mensajes suficientes para generar una encuesta.");
        }

        String userContent = buildConversationContext(sanitizedMessages, request == null ? null : request.getEstilo(), maxOptions);
        if (!hasText(userContent)) {
            return failure("AI_POLL_DRAFT_EMPTY_CONTEXT", "No hay mensajes suficientes para generar una encuesta.");
        }
        if (userContent.length() > aiProperties.getPollDraft().getMaxInputLength()) {
            return failure("AI_POLL_DRAFT_TOO_LONG", "El contexto enviado es demasiado largo para generar una encuesta.");
        }

        AiRateLimitCheck rateLimitCheck = aiRateLimitService.checkUsage(userId);
        if (!rateLimitCheck.isAllowed()) {
            return failure(rateLimitCheck.getCode(), rateLimitCheck.getMessage());
        }

        try {
            LOGGER.info("[AI][POLL_DRAFT] request userId={} chatGrupalId={} mensajes={} contextLength={} maxOpciones={}",
                    userId,
                    request.getChatGrupalId(),
                    sanitizedMessages.size(),
                    userContent.length(),
                    maxOptions);
            String rawOutput = deepSeekApiClient.completarTexto(
                    buildSystemPrompt(),
                    userContent,
                    deepSeekProperties.getPollDraftMaxOutputTokens()
            );
            aiRateLimitService.registrarUso(userId);
            PollDraftResult result = parseDraft(rawOutput, maxOptions);
            if (!result.valid()) {
                return failure(result.code(), result.message());
            }
            return success(result.question(), result.options(), result.multipleResponses());
        } catch (SemanticApiException ex) {
            LOGGER.warn("[AI][POLL_DRAFT] provider-error userId={} chatGrupalId={} code={} status={}",
                    userId,
                    request.getChatGrupalId(),
                    ex.getCode(),
                    ex.getStatus().value());
            return failure(ex.getCode(), ex.getMessage());
        }
    }

    private void validateChatAccess(AiPollDraftRequestDTO request, Long userId) {
        if (request == null || request.getChatGrupalId() == null) {
            throw new IllegalArgumentException("chatGrupalId es obligatorio");
        }
        Long chatGrupalId = request.getChatGrupalId();
        if (!chatGrupalRepository.existsActiveMemberByChatIdAndUserId(chatGrupalId, userId)) {
            if (chatGrupalRepository.existsById(chatGrupalId)) {
                throw new IllegalArgumentException(Constantes.MSG_NO_PERTENECE_GRUPO);
            }
            throw new RecursoNoEncontradoException(Constantes.MSG_CHAT_GRUPAL_NO_ENCONTRADO_ID + chatGrupalId);
        }
    }

    private List<AiPollDraftContextMessageDTO> sanitizeMessages(List<AiPollDraftContextMessageDTO> messages) {
        if (messages == null || messages.isEmpty()) {
            return List.of();
        }
        int maxMessages = Math.max(1, aiProperties.getPollDraft().getMaxMessages());
        int start = Math.max(0, messages.size() - maxMessages);
        int maxInputLength = aiProperties.getPollDraft().getMaxInputLength();
        int totalLength = 0;
        List<AiPollDraftContextMessageDTO> sanitized = new ArrayList<>();
        for (int i = start; i < messages.size(); i++) {
            AiPollDraftContextMessageDTO message = messages.get(i);
            if (message == null) {
                continue;
            }
            String content = normalizeInput(message.getContenido());
            if (!hasText(content) || isNonTextContent(content)) {
                continue;
            }
            content = truncate(content, MAX_MESSAGE_LENGTH);
            String line = buildContextLine(
                    defaultAuthor(message.getAutor(), message.isEsUsuarioActual()),
                    truncate(normalizeInput(message.getFecha()), MAX_DATE_LENGTH),
                    content,
                    message.isEsUsuarioActual()
            );
            if (!hasText(line)) {
                continue;
            }
            if (totalLength + line.length() + 1 > maxInputLength) {
                break;
            }
            AiPollDraftContextMessageDTO current = new AiPollDraftContextMessageDTO();
            current.setId(message.getId());
            current.setAutor(defaultAuthor(message.getAutor(), message.isEsUsuarioActual()));
            current.setContenido(content);
            current.setEsUsuarioActual(message.isEsUsuarioActual());
            current.setFecha(truncate(normalizeInput(message.getFecha()), MAX_DATE_LENGTH));
            sanitized.add(current);
            totalLength += line.length() + 1;
        }
        return sanitized;
    }

    private String buildConversationContext(List<AiPollDraftContextMessageDTO> messages, String estilo, int maxOptions) {
        StringBuilder builder = new StringBuilder("Mensajes recientes del chat grupal:\n");
        for (AiPollDraftContextMessageDTO message : messages) {
            String line = buildContextLine(message.getAutor(), message.getFecha(), message.getContenido(), message.isEsUsuarioActual());
            if (!hasText(line)) {
                continue;
            }
            builder.append(line).append('\n');
        }
        builder.append("Parametros:\n");
        builder.append("- maxOpciones: ").append(maxOptions).append('\n');
        if (hasText(estilo)) {
            builder.append("- estilo: ").append(truncate(normalizeInput(estilo), 20)).append('\n');
        }
        builder.append("- Genera solo un borrador para formulario.\n");
        builder.append("- No crees encuesta real.\n");
        builder.append("- No envies mensajes.\n");
        builder.append("- No inventes datos importantes.");
        return builder.toString();
    }

    private String buildContextLine(String author, String date, String content, boolean currentUser) {
        if (!hasText(content)) {
            return null;
        }
        StringBuilder line = new StringBuilder();
        if (hasText(date)) {
            line.append('[').append(date).append("] ");
        }
        line.append(currentUser ? "[YO] " : "[OTRO] ");
        line.append(author).append(": ").append(content);
        return line.toString();
    }

    private String buildSystemPrompt() {
        return "Analiza los ultimos mensajes de este chat grupal y genera un borrador de encuesta util basada en el tema principal de la conversacion. "
                + "La encuesta debe ser clara, breve y natural. Devuelve una pregunta y entre 2 y 4 opciones de respuesta, salvo que se indique otro maximo. "
                + "No inventes datos importantes. No anadas explicaciones. Si no hay contexto suficiente, genera una encuesta generica para ayudar al grupo a tomar una decision. "
                + "Devuelve exactamente este formato:\n\n"
                + "PREGUNTA: <pregunta de la encuesta>\n"
                + "OPCION: <opcion 1>\n"
                + "OPCION: <opcion 2>\n"
                + "OPCION: <opcion 3 opcional>\n"
                + "OPCION: <opcion 4 opcional>\n"
                + "MULTIPLE_RESPUESTAS: false";
    }

    private PollDraftResult parseDraft(String rawOutput, int maxOptions) {
        if (!hasText(rawOutput)) {
            return PollDraftResult.failure("AI_POLL_DRAFT_PARSE_ERROR", "No se pudo generar un borrador de encuesta valido.");
        }
        String normalized = rawOutput.replace("\r\n", "\n").replace('\r', '\n');
        String question = null;
        Boolean multipleResponses = null;
        Set<String> options = new LinkedHashSet<>();

        for (String rawLine : normalized.split("\n")) {
            String line = normalizeInput(rawLine);
            if (!hasText(line)) {
                continue;
            }
            String upper = line.toUpperCase(Locale.ROOT);
            if (upper.startsWith("PREGUNTA:")) {
                question = cleanupQuestion(QUESTION_PREFIX_PATTERN.matcher(line).replaceFirst(""));
                continue;
            }
            if (upper.startsWith("OPCION:")) {
                String option = cleanupOption(OPTION_PREFIX_PATTERN.matcher(line).replaceFirst(""));
                if (hasText(option)) {
                    options.add(option);
                }
                continue;
            }
            if (upper.startsWith("MULTIPLE_RESPUESTAS:")) {
                String value = MULTIPLE_PREFIX_PATTERN.matcher(line).replaceFirst("").trim();
                if (hasText(value)) {
                    multipleResponses = Boolean.parseBoolean(value);
                }
            }
        }

        if (!hasText(question)) {
            return PollDraftResult.failure("AI_POLL_DRAFT_INVALID_QUESTION", "No se pudo generar una pregunta valida para la encuesta.");
        }

        List<String> cleanedOptions = new ArrayList<>();
        for (String option : options) {
            if (!hasText(option)) {
                continue;
            }
            cleanedOptions.add(option);
            if (cleanedOptions.size() == maxOptions) {
                break;
            }
        }

        if (cleanedOptions.size() < 2) {
            return PollDraftResult.failure("AI_POLL_DRAFT_INVALID_OPTIONS", "No se pudieron generar opciones validas para la encuesta.");
        }

        return PollDraftResult.success(question, cleanedOptions, multipleResponses != null && multipleResponses);
    }

    private int resolveMaxOptions(Integer requestedMaxOptions) {
        int defaultOptions = aiProperties.getPollDraft().getDefaultOptions();
        int maxAllowed = aiProperties.getPollDraft().getMaxOptions();
        if (requestedMaxOptions == null) {
            return Math.min(defaultOptions, maxAllowed);
        }
        if (requestedMaxOptions < 2) {
            return Math.min(defaultOptions, maxAllowed);
        }
        return Math.min(requestedMaxOptions, maxAllowed);
    }

    private String cleanupQuestion(String value) {
        String normalized = cleanupCommon(value, MAX_QUESTION_LENGTH);
        if (!hasText(normalized)) {
            return null;
        }
        return normalized;
    }

    private String cleanupOption(String value) {
        String normalized = cleanupCommon(value, MAX_OPTION_LENGTH);
        if (!hasText(normalized)) {
            return null;
        }
        return normalized;
    }

    private String cleanupCommon(String value, int maxLength) {
        String normalized = normalizeInput(value);
        if (!hasText(normalized)) {
            return null;
        }
        normalized = normalized.replaceAll("^[\\-–—•*\\d.)\\s]+", "");
        normalized = normalized.replaceAll("^['\"`]+|['\"`]+$", "");
        normalized = normalized.replaceAll("\\s{2,}", " ").trim();
        if (!hasText(normalized)) {
            return null;
        }
        return truncate(normalized, maxLength);
    }

    private boolean isNonTextContent(String content) {
        String normalized = content.trim().toLowerCase(Locale.ROOT);
        return normalized.startsWith("data:image")
                || normalized.startsWith("data:audio")
                || normalized.startsWith("data:video")
                || normalized.startsWith("data:application")
                || normalized.equals("[audio]")
                || normalized.equals("[imagen]")
                || normalized.equals("[image]")
                || normalized.equals("[video]")
                || normalized.equals("[file]")
                || normalized.equals("[archivo]")
                || normalized.equals("[sticker]");
    }

    private String defaultAuthor(String author, boolean currentUser) {
        String normalized = normalizeInput(author);
        if (hasText(normalized)) {
            return truncate(normalized, MAX_AUTHOR_LENGTH);
        }
        return currentUser ? "Usuario actual" : "Participante";
    }

    private String normalizeInput(String text) {
        if (text == null) {
            return null;
        }
        String compact = text.replace("\r\n", "\n").replace('\r', '\n');
        compact = compact.replaceAll("[\\t\\f\\x0B]+", " ");
        compact = compact.replaceAll(" {2,}", " ");
        compact = compact.replaceAll("\\n{2,}", " ");
        return compact.trim();
    }

    private String truncate(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength).trim();
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private AiPollDraftResponseDTO success(String question, List<String> options, boolean multipleResponses) {
        AiPollDraftResponseDTO response = new AiPollDraftResponseDTO();
        response.setSuccess(true);
        response.setCodigo("OK");
        response.setMensaje("Borrador de encuesta generado correctamente");
        response.setPregunta(question);
        response.setOpciones(options);
        response.setMultipleRespuestas(multipleResponses);
        return response;
    }

    private AiPollDraftResponseDTO failure(String code, String message) {
        AiPollDraftResponseDTO response = new AiPollDraftResponseDTO();
        response.setSuccess(false);
        response.setCodigo(code);
        response.setMensaje(message);
        response.setPregunta(null);
        response.setOpciones(List.of());
        response.setMultipleRespuestas(false);
        return response;
    }

    private record PollDraftResult(boolean valid, String code, String message, String question, List<String> options,
                                   boolean multipleResponses) {
        private static PollDraftResult success(String question, List<String> options, boolean multipleResponses) {
            return new PollDraftResult(true, "OK", "OK", question, List.copyOf(options), multipleResponses);
        }

        private static PollDraftResult failure(String code, String message) {
            return new PollDraftResult(false, code, message, null, List.of(), false);
        }
    }
}
