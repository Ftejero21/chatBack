package com.chat.chat.Service.AiService;

import com.chat.chat.Configuracion.AiProperties;
import com.chat.chat.DTO.AudioTranscriptionResultDTO;
import com.chat.chat.DTO.AiTextContextMessageDTO;
import com.chat.chat.DTO.AiTextInternalRequestDTO;
import com.chat.chat.DTO.AiTextInternalResponseDTO;
import com.chat.chat.DTO.AiTextRequestDTO;
import com.chat.chat.DTO.AiTextResponseDTO;
import com.chat.chat.Entity.MensajeEntity;
import com.chat.chat.Entity.UsuarioEntity;
import com.chat.chat.Repository.ChatGrupalRepository;
import com.chat.chat.Repository.ChatIndividualRepository;
import com.chat.chat.Repository.MensajeRepository;
import com.chat.chat.Utils.AdminAuditCrypto;
import com.chat.chat.Utils.AiTextMode;
import com.chat.chat.Utils.Constantes;
import com.chat.chat.Utils.MessageType;
import com.chat.chat.Utils.SecurityUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Base64;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
@Validated
public class DeepSeekAiTextServiceImpl implements AiTextService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DeepSeekAiTextServiceImpl.class);
    private static final Pattern COMPLETE_MARKER_PATTERN = Pattern.compile("(?i)\\[?COMPLETAR CON IA\\]?");
    private static final String TRANSFORM_AES_GCM = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH_BITS = 128;
    private static final String AUDIO_TRANSCRIBED_PREFIX = "[Audio transcrito automaticamente]: ";
    private static final Set<String> ALLOWED_AUDIO_MIMES = Set.of("audio/webm", "audio/mpeg", "audio/mp3", "audio/wav", "audio/ogg", "audio/mp4");
    private static final int MAX_CONTEXT_MESSAGES = 50;
    private static final int MAX_STYLE_MESSAGES = 100;
    private static final int MAX_CONTEXT_CONTENT_LENGTH = 300;
    private static final int MAX_STYLE_CONTENT_LENGTH = 200;

    private final AiProperties aiProperties;
    private final AiRateLimitService aiRateLimitService;
    private final SecurityUtils securityUtils;
    private final AdminAuditCrypto adminAuditCrypto;
    private final AiEncryptedContextService aiEncryptedContextService;
    private final AiTextMicroserviceClient aiTextMicroserviceClient;
    private final MensajeRepository mensajeRepository;
    private final ChatIndividualRepository chatIndividualRepository;
    private final ChatGrupalRepository chatGrupalRepository;
    private final AudioTranscriptionService audioTranscriptionService;
    private final ObjectMapper objectMapper;
    private final String uploadsRoot;

    public DeepSeekAiTextServiceImpl(AiProperties aiProperties,
                                     AiRateLimitService aiRateLimitService,
                                     SecurityUtils securityUtils,
                                     AdminAuditCrypto adminAuditCrypto,
                                     AiEncryptedContextService aiEncryptedContextService,
                                     AiTextMicroserviceClient aiTextMicroserviceClient,
                                     MensajeRepository mensajeRepository,
                                     ChatIndividualRepository chatIndividualRepository,
                                     ChatGrupalRepository chatGrupalRepository,
                                     AudioTranscriptionService audioTranscriptionService,
                                     ObjectMapper objectMapper,
                                     @Value(Constantes.PROP_UPLOADS_ROOT) String uploadsRoot) {
        this.aiProperties = aiProperties;
        this.aiRateLimitService = aiRateLimitService;
        this.securityUtils = securityUtils;
        this.adminAuditCrypto = adminAuditCrypto;
        this.aiEncryptedContextService = aiEncryptedContextService;
        this.aiTextMicroserviceClient = aiTextMicroserviceClient;
        this.mensajeRepository = mensajeRepository;
        this.chatIndividualRepository = chatIndividualRepository;
        this.chatGrupalRepository = chatGrupalRepository;
        this.audioTranscriptionService = audioTranscriptionService;
        this.objectMapper = objectMapper;
        this.uploadsRoot = uploadsRoot;
    }

    @Override
    public AiTextResponseDTO procesarTexto(AiTextRequestDTO request) {
        String requestId = UUID.randomUUID().toString();
        String encryptedPayload = request == null ? null : request.getEncryptedPayload();
        String plainText = request == null ? null : request.getTexto();
        String originalText = null;
        String targetLanguage = request == null ? null : request.getIdiomaDestino();
        Long userId = securityUtils.getAuthenticatedUserId();
        boolean encryptedPayloadRecibido = hasText(encryptedPayload);
        boolean textoDescifrado = false;
        boolean audioDetectado = isAudioRequest(request);
        boolean audioTranscrito = false;
        Long messageId = request == null ? null : request.getMessageId();
        String tipoMensaje = normalizeInput(request == null ? null : request.getTipoMensaje());

        if (!aiProperties.isEnabled()) {
            return failure(null, null, "AI_DISABLED", "La ayuda de IA no esta habilitada.");
        }
        if (!"deepseek".equalsIgnoreCase(aiProperties.getProvider())) {
            return failure(null, null, "AI_PROVIDER_NOT_SUPPORTED", "El proveedor de IA configurado no es compatible.");
        }

        AiTextMode requestedMode;
        try {
            requestedMode = AiTextMode.fromValue(request == null ? null : request.getModo());
        } catch (IllegalArgumentException ex) {
            return failure(originalText, null, "AI_MODE_INVALID", "El modo de IA no es valido.");
        }

        if (audioDetectado) {
            if (requestedMode != AiTextMode.RESPONDER) {
                return failure(null, requestedMode == null ? null : requestedMode.name(), "AI_TEXT_AUDIO_MODE_INVALID", "El audio solo se admite con el modo RESPONDER.");
            }
            AudioResolutionResult audioResolution = resolveAudioOriginalText(request, userId);
            if (!audioResolution.success()) {
                return failure(null, requestedMode.name(), audioResolution.code(), audioResolution.message());
            }
            originalText = audioResolution.textoOriginal();
            textoDescifrado = true;
            audioTranscrito = true;
        } else if (encryptedPayloadRecibido) {
            if (!adminAuditCrypto.hasPrivateKeyConfigured()) {
                return failure(null, null, "AI_ADMIN_PRIVATE_KEY_MISSING", "No esta configurada la clave privada de auditoria para procesar texto cifrado.");
            }
            if (!adminAuditCrypto.hasMatchingPrivateKeyForAuditPublicKey()) {
                return failure(null, null, "AI_ADMIN_PRIVATE_KEY_MISMATCH", "La clave privada de auditoria configurada no corresponde a la audit public key actual.");
            }
            originalText = aiEncryptedContextService.decryptMessagePayload(encryptedPayload);
            textoDescifrado = hasText(originalText);
            if (!textoDescifrado) {
                return failure(null, null, "AI_TEXT_ENCRYPTED_PAYLOAD_INVALID", "No se pudo descifrar encryptedPayload con forAdmin.");
            }
        } else {
            originalText = plainText;
        }

        String normalizedOriginal = normalizeInput(originalText);
        if (!hasText(normalizedOriginal)) {
            return failure(originalText, null, encryptedPayloadRecibido ? "AI_TEXT_ENCRYPTED_PAYLOAD_REQUIRED" : "AI_TEXT_EMPTY",
                    encryptedPayloadRecibido ? "encryptedPayload es obligatorio." : "El texto es obligatorio.");
        }

        AiTextMode effectiveMode = resolveMode(requestedMode, normalizedOriginal);
        String normalizedTargetLanguage = normalizeInput(targetLanguage);
        if (effectiveMode == AiTextMode.TRADUCIR && !hasText(normalizedTargetLanguage)) {
            return failure(originalText, effectiveMode.name(), "AI_TARGET_LANGUAGE_REQUIRED", "Debes seleccionar un idioma de destino para traducir.");
        }

        int maxLength = resolveMaxInputLength(effectiveMode);
        logLengthValidation(effectiveMode, normalizedOriginal.length(), maxLength);
        if (normalizedOriginal.length() > maxLength) {
            return failure(originalText, effectiveMode.name(), "AI_TEXT_TOO_LONG", buildLengthExceededMessage(effectiveMode, normalizedOriginal.length(), maxLength));
        }

        AiRateLimitCheck rateLimitCheck = aiRateLimitService.checkUsage(userId);
        if (!rateLimitCheck.isAllowed()) {
            return failure(originalText, effectiveMode.name(), rateLimitCheck.getCode(), rateLimitCheck.getMessage());
        }

        String cleanedUserText = cleanupForMode(normalizedOriginal);
        if (!hasText(cleanedUserText)) {
            return failure(originalText, effectiveMode.name(), "AI_TEXT_EMPTY", "El texto es obligatorio.");
        }
        logLengthValidation(effectiveMode, cleanedUserText.length(), maxLength);
        if (cleanedUserText.length() > maxLength) {
            return failure(originalText, effectiveMode.name(), "AI_TEXT_TOO_LONG", buildLengthExceededMessage(effectiveMode, cleanedUserText.length(), maxLength));
        }

        // Carga de contexto para modo RESPONDER
        List<AiTextContextMessageDTO> contextoConversacion = List.of();
        List<String> ejemplosEstiloUsuario = List.of();
        if (effectiveMode == AiTextMode.RESPONDER) {
            ChatContextInfo chatContext = resolveChatContextInfo(request, userId);
            if (chatContext != null) {
                contextoConversacion = loadConversacionContext(chatContext, userId);
            }
            ejemplosEstiloUsuario = loadUserStyleExamples(userId);
        }
        int totalContextoConversacion = contextoConversacion.size();
        int totalEjemplosEstiloUsuario = ejemplosEstiloUsuario.size();

        try {
            LOGGER.info("[AI][TEXT] requestId={} modo={} tipoMensaje={} messageId={} audioDetectado={} audioTranscrito={} totalContextoConversacion={} totalEjemplosEstiloUsuario={} llamadaMicroservicioIA=true respuestaCifrada=false",
                    requestId, effectiveMode.name(), tipoMensaje, messageId, audioDetectado, audioTranscrito,
                    totalContextoConversacion, totalEjemplosEstiloUsuario);
            AiTextInternalResponseDTO internalResponse = aiTextMicroserviceClient.procesarTexto(
                    requestId,
                    buildInternalRequest(requestId, cleanedUserText, effectiveMode, normalizedTargetLanguage, contextoConversacion, ejemplosEstiloUsuario)
            );

            if (!isValidInternalResponse(internalResponse)) {
                LOGGER.warn("[AI][TEXT] requestId={} modo={} tipoMensaje={} messageId={} audioDetectado={} audioTranscrito={} llamadaMicroservicioIA=true respuestaCifrada=false invalid-service-response=true",
                        requestId, effectiveMode.name(), tipoMensaje, messageId, audioDetectado, audioTranscrito);
                return failure(originalText, effectiveMode.name(), "AI_TEXT_INVALID_SERVICE_RESPONSE", "La respuesta interna del servicio de IA no es valida.");
            }
            if (!internalResponse.isSuccess()) {
                LOGGER.warn("[AI][TEXT] requestId={} modo={} tipoMensaje={} messageId={} audioDetectado={} audioTranscrito={} llamadaMicroservicioIA=true respuestaCifrada=false service-error-code={}",
                        requestId, effectiveMode.name(), tipoMensaje, messageId, audioDetectado, audioTranscrito, internalResponse.getCodigo());
                return failure(originalText, effectiveMode.name(), "AI_TEXT_SERVICE_ERROR", resolveServiceErrorMessage(internalResponse));
            }

            aiRateLimitService.registrarUso(userId);
            AiTextResponseDTO response = successEncrypted(userId, originalText, internalResponse.getTextoGenerado(), effectiveMode.name());
            LOGGER.info("[AI][TEXT] requestId={} modo={} tipoMensaje={} messageId={} audioDetectado={} audioTranscrito={} totalContextoConversacion={} totalEjemplosEstiloUsuario={} llamadaMicroservicioIA=true respuestaCifrada={}",
                    requestId, effectiveMode.name(), tipoMensaje, messageId, audioDetectado, audioTranscrito,
                    totalContextoConversacion, totalEjemplosEstiloUsuario, hasText(response.getEncryptedPayload()));
            return response;
        } catch (AiTextMicroserviceUnavailableException ex) {
            LOGGER.warn("[AI][TEXT] requestId={} modo={} tipoMensaje={} messageId={} audioDetectado={} audioTranscrito={} llamadaMicroservicioIA=true respuestaCifrada=false service-unavailable=true errorClass={}",
                    requestId, effectiveMode.name(), tipoMensaje, messageId, audioDetectado, audioTranscrito, ex.getClass().getSimpleName());
            return failure(originalText, effectiveMode.name(), "AI_TEXT_SERVICE_UNAVAILABLE", "El microservicio de texto IA no esta disponible temporalmente.");
        } catch (AiTextMicroserviceException ex) {
            LOGGER.warn("[AI][TEXT] requestId={} modo={} tipoMensaje={} messageId={} audioDetectado={} audioTranscrito={} llamadaMicroservicioIA=true respuestaCifrada=false service-error=true errorClass={}",
                    requestId, effectiveMode.name(), tipoMensaje, messageId, audioDetectado, audioTranscrito, ex.getClass().getSimpleName());
            return failure(originalText, effectiveMode.name(), "AI_TEXT_SERVICE_ERROR", "El microservicio de texto IA devolvio un error.");
        }
    }

    private AiTextInternalRequestDTO buildInternalRequest(String requestId,
                                                          String texto,
                                                          AiTextMode effectiveMode,
                                                          String normalizedTargetLanguage,
                                                          List<AiTextContextMessageDTO> contextoConversacion,
                                                          List<String> ejemplosEstiloUsuario) {
        AiTextInternalRequestDTO internalRequest = new AiTextInternalRequestDTO();
        internalRequest.setRequestId(requestId);
        internalRequest.setTexto(texto);
        internalRequest.setModo(effectiveMode.name());
        if (effectiveMode == AiTextMode.RESPONDER) {
            internalRequest.setMensajeRecibido(texto);
            if (!contextoConversacion.isEmpty()) {
                internalRequest.setContextoConversacion(contextoConversacion);
            }
            if (!ejemplosEstiloUsuario.isEmpty()) {
                internalRequest.setEjemplosEstiloUsuario(ejemplosEstiloUsuario);
            }
        }
        if (hasText(normalizedTargetLanguage)) {
            Map<String, Object> metadata = new LinkedHashMap<>();
            metadata.put("idiomaDestino", normalizedTargetLanguage);
            internalRequest.setMetadata(metadata);
        }
        return internalRequest;
    }

    private ChatContextInfo resolveChatContextInfo(AiTextRequestDTO request, Long userId) {
        if (request == null || userId == null) {
            return null;
        }
        Long chatId = request.getChatId();
        Long chatGrupalId = request.getChatGrupalId();
        Long msgId = request.getMessageId();

        if (chatId != null && chatIndividualRepository.existsMemberByChatIdAndUserId(chatId, userId)) {
            return new ChatContextInfo(chatId, false);
        }
        if (chatGrupalId != null && chatGrupalRepository.existsActiveMemberByChatIdAndUserId(chatGrupalId, userId)) {
            return new ChatContextInfo(chatGrupalId, true);
        }
        if (msgId != null) {
            Long msgChatId = mensajeRepository.findChatIdByMessageId(msgId).orElse(null);
            if (msgChatId != null) {
                if (chatIndividualRepository.existsById(msgChatId)
                        && chatIndividualRepository.existsMemberByChatIdAndUserId(msgChatId, userId)) {
                    return new ChatContextInfo(msgChatId, false);
                }
                if (chatGrupalRepository.existsById(msgChatId)
                        && chatGrupalRepository.existsActiveMemberByChatIdAndUserId(msgChatId, userId)) {
                    return new ChatContextInfo(msgChatId, true);
                }
            }
        }
        return null;
    }

    private List<AiTextContextMessageDTO> loadConversacionContext(ChatContextInfo chatContext, Long userId) {
        PageRequest page = PageRequest.of(0, MAX_CONTEXT_MESSAGES);
        List<MensajeEntity> mensajes = chatContext.esGrupal()
                ? mensajeRepository.findLatestVisibleMessagesByChatGrupalId(chatContext.chatId(), page)
                : mensajeRepository.findLatestVisibleMessagesByChatIndividualId(chatContext.chatId(), page);

        List<MensajeEntity> cronologico = new ArrayList<>(mensajes);
        Collections.reverse(cronologico);

        List<AiTextContextMessageDTO> context = new ArrayList<>();
        for (MensajeEntity m : cronologico) {
            AiTextContextMessageDTO dto = toContextMessageDTO(m, userId);
            if (dto != null) {
                context.add(dto);
            }
        }
        return context;
    }

    private AiTextContextMessageDTO toContextMessageDTO(MensajeEntity m, Long userId) {
        if (m == null) return null;
        Long autorId = m.getEmisor() == null ? null : m.getEmisor().getId();
        String tipoMensaje = resolveContextTipoMensaje(m);
        String contenido = resolveContextContenido(m, tipoMensaje);
        if (!hasText(contenido)) return null;

        AiTextContextMessageDTO dto = new AiTextContextMessageDTO();
        dto.setAutor(buildDisplayName(m.getEmisor()));
        dto.setAutorId(autorId);
        dto.setEsUsuarioActual(userId != null && userId.equals(autorId));
        dto.setTipoMensaje(tipoMensaje);
        dto.setContenido(truncate(contenido, MAX_CONTEXT_CONTENT_LENGTH));
        dto.setFechaEnvio(m.getFechaEnvio() != null
                ? m.getFechaEnvio().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null);
        return dto;
    }

    private String resolveContextTipoMensaje(MensajeEntity m) {
        if (m.getStickerId() != null) return "STICKER";
        if (hasText(m.getMediaMime())) {
            String mime = m.getMediaMime().trim().toLowerCase(Locale.ROOT);
            if (mime.startsWith("audio/")) return "AUDIO";
            if (mime.startsWith("image/")) return "IMAGE";
            if (mime.startsWith("application/")) return "FILE";
        }
        if (m.getTipo() != null) {
            return switch (m.getTipo()) {
                case TEXT -> "TEXT";
                case AUDIO -> "AUDIO";
                case IMAGE -> "IMAGE";
                case STICKER -> "STICKER";
                case FILE -> "FILE";
                default -> "TEXT";
            };
        }
        return "TEXT";
    }

    private String resolveContextContenido(MensajeEntity m, String tipoMensaje) {
        if ("TEXT".equals(tipoMensaje)) {
            String decrypted = normalizeInput(aiEncryptedContextService.decryptMessagePayload(m.getContenido()));
            if (!hasText(decrypted) || isContextNonText(decrypted)) return null;
            return decrypted;
        }
        return switch (tipoMensaje) {
            case "AUDIO" -> "[Audio]";
            case "IMAGE" -> "[Imagen]";
            case "STICKER" -> "[Sticker]";
            case "FILE" -> "[Archivo]";
            default -> null;
        };
    }

    private boolean isContextNonText(String content) {
        String normalized = content.trim().toLowerCase(Locale.ROOT);
        return normalized.startsWith("data:image")
                || normalized.startsWith("data:audio")
                || normalized.startsWith("data:video");
    }

    private List<String> loadUserStyleExamples(Long userId) {
        if (userId == null) return List.of();
        List<MensajeEntity> mensajes = mensajeRepository.findMensajesEnviadosPorUsuarioParaAi(
                userId, MessageType.TEXT, null, null, PageRequest.of(0, MAX_STYLE_MESSAGES)
        ).getContent();

        List<String> examples = new ArrayList<>();
        for (MensajeEntity m : mensajes) {
            String decrypted = normalizeInput(aiEncryptedContextService.decryptMessagePayload(m.getContenido()));
            if (hasText(decrypted) && !isContextNonText(decrypted)) {
                examples.add(truncate(decrypted, MAX_STYLE_CONTENT_LENGTH));
            }
        }
        return examples;
    }

    private String buildDisplayName(UsuarioEntity user) {
        if (user == null) return null;
        String nombre = normalizeInput(user.getNombre());
        String apellido = normalizeInput(user.getApellido());
        String full = ((nombre != null ? nombre : "") + " " + (apellido != null ? apellido : "")).trim();
        return hasText(full) ? truncate(full, 80) : truncate(normalizeInput(user.getEmail()), 80);
    }

    private String truncate(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) return text;
        return text.substring(0, maxLength).trim();
    }

    private AudioResolutionResult resolveAudioOriginalText(AiTextRequestDTO request, Long userId) {
        if (!adminAuditCrypto.hasPrivateKeyConfigured()) {
            return AudioResolutionResult.failure("AI_ADMIN_PRIVATE_KEY_MISSING", "No esta configurada la clave privada de auditoria para procesar audio.");
        }
        if (!adminAuditCrypto.hasMatchingPrivateKeyForAuditPublicKey()) {
            return AudioResolutionResult.failure("AI_ADMIN_PRIVATE_KEY_MISMATCH", "La clave privada de auditoria configurada no corresponde a la audit public key actual.");
        }

        MensajeEntity mensaje = resolveAuthorizedAudioMessage(request, userId);
        if (mensaje == null) {
            return AudioResolutionResult.failure("AI_AUDIO_NOT_FOUND", "No se ha encontrado el mensaje de audio o no tienes acceso.");
        }

        Optional<AudioPayloadDTO> maybeAudioPayload = resolveAudioPayload(request, mensaje);
        if (maybeAudioPayload.isEmpty()) {
            return AudioResolutionResult.failure("AI_AUDIO_NOT_FOUND", "No se pudo resolver el audio para transcribir.");
        }

        AudioPayloadDTO audioPayload = maybeAudioPayload.get();
        if (!isAllowedAudioMime(audioPayload.mimeType())) {
            return AudioResolutionResult.failure("AI_AUDIO_INVALID_MIME", "El formato de audio no es compatible con la transcripcion.");
        }
        if (audioPayload.bytes().length > aiProperties.getAudioTranscription().getMaxAudioSizeBytes()) {
            return AudioResolutionResult.failure("AI_AUDIO_TOO_LARGE", "El audio supera el tamano maximo permitido para transcripcion.");
        }

        AudioTranscriptionResultDTO transcription = audioTranscriptionService.transcribirAudio(mensaje, audioPayload.bytes(), audioPayload.mimeType());
        if (transcription == null || !transcription.isSuccess() || !hasText(transcription.getTranscripcion())) {
            return AudioResolutionResult.failure("AI_AUDIO_TRANSCRIPTION_ERROR", "No se pudo transcribir el audio para generar la respuesta.");
        }
        return AudioResolutionResult.success(AUDIO_TRANSCRIBED_PREFIX + transcription.getTranscripcion());
    }

    private MensajeEntity resolveAuthorizedAudioMessage(AiTextRequestDTO request, Long userId) {
        Long messageId = request == null ? null : request.getMessageId();
        if (messageId == null) {
            return null;
        }
        Long chatId = mensajeRepository.findChatIdByMessageId(messageId).orElse(null);
        if (chatId == null) {
            return null;
        }
        boolean autorizado = false;
        if (chatIndividualRepository.existsById(chatId)) {
            autorizado = chatIndividualRepository.existsMemberByChatIdAndUserId(chatId, userId);
        }
        if (!autorizado && chatGrupalRepository.existsById(chatId)) {
            autorizado = chatGrupalRepository.existsActiveMemberByChatIdAndUserId(chatId, userId);
        }
        if (!autorizado) {
            return null;
        }
        return mensajeRepository.findById(messageId).orElse(null);
    }

    private Optional<AudioPayloadDTO> resolveAudioPayload(AiTextRequestDTO request, MensajeEntity mensaje) {
        Optional<AudioPayloadDTO> fromDb = resolveAudioFromDbMessage(mensaje);
        if (fromDb.isPresent() && looksLikeAudioBinary(fromDb.get().bytes(), fromDb.get().mimeType())) {
            return fromDb;
        }

        String audioEncryptedPayload = normalizeInput(request == null ? null : request.getAudioEncryptedPayload());
        if (hasText(audioEncryptedPayload)) {
            byte[] decrypted = aiEncryptedContextService.decryptMessagePayloadToBytes(audioEncryptedPayload);
            String mime = normalizeAudioMime(request == null ? null : request.getMimeType(), mensaje.getMediaMime());
            if (decrypted != null && decrypted.length > 0 && looksLikeAudioBinary(decrypted, mime)) {
                return Optional.of(new AudioPayloadDTO(decrypted, mime));
            }
        }

        String requestedAudioUrl = normalizeInput(request == null ? null : request.getAudioUrl());
        if (hasText(requestedAudioUrl)
                && hasText(mensaje.getMediaUrl())
                && requestedAudioUrl.equals(mensaje.getMediaUrl().trim())) {
            byte[] bytes = readAudioFromMediaUrl(mensaje.getMediaUrl());
            String mime = normalizeAudioMime(request == null ? null : request.getMimeType(), mensaje.getMediaMime());
            if (bytes != null && bytes.length > 0 && looksLikeAudioBinary(bytes, mime)) {
                return Optional.of(new AudioPayloadDTO(bytes, mime));
            }
        }

        return Optional.empty();
    }

    private boolean isValidInternalResponse(AiTextInternalResponseDTO response) {
        return response != null
                && response.getCodigo() != null
                && response.getMensaje() != null
                && (!response.isSuccess() || hasText(response.getTextoGenerado()));
    }

    private String resolveServiceErrorMessage(AiTextInternalResponseDTO response) {
        if (response == null || !hasText(response.getMensaje())) {
            return "El microservicio de texto IA devolvio un error.";
        }
        return response.getMensaje();
    }

    private Optional<AudioPayloadDTO> resolveAudioFromDbMessage(MensajeEntity mensaje) {
        if (mensaje == null) return Optional.empty();
        if (hasText(mensaje.getMediaUrl())) {
            byte[] bytes = readAudioFromMediaUrl(mensaje.getMediaUrl());
            if (bytes != null && bytes.length > 0) {
                String mime = normalizeAudioMime(mensaje.getMediaMime(), readContentAudioMime(mensaje.getContenido()));
                if (looksLikeAudioBinary(bytes, mime)) return Optional.of(new AudioPayloadDTO(bytes, mime));
                byte[] decrypted = decryptAudioFilePayload(bytes, mensaje.getContenido());
                if (decrypted != null && decrypted.length > 0) return Optional.of(new AudioPayloadDTO(decrypted, mime));
            }
        }
        String contentAudioUrl = readContentAudioUrl(mensaje.getContenido());
        if (hasText(contentAudioUrl)) {
            byte[] bytes = readAudioFromMediaUrl(contentAudioUrl);
            if (bytes != null && bytes.length > 0) {
                String mime = normalizeAudioMime(readContentAudioMime(mensaje.getContenido()), mensaje.getMediaMime());
                if (looksLikeAudioBinary(bytes, mime)) return Optional.of(new AudioPayloadDTO(bytes, mime));
                byte[] decrypted = decryptAudioFilePayload(bytes, mensaje.getContenido());
                if (decrypted != null && decrypted.length > 0) return Optional.of(new AudioPayloadDTO(decrypted, mime));
            }
        }
        String contenido = normalizeInput(mensaje.getContenido());
        if (hasText(contenido)) {
            byte[] decrypted = aiEncryptedContextService.decryptMessagePayloadToBytes(contenido);
            if (decrypted != null && decrypted.length > 0) {
                return Optional.of(new AudioPayloadDTO(decrypted, normalizeAudioMime(mensaje.getMediaMime(), null)));
            }
        }
        return Optional.empty();
    }

    private byte[] readAudioFromMediaUrl(String mediaUrl) {
        if (!hasText(mediaUrl) || !mediaUrl.startsWith(Constantes.UPLOADS_PREFIX)) return null;
        try {
            String relative = mediaUrl.substring(Constantes.UPLOADS_PREFIX.length());
            Path root = Paths.get(uploadsRoot).toAbsolutePath().normalize();
            Path file = root.resolve(relative).normalize();
            if (!file.startsWith(root) || !Files.exists(file) || !Files.isRegularFile(file)) return null;
            return Files.readAllBytes(file);
        } catch (Exception ex) {
            return null;
        }
    }

    private String readContentAudioUrl(String contenido) {
        return readContentTextField(contenido, "audioUrl");
    }

    private String readContentAudioMime(String contenido) {
        return readContentTextField(contenido, "audioMime");
    }

    private String readContentIvFile(String contenido) {
        return readContentTextField(contenido, "ivFile");
    }

    private String readContentAdminEnvelope(String contenido) {
        return readContentTextField(contenido, "forAdmin");
    }

    private String readContentTextField(String contenido, String fieldName) {
        if (!hasText(contenido) || !hasText(fieldName)) return null;
        try {
            JsonNode root = objectMapper.readTree(contenido);
            String value = normalizeInput(root.path(fieldName).asText(null));
            return hasText(value) ? value : null;
        } catch (Exception ex) {
            return null;
        }
    }

    private byte[] decryptAudioFilePayload(byte[] encryptedBytes, String contenido) {
        if (encryptedBytes == null || encryptedBytes.length == 0 || !hasText(contenido)) return null;
        String ivFile = readContentIvFile(contenido);
        String adminEnvelope = readContentAdminEnvelope(contenido);
        if (!hasText(ivFile) || !hasText(adminEnvelope)) return null;
        try {
            byte[] adminEnvelopeBytes = adminAuditCrypto.decryptBase64EnvelopeBytes(adminEnvelope);
            byte[] aesKey = resolveAesKey(adminEnvelopeBytes);
            byte[] iv = Base64.getDecoder().decode(ivFile);
            Cipher cipher = Cipher.getInstance(TRANSFORM_AES_GCM);
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(aesKey, "AES"), new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
            return cipher.doFinal(encryptedBytes);
        } catch (Exception ex) {
            return null;
        }
    }

    private byte[] resolveAesKey(byte[] adminEnvelopeBytes) {
        if (adminEnvelopeBytes == null || adminEnvelopeBytes.length == 0) return null;
        if (isValidAesKeyLength(adminEnvelopeBytes.length)) return adminEnvelopeBytes;
        try {
            byte[] decoded = Base64.getDecoder().decode(new String(adminEnvelopeBytes, StandardCharsets.UTF_8));
            return isValidAesKeyLength(decoded.length) ? decoded : null;
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private boolean isValidAesKeyLength(int len) {
        return len == 16 || len == 24 || len == 32;
    }

    private boolean isAllowedAudioMime(String mime) {
        return hasText(mime) && ALLOWED_AUDIO_MIMES.contains(normalizeAudioMime(mime, null));
    }

    private String normalizeAudioMime(String first, String second) {
        String mime = hasText(first) ? first : second;
        if (!hasText(mime)) return "audio/webm";
        String normalized = mime.trim().toLowerCase(Locale.ROOT);
        return "audio/mp3".equals(normalized) ? "audio/mpeg" : normalized;
    }

    private boolean looksLikeAudioBinary(byte[] bytes, String mime) {
        if (bytes == null || bytes.length < 4) return false;
        String m = normalizeAudioMime(mime, null);
        if ("audio/webm".equals(m)) return bytes.length > 4 && (bytes[0] & 0xFF) == 0x1A && (bytes[1] & 0xFF) == 0x45 && (bytes[2] & 0xFF) == 0xDF && (bytes[3] & 0xFF) == 0xA3;
        if ("audio/ogg".equals(m)) return bytes[0] == 'O' && bytes[1] == 'g' && bytes[2] == 'g' && bytes[3] == 'S';
        if ("audio/wav".equals(m)) return bytes.length > 12 && bytes[0] == 'R' && bytes[1] == 'I' && bytes[2] == 'F' && bytes[3] == 'F' && bytes[8] == 'W' && bytes[9] == 'A' && bytes[10] == 'V' && bytes[11] == 'E';
        if ("audio/mpeg".equals(m)) return (bytes[0] == 'I' && bytes[1] == 'D' && bytes[2] == '3') || ((bytes[0] & 0xFF) == 0xFF && ((bytes[1] & 0xE0) == 0xE0));
        if ("audio/mp4".equals(m)) return bytes.length > 12 && bytes[4] == 'f' && bytes[5] == 't' && bytes[6] == 'y' && bytes[7] == 'p';
        return true;
    }

    private boolean isAudioRequest(AiTextRequestDTO request) {
        if (request == null) return false;
        String tipoMensaje = normalizeInput(request.getTipoMensaje());
        String mimeType = normalizeInput(request.getMimeType());
        return "AUDIO".equalsIgnoreCase(tipoMensaje)
                || (hasText(mimeType) && mimeType.toLowerCase(Locale.ROOT).startsWith("audio/"))
                || hasText(request.getAudioUrl())
                || hasText(request.getAudioEncryptedPayload());
    }

    private AiTextMode resolveMode(AiTextMode requestedMode, String text) {
        if (requestedMode != null && requestedMode != AiTextMode.AUTO) {
            return requestedMode;
        }
        String normalized = text == null ? "" : text.trim();
        if (containsCompleteMarker(normalized) || normalized.endsWith(":")) {
            return AiTextMode.COMPLETAR_TEXTO;
        }
        return AiTextMode.REFORMULAR;
    }

    private boolean containsCompleteMarker(String text) {
        return text != null && COMPLETE_MARKER_PATTERN.matcher(text).find();
    }

    private String cleanupForMode(String text) {
        if (text == null) {
            return null;
        }
        String withoutMarker = COMPLETE_MARKER_PATTERN.matcher(text).replaceAll(" ");
        return normalizeInput(withoutMarker);
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
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

    private int resolveMaxInputLength(AiTextMode mode) {
        return mode == AiTextMode.RESPONDER
                ? aiProperties.getMaxInputLengthResponder()
                : aiProperties.getMaxInputLength();
    }

    private void logLengthValidation(AiTextMode mode, int textLength, int maxLength) {
        LOGGER.info("[AI][TEXT] length-check modoFinal={} longitudTexto={} limiteAplicado={}",
                mode.name(),
                textLength,
                maxLength);
    }

    private String buildLengthExceededMessage(AiTextMode mode, int textLength, int maxLength) {
        return "El texto supera la longitud maxima permitida para " + mode.name() + ": " + textLength + "/" + maxLength;
    }

    private AiTextResponseDTO successEncrypted(Long userId, String originalText, String generatedText, String mode) {
        String plainPayload;
        try {
            var payload = new LinkedHashMap<String, Object>();
            payload.put("textoOriginal", originalText);
            payload.put("textoGenerado", generatedText);
            payload.put("modo", mode);
            payload.put("success", true);
            payload.put("codigo", "OK");
            payload.put("mensaje", AiTextMode.TRADUCIR.name().equalsIgnoreCase(mode)
                    ? "Texto traducido correctamente"
                    : "Texto procesado correctamente");
            plainPayload = objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            return failure(null, mode, "AI_RESPONSE_ENCRYPTION_ERROR", "No se pudo cifrar la respuesta de IA para el usuario actual.");
        }

        var encrypted = aiEncryptedContextService.encryptAiResponseForUser(plainPayload, userId);
        if (encrypted == null || !encrypted.isSuccess() || !hasText(encrypted.getEncryptedPayload())) {
            return failure(null, mode, "AI_RESPONSE_ENCRYPTION_ERROR", "No se pudo cifrar la respuesta de IA para el usuario actual.");
        }

        AiTextResponseDTO response = new AiTextResponseDTO();
        response.setTextoOriginal(null);
        response.setTextoGenerado(null);
        response.setModo(mode);
        response.setSuccess(true);
        response.setCodigo("OK");
        response.setMensaje(AiTextMode.TRADUCIR.name().equalsIgnoreCase(mode)
                ? "Texto traducido correctamente"
                : "Texto procesado correctamente");
        response.setEncryptedPayload(encrypted.getEncryptedPayload());
        return response;
    }

    private AiTextResponseDTO failure(String originalText, String mode, String code, String message) {
        AiTextResponseDTO response = new AiTextResponseDTO();
        response.setTextoOriginal(originalText);
        response.setTextoGenerado(null);
        response.setModo(mode == null ? AiTextMode.AUTO.name() : mode.toUpperCase(Locale.ROOT));
        response.setSuccess(false);
        response.setCodigo(code);
        response.setMensaje(message);
        response.setEncryptedPayload(null);
        return response;
    }

    private record ChatContextInfo(Long chatId, boolean esGrupal) {
    }

    private record AudioPayloadDTO(byte[] bytes, String mimeType) {
    }

    private record AudioResolutionResult(boolean success, String code, String message, String textoOriginal) {
        private static AudioResolutionResult success(String textoOriginal) {
            return new AudioResolutionResult(true, "OK", "OK", textoOriginal);
        }

        private static AudioResolutionResult failure(String code, String message) {
            return new AudioResolutionResult(false, code, message, null);
        }
    }
}
