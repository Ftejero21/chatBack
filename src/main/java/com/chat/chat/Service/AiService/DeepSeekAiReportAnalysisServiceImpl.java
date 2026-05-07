package com.chat.chat.Service.AiService;

import com.chat.chat.Configuracion.AiProperties;
import com.chat.chat.DTO.AiReportAnalysisInternalContextMessageDTO;
import com.chat.chat.DTO.AiReportAnalysisInternalRequestDTO;
import com.chat.chat.DTO.AiReportAnalysisInternalResponseDTO;
import com.chat.chat.DTO.AiReportAnalysisRequestDTO;
import com.chat.chat.DTO.AiReportAnalysisResponseDTO;
import com.chat.chat.DTO.AudioTranscriptionResultDTO;
import com.chat.chat.Entity.ChatGrupalEntity;
import com.chat.chat.Entity.ChatIndividualEntity;
import com.chat.chat.Entity.MensajeEntity;
import com.chat.chat.Entity.UsuarioEntity;
import com.chat.chat.Exceptions.RecursoNoEncontradoException;
import com.chat.chat.Repository.ChatGrupalRepository;
import com.chat.chat.Repository.ChatIndividualRepository;
import com.chat.chat.Repository.MensajeRepository;
import com.chat.chat.Repository.UsuarioRepository;
import com.chat.chat.Utils.AdminAuditCrypto;
import com.chat.chat.Utils.Constantes;
import com.chat.chat.Utils.SecurityUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
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
import java.util.Base64;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
@Validated
public class DeepSeekAiReportAnalysisServiceImpl implements AiReportAnalysisService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DeepSeekAiReportAnalysisServiceImpl.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String TRANSFORM_AES_GCM = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH_BITS = 128;
    private static final int MAX_MESSAGE_LENGTH = 250;
    private static final int MAX_AUTHOR_LENGTH = 80;
    private static final int MAX_DATE_LENGTH = 40;
    private static final int DEFAULT_MAX_MENSAJES = 50;
    private static final int MAX_MAX_MENSAJES = 200;
    private static final String AUDIO_TRANSCRIBED_PREFIX = "[Audio transcrito automaticamente]: ";
    private static final String AUDIO_NOT_ALLOWED = "[Audio no transcrito: formato no permitido]";
    private static final String AUDIO_TOO_LARGE = "[Audio no transcrito: supera el tamano maximo permitido]";
    private static final String AUDIO_PROCESSING_ERROR = "[Audio no transcrito: error al procesar audio]";
    private static final Set<String> ALLOWED_AUDIO_MIMES = Set.of(
            "audio/webm", "audio/mpeg", "audio/mp3", "audio/wav", "audio/ogg", "audio/mp4"
    );

    private final AiProperties aiProperties;
    private final AiRateLimitService aiRateLimitService;
    private final SecurityUtils securityUtils;
    private final AiEncryptedContextService aiEncryptedContextService;
    private final AiReportAnalysisMicroserviceClient aiReportAnalysisMicroserviceClient;
    private final ChatIndividualRepository chatIndividualRepository;
    private final ChatGrupalRepository chatGrupalRepository;
    private final MensajeRepository mensajeRepository;
    private final UsuarioRepository usuarioRepository;
    private final AdminAuditCrypto adminAuditCrypto;
    private final AudioTranscriptionService audioTranscriptionService;
    private final String uploadsRoot;

    public DeepSeekAiReportAnalysisServiceImpl(AiProperties aiProperties,
                                               AiRateLimitService aiRateLimitService,
                                               SecurityUtils securityUtils,
                                               AiEncryptedContextService aiEncryptedContextService,
                                               AiReportAnalysisMicroserviceClient aiReportAnalysisMicroserviceClient,
                                               ChatIndividualRepository chatIndividualRepository,
                                               ChatGrupalRepository chatGrupalRepository,
                                               MensajeRepository mensajeRepository,
                                               UsuarioRepository usuarioRepository,
                                               AdminAuditCrypto adminAuditCrypto,
                                               AudioTranscriptionService audioTranscriptionService,
                                               @Value(Constantes.PROP_UPLOADS_ROOT) String uploadsRoot) {
        this.aiProperties = aiProperties;
        this.aiRateLimitService = aiRateLimitService;
        this.securityUtils = securityUtils;
        this.aiEncryptedContextService = aiEncryptedContextService;
        this.aiReportAnalysisMicroserviceClient = aiReportAnalysisMicroserviceClient;
        this.chatIndividualRepository = chatIndividualRepository;
        this.chatGrupalRepository = chatGrupalRepository;
        this.mensajeRepository = mensajeRepository;
        this.usuarioRepository = usuarioRepository;
        this.adminAuditCrypto = adminAuditCrypto;
        this.audioTranscriptionService = audioTranscriptionService;
        this.uploadsRoot = uploadsRoot;
    }

    @Override
    public AiReportAnalysisResponseDTO analizarDenuncia(AiReportAnalysisRequestDTO request) {
        Long userId = securityUtils.getAuthenticatedUserId();
        String requestId = UUID.randomUUID().toString();

        if (!aiProperties.isEnabled()) {
            return failure("AI_DISABLED", "La ayuda de IA no esta habilitada.");
        }
        if (!"deepseek".equalsIgnoreCase(aiProperties.getProvider())) {
            return failure("AI_PROVIDER_NOT_SUPPORTED", "El proveedor de IA configurado no es compatible.");
        }
        if (request == null || request.getUsuarioDenunciadoId() == null) {
            return failure("AI_REPORT_INVALID_REQUEST", "usuarioDenunciadoId es obligatorio");
        }
        if (!adminAuditCrypto.hasPrivateKeyConfigured()) {
            return failure("AI_ADMIN_PRIVATE_KEY_MISSING", "No esta configurada la clave privada de auditoria para analizar denuncias.");
        }
        if (!adminAuditCrypto.hasMatchingPrivateKeyForAuditPublicKey()) {
            return failure("AI_ADMIN_PRIVATE_KEY_MISMATCH", "La clave privada de auditoria configurada no corresponde a la audit public key actual.");
        }

        String chatType = normalizeChatType(request.getTipoChat());
        if (chatType == null) {
            return failure("AI_REPORT_INVALID_REQUEST", "tipoChat es obligatorio y debe ser INDIVIDUAL o GRUPAL");
        }

        int maxMensajes;
        try {
            maxMensajes = resolveMaxMensajes(request.getMaxMensajes());
        } catch (IllegalArgumentException ex) {
            return failure("AI_REPORT_INVALID_REQUEST", ex.getMessage());
        }

        ReportScope scope;
        try {
            scope = validateAccessAndResolveScope(request, userId, chatType);
        } catch (IllegalArgumentException ex) {
            return failure("AI_REPORT_INVALID_REQUEST", ex.getMessage());
        } catch (RecursoNoEncontradoException ex) {
            return failure("AI_REPORT_CHAT_NOT_FOUND", ex.getMessage());
        }

        List<MensajeEntity> databaseMessages = loadRecentChatMessages(scope.chatType(), scope.chatId(), scope.chatGrupalId(), maxMensajes);
        PreparedReportContext preparedContext = prepareContextMessages(databaseMessages, userId, scope.usuarioDenunciadoId());
        if (preparedContext.messages().isEmpty()) {
            return failure("AI_REPORT_EMPTY_CONTEXT", "No hay mensajes suficientes para analizar la denuncia.");
        }

        AiRateLimitCheck rateLimitCheck = aiRateLimitService.checkUsage(userId);
        if (!rateLimitCheck.isAllowed()) {
            return failure(rateLimitCheck.getCode(), rateLimitCheck.getMessage());
        }

        try {
            LOGGER.info("[AI][REPORT_ANALYSIS] requestId={} tipoChat={} chatId={} chatGrupalId={} usuarioDenunciadoId={} totalMensajesCargados={} totalMensajesPreparados={} llamadaMicroservicioIA=true",
                    requestId,
                    scope.chatType(),
                    scope.chatId(),
                    scope.chatGrupalId(),
                    scope.usuarioDenunciadoId(),
                    databaseMessages.size(),
                    preparedContext.messages().size());

            AiReportAnalysisInternalResponseDTO internalResponse = aiReportAnalysisMicroserviceClient.analizarDenuncia(
                    requestId,
                    buildInternalRequest(requestId, scope, preparedContext.messages())
            );

            if (!isValidInternalResponse(internalResponse)) {
                return failure("AI_REPORT_INVALID_SERVICE_RESPONSE", "La respuesta interna del servicio de IA no es valida.");
            }
            if (!internalResponse.isSuccess()) {
                return failure("AI_REPORT_SERVICE_ERROR", resolveServiceErrorMessage(internalResponse));
            }

            aiRateLimitService.registrarUso(userId);
            return successEncrypted(
                    userId,
                    internalResponse.getMotivoSeleccionado(),
                    internalResponse.getDescripcionDenuncia(),
                    internalResponse.getGravedad(),
                    internalResponse.getResumen(),
                    internalResponse.getAccionSugerida()
            );
        } catch (AiReportAnalysisMicroserviceUnavailableException ex) {
            return failure("AI_REPORT_SERVICE_UNAVAILABLE", "El microservicio de analisis IA no esta disponible temporalmente.");
        } catch (AiReportAnalysisMicroserviceException ex) {
            return failure("AI_REPORT_SERVICE_ERROR", "El microservicio de analisis IA devolvio un error.");
        }
    }

    private ReportScope validateAccessAndResolveScope(AiReportAnalysisRequestDTO request, Long userId, String chatType) {
        Long reportedUserId = request.getUsuarioDenunciadoId();
        if (reportedUserId == null) {
            throw new IllegalArgumentException("usuarioDenunciadoId es obligatorio");
        }
        if (Objects.equals(reportedUserId, userId)) {
            throw new IllegalArgumentException("No puedes denunciarte a ti mismo.");
        }

        if (Constantes.CHAT_TIPO_INDIVIDUAL.equals(chatType)) {
            if (request.getChatId() == null) {
                throw new IllegalArgumentException("chatId es obligatorio para chats individuales.");
            }
            if (request.getChatGrupalId() != null) {
                throw new IllegalArgumentException("chatGrupalId no aplica a chats individuales.");
            }
            if (!chatIndividualRepository.existsMemberByChatIdAndUserId(request.getChatId(), userId)) {
                if (chatIndividualRepository.existsById(request.getChatId())) {
                    throw new IllegalArgumentException(Constantes.MSG_NO_PERTENECE_CHAT);
                }
                throw new RecursoNoEncontradoException(Constantes.MSG_CHAT_NO_ENCONTRADO_ID + request.getChatId());
            }
            ChatIndividualEntity chat = chatIndividualRepository.findById(request.getChatId())
                    .orElseThrow(() -> new RecursoNoEncontradoException(Constantes.MSG_CHAT_NO_ENCONTRADO_ID + request.getChatId()));
            Long user1Id = chat.getUsuario1() == null ? null : chat.getUsuario1().getId();
            Long user2Id = chat.getUsuario2() == null ? null : chat.getUsuario2().getId();
            if (!Objects.equals(reportedUserId, user1Id) && !Objects.equals(reportedUserId, user2Id)) {
                throw new IllegalArgumentException("usuarioDenunciadoId no pertenece a este chat.");
            }
            String reportedName = resolveReportedUserName(chat.getUsuario1(), chat.getUsuario2(), reportedUserId);
            return new ReportScope(chatType, request.getChatId(), null, reportedUserId, reportedName);
        }

        if (request.getChatGrupalId() == null) {
            throw new IllegalArgumentException("chatGrupalId es obligatorio para chats grupales.");
        }
        if (request.getChatId() != null) {
            throw new IllegalArgumentException("chatId no aplica a chats grupales.");
        }
        if (!chatGrupalRepository.existsActiveMemberByChatIdAndUserId(request.getChatGrupalId(), userId)) {
            if (chatGrupalRepository.existsById(request.getChatGrupalId())) {
                throw new IllegalArgumentException(Constantes.MSG_NO_PERTENECE_GRUPO);
            }
            throw new RecursoNoEncontradoException(Constantes.MSG_CHAT_GRUPAL_NO_ENCONTRADO_ID + request.getChatGrupalId());
        }
        ChatGrupalEntity chat = chatGrupalRepository.findByIdWithUsuarios(request.getChatGrupalId())
                .orElseThrow(() -> new RecursoNoEncontradoException(Constantes.MSG_CHAT_GRUPAL_NO_ENCONTRADO_ID + request.getChatGrupalId()));
        UsuarioEntity reportedUser = chat.getUsuarios() == null ? null : chat.getUsuarios().stream()
                .filter(u -> u != null && Objects.equals(u.getId(), reportedUserId) && u.isActivo())
                .findFirst()
                .orElse(null);
        if (reportedUser == null) {
            throw new IllegalArgumentException("usuarioDenunciadoId no pertenece a este grupo.");
        }
        return new ReportScope(chatType, null, request.getChatGrupalId(), reportedUserId, displayName(reportedUser));
    }

    private int resolveMaxMensajes(Integer requestedMaxMensajes) {
        if (requestedMaxMensajes == null) {
            return DEFAULT_MAX_MENSAJES;
        }
        if (requestedMaxMensajes < 1 || requestedMaxMensajes > MAX_MAX_MENSAJES) {
            throw new IllegalArgumentException("maxMensajes debe estar entre 1 y 200.");
        }
        return requestedMaxMensajes;
    }

    private List<MensajeEntity> loadRecentChatMessages(String chatType, Long chatId, Long chatGrupalId, int maxMensajes) {
        PageRequest pageRequest = PageRequest.of(0, maxMensajes);
        List<MensajeEntity> loaded = Constantes.CHAT_TIPO_INDIVIDUAL.equals(chatType)
                ? mensajeRepository.findLatestVisibleMessagesByChatIndividualId(chatId, pageRequest)
                : mensajeRepository.findLatestVisibleMessagesByChatGrupalId(chatGrupalId, pageRequest);
        if (loaded == null || loaded.isEmpty()) {
            return List.of();
        }
        List<MensajeEntity> ordered = new ArrayList<>(loaded);
        Collections.reverse(ordered);
        return ordered;
    }

    private PreparedReportContext prepareContextMessages(List<MensajeEntity> messages, Long userId, Long reportedUserId) {
        List<AiReportAnalysisInternalContextMessageDTO> prepared = new ArrayList<>();
        int totalAudiosTranscritos = 0;
        for (MensajeEntity message : messages) {
            if (message == null) {
                continue;
            }
            String tipoMensaje = resolveTipoMensaje(message);
            String contenido = resolveContextContent(message);
            contenido = normalizeInput(contenido);
            if (!hasText(contenido)) {
                continue;
            }
            if ("AUDIO".equals(tipoMensaje) && contenido.startsWith(AUDIO_TRANSCRIBED_PREFIX)) {
                totalAudiosTranscritos++;
            }
            AiReportAnalysisInternalContextMessageDTO dto = new AiReportAnalysisInternalContextMessageDTO();
            dto.setId(message.getId());
            dto.setAutor(displayName(message.getEmisor()));
            dto.setAutorId(message.getEmisor() == null ? null : message.getEmisor().getId());
            dto.setEsUsuarioActual(message.getEmisor() != null && Objects.equals(message.getEmisor().getId(), userId));
            dto.setTipoMensaje(tipoMensaje);
            dto.setContenido(truncate(contenido, MAX_MESSAGE_LENGTH));
            dto.setFechaEnvio(formatDate(message));
            prepared.add(dto);
        }
        return new PreparedReportContext(prepared, totalAudiosTranscritos);
    }

    private String resolveContextContent(MensajeEntity message) {
        String tipoMensaje = resolveTipoMensaje(message);
        if ("AUDIO".equals(tipoMensaje)) {
            return resolveAudioContextFromDb(message);
        }
        if ("TEXT".equals(tipoMensaje)) {
            return resolveTextContentFromDb(message);
        }
        return switch (tipoMensaje) {
            case "IMAGE" -> "[Imagen]";
            case "STICKER" -> "[Sticker]";
            case "FILE" -> "[Archivo]";
            case "VIDEO" -> "[Video]";
            default -> "[Mensaje multimedia no incluido en el analisis]";
        };
    }

    private String resolveTextContentFromDb(MensajeEntity message) {
        String contenido = normalizeInput(message == null ? null : message.getContenido());
        String plain = null;
        if (hasText(contenido)) {
            plain = normalizeInput(aiEncryptedContextService.decryptMessagePayload(contenido));
            if (!hasText(plain)) {
                plain = contenido;
            }
        }
        if (!hasText(plain) || isNonTextContent(plain)) {
            return null;
        }
        return plain;
    }

    private String resolveAudioContextFromDb(MensajeEntity mensaje) {
        try {
            Optional<AudioPayloadDTO> maybeAudioPayload = resolveAudioFromDbMessage(mensaje);
            if (maybeAudioPayload.isEmpty()) {
                return AUDIO_PROCESSING_ERROR;
            }
            AudioPayloadDTO audioPayload = maybeAudioPayload.get();
            if (!isAllowedAudioMime(audioPayload.mimeType())) {
                return AUDIO_NOT_ALLOWED;
            }
            if (audioPayload.bytes().length > aiProperties.getAudioTranscription().getMaxAudioSizeBytes()) {
                return AUDIO_TOO_LARGE;
            }
            AudioTranscriptionResultDTO transcription = audioTranscriptionService.transcribirAudio(
                    mensaje,
                    audioPayload.bytes(),
                    audioPayload.mimeType()
            );
            if (transcription == null || !transcription.isSuccess() || !hasText(transcription.getTranscripcion())) {
                return "[Audio no transcrito: " + resolveAudioFailureMessage(transcription) + "]";
            }
            return AUDIO_TRANSCRIBED_PREFIX + transcription.getTranscripcion();
        } catch (RuntimeException ex) {
            return AUDIO_PROCESSING_ERROR;
        }
    }

    private Optional<AudioPayloadDTO> resolveAudioFromDbMessage(MensajeEntity mensaje) {
        if (mensaje == null) return Optional.empty();
        if (hasText(mensaje.getMediaUrl())) {
            byte[] bytes = readAudioFromMediaUrl(mensaje.getMediaUrl());
            if (bytes != null && bytes.length > 0) {
                String mime = normalizeAudioMime(mensaje.getMediaMime(), readContentAudioMime(mensaje.getContenido()));
                if (looksLikeAudioBinary(bytes, mime)) {
                    return Optional.of(new AudioPayloadDTO(bytes, mime));
                }
                byte[] decrypted = decryptAudioFilePayload(bytes, mensaje.getContenido());
                if (decrypted != null && decrypted.length > 0) {
                    return Optional.of(new AudioPayloadDTO(decrypted, mime));
                }
            }
        }
        String contentAudioUrl = readContentAudioUrl(mensaje.getContenido());
        if (hasText(contentAudioUrl)) {
            byte[] bytes = readAudioFromMediaUrl(contentAudioUrl);
            if (bytes != null && bytes.length > 0) {
                String mime = normalizeAudioMime(readContentAudioMime(mensaje.getContenido()), mensaje.getMediaMime());
                if (looksLikeAudioBinary(bytes, mime)) {
                    return Optional.of(new AudioPayloadDTO(bytes, mime));
                }
                byte[] decrypted = decryptAudioFilePayload(bytes, mensaje.getContenido());
                if (decrypted != null && decrypted.length > 0) {
                    return Optional.of(new AudioPayloadDTO(decrypted, mime));
                }
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
        if (!hasText(mediaUrl) || !mediaUrl.startsWith(Constantes.UPLOADS_PREFIX)) {
            return null;
        }
        try {
            String relative = mediaUrl.substring(Constantes.UPLOADS_PREFIX.length());
            Path root = Paths.get(uploadsRoot).toAbsolutePath().normalize();
            Path file = root.resolve(relative).normalize();
            if (!file.startsWith(root) || !Files.exists(file) || !Files.isRegularFile(file)) {
                return null;
            }
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
        if (!hasText(contenido) || !hasText(fieldName)) {
            return null;
        }
        try {
            JsonNode root = OBJECT_MAPPER.readTree(contenido);
            String value = normalizeInput(root.path(fieldName).asText(null));
            return hasText(value) ? value : null;
        } catch (Exception ex) {
            return null;
        }
    }

    private byte[] decryptAudioFilePayload(byte[] encryptedBytes, String contenido) {
        if (encryptedBytes == null || encryptedBytes.length == 0 || !hasText(contenido)) {
            return null;
        }
        String ivFile = readContentIvFile(contenido);
        String adminEnvelope = readContentAdminEnvelope(contenido);
        if (!hasText(ivFile) || !hasText(adminEnvelope)) {
            return null;
        }
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
        if (adminEnvelopeBytes == null || adminEnvelopeBytes.length == 0) {
            return null;
        }
        if (isValidAesKeyLength(adminEnvelopeBytes.length)) {
            return adminEnvelopeBytes;
        }
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

    private boolean looksLikeAudioBinary(byte[] bytes, String mime) {
        if (bytes == null || bytes.length < 4) return false;
        String m = normalizeAudioMime(mime, null);
        if ("audio/webm".equals(m)) {
            return bytes.length > 4
                    && (bytes[0] & 0xFF) == 0x1A
                    && (bytes[1] & 0xFF) == 0x45
                    && (bytes[2] & 0xFF) == 0xDF
                    && (bytes[3] & 0xFF) == 0xA3;
        }
        if ("audio/ogg".equals(m)) {
            return bytes[0] == 'O' && bytes[1] == 'g' && bytes[2] == 'g' && bytes[3] == 'S';
        }
        if ("audio/wav".equals(m)) {
            return bytes.length > 12
                    && bytes[0] == 'R' && bytes[1] == 'I' && bytes[2] == 'F' && bytes[3] == 'F'
                    && bytes[8] == 'W' && bytes[9] == 'A' && bytes[10] == 'V' && bytes[11] == 'E';
        }
        if ("audio/mpeg".equals(m)) {
            return (bytes[0] == 'I' && bytes[1] == 'D' && bytes[2] == '3')
                    || ((bytes[0] & 0xFF) == 0xFF && ((bytes[1] & 0xE0) == 0xE0));
        }
        if ("audio/mp4".equals(m)) {
            return bytes.length > 12
                    && bytes[4] == 'f' && bytes[5] == 't' && bytes[6] == 'y' && bytes[7] == 'p';
        }
        return true;
    }

    private boolean isAllowedAudioMime(String mime) {
        return hasText(mime) && ALLOWED_AUDIO_MIMES.contains(normalizeAudioMime(mime, null));
    }

    private String normalizeAudioMime(String first, String second) {
        String mime = hasText(first) ? first : second;
        if (!hasText(mime)) return "audio/webm";
        String normalized = mime.trim().toLowerCase(Locale.ROOT);
        if ("audio/mp3".equals(normalized)) return "audio/mpeg";
        return normalized;
    }

    private String resolveAudioFailureMessage(AudioTranscriptionResultDTO transcription) {
        if (transcription == null || !hasText(transcription.getCodigo())) return "error al procesar audio";
        return switch (transcription.getCodigo()) {
            case "AI_AUDIO_TRANSCRIPTION_DISABLED" -> "transcripcion deshabilitada";
            case "AI_AUDIO_INVALID_MIME" -> "formato no permitido";
            case "AI_AUDIO_TOO_LARGE" -> "supera el tamano maximo permitido";
            case "AI_AUDIO_TEMP_FILE_ERROR" -> "error temporal de archivo";
            case "AI_AUDIO_TRANSCRIPTION_TIMEOUT" -> "timeout de transcripcion";
            case "AI_AUDIO_TRANSCRIPTION_SCRIPT_NOT_FOUND" -> "script no encontrado";
            default -> "error al procesar audio";
        };
    }

    private AiReportAnalysisInternalRequestDTO buildInternalRequest(String requestId,
                                                                    ReportScope scope,
                                                                    List<AiReportAnalysisInternalContextMessageDTO> messages) {
        AiReportAnalysisInternalRequestDTO internalRequest = new AiReportAnalysisInternalRequestDTO();
        internalRequest.setRequestId(requestId);
        internalRequest.setTipoChat(scope.chatType());
        internalRequest.setChatId(scope.chatId());
        internalRequest.setChatGrupalId(scope.chatGrupalId());
        internalRequest.setUsuarioDenunciadoId(scope.usuarioDenunciadoId());
        internalRequest.setNombreUsuarioDenunciado(scope.nombreUsuarioDenunciado());
        internalRequest.setMotivosDisponibles(Constantes.AI_REPORT_AVAILABLE_REASONS);
        internalRequest.setMensajesContexto(messages);
        return internalRequest;
    }

    private boolean isValidInternalResponse(AiReportAnalysisInternalResponseDTO response) {
        return response != null && response.getCodigo() != null && response.getMensaje() != null;
    }

    private String resolveServiceErrorMessage(AiReportAnalysisInternalResponseDTO response) {
        if (response == null || !hasText(response.getMensaje())) {
            return "El microservicio de analisis IA devolvio un error.";
        }
        return response.getMensaje();
    }

    private String resolveReportedUserName(UsuarioEntity first, UsuarioEntity second, Long reportedUserId) {
        if (first != null && Objects.equals(first.getId(), reportedUserId)) {
            return displayName(first);
        }
        if (second != null && Objects.equals(second.getId(), reportedUserId)) {
            return displayName(second);
        }
        return usuarioRepository.findFreshById(reportedUserId).map(this::displayName).orElse("Usuario " + reportedUserId);
    }

    private String displayName(UsuarioEntity user) {
        if (user == null) {
            return null;
        }
        String nombre = normalizeInput(user.getNombre());
        String apellido = normalizeInput(user.getApellido());
        String full = ((nombre == null ? "" : nombre) + " " + (apellido == null ? "" : apellido)).trim();
        if (hasText(full)) {
            return truncate(full, 120);
        }
        return truncate(normalizeInput(user.getEmail()), 120);
    }

    private String resolveTipoMensaje(MensajeEntity mensaje) {
        if (mensaje == null) {
            return "UNKNOWN";
        }
        if (mensaje.getStickerId() != null) {
            return "STICKER";
        }
        if (hasText(mensaje.getMediaMime())) {
            String mime = mensaje.getMediaMime().trim().toLowerCase(Locale.ROOT);
            if (mime.startsWith("audio/")) return "AUDIO";
            if (mime.startsWith("image/")) return "IMAGE";
            if (mime.startsWith("video/")) return "VIDEO";
            if (mime.startsWith("application/") || mime.startsWith("text/")) return "FILE";
        }
        return mensaje.getTipo() == null ? "TEXT" : mensaje.getTipo().name();
    }

    private String formatDate(MensajeEntity mensaje) {
        return mensaje == null || mensaje.getFechaEnvio() == null ? null : mensaje.getFechaEnvio().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
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

    private boolean isNonTextContent(String content) {
        String normalized = content.trim().toLowerCase(Locale.ROOT);
        return normalized.startsWith("data:image")
                || normalized.startsWith("data:audio")
                || normalized.startsWith("data:video")
                || normalized.startsWith("data:application");
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

    private AiReportAnalysisResponseDTO successEncrypted(Long userId,
                                                         String motivoSeleccionado,
                                                         String descripcionDenuncia,
                                                         String gravedad,
                                                         String resumen,
                                                         String accionSugerida) {
        String jsonPlain;
        try {
            java.util.Map<String, String> payload = new LinkedHashMap<>();
            payload.put("motivoSeleccionado", motivoSeleccionado);
            payload.put("descripcionDenuncia", descripcionDenuncia);
            payload.put("gravedad", gravedad);
            payload.put("resumen", resumen);
            payload.put("accionSugerida", accionSugerida);
            payload.put("mensaje", "Analisis de denuncia generado correctamente");
            payload.put("success", "true");
            jsonPlain = OBJECT_MAPPER.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            return failure("AI_RESPONSE_ENCRYPTION_ERROR", "No se pudo cifrar el analisis de denuncia para el usuario actual.");
        }

        var encrypted = aiEncryptedContextService.encryptAiResponseForUser(jsonPlain, userId);
        if (encrypted == null || !encrypted.isSuccess() || !hasText(encrypted.getEncryptedPayload())) {
            return failure("AI_RESPONSE_ENCRYPTION_ERROR", "No se pudo cifrar el analisis de denuncia para el usuario actual.");
        }

        AiReportAnalysisResponseDTO response = new AiReportAnalysisResponseDTO();
        response.setSuccess(true);
        response.setCodigo("OK");
        response.setMensaje("Analisis de denuncia generado correctamente");
        response.setMotivoSeleccionado(null);
        response.setDescripcionDenuncia(null);
        response.setGravedad(null);
        response.setResumen(null);
        response.setAccionSugerida(null);
        response.setEncryptedPayload(encrypted.getEncryptedPayload());
        return response;
    }

    private AiReportAnalysisResponseDTO failure(String code, String message) {
        AiReportAnalysisResponseDTO response = new AiReportAnalysisResponseDTO();
        response.setSuccess(false);
        response.setCodigo(code);
        response.setMensaje(message);
        response.setMotivoSeleccionado(null);
        response.setDescripcionDenuncia(null);
        response.setGravedad(null);
        response.setResumen(null);
        response.setAccionSugerida(null);
        response.setEncryptedPayload(null);
        return response;
    }

    private record AudioPayloadDTO(byte[] bytes, String mimeType) {
    }

    private record PreparedReportContext(List<AiReportAnalysisInternalContextMessageDTO> messages, int totalAudiosTranscritos) {
    }

    private record ReportScope(String chatType,
                               Long chatId,
                               Long chatGrupalId,
                               Long usuarioDenunciadoId,
                               String nombreUsuarioDenunciado) {
    }
}
