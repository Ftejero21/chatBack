package com.chat.chat.Service.AiService;

import com.chat.chat.Configuracion.AiProperties;
import com.chat.chat.DTO.AiEncryptedConversationSummaryInternalMessageDTO;
import com.chat.chat.DTO.AiEncryptedConversationSummaryInternalRequestDTO;
import com.chat.chat.DTO.AiEncryptedConversationSummaryInternalResponseDTO;
import com.chat.chat.DTO.AiEncryptedContextMessageDTO;
import com.chat.chat.DTO.AiEncryptedConversationSummaryRequestDTO;
import com.chat.chat.DTO.AiEncryptedResponseDTO;
import com.chat.chat.DTO.AudioTranscriptionResultDTO;
import com.chat.chat.Entity.MensajeEntity;
import com.chat.chat.Entity.UsuarioEntity;
import com.chat.chat.Exceptions.RecursoNoEncontradoException;
import com.chat.chat.Repository.ChatGrupalRepository;
import com.chat.chat.Repository.ChatIndividualRepository;
import com.chat.chat.Repository.MensajeRepository;
import com.chat.chat.Repository.UploadFileMetadataRepository;
import com.chat.chat.Repository.UsuarioRepository;
import com.chat.chat.Utils.AdminAuditCrypto;
import com.chat.chat.Utils.Constantes;
import com.chat.chat.Utils.E2EDiagnosticUtils;
import com.chat.chat.Utils.SecurityUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import org.springframework.data.domain.PageRequest;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
@Validated
public class DeepSeekAiEncryptedConversationSummaryServiceImpl implements AiEncryptedConversationSummaryService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DeepSeekAiEncryptedConversationSummaryServiceImpl.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String TRANSFORM_AES_GCM = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH_BITS = 128;
    private static final int MAX_MESSAGE_CONTENT_LENGTH = 500;
    private static final int MAX_AUTHOR_LENGTH = 80;
    private static final int MAX_DATE_LENGTH = 40;
    private static final int DEFAULT_MAX_MENSAJES = 50;
    private static final int MAX_MAX_MENSAJES = 200;
    private static final String AUDIO_TRANSCRIBED_PREFIX = "[Audio transcrito automaticamente]: ";
    private static final String AUDIO_NOT_ALLOWED = "[Audio no transcrito: formato no permitido]";
    private static final String AUDIO_TOO_LARGE = "[Audio no transcrito: supera el tamano maximo permitido]";
    private static final String AUDIO_PROCESSING_ERROR = "[Audio no transcrito: error al procesar audio]";
    private static final String MULTIMEDIA_NOT_INCLUDED = "[Mensaje multimedia no incluido en el resumen]";
    private static final Set<String> ALLOWED_AUDIO_MIMES = Set.of(
            "audio/webm", "audio/mpeg", "audio/mp3", "audio/wav", "audio/ogg", "audio/mp4"
    );

    private final AiProperties aiProperties;
    private final AiRateLimitService aiRateLimitService;
    private final SecurityUtils securityUtils;
    private final ChatIndividualRepository chatIndividualRepository;
    private final ChatGrupalRepository chatGrupalRepository;
    private final MensajeRepository mensajeRepository;
    private final UploadFileMetadataRepository uploadFileMetadataRepository;
    private final UsuarioRepository usuarioRepository;
    private final AdminAuditCrypto adminAuditCrypto;
    private final AiEncryptedContextService aiEncryptedContextService;
    private final AiEncryptedSummaryMicroserviceClient aiEncryptedSummaryMicroserviceClient;
    private final AudioTranscriptionService audioTranscriptionService;
    private final String uploadsRoot;

    public DeepSeekAiEncryptedConversationSummaryServiceImpl(AiProperties aiProperties,
                                                             AiRateLimitService aiRateLimitService,
                                                             SecurityUtils securityUtils,
                                                             ChatIndividualRepository chatIndividualRepository,
                                                             ChatGrupalRepository chatGrupalRepository,
                                                             MensajeRepository mensajeRepository,
                                                             UploadFileMetadataRepository uploadFileMetadataRepository,
                                                             UsuarioRepository usuarioRepository,
                                                             AdminAuditCrypto adminAuditCrypto,
                                                             AiEncryptedContextService aiEncryptedContextService,
                                                             AiEncryptedSummaryMicroserviceClient aiEncryptedSummaryMicroserviceClient,
                                                             AudioTranscriptionService audioTranscriptionService,
                                                             @Value(Constantes.PROP_UPLOADS_ROOT) String uploadsRoot) {
        this.aiProperties = aiProperties;
        this.aiRateLimitService = aiRateLimitService;
        this.securityUtils = securityUtils;
        this.chatIndividualRepository = chatIndividualRepository;
        this.chatGrupalRepository = chatGrupalRepository;
        this.mensajeRepository = mensajeRepository;
        this.uploadFileMetadataRepository = uploadFileMetadataRepository;
        this.usuarioRepository = usuarioRepository;
        this.adminAuditCrypto = adminAuditCrypto;
        this.aiEncryptedContextService = aiEncryptedContextService;
        this.aiEncryptedSummaryMicroserviceClient = aiEncryptedSummaryMicroserviceClient;
        this.audioTranscriptionService = audioTranscriptionService;
        this.uploadsRoot = uploadsRoot;
    }

    @Override
    public AiEncryptedResponseDTO resumirConversacionCifrada(AiEncryptedConversationSummaryRequestDTO request) {
        Long userId = securityUtils.getAuthenticatedUserId();
        String requestId = UUID.randomUUID().toString();

        if (!aiProperties.isEnabled()) {
            return failure("AI_DISABLED", "La ayuda de IA no esta habilitada.");
        }
        if (!"deepseek".equalsIgnoreCase(aiProperties.getProvider())) {
            return failure("AI_PROVIDER_NOT_SUPPORTED", "El proveedor de IA configurado no es compatible.");
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
        int maxMensajes = resolveMaxMensajes(request == null ? null : request.getMaxMensajes());

        try {
            validateChatAccess(request, userId, chatType);
            validateRecipientPublicKeyIfProvided(request, userId);
        } catch (IllegalArgumentException ex) {
            return failure("AI_SUMMARY_INVALID_REQUEST", ex.getMessage());
        } catch (RecursoNoEncontradoException ex) {
            return failure("AI_SUMMARY_CHAT_NOT_FOUND", ex.getMessage());
        }

        List<MensajeEntity> databaseMessages = loadRecentChatMessages(request, chatType, maxMensajes);
        PreparedSummaryContext preparedContext = decryptAndSanitizeMessages(
                buildEncryptedContextMessages(databaseMessages, userId),
                request,
                userId,
                chatType
        );
        List<AiPlainContextMessage> decryptedMessages = preparedContext.messages();
        if (decryptedMessages.isEmpty()) {
            return failure("AI_SUMMARY_EMPTY_CONTEXT", "No hay mensajes suficientes para resumir.");
        }

        AiRateLimitCheck rateLimitCheck = aiRateLimitService.checkUsage(userId);
        if (!rateLimitCheck.isAllowed()) {
            return failure(rateLimitCheck.getCode(), rateLimitCheck.getMessage());
        }

        SummaryStyle style = SummaryStyle.fromValue(request == null ? null : request.getEstilo());
        int maxLineas = resolveMaxLineas(request == null ? null : request.getMaxLineas(), style);

        try {
            LOGGER.info("[AI][SUMMARY_ENCRYPTED] requestId={} tipoChat={} chatId={} chatGrupalId={} maxMensajes={} totalMensajesCargadosDesdeBBDD={} totalMensajesPreparados={} totalAudiosTranscritos={} llamadaMicroservicioIA=true",
                    requestId,
                    chatType,
                    request == null ? null : request.getChatId(),
                    request == null ? null : request.getChatGrupalId(),
                    maxMensajes,
                    databaseMessages.size(),
                    decryptedMessages.size(),
                    preparedContext.totalAudiosTranscritos());

            AiEncryptedConversationSummaryInternalResponseDTO internalResponse = aiEncryptedSummaryMicroserviceClient.resumirConversacion(
                    requestId,
                    buildInternalRequest(requestId, request, chatType, style, maxLineas, decryptedMessages)
            );
            if (!isValidInternalResponse(internalResponse)) {
                LOGGER.warn("[AI][SUMMARY_ENCRYPTED] requestId={} tipoChat={} chatId={} chatGrupalId={} maxMensajes={} totalMensajesCargadosDesdeBBDD={} totalMensajesPreparados={} totalAudiosTranscritos={} llamadaMicroservicioIA=true invalid-service-response=true",
                        requestId,
                        chatType,
                        request == null ? null : request.getChatId(),
                        request == null ? null : request.getChatGrupalId(),
                        maxMensajes,
                        databaseMessages.size(),
                        decryptedMessages.size(),
                        preparedContext.totalAudiosTranscritos());
                return failure("AI_ENCRYPTED_SUMMARY_INVALID_SERVICE_RESPONSE", "La respuesta interna del servicio de resumen IA no es valida.");
            }
            if (!internalResponse.isSuccess()) {
                LOGGER.warn("[AI][SUMMARY_ENCRYPTED] requestId={} tipoChat={} chatId={} chatGrupalId={} maxMensajes={} totalMensajesCargadosDesdeBBDD={} totalMensajesPreparados={} totalAudiosTranscritos={} llamadaMicroservicioIA=true service-error-code={}",
                        requestId,
                        chatType,
                        request == null ? null : request.getChatId(),
                        request == null ? null : request.getChatGrupalId(),
                        maxMensajes,
                        databaseMessages.size(),
                        decryptedMessages.size(),
                        preparedContext.totalAudiosTranscritos(),
                        internalResponse.getCodigo());
                return failure("AI_ENCRYPTED_SUMMARY_SERVICE_ERROR", resolveServiceErrorMessage(internalResponse));
            }

            String cleanedSummary = normalizeInput(internalResponse.getResumen());
            aiRateLimitService.registrarUso(userId);
            if (!hasText(cleanedSummary)) {
                return failure("AI_ENCRYPTED_SUMMARY_INVALID_SERVICE_RESPONSE", "La respuesta interna del servicio de resumen IA no es valida.");
            }
            AiEncryptedResponseDTO encryptedResponse = aiEncryptedContextService.encryptAiResponseForUser(cleanedSummary, userId);
            if (encryptedResponse == null || !encryptedResponse.isSuccess() || !hasText(encryptedResponse.getEncryptedPayload())) {
                return failure("AI_RESPONSE_ENCRYPTION_ERROR", "No se pudo cifrar el resumen para el usuario actual.");
            }
            encryptedResponse.setResumen(null);
            return encryptedResponse;
        } catch (AiEncryptedSummaryMicroserviceUnavailableException ex) {
            LOGGER.warn("[AI][SUMMARY_ENCRYPTED] requestId={} tipoChat={} chatId={} chatGrupalId={} maxMensajes={} totalMensajesCargadosDesdeBBDD={} totalMensajesPreparados={} totalAudiosTranscritos={} llamadaMicroservicioIA=true service-unavailable=true errorClass={}",
                    requestId,
                    chatType,
                    request == null ? null : request.getChatId(),
                    request == null ? null : request.getChatGrupalId(),
                    maxMensajes,
                    databaseMessages.size(),
                    decryptedMessages.size(),
                    preparedContext.totalAudiosTranscritos(),
                    ex.getClass().getSimpleName());
            return failure("AI_ENCRYPTED_SUMMARY_SERVICE_UNAVAILABLE", "El microservicio de resumen IA no esta disponible temporalmente.");
        } catch (AiEncryptedSummaryMicroserviceException ex) {
            LOGGER.warn("[AI][SUMMARY_ENCRYPTED] requestId={} tipoChat={} chatId={} chatGrupalId={} maxMensajes={} totalMensajesCargadosDesdeBBDD={} totalMensajesPreparados={} totalAudiosTranscritos={} llamadaMicroservicioIA=true service-error=true errorClass={}",
                    requestId,
                    chatType,
                    request == null ? null : request.getChatId(),
                    request == null ? null : request.getChatGrupalId(),
                    maxMensajes,
                    databaseMessages.size(),
                    decryptedMessages.size(),
                    preparedContext.totalAudiosTranscritos(),
                    ex.getClass().getSimpleName());
            return failure("AI_ENCRYPTED_SUMMARY_SERVICE_ERROR", "El microservicio de resumen IA devolvio un error.");
        } catch (RuntimeException ex) {
            LOGGER.warn("[AI][SUMMARY_ENCRYPTED] requestId={} tipoChat={} chatId={} chatGrupalId={} maxMensajes={} totalMensajesCargadosDesdeBBDD={} totalMensajesPreparados={} totalAudiosTranscritos={} llamadaMicroservicioIA=false runtime-error=true errorClass={}",
                    requestId,
                    chatType,
                    request == null ? null : request.getChatId(),
                    request == null ? null : request.getChatGrupalId(),
                    maxMensajes,
                    databaseMessages.size(),
                    decryptedMessages.size(),
                    preparedContext.totalAudiosTranscritos(),
                    ex.getClass().getSimpleName());
            return failure("AI_ENCRYPTED_SUMMARY_RUNTIME_ERROR", "No se pudo completar el resumen cifrado.");
        } catch (Exception ex) {
            LOGGER.warn("[AI][SUMMARY_ENCRYPTED] requestId={} tipoChat={} chatId={} chatGrupalId={} maxMensajes={} totalMensajesCargadosDesdeBBDD={} totalMensajesPreparados={} totalAudiosTranscritos={} llamadaMicroservicioIA=false unexpected-error=true errorClass={}",
                    requestId,
                    chatType,
                    request == null ? null : request.getChatId(),
                    request == null ? null : request.getChatGrupalId(),
                    maxMensajes,
                    databaseMessages.size(),
                    decryptedMessages.size(),
                    preparedContext.totalAudiosTranscritos(),
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

    private int resolveMaxMensajes(Integer requestedMaxMensajes) {
        if (requestedMaxMensajes == null) {
            return DEFAULT_MAX_MENSAJES;
        }
        if (requestedMaxMensajes < 1 || requestedMaxMensajes > MAX_MAX_MENSAJES) {
            throw new IllegalArgumentException("maxMensajes debe estar entre 1 y 200.");
        }
        return requestedMaxMensajes;
    }

    private List<MensajeEntity> loadRecentChatMessages(AiEncryptedConversationSummaryRequestDTO request,
                                                       String chatType,
                                                       int maxMensajes) {
        PageRequest pageRequest = PageRequest.of(0, maxMensajes);
        List<MensajeEntity> loaded = Constantes.CHAT_TIPO_INDIVIDUAL.equals(chatType)
                ? mensajeRepository.findLatestVisibleMessagesByChatIndividualId(request.getChatId(), pageRequest)
                : mensajeRepository.findLatestVisibleMessagesByChatGrupalId(request.getChatGrupalId(), pageRequest);
        if (loaded == null || loaded.isEmpty()) {
            return List.of();
        }
        List<MensajeEntity> ordered = new ArrayList<>(loaded);
        Collections.reverse(ordered);
        return ordered;
    }

    private List<AiEncryptedContextMessageDTO> buildEncryptedContextMessages(List<MensajeEntity> mensajes, Long userId) {
        List<AiEncryptedContextMessageDTO> out = new ArrayList<>();
        if (mensajes == null) {
            return out;
        }
        for (MensajeEntity mensaje : mensajes) {
            if (mensaje == null) {
                continue;
            }
            AiEncryptedContextMessageDTO dto = new AiEncryptedContextMessageDTO();
            dto.setId(mensaje.getId());
            dto.setMessageId(mensaje.getId());
            dto.setAutorId(mensaje.getEmisor() == null ? null : mensaje.getEmisor().getId());
            dto.setEmisorId(mensaje.getEmisor() == null ? null : mensaje.getEmisor().getId());
            dto.setReceptorId(mensaje.getReceptor() == null ? null : mensaje.getReceptor().getId());
            dto.setChatId(mensaje.getChat() == null ? null : mensaje.getChat().getId());
            dto.setChatGrupalId(mensaje.getChat() == null ? null : mensaje.getChat().getId());
            dto.setAutor(displayName(mensaje.getEmisor()));
            dto.setFechaEnvio(mensaje.getFechaEnvio() == null ? null : mensaje.getFechaEnvio().toString());
            dto.setFecha(dto.getFechaEnvio());
            dto.setEsUsuarioActual(mensaje.getEmisor() != null && Objects.equals(mensaje.getEmisor().getId(), userId));
            dto.setContenido(mensaje.getContenido());
            dto.setEncryptedPayload(mensaje.getContenido());
            dto.setAudioUrl(mensaje.getMediaUrl());
            dto.setMimeType(mensaje.getMediaMime());
            dto.setNombreArchivo(extractFileName(mensaje.getMediaUrl()));
            dto.setTipoMensaje(resolveTipoMensaje(mensaje));
            out.add(dto);
        }
        return out;
    }

    private PreparedSummaryContext decryptAndSanitizeMessages(List<AiEncryptedContextMessageDTO> messages,
                                                              AiEncryptedConversationSummaryRequestDTO request,
                                                              Long userId,
                                                              String chatType) {
        if (messages == null || messages.isEmpty()) {
            return new PreparedSummaryContext(List.of(), 0);
        }
        int maxMessages = Math.max(1, aiProperties.getMaxSummaryMessages());
        int start = Math.max(0, messages.size() - maxMessages);
        List<AiEncryptedContextMessageDTO> limitedMessages = new ArrayList<>(messages.subList(start, messages.size()));
        List<AiPlainContextMessage> sanitized = new ArrayList<>();
        int totalAudiosTranscritos = 0;
        for (AiEncryptedContextMessageDTO message : limitedMessages) {
            if (message == null) continue;
            boolean isAudio = isAudioMessage(message);
            boolean isText = !isAudio && isTextMessage(message);
            AiPlainContextMessage current = new AiPlainContextMessage();
            current.setId(resolveMessageId(message));
            current.setAutorId(firstNonNull(message.getAutorId(), message.getEmisorId()));
            current.setAutor(defaultAuthor(message.getAutor(), message.isEsUsuarioActual()));
            current.setEsUsuarioActual(message.isEsUsuarioActual());
            current.setFecha(truncate(normalizeInput(resolveFecha(message)), MAX_DATE_LENGTH));
            current.setTipoMensaje(resolvePreparedMessageType(message, isAudio, isText));
            String content;
            if (isAudio) {
                AudioContextResult audioContextResult = resolveAudioContext(message, request, userId, chatType);
                content = audioContextResult.content();
                if (audioContextResult.transcribed()) {
                    totalAudiosTranscritos++;
                }
            } else if (isText) {
                content = resolveTextContent(message);
            } else {
                content = MULTIMEDIA_NOT_INCLUDED;
            }
            content = normalizeInput(content);
            if (!hasText(content)) continue;
            current.setContenido(truncate(content, MAX_MESSAGE_CONTENT_LENGTH));
            sanitized.add(current);
        }
        return new PreparedSummaryContext(sanitized, totalAudiosTranscritos);
    }

    private String resolveTextContent(AiEncryptedContextMessageDTO message) {
        String encryptedPayload = normalizeInput(message.getEncryptedPayload());
        String contenido = normalizeInput(message.getContenido());
        String plain = null;
        if (hasText(encryptedPayload)) {
            plain = normalizeInput(aiEncryptedContextService.decryptMessagePayload(encryptedPayload));
        }
        if (!hasText(plain) && hasText(contenido)) {
            String fromContenido = normalizeInput(aiEncryptedContextService.decryptMessagePayload(contenido));
            plain = hasText(fromContenido) ? fromContenido : contenido;
        }
        if (!hasText(plain) || isNonTextContent(plain)) {
            return null;
        }
        return plain;
    }

    private AudioContextResult resolveAudioContext(AiEncryptedContextMessageDTO dto,
                                                   AiEncryptedConversationSummaryRequestDTO request,
                                                   Long userId,
                                                   String chatType) {
        Long messageId = resolveMessageId(dto);
        try {
            Optional<MensajeEntity> maybeMensaje = resolveAndValidateAudioMessage(dto, request, userId, chatType);
            if (maybeMensaje.isEmpty()) {
                LOGGER.warn("[AI][SUMMARY_ENCRYPTED][AUDIO] skip reason=AI_AUDIO_ACCESS_DENIED_OR_NOT_FOUND messageId={} chatType={}",
                        messageId, chatType);
                return new AudioContextResult(AUDIO_PROCESSING_ERROR, false);
            }
            MensajeEntity mensaje = maybeMensaje.get();
            Optional<AudioPayloadDTO> maybeAudioPayload = resolverAudioDesdeMensaje(dto, mensaje);
            if (maybeAudioPayload.isEmpty()) {
                LOGGER.warn("[AI][SUMMARY_ENCRYPTED][AUDIO] skip reason=AI_AUDIO_NOT_FOUND messageId={} resolvedMessageId={}",
                        messageId, mensaje.getId());
                return new AudioContextResult(AUDIO_PROCESSING_ERROR, false);
            }
            AudioPayloadDTO audioPayload = maybeAudioPayload.get();
            if (!isAllowedAudioMime(audioPayload.mimeType())) {
                LOGGER.warn("[AI][SUMMARY_ENCRYPTED][AUDIO] skip reason=AI_AUDIO_INVALID_MIME messageId={} mime={}",
                        messageId, normalizeAudioMime(audioPayload.mimeType(), null));
                return new AudioContextResult(AUDIO_NOT_ALLOWED, false);
            }
            if (audioPayload.bytes().length > aiProperties.getAudioTranscription().getMaxAudioSizeBytes()) {
                LOGGER.warn("[AI][SUMMARY_ENCRYPTED][AUDIO] skip reason=AI_AUDIO_TOO_LARGE messageId={} size={} max={}",
                        messageId, audioPayload.bytes().length, aiProperties.getAudioTranscription().getMaxAudioSizeBytes());
                return new AudioContextResult(AUDIO_TOO_LARGE, false);
            }
            AudioTranscriptionResultDTO transcription = audioTranscriptionService.transcribirAudio(
                    mensaje,
                    audioPayload.bytes(),
                    audioPayload.mimeType()
            );
            if (transcription == null || !transcription.isSuccess() || !hasText(transcription.getTranscripcion())) {
                LOGGER.warn("[AI][SUMMARY_ENCRYPTED][AUDIO] skip reason={} messageId={} resolvedMessageId={}",
                        transcription == null ? "AI_AUDIO_TRANSCRIPTION_ERROR" : transcription.getCodigo(),
                        messageId,
                        mensaje.getId());
                return new AudioContextResult("[Audio no transcrito: " + resolveAudioFailureMessage(transcription) + "]", false);
            }
            return new AudioContextResult(AUDIO_TRANSCRIBED_PREFIX + transcription.getTranscripcion(), true);
        } catch (RuntimeException ex) {
            LOGGER.warn("[AI][SUMMARY_ENCRYPTED][AUDIO] skip reason=AI_AUDIO_TRANSCRIPTION_ERROR messageId={} errClass={}",
                    messageId, ex.getClass().getSimpleName());
            return new AudioContextResult(AUDIO_PROCESSING_ERROR, false);
        }
    }

    private Optional<MensajeEntity> resolveAndValidateAudioMessage(AiEncryptedContextMessageDTO dto,
                                                                   AiEncryptedConversationSummaryRequestDTO request,
                                                                   Long userId,
                                                                   String chatType) {
        Long messageId = resolveMessageId(dto);
        if (messageId == null) {
            String audioUrl = normalizeInput(dto.getAudioUrl());
            if (!hasText(audioUrl)) {
                return Optional.empty();
            }
            Long scopeChatId = Constantes.CHAT_TIPO_INDIVIDUAL.equals(chatType) ? request.getChatId() : request.getChatGrupalId();
            if (scopeChatId == null) {
                return Optional.empty();
            }
            return mensajeRepository.findByChatIdAndMediaUrlOrderByIdDesc(scopeChatId, audioUrl, PageRequest.of(0, 1))
                    .stream()
                    .findFirst();
        }
        Optional<MensajeEntity> maybe = mensajeRepository.findById(messageId);
        if (maybe.isEmpty()) return Optional.empty();
        MensajeEntity mensaje = maybe.get();
        Long chatIdReal = mensaje.getChat() == null ? null : mensaje.getChat().getId();
        if (chatIdReal == null) return Optional.empty();
        if (Constantes.CHAT_TIPO_INDIVIDUAL.equals(chatType)) {
            if (!Objects.equals(chatIdReal, request.getChatId())) return Optional.empty();
            if (!chatIndividualRepository.existsMemberByChatIdAndUserId(chatIdReal, userId)) return Optional.empty();
        } else {
            if (!Objects.equals(chatIdReal, request.getChatGrupalId())) return Optional.empty();
            if (!chatGrupalRepository.existsActiveMemberByChatIdAndUserId(chatIdReal, userId)) return Optional.empty();
        }
        return Optional.of(mensaje);
    }

    private Optional<AudioPayloadDTO> resolverAudioDesdeMensaje(AiEncryptedContextMessageDTO dto, MensajeEntity mensaje) {
        Optional<AudioPayloadDTO> fromDb = resolveAudioFromDbMessage(mensaje);
        if (fromDb.isPresent() && looksLikeAudioBinary(fromDb.get().bytes(), fromDb.get().mimeType())) {
            return fromDb;
        }

        if (hasText(dto.getAudioEncryptedPayload())) {
            byte[] decrypted = aiEncryptedContextService.decryptMessagePayloadToBytes(dto.getAudioEncryptedPayload());
            if (decrypted != null && decrypted.length > 0) {
                String mime = normalizeAudioMime(dto.getMimeType(), mensaje.getMediaMime());
                if (looksLikeAudioBinary(decrypted, mime)) {
                    return Optional.of(new AudioPayloadDTO(decrypted, mime));
                }
            }
        }

        // Reintento con contenido del mensaje por compatibilidad de payloads legacy.
        String contenido = normalizeInput(mensaje.getContenido());
        if (hasText(contenido)) {
            byte[] decrypted = aiEncryptedContextService.decryptMessagePayloadToBytes(contenido);
            String mime = normalizeAudioMime(dto.getMimeType(), mensaje.getMediaMime());
            if (decrypted != null && decrypted.length > 0 && looksLikeAudioBinary(decrypted, mime)) {
                return Optional.of(new AudioPayloadDTO(decrypted, mime));
            }
        }

        if (hasText(dto.getAudioUrl())
                && hasText(mensaje.getMediaUrl())
                && dto.getAudioUrl().trim().equals(mensaje.getMediaUrl().trim())) {
            byte[] bytes = readAudioFromMediaUrl(mensaje.getMediaUrl());
            if (bytes != null && bytes.length > 0) {
                String mime = normalizeAudioMime(dto.getMimeType(), mensaje.getMediaMime());
                if (looksLikeAudioBinary(bytes, mime)) {
                    return Optional.of(new AudioPayloadDTO(bytes, mime));
                }
            }
        }
        return Optional.empty();
    }

    private Optional<AudioPayloadDTO> resolveAudioFromDbMessage(MensajeEntity mensaje) {
        if (mensaje == null) return Optional.empty();
        // Priorizar mediaUrl del mensaje persistido: es la fuente mas fiable del binario de audio.
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
        // Fallback: algunos flujos pueden llevar el audio cifrado en contenido.
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

    private boolean isAudioMessage(AiEncryptedContextMessageDTO dto) {
        String tipo = normalizeInput(dto.getTipoMensaje());
        String mime = normalizeInput(dto.getMimeType());
        return "AUDIO".equalsIgnoreCase(tipo)
                || (mime != null && mime.toLowerCase(Locale.ROOT).startsWith("audio/"))
                || hasText(dto.getAudioUrl())
                || hasText(dto.getAudioEncryptedPayload());
    }

    private boolean isTextMessage(AiEncryptedContextMessageDTO dto) {
        String tipo = normalizeInput(dto.getTipoMensaje());
        return !hasText(tipo) || "TEXT".equalsIgnoreCase(tipo);
    }

    private Long resolveMessageId(AiEncryptedContextMessageDTO dto) {
        return firstNonNull(dto.getMessageId(), dto.getId());
    }

    private String resolveFecha(AiEncryptedContextMessageDTO dto) {
        return hasText(dto.getFechaEnvio()) ? dto.getFechaEnvio() : dto.getFecha();
    }

    private String normalizeAudioMime(String first, String second) {
        String mime = hasText(first) ? first : second;
        if (!hasText(mime)) return "audio/webm";
        String normalized = mime.trim().toLowerCase(Locale.ROOT);
        if ("audio/mp3".equals(normalized)) return "audio/mpeg";
        return normalized;
    }

    private boolean isAllowedAudioMime(String mime) {
        if (!hasText(mime)) return false;
        return ALLOWED_AUDIO_MIMES.contains(normalizeAudioMime(mime, null));
    }

    private <T> T firstNonNull(T first, T second) {
        return first != null ? first : second;
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
            return bytes.length > 4
                    && bytes[0] == 'O' && bytes[1] == 'g' && bytes[2] == 'g' && bytes[3] == 'S';
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

    private record AudioPayloadDTO(byte[] bytes, String mimeType) {
    }

    private record AudioContextResult(String content, boolean transcribed) {
    }

    private record PreparedSummaryContext(List<AiPlainContextMessage> messages, int totalAudiosTranscritos) {
    }

    private void validateRecipientPublicKeyIfProvided(AiEncryptedConversationSummaryRequestDTO request, Long userId) {
        String provided = normalizePublicKey(request == null ? null : request.getRecipientPublicKey());
        if (!hasText(provided)) {
            return;
        }
        String currentUserPublicKey = usuarioRepository.findById(userId)
                .map(u -> normalizePublicKey(u.getPublicKey()))
                .orElse(null);
        if (!hasText(currentUserPublicKey)) {
            throw new IllegalArgumentException("El usuario actual no tiene clave publica E2E configurada.");
        }
        if (!provided.equals(currentUserPublicKey)) {
            throw new IllegalArgumentException("recipientPublicKey no coincide con la clave publica E2E del usuario autenticado.");
        }
        String providedFp = E2EDiagnosticUtils.fingerprint12(provided);
        String currentFp = E2EDiagnosticUtils.fingerprint12(currentUserPublicKey);
        if (!hasText(providedFp) || !providedFp.equals(currentFp)) {
            throw new IllegalArgumentException("Fingerprint de recipientPublicKey invalido.");
        }
        LOGGER.info("[AI][SUMMARY_ENCRYPTED] recipient-key-validated userId={} fp={}", userId, currentFp);
    }

    private String resolvePreparedMessageType(AiEncryptedContextMessageDTO message, boolean isAudio, boolean isText) {
        if (isAudio) {
            return "AUDIO";
        }
        if (isText) {
            return "TEXT";
        }
        String tipo = normalizeInput(message == null ? null : message.getTipoMensaje());
        return hasText(tipo) ? tipo.toUpperCase(Locale.ROOT) : "MULTIMEDIA";
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
            if (mime.startsWith("audio/")) {
                return "AUDIO";
            }
            if (mime.startsWith("image/")) {
                return "IMAGE";
            }
            if (mime.startsWith("application/") || mime.startsWith("text/")) {
                return "FILE";
            }
        }
        if (mensaje.getTipo() != null) {
            return mensaje.getTipo().name();
        }
        return "TEXT";
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

    private String extractFileName(String mediaUrl) {
        String normalized = normalizeInput(mediaUrl);
        if (!hasText(normalized)) {
            return null;
        }
        int slash = normalized.lastIndexOf('/');
        return slash >= 0 && slash + 1 < normalized.length() ? normalized.substring(slash + 1) : normalized;
    }

    private AiEncryptedConversationSummaryInternalRequestDTO buildInternalRequest(String requestId,
                                                                                  AiEncryptedConversationSummaryRequestDTO request,
                                                                                  String chatType,
                                                                                  SummaryStyle style,
                                                                                  int maxLineas,
                                                                                  List<AiPlainContextMessage> decryptedMessages) {
        AiEncryptedConversationSummaryInternalRequestDTO internalRequest = new AiEncryptedConversationSummaryInternalRequestDTO();
        internalRequest.setRequestId(requestId);
        internalRequest.setChatId(request == null ? null : request.getChatId());
        internalRequest.setChatGrupalId(request == null ? null : request.getChatGrupalId());
        internalRequest.setTipoChat(chatType);
        internalRequest.setEstilo(style.name());
        internalRequest.setMaxLineas(maxLineas);

        List<AiEncryptedConversationSummaryInternalMessageDTO> internalMessages = new ArrayList<>();
        for (AiPlainContextMessage message : decryptedMessages) {
            if (message == null || !hasText(message.getContenido())) {
                continue;
            }
            AiEncryptedConversationSummaryInternalMessageDTO dto = new AiEncryptedConversationSummaryInternalMessageDTO();
            dto.setAutor(truncate(normalizeInput(message.getAutor()), MAX_AUTHOR_LENGTH));
            dto.setEsUsuarioActual(message.isEsUsuarioActual());
            dto.setContenido(truncate(normalizeInput(message.getContenido()), MAX_MESSAGE_CONTENT_LENGTH));
            dto.setTipoMensaje(normalizeInput(message.getTipoMensaje()));
            dto.setFechaEnvio(truncate(normalizeInput(message.getFecha()), MAX_DATE_LENGTH));
            internalMessages.add(dto);
        }
        internalRequest.setMensajes(internalMessages);
        return internalRequest;
    }

    private boolean isValidInternalResponse(AiEncryptedConversationSummaryInternalResponseDTO response) {
        return response != null
                && response.getCodigo() != null
                && response.getMensaje() != null
                && (!response.isSuccess() || hasText(response.getResumen()));
    }

    private String resolveServiceErrorMessage(AiEncryptedConversationSummaryInternalResponseDTO response) {
        if (response == null || !hasText(response.getMensaje())) {
            return "El microservicio de resumen IA devolvio un error.";
        }
        return response.getMensaje();
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
        builder.append("- Algunos mensajes pueden aparecer como [Audio transcrito automaticamente]. Usalos como contexto, pero no inventes detalles si la transcripcion parece incompleta o dudosa.\n");
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

    private String normalizePublicKey(String key) {
        if (!hasText(key)) {
            return null;
        }
        return key
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s", "")
                .trim();
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
