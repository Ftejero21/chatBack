package com.chat.chat.Service.AiService;

import com.chat.chat.Configuracion.AiProperties;
import com.chat.chat.Configuracion.DeepSeekProperties;
import com.chat.chat.DTO.AiEncryptedConversationSummaryRequestDTO;
import com.chat.chat.DTO.AiEncryptedResponseDTO;
import com.chat.chat.Exceptions.RecursoNoEncontradoException;
import com.chat.chat.Exceptions.SemanticApiException;
import com.chat.chat.Repository.ChatGrupalRepository;
import com.chat.chat.Repository.ChatIndividualRepository;
import com.chat.chat.Utils.AdminAuditCrypto;
import com.chat.chat.Utils.Constantes;
import com.chat.chat.Utils.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
@Validated
public class DeepSeekAiEncryptedConversationSummaryServiceImpl implements AiEncryptedConversationSummaryService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DeepSeekAiEncryptedConversationSummaryServiceImpl.class);
    private static final int MAX_MESSAGE_CONTENT_LENGTH = 500;
    private static final int MAX_AUTHOR_LENGTH = 80;
    private static final int MAX_DATE_LENGTH = 40;

    private final AiProperties aiProperties;
    private final DeepSeekProperties deepSeekProperties;
    private final DeepSeekApiClient deepSeekApiClient;
    private final AiRateLimitService aiRateLimitService;
    private final SecurityUtils securityUtils;
    private final ChatIndividualRepository chatIndividualRepository;
    private final ChatGrupalRepository chatGrupalRepository;
    private final AdminAuditCrypto adminAuditCrypto;
    private final AiEncryptedContextService aiEncryptedContextService;

    public DeepSeekAiEncryptedConversationSummaryServiceImpl(AiProperties aiProperties,
                                                             DeepSeekProperties deepSeekProperties,
                                                             DeepSeekApiClient deepSeekApiClient,
                                                             AiRateLimitService aiRateLimitService,
                                                             SecurityUtils securityUtils,
                                                             ChatIndividualRepository chatIndividualRepository,
                                                             ChatGrupalRepository chatGrupalRepository,
                                                             AdminAuditCrypto adminAuditCrypto,
                                                             AiEncryptedContextService aiEncryptedContextService) {
        this.aiProperties = aiProperties;
        this.deepSeekProperties = deepSeekProperties;
        this.deepSeekApiClient = deepSeekApiClient;
        this.aiRateLimitService = aiRateLimitService;
        this.securityUtils = securityUtils;
        this.chatIndividualRepository = chatIndividualRepository;
        this.chatGrupalRepository = chatGrupalRepository;
        this.adminAuditCrypto = adminAuditCrypto;
        this.aiEncryptedContextService = aiEncryptedContextService;
    }

    @Override
    public AiEncryptedResponseDTO resumirConversacionCifrada(AiEncryptedConversationSummaryRequestDTO request) {
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
        if (!adminAuditCrypto.hasPrivateKeyConfigured()) {
            return failure("AI_ADMIN_PRIVATE_KEY_MISSING", "No esta configurada la clave privada de auditoria para resumir conversaciones cifradas.");
        }
        if (!adminAuditCrypto.hasMatchingPrivateKeyForAuditPublicKey()) {
            return failure("AI_ADMIN_PRIVATE_KEY_MISMATCH", "La clave privada de auditoria configurada no corresponde a la audit public key actual.");
        }

        String chatType = normalizeChatType(request == null ? null : request.getTipoChat());
        if (chatType == null) {
            return failure("AI_SUMMARY_CHAT_TYPE_INVALID", "El tipo de chat no es valido.");
        }

        try {
            validateChatAccess(request, userId, chatType);
        } catch (IllegalArgumentException ex) {
            return failure("AI_SUMMARY_INVALID_REQUEST", ex.getMessage());
        } catch (RecursoNoEncontradoException ex) {
            return failure("AI_SUMMARY_CHAT_NOT_FOUND", ex.getMessage());
        }

        List<AiPlainContextMessage> decryptedMessages = decryptAndSanitizeMessages(request == null ? null : request.getMensajes());
        if (decryptedMessages.isEmpty()) {
            return failure("AI_SUMMARY_EMPTY_CONTEXT", "No hay mensajes suficientes para resumir.");
        }

        String userContent = buildConversationContext(decryptedMessages);
        if (!hasText(userContent)) {
            return failure("AI_SUMMARY_EMPTY_CONTEXT", "No hay mensajes suficientes para resumir.");
        }
        if (userContent.length() > aiProperties.getMaxInputLengthSummary()) {
            return failure("AI_SUMMARY_TOO_LONG", "La conversacion enviada es demasiado larga para resumir.");
        }

        AiRateLimitCheck rateLimitCheck = aiRateLimitService.checkUsage(userId);
        if (!rateLimitCheck.isAllowed()) {
            return failure(rateLimitCheck.getCode(), rateLimitCheck.getMessage());
        }

        SummaryStyle style = SummaryStyle.fromValue(request == null ? null : request.getEstilo());
        int maxLineas = resolveMaxLineas(request == null ? null : request.getMaxLineas(), style);

        try {
            LOGGER.info("[AI][SUMMARY_ENCRYPTED] request userId={} tipoChat={} mensajes={} contextLength={} estilo={} maxLineas={}",
                    userId,
                    chatType,
                    decryptedMessages.size(),
                    userContent.length(),
                    style.name(),
                    maxLineas);
            // Nota tecnica: no es E2E puro; backend y DeepSeek ven texto plano solo en memoria para procesar la IA.
            String summary = deepSeekApiClient.completarTexto(
                    buildSystemPrompt(style, maxLineas),
                    userContent,
                    deepSeekProperties.getSummaryMaxOutputTokens()
            );
            aiRateLimitService.registrarUso(userId);
            String cleanedSummary = normalizeInput(summary);
            if (!hasText(cleanedSummary)) {
                return failure("AI_EMPTY_RESPONSE", "No se pudo generar el resumen.");
            }
            return successPlaintext(cleanedSummary);
        } catch (SemanticApiException ex) {
            LOGGER.warn("[AI][SUMMARY_ENCRYPTED] provider-error userId={} tipoChat={} code={} status={}",
                    userId,
                    chatType,
                    ex.getCode(),
                    ex.getStatus().value());
            return failure(ex.getCode(), ex.getMessage());
        } catch (RuntimeException ex) {
            LOGGER.warn("[AI][SUMMARY_ENCRYPTED] runtime-error userId={} tipoChat={} errorClass={}",
                    userId,
                    chatType,
                    ex.getClass().getSimpleName());
            return failure("AI_SUMMARY_ENCRYPTED_RUNTIME_ERROR", "No se pudo completar el resumen cifrado.");
        } catch (Exception ex) {
            LOGGER.warn("[AI][SUMMARY_ENCRYPTED] unexpected-error userId={} tipoChat={} errorClass={}",
                    userId,
                    chatType,
                    ex.getClass().getSimpleName());
            return failure("AI_SUMMARY_ENCRYPTED_ERROR", "No se pudo resumir la conversacion cifrada.");
        }
    }

    private void validateChatAccess(AiEncryptedConversationSummaryRequestDTO request, Long userId, String chatType) {
        Long chatId = request == null ? null : request.getChatId();
        Long chatGrupalId = request == null ? null : request.getChatGrupalId();
        if (Constantes.CHAT_TIPO_INDIVIDUAL.equals(chatType)) {
            if (chatId == null) {
                throw new IllegalArgumentException("chatId es obligatorio para chats individuales.");
            }
            if (chatGrupalId != null) {
                throw new IllegalArgumentException("chatGrupalId no aplica a chats individuales.");
            }
            if (!chatIndividualRepository.existsMemberByChatIdAndUserId(chatId, userId)) {
                if (chatIndividualRepository.existsById(chatId)) {
                    throw new IllegalArgumentException(Constantes.MSG_NO_PERTENECE_CHAT);
                }
                throw new RecursoNoEncontradoException(Constantes.MSG_CHAT_NO_ENCONTRADO_ID + chatId);
            }
            return;
        }

        if (chatGrupalId == null) {
            throw new IllegalArgumentException("chatGrupalId es obligatorio para chats grupales.");
        }
        if (chatId != null) {
            throw new IllegalArgumentException("chatId no aplica a chats grupales.");
        }
        if (!chatGrupalRepository.existsActiveMemberByChatIdAndUserId(chatGrupalId, userId)) {
            if (chatGrupalRepository.existsById(chatGrupalId)) {
                throw new IllegalArgumentException(Constantes.MSG_NO_PERTENECE_GRUPO);
            }
            throw new RecursoNoEncontradoException(Constantes.MSG_CHAT_GRUPAL_NO_ENCONTRADO_ID + chatGrupalId);
        }
    }

    private List<AiPlainContextMessage> decryptAndSanitizeMessages(List<com.chat.chat.DTO.AiEncryptedContextMessageDTO> messages) {
        if (messages == null || messages.isEmpty()) {
            return List.of();
        }
        int maxMessages = Math.max(1, aiProperties.getMaxSummaryMessages());
        int start = Math.max(0, messages.size() - maxMessages);
        List<com.chat.chat.DTO.AiEncryptedContextMessageDTO> limitedMessages = new ArrayList<>(messages.subList(start, messages.size()));
        List<AiPlainContextMessage> decrypted = aiEncryptedContextService.decryptContextMessages(limitedMessages);
        List<AiPlainContextMessage> sanitized = new ArrayList<>();
        for (AiPlainContextMessage message : decrypted) {
            if (message == null) {
                continue;
            }
            String content = normalizeInput(message.getContenido());
            if (!hasText(content) || isNonTextContent(content)) {
                continue;
            }
            AiPlainContextMessage current = new AiPlainContextMessage();
            current.setId(message.getId());
            current.setAutorId(message.getAutorId());
            current.setAutor(defaultAuthor(message.getAutor(), message.isEsUsuarioActual()));
            current.setContenido(truncate(content, MAX_MESSAGE_CONTENT_LENGTH));
            current.setEsUsuarioActual(message.isEsUsuarioActual());
            current.setFecha(truncate(normalizeInput(message.getFecha()), MAX_DATE_LENGTH));
            sanitized.add(current);
        }
        return sanitized;
    }

    private String buildConversationContext(List<AiPlainContextMessage> messages) {
        StringBuilder builder = new StringBuilder("Conversacion reciente:\n");
        for (AiPlainContextMessage message : messages) {
            String line = buildLine(message);
            if (!hasText(line)) {
                continue;
            }
            if (builder.length() + line.length() + 1 > aiProperties.getMaxInputLengthSummary()) {
                break;
            }
            builder.append(line).append('\n');
        }
        if (builder.toString().equals("Conversacion reciente:\n")) {
            return null;
        }
        builder.append("Instrucciones:\n");
        builder.append("- Resume lo importante.\n");
        builder.append("- No inventes.\n");
        builder.append("- No copies toda la conversacion.\n");
        builder.append("- Devuelve solo el resumen final.");
        return builder.toString();
    }

    private String buildLine(AiPlainContextMessage message) {
        StringBuilder line = new StringBuilder();
        if (hasText(message.getFecha())) {
            line.append('[').append(message.getFecha()).append("] ");
        }
        line.append(truncate(message.getAutor(), MAX_AUTHOR_LENGTH));
        line.append(": ");
        line.append(message.getContenido());
        return line.toString();
    }

    private String buildSystemPrompt(SummaryStyle style, int maxLineas) {
        return "Resume los ultimos mensajes de esta conversacion de forma clara, util y natural. "
                + "Identifica los puntos importantes, decisiones, dudas pendientes y proximos pasos si existen. "
                + "No inventes informacion y no anadas datos que no aparezcan en los mensajes. "
                + "Puedes usar algun emoticono solo si encaja con el tono de la conversacion. "
                + "Anade un toque ligero de sarcasmo solo si procede, sin exagerar. "
                + "Nunca uses sarcasmo en temas sensibles, conflictos serios, salud, dinero, denuncias, amenazas o asuntos personales graves. "
                + "No hagas bromas ofensivas ni sacrifiques informacion importante por intentar ser gracioso. "
                + "El resumen debe sonar cercano, pero seguir siendo breve y util. "
                + style.instruction(maxLineas)
                + " Devuelve solo el resumen final, sin encabezados innecesarios.";
    }

    private int resolveMaxLineas(Integer requestedMaxLineas, SummaryStyle style) {
        int defaultValue = style.defaultMaxLines();
        if (requestedMaxLineas == null) {
            return defaultValue;
        }
        int maxAllowed = style.maxAllowedLines();
        if (requestedMaxLineas < 1) {
            return defaultValue;
        }
        return Math.min(requestedMaxLineas, maxAllowed);
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

    private String defaultAuthor(String author, boolean esUsuarioActual) {
        String normalized = normalizeInput(author);
        if (hasText(normalized)) {
            return truncate(normalized, MAX_AUTHOR_LENGTH);
        }
        return esUsuarioActual ? "Usuario actual" : "Participante";
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

    private AiEncryptedResponseDTO failure(String code, String message) {
        AiEncryptedResponseDTO response = new AiEncryptedResponseDTO();
        response.setSuccess(false);
        response.setCodigo(code);
        response.setMensaje(message);
        response.setResumen(null);
        response.setEncryptedPayload(null);
        return response;
    }

    private AiEncryptedResponseDTO successPlaintext(String summary) {
        AiEncryptedResponseDTO response = new AiEncryptedResponseDTO();
        response.setSuccess(true);
        response.setCodigo("OK");
        response.setMensaje("Conversacion resumida correctamente");
        response.setResumen(summary);
        response.setEncryptedPayload(null);
        return response;
    }

    private enum SummaryStyle {
        BREVE(5, 5),
        NORMAL(8, 8),
        DETALLADO(12, 12);

        private final int defaultMaxLines;
        private final int maxAllowedLines;

        SummaryStyle(int defaultMaxLines, int maxAllowedLines) {
            this.defaultMaxLines = defaultMaxLines;
            this.maxAllowedLines = maxAllowedLines;
        }

        public static SummaryStyle fromValue(String raw) {
            if (raw == null || raw.isBlank()) {
                return NORMAL;
            }
            try {
                return SummaryStyle.valueOf(raw.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ex) {
                return NORMAL;
            }
        }

        public int defaultMaxLines() {
            return defaultMaxLines;
        }

        public int maxAllowedLines() {
            return maxAllowedLines;
        }

        public String instruction(int maxLineas) {
            return switch (this) {
                case BREVE -> "Maximo " + maxLineas + " lineas.";
                case NORMAL -> "Maximo " + maxLineas + " lineas.";
                case DETALLADO -> "Puedes usar hasta " + maxLineas + " lineas, manteniendo claridad.";
            };
        }
    }
}
