package com.chat.chat.Service.AiService;

import com.chat.chat.Configuracion.AiProperties;
import com.chat.chat.DTO.*;
import com.chat.chat.Entity.MensajeEntity;
import com.chat.chat.Entity.UsuarioEntity;
import com.chat.chat.Exceptions.RecursoNoEncontradoException;
import com.chat.chat.Repository.ChatGrupalRepository;
import com.chat.chat.Repository.ChatIndividualRepository;
import com.chat.chat.Repository.MensajeRepository;
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
import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Validated
public class DeepSeekAiQuickReplyServiceImpl implements AiQuickReplyService {
    private static final Logger LOGGER = LoggerFactory.getLogger(DeepSeekAiQuickReplyServiceImpl.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String TRANSFORM_AES_GCM = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH_BITS = 128;
    private static final int DEFAULT_MAX_MENSAJES = 20, MIN_MAX_MENSAJES = 5, MAX_MAX_MENSAJES = 50, MAX_MESSAGE_LENGTH = 250, MAX_AUTHOR_LENGTH = 80, MAX_SUGGESTION_LENGTH = 140;
    private static final long QUICK_REPLY_DAY_WINDOW_MS = Duration.ofDays(1).toMillis();
    private static final String AUDIO_TRANSCRIBED_PREFIX = "[Audio transcrito automaticamente]: ", AUDIO_NOT_ALLOWED = "[Audio no transcrito]", AUDIO_TOO_LARGE = "[Audio no transcrito]", AUDIO_PROCESSING_ERROR = "[Audio no transcrito]";
    private static final Set<String> ALLOWED_AUDIO_MIMES = Set.of("audio/webm", "audio/mpeg", "audio/mp3", "audio/wav", "audio/ogg", "audio/mp4");

    private final AiProperties aiProperties; private final AiRateLimitService aiRateLimitService; private final SecurityUtils securityUtils;
    private final ChatIndividualRepository chatIndividualRepository; private final ChatGrupalRepository chatGrupalRepository; private final MensajeRepository mensajeRepository;
    private final AiEncryptedContextService aiEncryptedContextService; private final AiQuickReplyMicroserviceClient aiQuickReplyMicroserviceClient; private final AdminAuditCrypto adminAuditCrypto;
    private final AudioTranscriptionService audioTranscriptionService; private final String uploadsRoot;
    private final Map<String, CachedQuickReply> quickReplyCache = new ConcurrentHashMap<>(); private final Map<String, Long> quickReplyCooldownByChat = new ConcurrentHashMap<>(); private final Map<Long, WindowCounter> quickReplyDailyUsage = new ConcurrentHashMap<>();

    public DeepSeekAiQuickReplyServiceImpl(AiProperties aiProperties, AiRateLimitService aiRateLimitService, SecurityUtils securityUtils, ChatIndividualRepository chatIndividualRepository, ChatGrupalRepository chatGrupalRepository, MensajeRepository mensajeRepository, AiEncryptedContextService aiEncryptedContextService, AiQuickReplyMicroserviceClient aiQuickReplyMicroserviceClient, AdminAuditCrypto adminAuditCrypto, AudioTranscriptionService audioTranscriptionService, @Value(Constantes.PROP_UPLOADS_ROOT) String uploadsRoot) {
        this.aiProperties = aiProperties; this.aiRateLimitService = aiRateLimitService; this.securityUtils = securityUtils; this.chatIndividualRepository = chatIndividualRepository; this.chatGrupalRepository = chatGrupalRepository; this.mensajeRepository = mensajeRepository; this.aiEncryptedContextService = aiEncryptedContextService; this.aiQuickReplyMicroserviceClient = aiQuickReplyMicroserviceClient; this.adminAuditCrypto = adminAuditCrypto; this.audioTranscriptionService = audioTranscriptionService; this.uploadsRoot = uploadsRoot;
    }

    @Override
    public AiQuickReplyResponseDTO generarSugerencias(AiQuickReplyRequestDTO request) {
        Long userId = securityUtils.getAuthenticatedUserId(); String requestId = UUID.randomUUID().toString();
        if (!aiProperties.isEnabled()) return failure("AI_DISABLED", "La ayuda de IA no esta habilitada.");
        if (!"deepseek".equalsIgnoreCase(aiProperties.getProvider())) return failure("AI_PROVIDER_NOT_SUPPORTED", "El proveedor de IA configurado no es compatible.");
        if (!adminAuditCrypto.hasPrivateKeyConfigured()) return failure("AI_ADMIN_PRIVATE_KEY_MISSING", "No esta configurada la clave privada de auditoria para generar respuestas rapidas.");
        if (!adminAuditCrypto.hasMatchingPrivateKeyForAuditPublicKey()) return failure("AI_ADMIN_PRIVATE_KEY_MISMATCH", "La clave privada de auditoria configurada no corresponde a la audit public key actual.");
        String chatType = normalizeChatType(request == null ? null : request.getTipoChat()); if (chatType == null) return failure("AI_QUICK_REPLY_CHAT_TYPE_INVALID", "El tipo de chat no es valido.");
        int maxMensajes; try { maxMensajes = resolveMaxMensajes(request == null ? null : request.getMaxMensajes()); } catch (IllegalArgumentException ex) { return failure("AI_QUICK_REPLY_INVALID_REQUEST", ex.getMessage()); }
        ChatScope scope; try { scope = validateChatAccess(request, userId, chatType); } catch (IllegalArgumentException ex) { return failure("AI_QUICK_REPLY_INVALID_REQUEST", ex.getMessage()); } catch (RecursoNoEncontradoException ex) { return failure("AI_QUICK_REPLY_CHAT_NOT_FOUND", ex.getMessage()); }
        List<MensajeEntity> databaseMessages = loadRecentChatMessages(scope.chatType(), scope.chatId(), scope.chatGrupalId(), maxMensajes);
        if (databaseMessages.isEmpty()) return success(List.of(), null, "No hay mensajes suficientes para generar sugerencias.");
        MensajeEntity lastMessage = databaseMessages.get(databaseMessages.size() - 1); boolean ultimoMensajeEsUsuarioActual = lastMessage.getEmisor() != null && Objects.equals(lastMessage.getEmisor().getId(), userId); String ultimoMensajeTipo = resolveTipoMensaje(lastMessage);
        if (ultimoMensajeEsUsuarioActual) return success(List.of(), null, "No se generan sugerencias si el ultimo mensaje es tuyo.");
        String cacheKey = buildCacheKey(userId, lastMessage.getId()); CachedQuickReply cached = cacheKey == null ? null : quickReplyCache.get(cacheKey); if (cached != null) return successEncrypted(userId, cached.sugerencias(), "Sugerencias obtenidas de cache");
        String chatScopeKey = resolveChatScopeKey(scope.chatType(), scope.chatId(), scope.chatGrupalId()); QuickReplyLimitCheck quickReplyLimitCheck = checkQuickReplySpecificLimits(userId, chatScopeKey); if (!quickReplyLimitCheck.allowed()) return failure(quickReplyLimitCheck.code(), quickReplyLimitCheck.message());
        AiRateLimitCheck rateLimitCheck = aiRateLimitService.checkUsage(userId); if (!rateLimitCheck.isAllowed()) return failure(rateLimitCheck.getCode(), rateLimitCheck.getMessage());
        PreparedQuickReplyContext preparedContext = prepareContextMessages(databaseMessages, userId);
        LOGGER.info("[AI][QUICK_REPLY] requestId={} ultimoMensajeTipo={} ultimoMensajeAudioTranscrito={} totalAudiosContexto={} totalAudiosTranscritos={} llamadaMicroservicioIA=false",
                requestId, ultimoMensajeTipo, preparedContext.ultimoMensajeAudioTranscrito(), preparedContext.totalAudiosContexto(), preparedContext.totalAudiosTranscritos());
        if (!preparedContext.ultimoMensajeValido()) return failure("AI_QUICK_REPLY_LAST_AUDIO_NOT_TRANSCRIBED", "No se pueden generar respuestas rapidas para este audio.");
        if (preparedContext.contexto().isEmpty()) return success(List.of(), null, "No hay mensajes suficientes para generar sugerencias.");
        AiQuickReplyInternalContextDTO latestPrepared = preparedContext.ultimoMensaje(); if (latestPrepared == null || !hasText(latestPrepared.getContenido())) return success(List.of(), null, "No hay contenido suficiente en el ultimo mensaje para generar sugerencias.");
        if (isControlledPlaceholderOnly(latestPrepared)) return success(List.of(), null, "No se generan sugerencias para el ultimo mensaje multimedia sin texto o transcripcion.");
        if (latestPrepared.isEsUsuarioActual()) return success(List.of(), null, "No se generan sugerencias si el ultimo mensaje es tuyo.");
        try {
            LOGGER.info("[AI][QUICK_REPLY] requestId={} tipoChat={} chatId={} chatGrupalId={} totalMensajesCargados={} ultimoMensajeEsUsuarioActual={} ultimoMensajeTipo={} ultimoMensajeAudioTranscrito={} totalAudiosContexto={} totalAudiosTranscritos={} llamadaMicroservicioIA=true",
                    requestId, scope.chatType(), scope.chatId(), scope.chatGrupalId(), databaseMessages.size(), false, ultimoMensajeTipo, preparedContext.ultimoMensajeAudioTranscrito(), preparedContext.totalAudiosContexto(), preparedContext.totalAudiosTranscritos());
            AiQuickReplyInternalResponseDTO internalResponse = aiQuickReplyMicroserviceClient.generarSugerencias(requestId, buildInternalRequest(requestId, scope, latestPrepared.getContenido(), preparedContext.contexto()));
            if (!isValidInternalResponse(internalResponse)) return failure("AI_QUICK_REPLY_INVALID_SERVICE_RESPONSE", "La respuesta interna del servicio de IA no es valida.");
            if (!internalResponse.isSuccess()) return failure("AI_QUICK_REPLY_SERVICE_ERROR", resolveServiceErrorMessage(internalResponse));
            List<String> suggestions = sanitizeSuggestions(internalResponse.getSugerencias()); if (suggestions.isEmpty()) return success(List.of(), null, "La IA no devolvio sugerencias validas.");
            aiRateLimitService.registrarUso(userId); registerQuickReplyGeneration(userId, chatScopeKey, cacheKey, suggestions); return successEncrypted(userId, suggestions, "Sugerencias generadas correctamente");
        } catch (AiQuickReplyMicroserviceUnavailableException ex) { return failure("AI_QUICK_REPLY_SERVICE_UNAVAILABLE", "El microservicio de respuestas rapidas no esta disponible temporalmente."); } catch (AiQuickReplyMicroserviceException ex) { return failure("AI_QUICK_REPLY_SERVICE_ERROR", "El microservicio de respuestas rapidas devolvio un error."); }
    }

    private ChatScope validateChatAccess(AiQuickReplyRequestDTO request, Long userId, String chatType) {
        Long chatId = request == null ? null : request.getChatId(), chatGrupalId = request == null ? null : request.getChatGrupalId();
        if (Constantes.CHAT_TIPO_INDIVIDUAL.equals(chatType)) {
            if (chatId == null) throw new IllegalArgumentException("chatId es obligatorio para chats individuales.");
            if (chatGrupalId != null) throw new IllegalArgumentException("chatGrupalId no aplica a chats individuales.");
            if (!chatIndividualRepository.existsMemberByChatIdAndUserId(chatId, userId)) { if (chatIndividualRepository.existsById(chatId)) throw new IllegalArgumentException(Constantes.MSG_NO_PERTENECE_CHAT); throw new RecursoNoEncontradoException(Constantes.MSG_CHAT_NO_ENCONTRADO_ID + chatId); }
            return new ChatScope(chatType, chatId, null);
        }
        if (chatGrupalId == null) throw new IllegalArgumentException("chatGrupalId es obligatorio para chats grupales.");
        if (chatId != null) throw new IllegalArgumentException("chatId no aplica a chats grupales.");
        if (!chatGrupalRepository.existsActiveMemberByChatIdAndUserId(chatGrupalId, userId)) { if (chatGrupalRepository.existsById(chatGrupalId)) throw new IllegalArgumentException(Constantes.MSG_NO_PERTENECE_GRUPO); throw new RecursoNoEncontradoException(Constantes.MSG_CHAT_GRUPAL_NO_ENCONTRADO_ID + chatGrupalId); }
        return new ChatScope(chatType, null, chatGrupalId);
    }

    private int resolveMaxMensajes(Integer requestedMaxMensajes) { if (requestedMaxMensajes == null) return DEFAULT_MAX_MENSAJES; if (requestedMaxMensajes < MIN_MAX_MENSAJES || requestedMaxMensajes > MAX_MAX_MENSAJES) throw new IllegalArgumentException("maxMensajes debe estar entre 5 y 50."); return requestedMaxMensajes; }
    private List<MensajeEntity> loadRecentChatMessages(String chatType, Long chatId, Long chatGrupalId, int maxMensajes) { List<MensajeEntity> loaded = Constantes.CHAT_TIPO_INDIVIDUAL.equals(chatType) ? mensajeRepository.findLatestVisibleMessagesByChatIndividualId(chatId, PageRequest.of(0, maxMensajes)) : mensajeRepository.findLatestVisibleMessagesByChatGrupalId(chatGrupalId, PageRequest.of(0, maxMensajes)); if (loaded == null || loaded.isEmpty()) return List.of(); List<MensajeEntity> ordered = new ArrayList<>(loaded); Collections.reverse(ordered); return ordered; }
    private PreparedQuickReplyContext prepareContextMessages(List<MensajeEntity> messages, Long userId) {
        List<AiQuickReplyInternalContextDTO> prepared = new ArrayList<>();
        AiQuickReplyInternalContextDTO ultimoMensaje = null;
        int totalAudiosContexto = 0;
        int totalAudiosTranscritos = 0;
        boolean ultimoMensajeValido = true;
        boolean ultimoMensajeAudioTranscrito = false;

        for (int i = 0; i < messages.size(); i++) {
            MensajeEntity message = messages.get(i);
            if (message == null) continue;
            boolean esUltimoMensaje = i == messages.size() - 1;
            String tipoMensaje = resolveTipoMensaje(message);
            AudioResolution audioResolution = resolveContextContent(message, esUltimoMensaje);
            if (Constantes.TIPO_AUDIO.equals(tipoMensaje)) {
                totalAudiosContexto++;
                if (audioResolution.audioTranscrito()) totalAudiosTranscritos++;
                if (esUltimoMensaje) ultimoMensajeAudioTranscrito = audioResolution.audioTranscrito();
            }
            String contenido = normalizeInput(audioResolution.contenido());
            if (esUltimoMensaje && !audioResolution.validoParaUltimoMensaje()) {
                ultimoMensajeValido = false;
            }
            if (!hasText(contenido)) continue;
            AiQuickReplyInternalContextDTO dto = new AiQuickReplyInternalContextDTO();
            dto.setAutor(displayName(message.getEmisor()));
            dto.setAutorId(message.getEmisor() == null ? null : message.getEmisor().getId());
            dto.setEsUsuarioActual(message.getEmisor() != null && Objects.equals(message.getEmisor().getId(), userId));
            dto.setTipoMensaje(tipoMensaje);
            dto.setContenido(truncate(contenido, MAX_MESSAGE_LENGTH));
            dto.setFechaEnvio(formatDate(message));
            prepared.add(dto);
            ultimoMensaje = dto;
        }
        return new PreparedQuickReplyContext(prepared, ultimoMensaje, totalAudiosContexto, totalAudiosTranscritos, ultimoMensajeValido, ultimoMensajeAudioTranscrito);
    }

    private AudioResolution resolveContextContent(MensajeEntity message, boolean strictForLastMessage) {
        String tipoMensaje = resolveTipoMensaje(message);
        if (Constantes.TIPO_AUDIO.equals(tipoMensaje)) return resolveAudioContextFromDb(message, strictForLastMessage);
        if (Constantes.TIPO_TEXT.equals(tipoMensaje)) return AudioResolution.ok(resolveTextContentFromDb(message), false);
        return AudioResolution.ok(switch (tipoMensaje) {
            case Constantes.TIPO_IMAGE -> "[Imagen]";
            case Constantes.TIPO_STICKER -> "[Sticker]";
            case Constantes.TIPO_FILE -> "[Archivo]";
            case Constantes.TIPO_VIDEO -> "[Video]";
            default -> "[Mensaje multimedia]";
        }, false);
    }
    private String resolveTextContentFromDb(MensajeEntity message) { String contenido = normalizeInput(message == null ? null : message.getContenido()); String plain = null; if (hasText(contenido)) { plain = normalizeInput(aiEncryptedContextService.decryptMessagePayload(contenido)); if (!hasText(plain)) plain = contenido; } return !hasText(plain) || isNonTextContent(plain) ? null : plain; }
    private AudioResolution resolveAudioContextFromDb(MensajeEntity mensaje, boolean strictForLastMessage) {
        try {
            Optional<AudioPayloadDTO> maybeAudioPayload = resolveAudioFromDbMessage(mensaje);
            if (maybeAudioPayload.isEmpty()) return strictForLastMessage ? AudioResolution.invalidLastAudio() : AudioResolution.ok(AUDIO_PROCESSING_ERROR, false);
            AudioPayloadDTO audioPayload = maybeAudioPayload.get();
            if (!isAllowedAudioMime(audioPayload.mimeType())) return strictForLastMessage ? AudioResolution.invalidLastAudio() : AudioResolution.ok(AUDIO_NOT_ALLOWED, false);
            if (audioPayload.bytes().length > aiProperties.getAudioTranscription().getMaxAudioSizeBytes()) return strictForLastMessage ? AudioResolution.invalidLastAudio() : AudioResolution.ok(AUDIO_TOO_LARGE, false);
            AudioTranscriptionResultDTO transcription = audioTranscriptionService.transcribirAudio(mensaje, audioPayload.bytes(), audioPayload.mimeType());
            if (transcription == null || !transcription.isSuccess() || !hasText(transcription.getTranscripcion())) {
                return strictForLastMessage ? AudioResolution.invalidLastAudio() : AudioResolution.ok("[Audio no transcrito]", false);
            }
            return AudioResolution.ok(AUDIO_TRANSCRIBED_PREFIX + transcription.getTranscripcion(), true);
        } catch (RuntimeException ex) {
            return strictForLastMessage ? AudioResolution.invalidLastAudio() : AudioResolution.ok(AUDIO_PROCESSING_ERROR, false);
        }
    }
    private Optional<AudioPayloadDTO> resolveAudioFromDbMessage(MensajeEntity mensaje) { if (mensaje == null) return Optional.empty(); if (hasText(mensaje.getMediaUrl())) { byte[] bytes = readAudioFromMediaUrl(mensaje.getMediaUrl()); if (bytes != null && bytes.length > 0) { String mime = normalizeAudioMime(mensaje.getMediaMime(), readContentAudioMime(mensaje.getContenido())); if (looksLikeAudioBinary(bytes, mime)) return Optional.of(new AudioPayloadDTO(bytes, mime)); byte[] decrypted = decryptAudioFilePayload(bytes, mensaje.getContenido()); if (decrypted != null && decrypted.length > 0) return Optional.of(new AudioPayloadDTO(decrypted, mime)); } } String contentAudioUrl = readContentAudioUrl(mensaje.getContenido()); if (hasText(contentAudioUrl)) { byte[] bytes = readAudioFromMediaUrl(contentAudioUrl); if (bytes != null && bytes.length > 0) { String mime = normalizeAudioMime(readContentAudioMime(mensaje.getContenido()), mensaje.getMediaMime()); if (looksLikeAudioBinary(bytes, mime)) return Optional.of(new AudioPayloadDTO(bytes, mime)); byte[] decrypted = decryptAudioFilePayload(bytes, mensaje.getContenido()); if (decrypted != null && decrypted.length > 0) return Optional.of(new AudioPayloadDTO(decrypted, mime)); } } String contenido = normalizeInput(mensaje.getContenido()); if (hasText(contenido)) { byte[] decrypted = aiEncryptedContextService.decryptMessagePayloadToBytes(contenido); if (decrypted != null && decrypted.length > 0) return Optional.of(new AudioPayloadDTO(decrypted, normalizeAudioMime(mensaje.getMediaMime(), null))); } return Optional.empty(); }
    private byte[] readAudioFromMediaUrl(String mediaUrl) { if (!hasText(mediaUrl) || !mediaUrl.startsWith(Constantes.UPLOADS_PREFIX)) return null; try { String relative = mediaUrl.substring(Constantes.UPLOADS_PREFIX.length()); Path root = Paths.get(uploadsRoot).toAbsolutePath().normalize(); Path file = root.resolve(relative).normalize(); if (!file.startsWith(root) || !Files.exists(file) || !Files.isRegularFile(file)) return null; return Files.readAllBytes(file); } catch (Exception ex) { return null; } }
    private String readContentAudioUrl(String contenido) { return readContentTextField(contenido, "audioUrl"); }
    private String readContentAudioMime(String contenido) { return readContentTextField(contenido, "audioMime"); }
    private String readContentIvFile(String contenido) { return readContentTextField(contenido, "ivFile"); }
    private String readContentAdminEnvelope(String contenido) { return readContentTextField(contenido, "forAdmin"); }
    private String readContentTextField(String contenido, String fieldName) { if (!hasText(contenido) || !hasText(fieldName)) return null; try { JsonNode root = OBJECT_MAPPER.readTree(contenido); String value = normalizeInput(root.path(fieldName).asText(null)); return hasText(value) ? value : null; } catch (Exception ex) { return null; } }
    private byte[] decryptAudioFilePayload(byte[] encryptedBytes, String contenido) { if (encryptedBytes == null || encryptedBytes.length == 0 || !hasText(contenido)) return null; String ivFile = readContentIvFile(contenido), adminEnvelope = readContentAdminEnvelope(contenido); if (!hasText(ivFile) || !hasText(adminEnvelope)) return null; try { byte[] adminEnvelopeBytes = adminAuditCrypto.decryptBase64EnvelopeBytes(adminEnvelope); byte[] aesKey = resolveAesKey(adminEnvelopeBytes); byte[] iv = Base64.getDecoder().decode(ivFile); Cipher cipher = Cipher.getInstance(TRANSFORM_AES_GCM); cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(aesKey, "AES"), new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv)); return cipher.doFinal(encryptedBytes); } catch (Exception ex) { return null; } }
    private byte[] resolveAesKey(byte[] adminEnvelopeBytes) { if (adminEnvelopeBytes == null || adminEnvelopeBytes.length == 0) return null; if (isValidAesKeyLength(adminEnvelopeBytes.length)) return adminEnvelopeBytes; try { byte[] decoded = Base64.getDecoder().decode(new String(adminEnvelopeBytes, StandardCharsets.UTF_8)); return isValidAesKeyLength(decoded.length) ? decoded : null; } catch (IllegalArgumentException ex) { return null; } }
    private boolean isValidAesKeyLength(int len) { return len == 16 || len == 24 || len == 32; }
    private boolean looksLikeAudioBinary(byte[] bytes, String mime) { if (bytes == null || bytes.length < 4) return false; String m = normalizeAudioMime(mime, null); if ("audio/webm".equals(m)) return bytes.length > 4 && (bytes[0] & 0xFF) == 0x1A && (bytes[1] & 0xFF) == 0x45 && (bytes[2] & 0xFF) == 0xDF && (bytes[3] & 0xFF) == 0xA3; if ("audio/ogg".equals(m)) return bytes[0] == 'O' && bytes[1] == 'g' && bytes[2] == 'g' && bytes[3] == 'S'; if ("audio/wav".equals(m)) return bytes.length > 12 && bytes[0] == 'R' && bytes[1] == 'I' && bytes[2] == 'F' && bytes[3] == 'F' && bytes[8] == 'W' && bytes[9] == 'A' && bytes[10] == 'V' && bytes[11] == 'E'; if ("audio/mpeg".equals(m)) return (bytes[0] == 'I' && bytes[1] == 'D' && bytes[2] == '3') || ((bytes[0] & 0xFF) == 0xFF && ((bytes[1] & 0xE0) == 0xE0)); if ("audio/mp4".equals(m)) return bytes.length > 12 && bytes[4] == 'f' && bytes[5] == 't' && bytes[6] == 'y' && bytes[7] == 'p'; return true; }
    private boolean isAllowedAudioMime(String mime) { return hasText(mime) && ALLOWED_AUDIO_MIMES.contains(normalizeAudioMime(mime, null)); }
    private String normalizeAudioMime(String first, String second) { String mime = hasText(first) ? first : second; if (!hasText(mime)) return "audio/webm"; String normalized = mime.trim().toLowerCase(Locale.ROOT); return "audio/mp3".equals(normalized) ? "audio/mpeg" : normalized; }
    private String resolveAudioFailureMessage(AudioTranscriptionResultDTO transcription) { if (transcription == null || !hasText(transcription.getCodigo())) return "error al procesar audio"; return switch (transcription.getCodigo()) { case "AI_AUDIO_TRANSCRIPTION_DISABLED" -> "transcripcion deshabilitada"; case "AI_AUDIO_INVALID_MIME" -> "formato no permitido"; case "AI_AUDIO_TOO_LARGE" -> "supera el tamano maximo permitido"; case "AI_AUDIO_TEMP_FILE_ERROR" -> "error temporal de archivo"; case "AI_AUDIO_TRANSCRIPTION_TIMEOUT" -> "timeout de transcripcion"; case "AI_AUDIO_TRANSCRIPTION_SCRIPT_NOT_FOUND" -> "script no encontrado"; default -> "error al procesar audio"; }; }
    private AiQuickReplyInternalRequestDTO buildInternalRequest(String requestId, ChatScope scope, String mensajeRecibido, List<AiQuickReplyInternalContextDTO> contexto) { AiQuickReplyInternalRequestDTO internalRequest = new AiQuickReplyInternalRequestDTO(); internalRequest.setRequestId(requestId); internalRequest.setTipoChat(scope.chatType()); internalRequest.setChatId(scope.chatId()); internalRequest.setChatGrupalId(scope.chatGrupalId()); internalRequest.setMensajeRecibido(mensajeRecibido); internalRequest.setContexto(contexto); return internalRequest; }
    private boolean isValidInternalResponse(AiQuickReplyInternalResponseDTO response) { return response != null && response.getCodigo() != null && response.getMensaje() != null && (!response.isSuccess() || response.getSugerencias() != null); }
    private String resolveServiceErrorMessage(AiQuickReplyInternalResponseDTO response) { return response == null || !hasText(response.getMensaje()) ? "El microservicio de respuestas rapidas devolvio un error." : response.getMensaje(); }
    private List<String> sanitizeSuggestions(List<String> suggestions) { if (suggestions == null || suggestions.isEmpty()) return List.of(); List<String> sanitized = new ArrayList<>(); for (String suggestion : suggestions) { String current = cleanupSuggestion(suggestion); if (!hasText(current)) continue; sanitized.add(current); if (sanitized.size() == 3) break; } return sanitized; }
    private String cleanupSuggestion(String value) { String normalized = normalizeInput(value); if (!hasText(normalized)) return null; normalized = normalized.replaceAll("^[-*\\d.)\\s]+", "").replaceAll("^['\"`]+|['\"`]+$", "").replaceAll("\\s{2,}", " ").trim(); return hasText(normalized) ? truncate(normalized, MAX_SUGGESTION_LENGTH) : null; }
    private QuickReplyLimitCheck checkQuickReplySpecificLimits(Long userId, String chatScopeKey) { long now = System.currentTimeMillis(); if (chatScopeKey != null) { Long lastGeneratedAt = quickReplyCooldownByChat.get(buildCooldownKey(userId, chatScopeKey)); long cooldownMs = Math.max(1, aiProperties.getQuickReplies().getCooldownSeconds()) * 1000L; if (lastGeneratedAt != null && now - lastGeneratedAt < cooldownMs) return new QuickReplyLimitCheck(false, "AI_QUICK_REPLIES_COOLDOWN", "Espera un momento antes de generar nuevas respuestas rapidas."); } WindowCounter usageCounter = quickReplyDailyUsage.computeIfAbsent(safeUserId(userId), ignored -> new WindowCounter()); synchronized (usageCounter) { usageCounter.cleanup(now, QUICK_REPLY_DAY_WINDOW_MS); if (usageCounter.size() >= aiProperties.getQuickReplies().getMaxPerUserDay()) return new QuickReplyLimitCheck(false, "AI_QUICK_REPLIES_DAILY_LIMIT", "Has alcanzado el limite diario de respuestas rapidas."); } return QuickReplyLimitCheck.ok(); }
    private void registerQuickReplyGeneration(Long userId, String chatScopeKey, String cacheKey, List<String> suggestions) { long now = System.currentTimeMillis(); WindowCounter usageCounter = quickReplyDailyUsage.computeIfAbsent(safeUserId(userId), ignored -> new WindowCounter()); synchronized (usageCounter) { usageCounter.add(now, QUICK_REPLY_DAY_WINDOW_MS); } if (chatScopeKey != null) quickReplyCooldownByChat.put(buildCooldownKey(userId, chatScopeKey), now); if (cacheKey != null && suggestions != null && !suggestions.isEmpty()) quickReplyCache.put(cacheKey, new CachedQuickReply(List.copyOf(suggestions), now)); }
    private String resolveChatScopeKey(String chatType, Long chatId, Long chatGrupalId) { if (Constantes.CHAT_TIPO_INDIVIDUAL.equals(chatType) && chatId != null) return chatType + ":" + chatId; if (Constantes.CHAT_TIPO_GRUPAL.equals(chatType) && chatGrupalId != null) return chatType + ":" + chatGrupalId; return null; }
    private String buildCacheKey(Long userId, Long messageId) { return messageId == null ? null : safeUserId(userId) + ":" + messageId; }
    private String buildCooldownKey(Long userId, String chatScopeKey) { return safeUserId(userId) + ":" + chatScopeKey; }
    private long safeUserId(Long userId) { return userId == null ? -1L : userId; }
    private boolean isControlledPlaceholderOnly(AiQuickReplyInternalContextDTO latestPrepared) { if (latestPrepared == null) return true; String tipoMensaje = normalizeInput(latestPrepared.getTipoMensaje()), contenido = normalizeInput(latestPrepared.getContenido()); if (!hasText(contenido)) return true; if (Constantes.TIPO_AUDIO.equals(tipoMensaje)) return contenido.startsWith("[") && !contenido.startsWith(AUDIO_TRANSCRIBED_PREFIX); return !Constantes.TIPO_TEXT.equals(tipoMensaje) && contenido.startsWith("[") && contenido.endsWith("]"); }
    private String resolveTipoMensaje(MensajeEntity mensaje) { if (mensaje == null) return "UNKNOWN"; if (mensaje.getStickerId() != null) return Constantes.TIPO_STICKER; if (hasText(mensaje.getMediaMime())) { String mime = mensaje.getMediaMime().trim().toLowerCase(Locale.ROOT); if (mime.startsWith("audio/")) return Constantes.TIPO_AUDIO; if (mime.startsWith("image/")) return Constantes.TIPO_IMAGE; if (mime.startsWith("video/")) return Constantes.TIPO_VIDEO; if (mime.startsWith("application/") || mime.startsWith("text/")) return Constantes.TIPO_FILE; } return mensaje.getTipo() == null ? Constantes.TIPO_TEXT : mensaje.getTipo().name(); }
    private String displayName(UsuarioEntity user) { if (user == null) return null; String nombre = normalizeInput(user.getNombre()), apellido = normalizeInput(user.getApellido()), full = ((nombre == null ? "" : nombre) + " " + (apellido == null ? "" : apellido)).trim(); if (hasText(full)) return truncate(full, MAX_AUTHOR_LENGTH); return truncate(normalizeInput(user.getEmail()), MAX_AUTHOR_LENGTH); }
    private String formatDate(MensajeEntity mensaje) { return mensaje == null || mensaje.getFechaEnvio() == null ? null : mensaje.getFechaEnvio().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME); }
    private String normalizeChatType(String chatType) { if (!hasText(chatType)) return null; String normalized = chatType.trim().toUpperCase(Locale.ROOT); return Constantes.CHAT_TIPO_INDIVIDUAL.equals(normalized) || Constantes.CHAT_TIPO_GRUPAL.equals(normalized) ? normalized : null; }
    private String normalizeInput(String text) { if (text == null) return null; String compact = text.replace("\r\n", "\n").replace('\r', '\n'); compact = compact.replaceAll("[\\t\\f\\x0B]+", " ").replaceAll(" {2,}", " ").replaceAll("\\n{2,}", " "); return compact.trim(); }
    private boolean isNonTextContent(String content) { String normalized = content.trim().toLowerCase(Locale.ROOT); return normalized.startsWith("data:image") || normalized.startsWith("data:audio") || normalized.startsWith("data:video") || normalized.startsWith("data:application"); }
    private String truncate(String text, int maxLength) { return text == null || text.length() <= maxLength ? text : text.substring(0, maxLength).trim(); }
    private boolean hasText(String value) { return value != null && !value.trim().isEmpty(); }
    private AiQuickReplyResponseDTO successEncrypted(Long userId, List<String> suggestions, String message) { String plainPayload; try { Map<String, Object> payload = new LinkedHashMap<>(); payload.put("success", true); payload.put("codigo", "OK"); payload.put("mensaje", message); payload.put("sugerencias", suggestions); plainPayload = OBJECT_MAPPER.writeValueAsString(payload); } catch (JsonProcessingException ex) { return failure("AI_RESPONSE_ENCRYPTION_ERROR", "No se pudo cifrar las sugerencias para el usuario actual."); } var encrypted = aiEncryptedContextService.encryptAiResponseForUser(plainPayload, userId); if (encrypted == null || !encrypted.isSuccess() || !hasText(encrypted.getEncryptedPayload())) return failure("AI_RESPONSE_ENCRYPTION_ERROR", "No se pudo cifrar las sugerencias para el usuario actual."); AiQuickReplyResponseDTO response = new AiQuickReplyResponseDTO(); response.setSuccess(true); response.setCodigo("OK"); response.setMensaje(message); response.setSugerencias(null); response.setEncryptedPayload(encrypted.getEncryptedPayload()); return response; }
    private AiQuickReplyResponseDTO success(List<String> suggestions, String encryptedPayload, String message) { AiQuickReplyResponseDTO response = new AiQuickReplyResponseDTO(); response.setSuccess(true); response.setCodigo("OK"); response.setMensaje(message); response.setSugerencias(suggestions); response.setEncryptedPayload(encryptedPayload); return response; }
    private AiQuickReplyResponseDTO failure(String code, String message) { AiQuickReplyResponseDTO response = new AiQuickReplyResponseDTO(); response.setSuccess(false); response.setCodigo(code); response.setMensaje(message); response.setSugerencias(List.of()); response.setEncryptedPayload(null); return response; }

    private record AudioPayloadDTO(byte[] bytes, String mimeType) {}
    private record AudioResolution(String contenido, boolean audioTranscrito, boolean validoParaUltimoMensaje) {
        private static AudioResolution ok(String contenido, boolean audioTranscrito) { return new AudioResolution(contenido, audioTranscrito, true); }
        private static AudioResolution invalidLastAudio() { return new AudioResolution(null, false, false); }
    }
    private record PreparedQuickReplyContext(List<AiQuickReplyInternalContextDTO> contexto, AiQuickReplyInternalContextDTO ultimoMensaje, int totalAudiosContexto, int totalAudiosTranscritos, boolean ultimoMensajeValido, boolean ultimoMensajeAudioTranscrito) {}
    private record CachedQuickReply(List<String> sugerencias, long createdAt) {}
    private record QuickReplyLimitCheck(boolean allowed, String code, String message) { private static QuickReplyLimitCheck ok() { return new QuickReplyLimitCheck(true, "OK", "OK"); } }
    private record ChatScope(String chatType, Long chatId, Long chatGrupalId) {}
    private static final class WindowCounter { private final Deque<Long> timestamps = new ArrayDeque<>(); private void add(long now, long windowMs) { cleanup(now, windowMs); timestamps.addLast(now); } private void cleanup(long now, long windowMs) { while (!timestamps.isEmpty() && now - timestamps.peekFirst() >= windowMs) timestamps.pollFirst(); } private int size() { return timestamps.size(); } }
}
