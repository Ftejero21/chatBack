package com.chat.chat.Service.AiService;

import com.chat.chat.DTO.AiEncryptedMessageSearchRequestDTO;
import com.chat.chat.DTO.AiEncryptedMessageSearchResponseDTO;
import com.chat.chat.DTO.AiSearchIntentInternalRequestDTO;
import com.chat.chat.DTO.AiSearchIntentInternalResponseDTO;
import com.chat.chat.DTO.AiSmartActionRequestDTO;
import com.chat.chat.DTO.AiUiCustomizationIntentInternalRequestDTO;
import com.chat.chat.DTO.AiUiCustomizationIntentInternalResponseDTO;
import com.chat.chat.DTO.AiUiCustomizationResponseDTO;
import com.chat.chat.DTO.AiSearchProgressWS;
import com.chat.chat.DTO.AiEncryptedResponseDTO;
import com.chat.chat.DTO.ProgramarMensajeRequestDTO;
import com.chat.chat.DTO.ProgramarMensajeResponseDTO;
import com.chat.chat.DTO.UiCustomizationContextDTO;
import com.chat.chat.DTO.UiCustomizationChangeDTO;
import com.chat.chat.DTO.UiCustomizationScopeDTO;
import com.chat.chat.Entity.ChatGrupalEntity;
import com.chat.chat.Entity.ChatIndividualEntity;
import com.chat.chat.Entity.UsuarioEntity;
import com.chat.chat.Exceptions.RecursoNoEncontradoException;
import com.chat.chat.Repository.ChatGrupalRepository;
import com.chat.chat.Repository.ChatIndividualRepository;
import com.chat.chat.Repository.UsuarioRepository;
import com.chat.chat.Service.MensajeProgramadoService.CifradorE2EMensajeProgramadoService;
import com.chat.chat.Service.MensajeProgramadoService.MensajeProgramadoService;
import com.chat.chat.Utils.Constantes;
import com.chat.chat.Utils.SecurityUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAdjusters;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class AiSmartActionServiceImpl implements AiSmartActionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AiSmartActionServiceImpl.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String SMART_ACTION_NEEDS_CLARIFICATION = "SMART_ACTION_NEEDS_CLARIFICATION";
    private static final Set<String> SUPPORTED_TARGETS = Set.of(
            "MESSAGES",
            "COMPLAINTS_CREATED",
            "COMPLAINTS_RECEIVED",
            "MIXED",
            "SCHEDULED_MESSAGES",
            "SCHEDULED_MESSAGE_CREATE",
            "UNREAD_MESSAGES",
            "OFFENSIVE_CONTENT_SEARCH",
            "APP_REPORT",
            "APP_REPORT_STATUS",
            "UI_CUSTOMIZATION"
    );
    private static final Set<String> ALLOWED_THEME_MODES = Set.of("LIGHT", "DARK");
    private static final Set<String> ALLOWED_UI_CONTEXT_SCOPES = Set.of("CHAT_LIST", "SIDEBAR", "CHAT_LIST_AND_SIDEBAR");
    private static final int MAX_UI_AREAS = 60;
    private static final int MAX_UI_CONTEXT_CANDIDATE_AREAS = 24;
    private static final int MAX_UI_PROPERTIES_PER_AREA = 20;
    private static final int MAX_UI_VALUE_LENGTH = 50;
    private static final Set<String> ALLOWED_UI_AREAS = Set.of(
            "SIDEBAR_NAV_PANEL", "SIDEBAR_NAV_GROUP", "SIDEBAR_NAV_BOTTOM", "SIDEBAR_NAV_ITEM",
            "SIDEBAR_NAV_ITEM_ACTIVE", "SIDEBAR_NAV_ACTIVE_INDICATOR", "SIDEBAR_NAV_LOGO", "SIDEBAR_NAV_ICON",
            "SIDEBAR_NAV_ICON_ACTIVE", "SIDEBAR_NAV_AI_ICON", "SIDEBAR_NAV_TOOLTIP", "SIDEBAR_NAV_AVATAR",
            "SIDEBAR_NAV_NOTIF_BADGE",
            "SIDEBAR_NAV_SETTINGS",
            "CHAT_LIST_PANEL", "CHAT_LIST_HEADER", "CHAT_LIST_SEARCH", "CHAT_LIST_FILTERS", "CHAT_LIST_FILTER_BUTTONS",
            "CHAT_LIST_ITEM", "CHAT_LIST_ITEM_GROUP", "CHAT_LIST_ITEM_CHILDREN", "CHAT_LIST_PREVIEW",
            "CHAT_LIST_ITEM_PREVIEW", "CHAT_LIST_ITEM_DRAFT_PREVIEW", "CHAT_LIST_ITEM_AUDIO_PREVIEW",
            "CHAT_LIST_ITEM_IMAGE_PREVIEW", "CHAT_LIST_ITEM_FILE_PREVIEW", "CHAT_LIST_ITEM_BADGES",
            "CHAT_LIST_ITEM_ACTIONS_SCOPED", "CHAT_LIST_ITEM_STATUS_PILLS", "CHAT_LIST_ITEM_NAME_SCOPED",
            "CHAT_LIST_ITEM_GROUP_NAME", "CHAT_LIST_ITEM_GROUP_PREVIEW", "CHAT_LIST_ITEM_GROUP_DRAFT_PREVIEW",
            "CHAT_LIST_ITEM_GROUP_AUDIO_PREVIEW", "CHAT_LIST_ITEM_GROUP_IMAGE_PREVIEW", "CHAT_LIST_ITEM_GROUP_FILE_PREVIEW",
            "CHAT_LIST_ITEM_GROUP_BADGES", "CHAT_LIST_ITEM_GROUP_ACTIONS", "CHAT_LIST_ITEM_GROUP_STATUS_PILLS",
            "CHAT_LIST_GROUP_PILL", "CHAT_LIST_STATUS_PILLS", "CHAT_LIST_DRAFT_PREVIEW",
            "CHAT_LIST_AUDIO_PREVIEW", "CHAT_LIST_ITEM_ACTIONS", "CHAT_LIST_ITEM_ACTIVE", "CHAT_LIST_ITEM_GROUP_ACTIVE", "CHAT_LIST_ITEM_UNREAD",
            "CHAT_LIST_FILTER_BUTTONS_ACTIVE", "CHAT_LIST_PIN_MENU", "CHAT_LIST_PIN_MENU_ITEM", "CHAT_LIST_PIN_MENU_REPORT", "CHAT_LIST_PIN_MENU_DANGER", "CHAT_LIST_PIN_TOGGLE",
            "CHAT_LIST_TITLE", "CHAT_LIST_HEADER_ACTIONS", "CHAT_LIST_HEADER_ICON_BUTTON", "CHAT_LIST_HEADER_ICON", "CHAT_LIST_HEADER_MENU", "CHAT_LIST_HEADER_MENU_ITEM", "CHAT_LIST_SCROLL", "CHAT_LIST_AVATAR",
            "CHAT_LIST_STATUS_DOT", "CHAT_LIST_ITEM_CONTENT", "CHAT_LIST_ITEM_NAME", "CHAT_LIST_NAME", "CHAT_LIST_ITEM_DATE", "CHAT_LIST_IMAGE_PREVIEW", "CHAT_LIST_FILE_PREVIEW",
            "CHAT_LIST_STICKER_PREVIEW", "CHAT_LIST_ITEM_STICKER_PREVIEW", "CHAT_LIST_ITEM_GROUP_STICKER_PREVIEW",
            "CHAT_LIST_MUTED_INDICATOR", "CHAT_LIST_FAVORITE_INDICATOR", "CHAT_LIST_CLOSED_INDICATOR",
            "CHAT_LIST_EMPTY_STATE", "CHAT_LIST_PUBLIC_PANEL", "CHAT_LIST_PUBLIC_CARD",
            "CHAT_LIST_ACTIONS_MENU", "CHAT_LIST_ACTIONS_MENU_ITEM"
    );
    private static final Set<String> ALLOWED_UI_PROPERTIES = Set.of(
            "BACKGROUND_COLOR", "TEXT_COLOR", "BORDER_COLOR", "BORDER_WIDTH", "BORDER_RADIUS", "FONT_SIZE", "HOVER_BACKGROUND_COLOR",
            "HOVER_TEXT_COLOR", "HOVER_ICON_COLOR", "WIDTH", "HEIGHT", "GAP", "FONT_WEIGHT",
            "ICON_COLOR", "ACTIVE_BACKGROUND_COLOR", "ACTIVE_TEXT_COLOR", "BADGE_COLOR",
            "PREVIEW_SENDER_TEXT_COLOR", "PREVIEW_TEXT_COLOR", "LABEL_COLOR", "SEPARATOR_COLOR", "TIME_COLOR",
            "REPORTED_BACKGROUND_COLOR", "REPORTED_TEXT_COLOR", "BLOCKED_BACKGROUND_COLOR", "BLOCKED_TEXT_COLOR",
            "OPACITY", "SHADOW", "SHADOW_PRESET"
    );
    private static final int MAX_UI_HINTS_PER_PARENT = 20;
    private static final Set<String> GROUP_ACTIVE_REPAIR_PROPERTIES = Set.of(
            "BACKGROUND_COLOR", "TEXT_COLOR", "BORDER_COLOR", "BORDER_WIDTH",
            "BORDER_RADIUS", "HOVER_BACKGROUND_COLOR", "ICON_COLOR", "PREVIEW_TEXT_COLOR"
    );
    private static final Map<String, String> GROUP_ACTIVE_COLOR_MAP = Map.ofEntries(
            Map.entry("morado", "#7c3aed"),
            Map.entry("violeta", "#7c3aed"),
            Map.entry("purpura", "#7c3aed"),
            Map.entry("blanco", "#ffffff"),
            Map.entry("amarillo", "#eab308"),
            Map.entry("verde", "#22c55e"),
            Map.entry("azul", "#2563eb"),
            Map.entry("rojo", "#ef4444"),
            Map.entry("negro", "#111827"),
            Map.entry("gris", "#6b7280"),
            Map.entry("rosa", "#ec4899"),
            Map.entry("naranja", "#f97316"),
            Map.entry("transparent", "transparent")
    );
    private static final Pattern HEX_COLOR_PATTERN = Pattern.compile("#(?:[0-9a-f]{3}|[0-9a-f]{6})\\b", Pattern.CASE_INSENSITIVE);

    private final SecurityUtils securityUtils;
    private final UsuarioRepository usuarioRepository;
    private final ChatIndividualRepository chatIndividualRepository;
    private final ChatGrupalRepository chatGrupalRepository;
    private final AiSearchIntentMicroserviceClient aiSearchIntentMicroserviceClient;
    private final AiEncryptedMessageSearchService aiEncryptedMessageSearchService;
    private final AiUiCustomizationValidationService aiUiCustomizationValidationService;
    private final AiUiCustomizationMicroserviceClient aiUiCustomizationMicroserviceClient;
    private final UiContextFilterService uiContextFilterService;
    private final SimpMessagingTemplate messagingTemplate;
    private final AiSmartActionHistoryService aiSmartActionHistoryService;
    private final AiEncryptedContextService aiEncryptedContextService;
    private final MensajeProgramadoService mensajeProgramadoService;
    private final CifradorE2EMensajeProgramadoService cifradorE2EMensajeProgramadoService;

    @Autowired
    public AiSmartActionServiceImpl(SecurityUtils securityUtils,
                                    UsuarioRepository usuarioRepository,
                                    ChatIndividualRepository chatIndividualRepository,
                                    ChatGrupalRepository chatGrupalRepository,
                                    AiSearchIntentMicroserviceClient aiSearchIntentMicroserviceClient,
                                    AiEncryptedMessageSearchService aiEncryptedMessageSearchService,
                                    AiUiCustomizationValidationService aiUiCustomizationValidationService,
                                    AiUiCustomizationMicroserviceClient aiUiCustomizationMicroserviceClient,
                                    UiContextFilterService uiContextFilterService,
                                    SimpMessagingTemplate messagingTemplate,
                                    AiSmartActionHistoryService aiSmartActionHistoryService,
                                    AiEncryptedContextService aiEncryptedContextService,
                                    MensajeProgramadoService mensajeProgramadoService,
                                    CifradorE2EMensajeProgramadoService cifradorE2EMensajeProgramadoService) {
        this.securityUtils = securityUtils;
        this.usuarioRepository = usuarioRepository;
        this.chatIndividualRepository = chatIndividualRepository;
        this.chatGrupalRepository = chatGrupalRepository;
        this.aiSearchIntentMicroserviceClient = aiSearchIntentMicroserviceClient;
        this.aiEncryptedMessageSearchService = aiEncryptedMessageSearchService;
        this.aiUiCustomizationValidationService = aiUiCustomizationValidationService;
        this.aiUiCustomizationMicroserviceClient = aiUiCustomizationMicroserviceClient;
        this.uiContextFilterService = uiContextFilterService;
        this.messagingTemplate = messagingTemplate;
        this.aiSmartActionHistoryService = aiSmartActionHistoryService;
        this.aiEncryptedContextService = aiEncryptedContextService;
        this.mensajeProgramadoService = mensajeProgramadoService;
        this.cifradorE2EMensajeProgramadoService = cifradorE2EMensajeProgramadoService;
    }

    public AiSmartActionServiceImpl(SecurityUtils securityUtils,
                                    UsuarioRepository usuarioRepository,
                                    AiSearchIntentMicroserviceClient aiSearchIntentMicroserviceClient,
                                    AiEncryptedMessageSearchService aiEncryptedMessageSearchService,
                                    AiUiCustomizationValidationService aiUiCustomizationValidationService,
                                    AiUiCustomizationMicroserviceClient aiUiCustomizationMicroserviceClient,
                                    SimpMessagingTemplate messagingTemplate,
                                    AiSmartActionHistoryService aiSmartActionHistoryService) {
        this(securityUtils,
                usuarioRepository,
                null,
                null,
                aiSearchIntentMicroserviceClient,
                aiEncryptedMessageSearchService,
                aiUiCustomizationValidationService,
                aiUiCustomizationMicroserviceClient,
                new UiContextFilterService(new ObjectMapper()),
                messagingTemplate,
                aiSmartActionHistoryService,
                null,
                null,
                null);
    }

    @Override
    public Object process(AiSmartActionRequestDTO request) {
        Long userId = securityUtils.getAuthenticatedUserId();
        if (userId == null) {
            return buildUiFailure("UI_CUSTOMIZATION_UNAUTHORIZED", "Usuario no autenticado");
        }

        String requestId = UUID.randomUUID().toString();
        String consultaRaw = request == null ? null : request.getConsulta();
        boolean encryptedInput = looksLikeEncryptedPayload(safeTrim(consultaRaw));
        String consulta = resolveConsultaForAi(consultaRaw);
        if (encryptedInput && !hasText(consulta)) {
            AiEncryptedMessageSearchResponseDTO response = new AiEncryptedMessageSearchResponseDTO();
            response.setSuccess(false);
            response.setCodigo("AI_SMART_ACTION_ENCRYPTED_INPUT_INVALID");
            response.setMensaje("No se pudo descifrar la consulta cifrada.");
            response.setTarget("SCHEDULED_MESSAGE_CREATE");
            response.setAction("NEEDS_CLARIFICATION");
            response.setNeedsClarification(Boolean.TRUE);
            response.setScheduledMissingFields(List.of());
            return response;
        }
        LOGGER.info("[AI][SMART_ACTION] requestId={} consultaLength={} decrypted={}",
                requestId, consulta == null ? 0 : consulta.length(), !Objects.equals(consultaRaw, consulta));
        LOGGER.info("[AI][SMART_ACTION_ROUTE_START] requestId={}", requestId);

        AiSearchIntentInternalRequestDTO intentRequest = new AiSearchIntentInternalRequestDTO();
        intentRequest.setRequestId(requestId);
        intentRequest.setConsulta(consulta);
        intentRequest.setUsuarioActualNombre(resolveUserDisplayName(userId));
        UiCustomizationContextDTO sanitizedUiContext = sanitizeUiContext(requestId, consulta, request == null ? null : request.getUiContext());
        intentRequest.setUiContext(sanitizedUiContext);

        AiSearchIntentInternalResponseDTO intentResponse = aiSearchIntentMicroserviceClient.classifyIntent(requestId, intentRequest);
        String normalizedTarget = normalizeTarget(intentResponse == null ? null : intentResponse.getTarget());
        String aiAction = normalizeUpper(intentResponse == null ? null : intentResponse.getAction());
        LOGGER.info("[AI][SMART_ACTION_ROUTE_AI_RESULT] requestId={} target={} action={}",
                requestId, normalizedTarget, aiAction);
        LOGGER.info("[AI][SMART_ACTION_ROUTE_LIGHT] target={}", normalizedTarget);
        boolean forcedUiTarget = shouldForceUiCustomization(consulta);
        String routeReason = "AI_RESULT";
        if (forcedUiTarget) {
            String previousTarget = normalizedTarget;
            LOGGER.info("[AI][SMART_ACTION][UI_FORCE] reason=visual_intent_detected requestId={}", requestId);
            normalizedTarget = "UI_CUSTOMIZATION";
            routeReason = "VISUAL_INTENT_PRIORITY";
            if (intentResponse == null) {
                intentResponse = new AiSearchIntentInternalResponseDTO();
                intentResponse.setSuccess(true);
                intentResponse.setCodigo("OK");
            }
            intentResponse.setTarget("UI_CUSTOMIZATION");
            if (!"UPDATE_STYLE_GROUP".equals(normalizeUpper(intentResponse.getAction()))
                    && !"UPDATE_STYLE_MULTI".equals(normalizeUpper(intentResponse.getAction()))
                    && !"UPDATE_STYLE".equals(normalizeUpper(intentResponse.getAction()))
                    && !"RESET_THEME".equals(normalizeUpper(intentResponse.getAction()))) {
                intentResponse.setAction("UPDATE_STYLE_MULTI");
            }
            if (intentResponse.getConfidence() == null || intentResponse.getConfidence() < 0.75d) {
                intentResponse.setConfidence(0.85d);
            }
            if (!"UI_CUSTOMIZATION".equals(previousTarget)) {
                LOGGER.info("[AI][SMART_ACTION_ROUTE_DETERMINISTIC_OVERRIDE] requestId={} from={} to={} reason={}",
                        requestId, previousTarget, "UI_CUSTOMIZATION", "VISUAL_INTENT_PRIORITY");
            }
        }
        LOGGER.info("[AI][SMART_ACTION_INTENT] requestId={} target={} confidence={}",
                requestId, normalizedTarget, intentResponse == null ? null : intentResponse.getConfidence());
        if ("UI_CUSTOMIZATION".equals(normalizedTarget)) {
            LOGGER.info("[AI][SMART_INTENT][UI_HINTS] requestId={} uiAreaHint={} uiPropertyHint={} uiValueHint={} uiExpansionMode={} confidence={}",
                    requestId,
                    intentResponse == null ? null : intentResponse.getUiAreaHint(),
                    intentResponse == null ? null : intentResponse.getUiPropertyHint(),
                    intentResponse == null ? null : intentResponse.getUiValueHint(),
                    intentResponse == null ? null : intentResponse.getUiExpansionMode(),
                    intentResponse == null ? null : intentResponse.getConfidence());
        }

        AiEncryptedMessageSearchResponseDTO clarificationResponse = resolveSmartActionClarification(consulta, intentResponse);
        if (clarificationResponse != null) {
            LOGGER.info("[AI][SMART_ACTION_ROUTE_FINAL] requestId={} target={} reason={}",
                    requestId, clarificationResponse.getTarget(), "NEEDS_CLARIFICATION");
            logResponse(requestId, clarificationResponse);
            saveSmartActionHistory(userId, consulta, clarificationResponse, clarificationResponse.getTarget(),
                    clarificationResponse.getAction(), clarificationResponse.getClarificationReason(),
                    null, null, null, null);
            return clarificationResponse;
        }

        AiEncryptedMessageSearchRequestDTO searchRequest = buildSearchRequest(request);
        if (intentResponse == null || !intentResponse.isSuccess() || !SUPPORTED_TARGETS.contains(normalizedTarget)) {
            LOGGER.info("[AI][SMART_ACTION_ROUTE_FINAL] requestId={} target={} reason={}",
                    requestId, "MESSAGES", forcedUiTarget ? routeReason : "LEGACY_FALLBACK");
            LOGGER.info("[AI][SMART_ACTION_ROUTING] requestId={} branch=LEGACY_FALLBACK", requestId);
            Object response = aiEncryptedMessageSearchService.buscarMensajes(searchRequest);
            logResponse(requestId, response);
            saveSmartActionHistory(userId, consulta, response, "MESSAGES", null,
                    intentResponse == null ? null : intentResponse.getClarificationReason(),
                    intentResponse == null ? null : intentResponse.getLabel(),
                    intentResponse == null ? null : intentResponse.getArea(),
                    intentResponse == null ? null : intentResponse.getProperty(),
                    intentResponse == null ? null : intentResponse.getValue());
            return response;
        }

        intentResponse.setTarget(normalizedTarget);
        LOGGER.info("[AI][SMART_ACTION_ROUTE_FINAL] requestId={} target={} reason={}", requestId, normalizedTarget, routeReason);
        LOGGER.info("[AI][SMART_ACTION_ROUTING] requestId={} branch={}", requestId, normalizedTarget);

        Object response;
        if ("SCHEDULED_MESSAGE_CREATE".equals(normalizedTarget)) {
            response = processScheduledMessageCreateIntent(userId, consulta, intentResponse);
        } else if ("UI_CUSTOMIZATION".equals(normalizedTarget)) {
            response = processDedicatedUiCustomization(requestId, userId, consulta, intentResponse, sanitizedUiContext);
        } else {
            response = aiEncryptedMessageSearchService.buscarMensajes(searchRequest, requestId, intentResponse, true);
        }

        logResponse(requestId, response);
        saveSmartActionHistory(userId, consulta, response, normalizedTarget,
                intentResponse.getAction(), intentResponse.getClarificationReason(),
                intentResponse.getLabel(), intentResponse.getArea(),
                intentResponse.getProperty(), intentResponse.getValue());
        return response;
    }

    private AiEncryptedMessageSearchResponseDTO processScheduledMessageCreateIntent(Long userId,
                                                                                     String consulta,
                                                                                     AiSearchIntentInternalResponseDTO intentResponse) {
        List<String> missingFields = normalizeScheduledMissingFields(intentResponse);
        if (!missingFields.isEmpty()) {
            return buildScheduledNeedsClarification(missingFields, intentResponse == null ? null : intentResponse.getClarificationQuestion());
        }

        String recipientName = safeTrim(intentResponse == null ? null : intentResponse.getScheduledRecipientName());
        String correctedMessage = safeTrim(intentResponse == null ? null : intentResponse.getScheduledMessageTextCorrected());
        String rawMessage = safeTrim(intentResponse == null ? null : intentResponse.getScheduledMessageText());
        String message = hasText(correctedMessage) ? correctedMessage : rawMessage;
        String dateIso = safeTrim(intentResponse == null ? null : intentResponse.getScheduledDateTimeIso());
        String timezone = safeTrim(intentResponse == null ? null : intentResponse.getScheduledTimezone());
        if (!hasText(timezone)) {
            timezone = "Europe/Madrid";
        }

        List<String> computedMissing = new ArrayList<>();
        if (!hasText(recipientName)) {
            computedMissing.add("RECIPIENT");
        }
        if (!hasText(message)) {
            computedMissing.add("MESSAGE");
        }
        if (!hasText(dateIso)) {
            String fallbackDateIso = resolveScheduledDateTimeFallback(
                    safeTrim(intentResponse == null ? null : intentResponse.getScheduledDateTimeExpression()),
                    consulta,
                    timezone
            );
            if (hasText(fallbackDateIso)) {
                dateIso = fallbackDateIso;
            } else {
                computedMissing.add("DATETIME");
            }
        }
        if (!computedMissing.isEmpty()) {
            return buildScheduledNeedsClarification(computedMissing, null);
        }

        ResolvedScheduledChat resolvedChat = resolveScheduledChat(userId, recipientName,
                safeTrim(intentResponse == null ? null : intentResponse.getScheduledTargetType()));
        if (resolvedChat == null) {
            return buildScheduledNeedsClarification(List.of("RECIPIENT"),
                    "No encontré un chat con ese nombre. ¿A quién quieres enviarlo exactamente?");
        }
        if (resolvedChat.ambiguous()) {
            return buildScheduledNeedsClarification(List.of("RECIPIENT"),
                    "He encontrado varios chats llamados " + recipientName + ". ¿A cuál quieres enviarlo?");
        }

        String scheduledAtUtc = toUtcIsoOrNull(dateIso, timezone);
        if (!hasText(scheduledAtUtc)) {
            return buildScheduledNeedsClarification(List.of("DATETIME"),
                    "¿Para qué día y hora quieres programar el mensaje?");
        }
        if (isPastDateTime(scheduledAtUtc)) {
            return buildScheduledInvalidDate();
        }

        String scheduledContent = buildScheduledE2EPayload(userId, resolvedChat, message, Instant.parse(scheduledAtUtc));
        if (!hasText(scheduledContent)) {
            return buildScheduledBackendEncryptionNotAvailable();
        }

        ProgramarMensajeRequestDTO createRequest = new ProgramarMensajeRequestDTO();
        createRequest.setChatIds(List.of(resolvedChat.chatId()));
        createRequest.setContenido(scheduledContent);
        createRequest.setMessage(message);
        createRequest.setScheduledAt(Instant.parse(scheduledAtUtc));

        ProgramarMensajeResponseDTO createResponse;
        try {
            createResponse = mensajeProgramadoService.crearMensajesProgramados(createRequest);
        } catch (RuntimeException ex) {
            LOGGER.warn("[AI][SMART_ACTION][SCHEDULED_CREATE_FAIL] errorClass={}", ex.getClass().getSimpleName());
            return buildScheduledBackendEncryptionNotAvailable();
        }
        String scheduledBatchId = createResponse == null ? null : createResponse.getScheduledBatchId();

        String encryptedScheduledConfirmation = buildEncryptedScheduledConfirmation(
                userId,
                resolvedChat,
                message,
                normalizeForSearch(message),
                scheduledAtUtc,
                scheduledBatchId);
        if (!hasText(encryptedScheduledConfirmation)) {
            return buildScheduledBackendEncryptionNotAvailable();
        }

        AiEncryptedMessageSearchResponseDTO response = new AiEncryptedMessageSearchResponseDTO();
        response.setSuccess(true);
        response.setCodigo("SCHEDULED_MESSAGE_CREATED");
        response.setMensaje("Mensaje programado correctamente.");
        response.setTarget("SCHEDULED_MESSAGE_CREATE");
        response.setAction("CREATE_SCHEDULED_MESSAGE");
        response.setChatId(null);
        response.setRecipientId(null);
        response.setRecipientName(null);
        response.setMessage(null);
        response.setContenidoBusqueda(null);
        response.setScheduledAt(scheduledAtUtc);
        response.setScheduledBatchId(scheduledBatchId);
        response.setRequiresClientEncryption(Boolean.FALSE);
        response.setSensitivePayloadEncrypted(Boolean.TRUE);
        response.setEncryptedScheduledConfirmation(encryptedScheduledConfirmation);
        response.setNeedsClarification(Boolean.FALSE);
        response.setClarificationReason(null);
        response.setClarificationQuestion(null);
        response.setEncryptedPayload(null);
        response.setScheduledMissingFields(List.of());
        return response;
    }

    private AiUiCustomizationResponseDTO processDedicatedUiCustomization(String requestId,
                                                                        Long userId,
                                                                        String consulta,
                                                                        AiSearchIntentInternalResponseDTO intentResponse,
                                                                        UiCustomizationContextDTO sanitizedUiContext) {
        LOGGER.info("[AI][UI_CUSTOMIZATION_DEDICATED_CLASSIFIER] enabled=true");
        notifyUiProgress(userId, requestId, "UI_CUSTOMIZATION_ANALYZING", "STARTED", resolveUiStartedMessage(intentResponse),
                intentResponse == null ? null : intentResponse.getAction(),
                intentResponse == null ? null : intentResponse.getArea(),
                intentResponse == null ? null : intentResponse.getProperty());

        AiUiCustomizationIntentInternalRequestDTO internalRequest = new AiUiCustomizationIntentInternalRequestDTO();
        internalRequest.setRequestId(requestId);
        internalRequest.setConsulta(consulta);
        internalRequest.setUsuarioActualNombre(resolveUserDisplayName(userId));
        UiCustomizationContextDTO filteredUiContext = uiContextFilterService.filterForSmartIntent(requestId, sanitizedUiContext, intentResponse, consulta);
        internalRequest.setUiContext(filteredUiContext);

        AiUiCustomizationIntentInternalResponseDTO uiIntent = aiUiCustomizationMicroserviceClient.classifyIntent(requestId, internalRequest);
        if (uiIntent == null || !uiIntent.isSuccess()) {
            notifyUiProgress(userId, requestId, "UI_CUSTOMIZATION_FAILED", "FAILED", "No se pudo preparar el cambio visual",
                    intentResponse == null ? null : intentResponse.getAction(),
                    intentResponse == null ? null : intentResponse.getArea(),
                    intentResponse == null ? null : intentResponse.getProperty());
            return buildUiFailure("AI_SERVICE_UNAVAILABLE", "El servicio de IA est\u00E1 tardando demasiado. Int\u00E9ntalo de nuevo.");
        }

        notifyUiProgress(userId, requestId, "UI_CUSTOMIZATION_ANALYZING", "COMPLETED", "Interpretacion visual completada",
                uiIntent.getAction(), uiIntent.getArea(), uiIntent.getProperty());

        AiSearchIntentInternalResponseDTO normalizedIntent = mapUiIntentToSearchIntent(uiIntent);
        repairGroupActiveUiCustomizationIntentFromText(consulta, normalizedIntent);
        repairUiCustomizationIntentFromScope(normalizedIntent);

        notifyUiProgress(userId, requestId, "UI_CUSTOMIZATION_VALIDATING", "STARTED", "Validando cambios visuales...",
                normalizedIntent.getAction(), normalizedIntent.getArea(), normalizedIntent.getProperty());
        try {
            AiUiCustomizationResponseDTO response = aiUiCustomizationValidationService.validate(
                    requestId,
                    consulta,
                    normalizedIntent.getAction(),
                    normalizedIntent.getArea(),
                    normalizedIntent.getProperty(),
                    normalizedIntent.getValue(),
                    normalizedIntent.getValuePreset(),
                    normalizedIntent.getLabel(),
                    normalizedIntent.getConfidence(),
                    normalizedIntent.getColorIntent(),
                    normalizedIntent.getScope(),
                    normalizedIntent.getNeedsClarification(),
                    normalizedIntent.getClarificationReason(),
                    normalizedIntent.getClarificationQuestion(),
                    normalizedIntent.getChanges(),
                    filteredUiContext
            );
            int changesCount = response.getChanges() == null ? 0 : response.getChanges().size();
            LOGGER.info("[AI][UI_CUSTOMIZATION_VALIDATE_RESULT] requestId={} consulta=\"{}\" action={} area={} property={} changesCount={} changes={}",
                    requestId, safe(consulta), response.getAction(), response.getArea(), response.getProperty(), changesCount, response.getChanges());
            if (response.isSuccess()) {
                notifyUiProgress(userId, requestId, "UI_CUSTOMIZATION_VALIDATING", "COMPLETED", "Validacion visual completada",
                        response.getAction(), response.getArea(), response.getProperty());
                notifyUiProgress(userId, requestId, "UI_CUSTOMIZATION_READY", "COMPLETED", resolveUiReadyMessage(response.getAction()),
                        response.getAction(), response.getArea(), response.getProperty());
            } else {
                notifyUiProgress(userId, requestId, "UI_CUSTOMIZATION_FAILED", "FAILED", "No se pudo preparar el cambio visual",
                        normalizedIntent.getAction(), normalizedIntent.getArea(), normalizedIntent.getProperty());
            }
            return response;
        } catch (RuntimeException ex) {
            notifyUiProgress(userId, requestId, "UI_CUSTOMIZATION_FAILED", "FAILED", "No se pudo preparar el cambio visual",
                    normalizedIntent.getAction(), normalizedIntent.getArea(), normalizedIntent.getProperty());
            throw ex;
        }
    }

    private AiSearchIntentInternalResponseDTO mapUiIntentToSearchIntent(AiUiCustomizationIntentInternalResponseDTO uiIntent) {
        AiSearchIntentInternalResponseDTO mapped = new AiSearchIntentInternalResponseDTO();
        mapped.setSuccess(uiIntent.isSuccess());
        mapped.setCodigo(uiIntent.getCodigo());
        mapped.setMensaje(uiIntent.getMensaje());
        mapped.setTarget(uiIntent.getTarget());
        mapped.setAction(uiIntent.getAction());
        mapped.setArea(uiIntent.getArea());
        mapped.setProperty(uiIntent.getProperty());
        mapped.setValue(uiIntent.getValue());
        mapped.setValuePreset(uiIntent.getValuePreset());
        mapped.setLabel(uiIntent.getLabel());
        mapped.setConfidence(uiIntent.getConfidence());
        mapped.setColorIntent(uiIntent.getColorIntent());
        mapped.setScope(uiIntent.getScope());
        mapped.setNeedsClarification(uiIntent.getNeedsClarification());
        mapped.setClarificationReason(uiIntent.getClarificationReason());
        mapped.setClarificationQuestion(uiIntent.getClarificationQuestion());
        mapped.setChanges(uiIntent.getChanges());
        return mapped;
    }

    private void saveSmartActionHistory(Long userId,
                                        String consulta,
                                        Object response,
                                        String fallbackTarget,
                                        String fallbackAction,
                                        String fallbackClarificationReason,
                                        String fallbackLabel,
                                        String fallbackArea,
                                        String fallbackProperty,
                                        String fallbackValue) {
        try {
            aiSmartActionHistoryService.saveHistory(userId, consulta, response, fallbackTarget, fallbackAction,
                    fallbackClarificationReason, fallbackLabel, fallbackArea, fallbackProperty, fallbackValue);
        } catch (Exception ex) {
            LOGGER.warn("[AI][SMART_ACTION_HISTORY_SAVE] usuarioId={} errorClass={}", userId, ex.getClass().getSimpleName());
        }
    }

    private AiEncryptedMessageSearchRequestDTO buildSearchRequest(AiSmartActionRequestDTO request) {
        AiEncryptedMessageSearchRequestDTO searchRequest = new AiEncryptedMessageSearchRequestDTO();
        if (request != null) {
            searchRequest.setConsulta(request.getConsulta());
            searchRequest.setImagenReporteBase64(request.getImagenReporteBase64());
            searchRequest.setImagenReporteMimeType(request.getImagenReporteMimeType());
            searchRequest.setImagenReporteNombre(request.getImagenReporteNombre());
        }
        return searchRequest;
    }

    private void logResponse(String requestId, Object response) {
        LOGGER.info("[AI][SMART_ACTION_RESPONSE] requestId={} codigo={} success={} action={}",
                requestId, extractCodigo(response), extractSuccess(response), extractAction(response));
    }

    private boolean extractSuccess(Object response) {
        if (response instanceof AiEncryptedMessageSearchResponseDTO searchResponse) {
            return searchResponse.isSuccess();
        }
        if (response instanceof AiUiCustomizationResponseDTO uiResponse) {
            return uiResponse.isSuccess();
        }
        return false;
    }

    private String extractCodigo(Object response) {
        if (response instanceof AiEncryptedMessageSearchResponseDTO searchResponse) {
            return searchResponse.getCodigo();
        }
        if (response instanceof AiUiCustomizationResponseDTO uiResponse) {
            return uiResponse.getCodigo();
        }
        return null;
    }

    private String extractAction(Object response) {
        if (response instanceof AiEncryptedMessageSearchResponseDTO searchResponse) {
            return searchResponse.getAction();
        }
        if (response instanceof AiUiCustomizationResponseDTO uiResponse) {
            return uiResponse.getAction();
        }
        return null;
    }

    private AiUiCustomizationResponseDTO buildUiFailure(String codigo, String mensaje) {
        AiUiCustomizationResponseDTO response = new AiUiCustomizationResponseDTO();
        response.setSuccess(false);
        response.setCodigo(codigo);
        response.setMensaje(mensaje);
        response.setTarget("UI_CUSTOMIZATION");
        return response;
    }

    private AiEncryptedMessageSearchResponseDTO resolveSmartActionClarification(String consulta,
                                                                                AiSearchIntentInternalResponseDTO intentResponse) {
        String normalized = normalizeSemanticText(consulta);
        String target = normalizeTarget(intentResponse == null ? null : intentResponse.getTarget());
        if ("UI_CUSTOMIZATION".equals(target)) {
            return null;
        }

        String clarificationReason = intentResponse == null ? null : intentResponse.getClarificationReason();
        String clarificationQuestion = intentResponse == null ? null : intentResponse.getClarificationQuestion();
        boolean explicitClarification = intentResponse != null
                && ("NEEDS_CLARIFICATION".equals(normalizeUpper(intentResponse.getAction()))
                || Boolean.TRUE.equals(intentResponse.getNeedsClarification()));

        if (shouldSuppressDirectionAmbiguity(normalized, intentResponse, clarificationReason)) {
            return null;
        }

        if (!explicitClarification) {
            if (isGenericComplaintLookup(normalized, intentResponse)) {
                target = "MIXED";
                clarificationReason = "COMPLAINT_DIRECTION_AMBIGUOUS";
            } else if (isGenericReportLookup(normalized, intentResponse)) {
                target = "APP_REPORT_STATUS";
                clarificationReason = "REPORT_DIRECTION_AMBIGUOUS";
            } else if (isGenericMessageSearchLookup(normalized, intentResponse)) {
                target = "MESSAGES";
                clarificationReason = "MESSAGE_SEARCH_QUERY_MISSING";
            } else {
                return null;
            }
        }

        if (!hasText(target)) {
            target = inferClarificationTarget(normalized, intentResponse);
        }
        String resolvedQuestion = resolveSmartClarificationMessage(clarificationQuestion, clarificationReason);
        LOGGER.info("[AI][SMART_ACTION_CLARIFICATION] target={} reason={} question={}",
                safe(target), safe(clarificationReason), safe(resolvedQuestion));
        return buildSearchClarificationResponse(target, clarificationReason, resolvedQuestion);
    }

    private AiEncryptedMessageSearchResponseDTO buildSearchClarificationResponse(String target,
                                                                                 String clarificationReason,
                                                                                 String clarificationQuestion) {
        AiEncryptedMessageSearchResponseDTO response = new AiEncryptedMessageSearchResponseDTO();
        response.setSuccess(false);
        response.setCodigo(SMART_ACTION_NEEDS_CLARIFICATION);
        response.setMensaje(clarificationQuestion);
        response.setTarget(hasText(target) ? target : "MESSAGES");
        response.setAction("NEEDS_CLARIFICATION");
        response.setNeedsClarification(Boolean.TRUE);
        response.setClarificationReason(clarificationReason);
        response.setClarificationQuestion(clarificationQuestion);
        response.setResultados(List.of());
        return response;
    }

    private String resolveSmartClarificationMessage(String clarificationQuestion, String clarificationReason) {
        if (hasText(clarificationQuestion)) {
            return clarificationQuestion;
        }
        if ("AREA_SCOPE_AMBIGUOUS".equalsIgnoreCase(safe(clarificationReason))) {
            return "¿Qué zona quieres cambiar exactamente?";
        }
        if ("COLOR_VALUE_MISSING".equalsIgnoreCase(safe(clarificationReason))) {
            return "¿Qué color quieres aplicar?";
        }
        if ("SIZE_VALUE_MISSING".equalsIgnoreCase(safe(clarificationReason))) {
            return "¿Qué tamaño quieres aplicar?";
        }
        if ("BORDER_PROPERTY_AMBIGUOUS".equalsIgnoreCase(safe(clarificationReason))) {
            return "¿Qué parte del borde quieres cambiar: color, grosor o redondeo?";
        }
        if ("MESSAGE_SEARCH_QUERY_MISSING".equalsIgnoreCase(safe(clarificationReason))) {
            return "¿Qué mensaje quieres consultar: recuerdas algo del contenido, la persona, el chat o la fecha aproximada?";
        }
        if ("COMPLAINT_DIRECTION_AMBIGUOUS".equalsIgnoreCase(safe(clarificationReason))) {
            return "¿Quieres consultar denuncias recibidas, denuncias que hiciste tú o denuncias sobre una persona concreta?";
        }
        if ("REPORT_DIRECTION_AMBIGUOUS".equalsIgnoreCase(safe(clarificationReason))) {
            return "¿Quieres ver reportes recibidos, reportes creados por ti o reportes sobre una persona/contenido concreto?";
        }
        return "¿Puedes especificar un poco más?";
    }

    private String inferClarificationTarget(String normalized,
                                            AiSearchIntentInternalResponseDTO intentResponse) {
        if (isGenericComplaintLookup(normalized, intentResponse)) {
            return "MIXED";
        }
        if (isGenericReportLookup(normalized, intentResponse)) {
            return "APP_REPORT_STATUS";
        }
        return "MESSAGES";
    }

    private boolean isGenericMessageSearchLookup(String normalized,
                                                 AiSearchIntentInternalResponseDTO intentResponse) {
        String target = normalizeTarget(intentResponse == null ? null : intentResponse.getTarget());
        boolean messageDomain = "MESSAGES".equals(target) || "UNREAD_MESSAGES".equals(target)
                || containsAny(normalized, "mensaje", "mensajes", "chat", "chats", "conversacion", "conversaciones");
        boolean lookupIntent = containsAny(normalized,
                "buscar", "busca", "consulta", "consultar", "ver", "ensename", "muestrame",
                "necesito", "quiero", "localiza", "encuentra");
        if (!messageDomain || !lookupIntent) {
            return false;
        }
        if (intentResponse != null && (
                hasText(intentResponse.getPersonaMencionada())
                        || hasText(intentResponse.getGrupoMencionado())
                        || hasText(intentResponse.getTemporalExpression())
                        || hasText(intentResponse.getRangoTemporalSugerido())
                        || hasText(intentResponse.getOrden())
                        || (hasText(intentResponse.getTipoMensajeSolicitado()) && !"ANY".equals(normalizeUpper(intentResponse.getTipoMensajeSolicitado())))
                        || (hasText(intentResponse.getTipoScopeSolicitado()) && !"GLOBAL".equals(normalizeUpper(intentResponse.getTipoScopeSolicitado())))
        )) {
            return false;
        }
        return !hasSearchCriteriaTokens(normalized);
    }

    private boolean hasSearchCriteriaTokens(String normalized) {
        if (!hasText(normalized)) {
            return false;
        }
        for (String token : normalized.split(" ")) {
            if (!hasText(token)) {
                continue;
            }
            if (Set.of("necesito", "quiero", "consultar", "consulta", "buscar", "busca", "ver", "ensename",
                    "muestrame", "mensaje", "mensajes", "chat", "chats", "un", "una", "el", "la", "los", "las")
                    .contains(token)) {
                continue;
            }
            return true;
        }
        return false;
    }

    private boolean isGenericComplaintLookup(String normalized,
                                             AiSearchIntentInternalResponseDTO intentResponse) {
        String target = normalizeTarget(intentResponse == null ? null : intentResponse.getTarget());
        boolean complaintDomain = "COMPLAINTS_CREATED".equals(target)
                || "COMPLAINTS_RECEIVED".equals(target)
                || "MIXED".equals(target)
                || containsAny(normalized, "denuncia", "denuncias", "denunciado", "denunciaron");
        if (!complaintDomain) {
            return false;
        }
        if (containsAny(normalized,
                "contra mi", "me han denunciado", "me pusieron una denuncia", "me han puesto denuncia",
                "denuncias recibidas", "que denuncias me han puesto", "mis denuncias",
                "denuncias que he puesto", "denuncias creadas por mi", "he denunciado",
                "denunciar a", "denunciar este mensaje",
                "he puesto", "hice", "yo puse", "contra juan", "contra maria", "contra daniel")) {
            return false;
        }
        return containsAny(normalized,
                "consultar una denuncia", "consulta una denuncia", "buscar denuncias", "busca denuncias",
                "ensename una denuncia", "muestrame una denuncia", "quiero ver una denuncia",
                "denuncia", "denuncias");
    }

    private boolean isGenericReportLookup(String normalized,
                                          AiSearchIntentInternalResponseDTO intentResponse) {
        String target = normalizeTarget(intentResponse == null ? null : intentResponse.getTarget());
        boolean reportDomain = "APP_REPORT_STATUS".equals(target)
                || containsAny(normalized, "reporte", "reportes", "incidencia", "incidencias", "queja", "quejas", "sugerencia", "sugerencias");
        if (!reportDomain) {
            return false;
        }
        if (containsAny(normalized,
                "crear reporte", "crear un reporte", "poner reporte", "poner un reporte", "enviar reporte", "mandar incidencia",
                "abrir incidencia", "reportar un problema", "quiero reportar", "quiero crear", "quiero enviar", "quiero poner",
                "he creado", "he enviado", "he reportado", "he puesto", "mis reportes", "mis incidencias",
                "estado de mis reportes", "que paso con la incidencia que envie", "reportes creados por mi",
                "sobre ", "del ", "de la ", "de los ", "de las ", "pendiente", "revision", "aprobada", "rechazada")) {
            return false;
        }
        return containsAny(normalized,
                "consultar un reporte", "consulta un reporte", "buscar reportes", "busca reportes",
                "ensename un reporte", "muestrame un reporte", "quiero ver un reporte",
                "consultar una incidencia", "consultar una queja", "consultar una sugerencia");
    }

    private boolean shouldSuppressDirectionAmbiguity(String normalized,
                                                     AiSearchIntentInternalResponseDTO intentResponse,
                                                     String clarificationReason) {
        String reason = normalizeUpper(clarificationReason);
        String target = normalizeTarget(intentResponse == null ? null : intentResponse.getTarget());
        String action = normalizeUpper(intentResponse == null ? null : intentResponse.getAction());

        if ("APP_REPORT".equals(target) && containsAny(action, "CREATE", "REPORT", "SEND", "SUBMIT")) {
            return true;
        }
        if ("APP_REPORT_STATUS".equals(target)
                && containsAny(normalized, "mis reportes", "mis incidencias", "he creado", "he enviado", "estado")) {
            return true;
        }
        if ("COMPLAINTS_RECEIVED".equals(target) || "COMPLAINTS_CREATED".equals(target)) {
            return true;
        }
        if ("REPORT_DIRECTION_AMBIGUOUS".equals(reason)
                && containsAny(normalized, "crear", "poner", "enviar", "mandar", "abrir", "reportar", "registrar", "levantar")) {
            return true;
        }
        if ("COMPLAINT_DIRECTION_AMBIGUOUS".equals(reason)
                && containsAny(normalized, "denunciar a", "denunciar este mensaje", "denunciar el mensaje")) {
            return true;
        }
        return false;
    }

    private String resolveUserDisplayName(Long userId) {
        if (userId == null) {
            return null;
        }
        try {
            return usuarioRepository.findById(userId)
                    .map(user -> {
                        String nombre = (user.getNombre() == null ? "" : user.getNombre()).trim();
                        String apellido = (user.getApellido() == null ? "" : user.getApellido()).trim();
                        String fullName = (nombre + " " + apellido).trim();
                        return fullName.isEmpty() ? user.getEmail() : fullName;
                    })
                    .orElse(null);
        } catch (Exception ex) {
            return null;
        }
    }

    private String normalizeTarget(String target) {
        if (target == null) {
            return null;
        }
        String normalized = target.trim().toUpperCase(Locale.ROOT);
        return normalized.isEmpty() ? null : normalized;
    }

    private String safe(String value) {
        if (value == null) {
            return "";
        }
        return value.replaceAll("\\s+", " ").trim();
    }

    private UiCustomizationContextDTO sanitizeUiContext(String requestId, String consulta, UiCustomizationContextDTO context) {
        if (context == null) {
            return null;
        }
        String scope = normalizeUpper(context.getScope());
        String themeMode = normalizeUpper(context.getThemeMode());
        String filterScope = resolveUiContextFilterScope(consulta);
        Set<String> candidateAreas = resolveUiContextCandidateAreas(consulta);
        Map<String, Map<String, String>> sanitizedStyles = sanitizeStylesMap(context.getCurrentStyles(), candidateAreas);
        Map<String, Map<String, String>> sanitizedComputed = sanitizeStylesMap(context.getComputedStyles(), candidateAreas);
        Map<String, List<String>> sanitizedHints = sanitizeGroupHints(context.getGroupExpansionHints(), candidateAreas);
        Map<String, Object> sanitizedAreaCatalog = sanitizeAreaObjectMap(context.getAreaCatalog(), candidateAreas);
        Map<String, Object> sanitizedDomState = sanitizeAreaObjectMap(context.getDomState(), candidateAreas);
        boolean validScope = ALLOWED_UI_CONTEXT_SCOPES.contains(scope);
        boolean validTheme = ALLOWED_THEME_MODES.contains(themeMode);
        int totalAreasSent = sanitizedStyles.size() + sanitizedComputed.size() + sanitizedHints.size() + sanitizedAreaCatalog.size();
        LOGGER.info("[AI][UI_CONTEXT_FILTER] scope={} areasSent={}", filterScope, totalAreasSent);
        if (totalAreasSent == 0 && sanitizedDomState.isEmpty()) {
            LOGGER.info("[AI][UI_CONTEXT_VALIDATE] requestId={} validAreas=0 ignoredAreas=0", requestId);
            return null;
        }
        UiCustomizationContextDTO sanitized = new UiCustomizationContextDTO();
        sanitized.setVersion(context.getVersion());
        sanitized.setScope(validScope ? scope : "CHAT_LIST");
        sanitized.setThemeMode(validTheme ? themeMode.toLowerCase(Locale.ROOT) : "light");
        if (!sanitizedStyles.isEmpty()) {
            sanitized.setCurrentStyles(sanitizedStyles);
        }
        if (!sanitizedComputed.isEmpty()) {
            sanitized.setComputedStyles(sanitizedComputed);
        }
        if (!sanitizedHints.isEmpty()) {
            sanitized.setGroupExpansionHints(sanitizedHints);
        }
        if (!sanitizedAreaCatalog.isEmpty()) {
            sanitized.setAreaCatalog(sanitizedAreaCatalog);
        }
        if (!sanitizedDomState.isEmpty()) {
            sanitized.setDomState(sanitizedDomState);
        }
        LOGGER.info("[AI][UI_CONTEXT] requestId={} scope={} styles={} computed={} hints={} areaCatalog={} domState={}",
                requestId, sanitized.getScope(),
                sanitizedStyles.size(), sanitizedComputed.size(), sanitizedHints.size(), sanitizedAreaCatalog.size(), sanitizedDomState.size());
        LOGGER.info("[AI][UI_CONTEXT_VALIDATE] requestId={} validAreas={} ignoredAreas={}",
                requestId, totalAreasSent, 0);
        return sanitized;
    }

    private Map<String, Map<String, String>> sanitizeStylesMap(Map<String, Map<String, String>> source, Set<String> candidateAreas) {
        Map<String, Map<String, String>> sanitizedStyles = new LinkedHashMap<>();
        if (source == null || source.isEmpty()) {
            return sanitizedStyles;
        }
        for (Map.Entry<String, Map<String, String>> areaEntry : source.entrySet()) {
            if (sanitizedStyles.size() >= MAX_UI_AREAS) {
                break;
            }
            String area = normalizeUpper(areaEntry.getKey());
            if (!ALLOWED_UI_AREAS.contains(area) || !isCandidateArea(area, candidateAreas) || areaEntry.getValue() == null) {
                continue;
            }
            Map<String, String> sanitizedProperties = new LinkedHashMap<>();
            for (Map.Entry<String, String> propertyEntry : areaEntry.getValue().entrySet()) {
                if (sanitizedProperties.size() >= MAX_UI_PROPERTIES_PER_AREA) {
                    break;
                }
                String property = normalizeUpper(propertyEntry.getKey());
                String value = propertyEntry.getValue();
                if (!ALLOWED_UI_PROPERTIES.contains(property) || value == null) {
                    continue;
                }
                String normalizedValue = value.trim();
                if (normalizedValue.isEmpty() || normalizedValue.length() > MAX_UI_VALUE_LENGTH) {
                    continue;
                }
                sanitizedProperties.put(property, normalizedValue);
            }
            if (!sanitizedProperties.isEmpty()) {
                sanitizedStyles.put(area, sanitizedProperties);
            }
        }
        return sanitizedStyles;
    }

    private Map<String, List<String>> sanitizeGroupHints(Map<String, List<String>> source, Set<String> candidateAreas) {
        Map<String, List<String>> sanitized = new LinkedHashMap<>();
        if (source == null || source.isEmpty()) {
            return sanitized;
        }
        for (Map.Entry<String, List<String>> entry : source.entrySet()) {
            String parent = normalizeUpper(entry.getKey());
            if (!ALLOWED_UI_AREAS.contains(parent) || !isCandidateArea(parent, candidateAreas) || entry.getValue() == null || entry.getValue().isEmpty()) {
                continue;
            }
            List<String> children = new ArrayList<>();
            for (String child : entry.getValue()) {
                if (children.size() >= MAX_UI_HINTS_PER_PARENT) {
                    break;
                }
                String normalizedChild = normalizeUpper(child);
                if (ALLOWED_UI_AREAS.contains(normalizedChild) && isCandidateArea(normalizedChild, candidateAreas)) {
                    children.add(normalizedChild);
                }
            }
            if (!children.isEmpty()) {
                sanitized.put(parent, children);
            }
        }
        return sanitized;
    }

    private Map<String, Object> sanitizeAreaObjectMap(Map<String, Object> source, Set<String> candidateAreas) {
        Map<String, Object> sanitized = new LinkedHashMap<>();
        if (source == null || source.isEmpty()) {
            return sanitized;
        }
        for (Map.Entry<String, Object> entry : source.entrySet()) {
            if (sanitized.size() >= MAX_UI_CONTEXT_CANDIDATE_AREAS) {
                break;
            }
            String area = normalizeUpper(entry.getKey());
            if (!ALLOWED_UI_AREAS.contains(area) || !isCandidateArea(area, candidateAreas) || entry.getValue() == null) {
                continue;
            }
            sanitized.put(area, entry.getValue());
        }
        return sanitized;
    }

    private String resolveUiContextFilterScope(String consulta) {
        String normalized = normalizeSemanticText(consulta);
        if (containsAny(normalized, "sidebar", "barra lateral", "menu lateral", "panel lateral", "barra izquierda")) {
            return "SIDEBAR";
        }
        if (containsAny(normalized, "desplegable", "menu de opciones", "opciones del chat", "tres puntos")) {
            return "CHAT_LIST_PIN_MENU";
        }
        if (containsAny(normalized, "mensaje", "mensajes", "burbuja", "burbujas", "composer", "caja de escribir", "input")) {
            return "CHAT_MESSAGES";
        }
        return "CHAT_LIST";
    }

    private Set<String> resolveUiContextCandidateAreas(String consulta) {
        String scope = resolveUiContextFilterScope(consulta);
        LinkedHashSet<String> out = new LinkedHashSet<>();
        switch (scope) {
            case "SIDEBAR" -> addAreas(out,
                    "SIDEBAR_NAV_PANEL", "SIDEBAR_NAV_GROUP", "SIDEBAR_NAV_BOTTOM", "SIDEBAR_NAV_ITEM",
                    "SIDEBAR_NAV_ITEM_ACTIVE", "SIDEBAR_NAV_ACTIVE_INDICATOR", "SIDEBAR_NAV_LOGO", "SIDEBAR_NAV_ICON",
                    "SIDEBAR_NAV_ICON_ACTIVE", "SIDEBAR_NAV_AI_ICON", "SIDEBAR_NAV_TOOLTIP", "SIDEBAR_NAV_AVATAR",
                    "SIDEBAR_NAV_NOTIF_BADGE", "SIDEBAR_NAV_SETTINGS");
            case "CHAT_LIST_PIN_MENU" -> addAreas(out,
                    "CHAT_LIST_PIN_TOGGLE", "CHAT_LIST_PIN_MENU", "CHAT_LIST_PIN_MENU_ITEM",
                    "CHAT_LIST_PIN_MENU_REPORT", "CHAT_LIST_PIN_MENU_DANGER",
                    "CHAT_LIST_ACTIONS_MENU", "CHAT_LIST_ACTIONS_MENU_ITEM");
            case "CHAT_MESSAGES" -> addAreas(out,
                    "CHAT_MESSAGES_AREA", "MESSAGE_BUBBLES", "OWN_MESSAGE_BUBBLE", "OTHER_MESSAGE_BUBBLE",
                    "GROUP_MESSAGE_BUBBLE", "MESSAGE_META", "MESSAGE_REACTIONS", "CHAT_COMPOSER",
                    "CHAT_COMPOSER_TEXTAREA", "CHAT_COMPOSER_ACTIONS", "CHAT_COMPOSER_SEND_BUTTON",
                    "COMPOSE_ACTIONS_POPUP", "MESSAGE_OPTIONS_DROPDOWN", "REPLY_BANNER");
            default -> addAreas(out,
                    "CHAT_LIST_PANEL", "CHAT_LIST_HEADER", "CHAT_LIST_TITLE", "CHAT_LIST_HEADER_ACTIONS", "CHAT_LIST_HEADER_ICON_BUTTON",
                    "CHAT_LIST_HEADER_ICON", "CHAT_LIST_HEADER_MENU", "CHAT_LIST_HEADER_MENU_ITEM", "CHAT_LIST_SEARCH", "CHAT_LIST_FILTERS",
                    "CHAT_LIST_FILTER_BUTTONS", "CHAT_LIST_FILTER_BUTTONS_ACTIVE", "CHAT_LIST_ITEM", "CHAT_LIST_ITEM_ACTIVE",
                    "CHAT_LIST_ITEM_GROUP", "CHAT_LIST_ITEM_GROUP_ACTIVE", "CHAT_LIST_PREVIEW", "CHAT_LIST_ITEM_PREVIEW",
                    "CHAT_LIST_ITEM_GROUP_PREVIEW", "CHAT_LIST_AUDIO_PREVIEW", "CHAT_LIST_ITEM_AUDIO_PREVIEW",
                    "CHAT_LIST_ITEM_GROUP_AUDIO_PREVIEW", "CHAT_LIST_ITEM_NAME", "CHAT_LIST_ITEM_DATE", "CHAT_LIST_BADGES",
                    "CHAT_LIST_PIN_TOGGLE", "CHAT_LIST_PIN_MENU", "CHAT_LIST_PIN_MENU_ITEM", "CHAT_LIST_PIN_MENU_REPORT",
                    "CHAT_LIST_PIN_MENU_DANGER");
        }
        return out;
    }

    private void addAreas(Set<String> target, String... areas) {
        if (target == null || areas == null) {
            return;
        }
        for (String area : areas) {
            if (target.size() >= MAX_UI_CONTEXT_CANDIDATE_AREAS) {
                break;
            }
            if (area != null && ALLOWED_UI_AREAS.contains(area)) {
                target.add(area);
            }
        }
    }

    private boolean isCandidateArea(String area, Set<String> candidateAreas) {
        return candidateAreas == null || candidateAreas.isEmpty() || candidateAreas.contains(area);
    }

    private String normalizeUpper(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private boolean shouldForceUiCustomization(String consulta) {
        String normalized = normalizeSemanticText(consulta);
        if (normalized.isEmpty()) {
            return false;
        }
        if (isExplicitMessageSearchRequest(normalized) && !looksLikeStrongVisualCustomization(normalized)) {
            return false;
        }
        return looksLikeStrongVisualCustomization(normalized);
    }

    private boolean looksLikeStrongVisualCustomization(String normalized) {
        boolean hasActionVerb = containsAny(normalized,
                "pon", "pone", "cambia", "cambiame", "cambiar", "modifica", "modificar",
                "ajusta", "ajusta", "haz", "personaliza", "aplica", "quita", "restaura");
        boolean hasVisualWord = containsAny(normalized,
                "estilo", "estilos", "color", "colores", "fondo", "texto", "borde", "sombra", "tamano",
                "grande", "pequeno", "claro", "oscuro", "elegante", "tema", "darkmode", "dark mode",
                "light mode", "modo oscuro", "modo claro", "icono", "iconos",
                "blanco", "negro", "rojo", "azul", "verde", "morado", "violeta", "amarillo", "purpura", "rosa");
        boolean hasUiArea = containsAny(normalized,
                "listado de chats", "listado de chat", "lista de chats", "lista de chat", "panel de chats", "panel de chat", "zona izquierda",
                "buscador", "encabezado", "header", "iconos del encabezado",
                "filtros", "filtrado", "boton activo", "chat no leido", "chats no leidos",
                "preview", "previews", "imagen", "imagenes", "archivo", "archivos", "audio", "audios",
                "desplegable", "menu de opciones", "listado", "badge", "badges", "contador", "contadores", "etiqueta", "pill",
                "chat grupal", "chats grupales", "chat individual", "chats individuales");
        return hasActionVerb && hasVisualWord && hasUiArea;
    }

    private boolean isExplicitMessageSearchRequest(String normalized) {
        return containsAny(normalized,
                "busca", "buscar", "encuentra", "encontrar", "localiza", "localizar",
                "dime el mensaje", "ultimo mensaje", "mensajes donde", "conversaciones donde",
                "que mensajes", "buscar mensajes", "encuentrame el mensaje");
    }

    private String normalizeSemanticText(String value) {
        if (value == null) {
            return "";
        }
        String lower = value.toLowerCase(Locale.ROOT);
        String normalized = Normalizer.normalize(lower, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "");
        return normalized.replaceAll("[^a-z0-9# ]", " ").replaceAll("\\s+", " ").trim();
    }

    private boolean containsAny(String text, String... terms) {
        if (text == null || text.isEmpty() || terms == null) {
            return false;
        }
        for (String term : terms) {
            if (term != null && !term.isEmpty() && text.contains(term)) {
                return true;
            }
        }
        return false;
    }

    private String resolveAreaFromScope(UiCustomizationScopeDTO scope) {
        if (scope == null) {
            return null;
        }
        String module = normalizeUpper(scope.getModule());
        String element = normalizeUpper(scope.getElement());
        String chatType = normalizeUpper(scope.getChatType());
        String state = normalizeUpper(scope.getState());
        String subElement = normalizeUpper(scope.getSubElement());
        if (!"CHAT_LIST".equals(module)) {
            return switch (module) {
                case "AI_POPUP" -> "AI_SEARCH_POPUP";
                case "SIDEBAR" -> resolveSidebarAreaFromScope(element, state, subElement);
                case "PROFILE" -> "TOPBAR_PROFILE";
                default -> null;
            };
        }
        return switch (element) {
            case "PANEL" -> "CHAT_LIST_PANEL";
            case "HEADER" -> "CHAT_LIST_HEADER";
            case "TITLE" -> "CHAT_LIST_TITLE";
            case "HEADER_ACTIONS" -> "CHAT_LIST_HEADER_ACTIONS";
            case "SEARCH" -> "CHAT_LIST_SEARCH";
            case "FILTERS" -> "CHAT_LIST_FILTERS";
            case "FILTER_BUTTON" -> "ACTIVE".equals(state) || "SELECTED".equals(state)
                    ? "CHAT_LIST_FILTER_BUTTONS_ACTIVE"
                    : "CHAT_LIST_FILTER_BUTTONS";
            case "CHAT_ITEM" -> resolveChatItemArea(chatType, state);
            case "PREVIEW" -> "GROUP".equals(chatType) ? "CHAT_LIST_ITEM_GROUP_PREVIEW"
                    : "INDIVIDUAL".equals(chatType) ? "CHAT_LIST_ITEM_PREVIEW" : "CHAT_LIST_PREVIEW";
            case "AUDIO_PREVIEW" -> "GROUP".equals(chatType) ? "CHAT_LIST_ITEM_GROUP_AUDIO_PREVIEW"
                    : "INDIVIDUAL".equals(chatType) ? "CHAT_LIST_ITEM_AUDIO_PREVIEW" : "CHAT_LIST_AUDIO_PREVIEW";
            case "FILE_PREVIEW" -> "GROUP".equals(chatType) ? "CHAT_LIST_ITEM_GROUP_FILE_PREVIEW"
                    : "INDIVIDUAL".equals(chatType) ? "CHAT_LIST_ITEM_FILE_PREVIEW" : "CHAT_LIST_FILE_PREVIEW";
            case "IMAGE_PREVIEW" -> "GROUP".equals(chatType) ? "CHAT_LIST_ITEM_GROUP_IMAGE_PREVIEW"
                    : "INDIVIDUAL".equals(chatType) ? "CHAT_LIST_ITEM_IMAGE_PREVIEW" : "CHAT_LIST_IMAGE_PREVIEW";
            case "STICKER_PREVIEW" -> "GROUP".equals(chatType) ? "CHAT_LIST_ITEM_GROUP_STICKER_PREVIEW"
                    : "INDIVIDUAL".equals(chatType) ? "CHAT_LIST_ITEM_STICKER_PREVIEW" : "CHAT_LIST_STICKER_PREVIEW";
            case "BADGE" -> "GROUP".equals(chatType) ? "CHAT_LIST_ITEM_GROUP_BADGES" : "CHAT_LIST_BADGES";
            case "DATE" -> "CHAT_LIST_ITEM_DATE";
            case "AVATAR" -> "CHAT_LIST_AVATAR";
            case "STATUS_DOT" -> "CHAT_LIST_STATUS_DOT";
            case "NAME" -> "CHAT_LIST_ITEM_NAME";
            case "MUTED_INDICATOR" -> "CHAT_LIST_MUTED_INDICATOR";
            case "FAVORITE_INDICATOR" -> "CHAT_LIST_FAVORITE_INDICATOR";
            case "CLOSED_INDICATOR" -> "CHAT_LIST_CLOSED_INDICATOR";
            case "EMPTY_STATE" -> "CHAT_LIST_EMPTY_STATE";
            case "PUBLIC_PANEL" -> "CHAT_LIST_PUBLIC_PANEL";
            case "PUBLIC_CARD" -> "CHAT_LIST_PUBLIC_CARD";
            case "PIN_MENU" -> switch (subElement) {
                case "REPORT_ACTION" -> "CHAT_LIST_PIN_MENU_REPORT";
                case "DANGER_ACTION" -> "CHAT_LIST_PIN_MENU_DANGER";
                case "ITEM" -> "CHAT_LIST_PIN_MENU_ITEM";
                default -> "CHAT_LIST_PIN_MENU";
            };
            case "PIN_TOGGLE" -> "CHAT_LIST_PIN_TOGGLE";
            case "ACTIONS_MENU" -> "CHAT_LIST_ACTIONS_MENU";
            default -> null;
        };
    }

    private String resolveChatItemArea(String chatType, String state) {
        if ("UNREAD".equals(state)) {
            return "CHAT_LIST_ITEM_UNREAD";
        }
        if ("ACTIVE".equals(state) || "SELECTED".equals(state)) {
            return "GROUP".equals(chatType) ? "CHAT_LIST_ITEM_GROUP_ACTIVE" : "CHAT_LIST_ITEM_ACTIVE";
        }
        return "GROUP".equals(chatType) ? "CHAT_LIST_ITEM_GROUP" : "CHAT_LIST_ITEM";
    }

    private String resolveSidebarAreaFromScope(String element, String state, String subElement) {
        if ("ICON".equals(subElement)) {
            return "ACTIVE".equals(state) || "SELECTED".equals(state)
                    ? "SIDEBAR_NAV_ICON_ACTIVE"
                    : "SIDEBAR_NAV_ICON";
        }
        if ("ITEM".equals(element) || "CHAT_ITEM".equals(element)) {
            return "ACTIVE".equals(state) || "SELECTED".equals(state)
                    ? "SIDEBAR_NAV_ITEM_ACTIVE"
                    : "SIDEBAR_NAV_ITEM";
        }
        return switch (element) {
            case "GROUP" -> "SIDEBAR_NAV_GROUP";
            case "BOTTOM" -> "SIDEBAR_NAV_BOTTOM";
            case "TOOLTIP" -> "SIDEBAR_NAV_TOOLTIP";
            case "AVATAR" -> "SIDEBAR_NAV_AVATAR";
            case "BADGE" -> "SIDEBAR_NAV_NOTIF_BADGE";
            case "ACTIVE_INDICATOR" -> "SIDEBAR_NAV_ACTIVE_INDICATOR";
            case "LOGO" -> "SIDEBAR_NAV_LOGO";
            default -> "SIDEBAR_NAV_PANEL";
        };
    }

    private List<UiCustomizationChangeDTO> applyResolvedAreaToChanges(List<UiCustomizationChangeDTO> changes, String area) {
        if (!hasText(area) || changes == null || changes.isEmpty()) {
            return changes;
        }
        List<UiCustomizationChangeDTO> normalized = new ArrayList<>();
        for (UiCustomizationChangeDTO change : changes) {
            if (change == null) {
                continue;
            }
            UiCustomizationChangeDTO copy = new UiCustomizationChangeDTO();
            copy.setArea(hasText(change.getArea()) ? change.getArea() : area);
            copy.setProperty(change.getProperty());
            copy.setValue(change.getValue());
            copy.setValuePreset(change.getValuePreset());
            normalized.add(copy);
        }
        return normalized.isEmpty() ? changes : normalized;
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private void repairUiCustomizationIntentFromScope(AiSearchIntentInternalResponseDTO intentResponse) {
        if (intentResponse == null || !"UI_CUSTOMIZATION".equals(normalizeTarget(intentResponse.getTarget()))) {
            return;
        }
        UiCustomizationScopeDTO scope = intentResponse.getScope();
        if (scope != null) {
            LOGGER.info("[AI][UI_SCOPE_RESULT] module={} element={} chatType={} state={} subElement={}",
                    safe(scope.getModule()), safe(scope.getElement()), safe(scope.getChatType()),
                    safe(scope.getState()), safe(scope.getSubElement()));
        }
        String resolvedArea = resolveAreaFromScope(scope);
        if (!hasText(resolvedArea)) {
            return;
        }
        LOGGER.info("[AI][UI_SCOPE_AREA_RESOLVED] area={}", safe(resolvedArea));
        List<UiCustomizationChangeDTO> repairedChanges = applyResolvedAreaToChanges(intentResponse.getChanges(), resolvedArea);
        intentResponse.setChanges(repairedChanges);
        if (!hasText(intentResponse.getArea())
                || "CHAT_LIST_ITEM_ACTIVE".equals(normalizeUpper(intentResponse.getArea()))) {
            intentResponse.setArea(resolvedArea);
        }
        boolean hasPayload = (repairedChanges != null && !repairedChanges.isEmpty())
                || (hasText(intentResponse.getProperty()) && hasText(intentResponse.getValue()));
        if (("NEEDS_CLARIFICATION".equals(normalizeUpper(intentResponse.getAction()))
                || Boolean.TRUE.equals(intentResponse.getNeedsClarification()))
                && hasPayload) {
            if (repairedChanges != null && repairedChanges.size() > 1) {
                intentResponse.setAction("UPDATE_STYLE_MULTI");
                intentResponse.setArea(null);
                intentResponse.setProperty(null);
                intentResponse.setValue(null);
            } else if (repairedChanges != null && repairedChanges.size() == 1) {
                UiCustomizationChangeDTO single = repairedChanges.get(0);
                intentResponse.setAction("UPDATE_STYLE");
                intentResponse.setArea(single.getArea());
                intentResponse.setProperty(single.getProperty());
                intentResponse.setValue(single.getValue());
                intentResponse.setValuePreset(single.getValuePreset());
                intentResponse.setChanges(null);
            } else {
                intentResponse.setAction("UPDATE_STYLE");
                intentResponse.setArea(resolvedArea);
            }
            intentResponse.setNeedsClarification(Boolean.FALSE);
            if (intentResponse.getConfidence() == null || intentResponse.getConfidence() < 0.90d) {
                intentResponse.setConfidence(0.95d);
            }
            LOGGER.info("[AI][SMART_ACTION_UI_REPAIR] rule=SCOPE_RESOLVABLE action={} changesCount={}",
                    safe(intentResponse.getAction()), repairedChanges == null ? 0 : repairedChanges.size());
        }
    }

    private void repairGroupActiveUiCustomizationIntentFromText(String consulta,
                                                                AiSearchIntentInternalResponseDTO intentResponse) {
        if (intentResponse == null || !"UI_CUSTOMIZATION".equals(normalizeTarget(intentResponse.getTarget()))) {
            return;
        }
        String semantic = normalizeSemanticText((consulta == null ? "" : consulta) + " "
                + (intentResponse.getLabel() == null ? "" : intentResponse.getLabel()));
        if (!isGroupActiveChatRequest(semantic)) {
            return;
        }
        List<UiCustomizationChangeDTO> repairedChanges = buildGroupActiveChangesFromText(semantic, intentResponse);
        if (repairedChanges.size() > 1) {
            intentResponse.setAction("UPDATE_STYLE_MULTI");
            intentResponse.setArea(null);
            intentResponse.setProperty(null);
            intentResponse.setValue(null);
            intentResponse.setValuePreset(null);
            intentResponse.setChanges(repairedChanges);
        } else if (repairedChanges.size() == 1) {
            UiCustomizationChangeDTO single = repairedChanges.get(0);
            intentResponse.setAction("UPDATE_STYLE");
            intentResponse.setArea(single.getArea());
            intentResponse.setProperty(single.getProperty());
            intentResponse.setValue(single.getValue());
            intentResponse.setValuePreset(single.getValuePreset());
            intentResponse.setChanges(null);
        } else {
            intentResponse.setAction("UPDATE_STYLE");
            intentResponse.setArea("CHAT_LIST_ITEM_GROUP_ACTIVE");
            intentResponse.setChanges(null);
        }
        intentResponse.setNeedsClarification(Boolean.FALSE);
        intentResponse.setClarificationReason(null);
        intentResponse.setClarificationQuestion(null);
        if (intentResponse.getConfidence() == null || intentResponse.getConfidence() < 0.90d) {
            intentResponse.setConfidence(0.95d);
        }
        LOGGER.info("[AI][SMART_ACTION_UI_REPAIR] rule=GROUP_ACTIVE_ITEM action={} changesCount={}",
                safe(intentResponse.getAction()), repairedChanges.size());
    }

    private List<UiCustomizationChangeDTO> buildGroupActiveChangesFromText(String semantic,
                                                                           AiSearchIntentInternalResponseDTO intentResponse) {
        Map<String, UiCustomizationChangeDTO> changesByProperty = new LinkedHashMap<>();
        if (intentResponse != null && intentResponse.getChanges() != null) {
            for (UiCustomizationChangeDTO change : intentResponse.getChanges()) {
                if (change == null || !isEligibleGroupActiveArea(change.getArea())) {
                    continue;
                }
                putGroupActiveChange(changesByProperty, change.getProperty(), change.getValue(), change.getValuePreset());
            }
        }

        if (intentResponse != null && isEligibleGroupActiveArea(intentResponse.getArea())) {
            putGroupActiveChange(changesByProperty, intentResponse.getProperty(), intentResponse.getValue(), intentResponse.getValuePreset());
        }

        if (!changesByProperty.containsKey("BACKGROUND_COLOR")) {
            String backgroundColor = extractColorForKeyword(semantic, "fondo");
            if (backgroundColor == null) {
                backgroundColor = extractColorForKeyword(semantic, "background");
            }
            if (backgroundColor == null) {
                backgroundColor = extractColorForKeyword(semantic, "color de fondo");
            }
            putGroupActiveChange(changesByProperty, "BACKGROUND_COLOR", backgroundColor, null);
        }
        if (!changesByProperty.containsKey("TEXT_COLOR")) {
            String textColor = extractColorForKeyword(semantic, "texto");
            if (textColor == null) {
                textColor = extractColorForKeyword(semantic, "textos");
            }
            if (textColor == null) {
                textColor = extractColorForKeyword(semantic, "letra");
            }
            if (textColor == null) {
                textColor = extractColorForKeyword(semantic, "letras");
            }
            if (textColor == null) {
                textColor = extractColorForKeyword(semantic, "color del texto");
            }
            if (textColor == null) {
                textColor = extractColorForKeyword(semantic, "color de texto");
            }
            putGroupActiveChange(changesByProperty, "TEXT_COLOR", textColor, null);
        }
        if (containsAny(semantic, "sin borde", "quita el borde", "quitar el borde")) {
            putGroupActiveChange(changesByProperty, "BORDER_WIDTH", "0px", null);
        } else if (!changesByProperty.containsKey("BORDER_COLOR")) {
            String borderColor = extractColorForKeyword(semantic, "borde");
            if (borderColor == null) {
                borderColor = extractColorForKeyword(semantic, "bordes");
            }
            if (borderColor == null) {
                borderColor = extractColorForKeyword(semantic, "contorno");
            }
            if (borderColor == null) {
                borderColor = extractColorForKeyword(semantic, "border");
            }
            putGroupActiveChange(changesByProperty, "BORDER_COLOR", borderColor, null);
        }

        return new ArrayList<>(changesByProperty.values());
    }

    private void putGroupActiveChange(Map<String, UiCustomizationChangeDTO> changesByProperty,
                                      String property,
                                      String value,
                                      String valuePreset) {
        String normalizedProperty = normalizeUpper(property);
        if (!GROUP_ACTIVE_REPAIR_PROPERTIES.contains(normalizedProperty)) {
            return;
        }
        if ((value == null || value.isBlank()) && (valuePreset == null || valuePreset.isBlank())) {
            return;
        }
        UiCustomizationChangeDTO change = new UiCustomizationChangeDTO();
        change.setArea("CHAT_LIST_ITEM_GROUP_ACTIVE");
        change.setProperty(normalizedProperty);
        change.setValue(value == null || value.isBlank() ? null : value.trim());
        change.setValuePreset(valuePreset == null || valuePreset.isBlank() ? null : valuePreset.trim());
        changesByProperty.putIfAbsent(normalizedProperty, change);
    }

    private boolean isEligibleGroupActiveArea(String area) {
        return area == null
                || area.isBlank()
                || "CHAT_LIST_ITEM_ACTIVE".equals(normalizeUpper(area))
                || "CHAT_LIST_ITEM_GROUP_ACTIVE".equals(normalizeUpper(area));
    }

    private String extractColorForKeyword(String semantic, String keyword) {
        if (semantic == null || semantic.isBlank() || keyword == null || keyword.isBlank()) {
            return null;
        }
        int index = semantic.indexOf(keyword);
        while (index >= 0) {
            String window = semantic.substring(index, Math.min(semantic.length(), index + 64));
            Matcher matcher = HEX_COLOR_PATTERN.matcher(window);
            if (matcher.find()) {
                return matcher.group().toLowerCase(Locale.ROOT);
            }
            for (Map.Entry<String, String> entry : GROUP_ACTIVE_COLOR_MAP.entrySet()) {
                if (window.contains(entry.getKey())) {
                    return entry.getValue();
                }
            }
            index = semantic.indexOf(keyword, index + keyword.length());
        }
        return null;
    }

    private boolean isGroupActiveChatRequest(String semantic) {
        String normalized = normalizeSemanticText(semantic);
        boolean mentionsGroup = containsAny(normalized, "grupal", "grupales", "grupo", "grupos");
        boolean mentionsActive = containsAny(normalized,
                "activo", "activos", "seleccionado", "seleccionados", "pulsado", "pulsados", "marcado", "marcados");
        boolean mentionsChat = containsAny(normalized, "chat", "chats", "item", "items", "fila", "filas");
        return mentionsGroup && mentionsActive && (mentionsChat || mentionsGroup);
    }

    private String resolveUiStartedMessage(AiSearchIntentInternalResponseDTO intentResponse) {
        String action = intentResponse == null ? null : normalizeUpper(intentResponse.getAction());
        if ("UPDATE_STYLE_GROUP".equals(action)) {
            return "Interpretando grupo de estilos...";
        }
        if ("UPDATE_STYLE_MULTI".equals(action)) {
            return "Interpretando cambios visuales...";
        }
        if ("RESET_THEME".equals(action)) {
            return "Preparando restauracion de estilos...";
        }
        return "Interpretando cambio visual...";
    }

    private String resolveUiReadyMessage(String action) {
        String normalizedAction = normalizeUpper(action);
        if ("UPDATE_STYLE_GROUP".equals(normalizedAction)) {
            return "Grupo de estilos preparado";
        }
        if ("UPDATE_STYLE_MULTI".equals(normalizedAction)) {
            return "Cambios visuales preparados";
        }
        if ("RESET_THEME".equals(normalizedAction)) {
            return "Restauracion de estilos preparada";
        }
        return "Cambio visual preparado";
    }

    private void notifyUiProgress(Long userId,
                                  String requestId,
                                  String step,
                                  String status,
                                  String message,
                                  String action,
                                  String area,
                                  String property) {
        if (userId == null || messagingTemplate == null) {
            return;
        }
        String username = securityUtils.getAuthenticatedUserEmail();
        if (username == null || username.isBlank()) {
            return;
        }
        try {
            AiSearchProgressWS payload = new AiSearchProgressWS(
                    requestId,
                    step,
                    status,
                    message,
                    null,
                    "UI_CUSTOMIZATION",
                    null
            );
            messagingTemplate.convertAndSendToUser(username, Constantes.WS_QUEUE_AI_SEARCH_PROGRESS, payload);
            LOGGER.info("[AI][UI_CUSTOMIZATION_PROGRESS] requestId={} step={} status={} message=\"{}\"",
                    requestId, step, status, message);
            LOGGER.info("[AI][UI_CUSTOMIZATION_PROGRESS] requestId={} action={} area={} property={}",
                    requestId, action, area, property);
        } catch (Exception ex) {
            LOGGER.warn("[AI][UI_CUSTOMIZATION_PROGRESS] requestId={} ws-send-error step={} status={} errorClass={}",
                    requestId, step, status, ex.getClass().getSimpleName());
        }
    }

    private List<String> normalizeScheduledMissingFields(AiSearchIntentInternalResponseDTO intentResponse) {
        if (intentResponse == null || intentResponse.getScheduledMissingFields() == null) {
            return List.of();
        }
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        for (String field : intentResponse.getScheduledMissingFields()) {
            String n = normalizeUpper(field);
            if ("RECIPIENT".equals(n) || "MESSAGE".equals(n) || "DATETIME".equals(n)) {
                normalized.add(n);
            }
        }
        return new ArrayList<>(normalized);
    }

    private AiEncryptedMessageSearchResponseDTO buildScheduledNeedsClarification(List<String> missingFields, String clarificationQuestion) {
        AiEncryptedMessageSearchResponseDTO response = new AiEncryptedMessageSearchResponseDTO();
        response.setSuccess(false);
        response.setCodigo("SCHEDULED_MESSAGE_NEEDS_CLARIFICATION");
        response.setTarget("SCHEDULED_MESSAGE_CREATE");
        response.setNeedsClarification(Boolean.TRUE);
        response.setScheduledMissingFields(missingFields == null ? List.of() : missingFields);
        response.setClarificationQuestion(hasText(clarificationQuestion) ? clarificationQuestion : defaultClarificationQuestion(missingFields));
        response.setMensaje("Faltan datos para programar el mensaje.");
        return response;
    }

    private String defaultClarificationQuestion(List<String> missingFields) {
        if (missingFields == null || missingFields.isEmpty()) {
            return "Necesito más detalles para programar el mensaje.";
        }
        boolean recipient = missingFields.contains("RECIPIENT");
        boolean message = missingFields.contains("MESSAGE");
        boolean datetime = missingFields.contains("DATETIME");
        if (recipient && message && datetime) {
            return "¿A quién quieres enviarlo, qué mensaje y para qué día y hora?";
        }
        if (recipient && datetime) {
            return "¿A quién quieres enviarlo y para qué día y hora?";
        }
        if (recipient && message) {
            return "¿A quién quieres enviarlo y qué mensaje quieres programar?";
        }
        if (message && datetime) {
            return "¿Qué mensaje quieres programar y para qué día y hora?";
        }
        if (recipient) {
            return "¿A quién quieres enviar el mensaje programado?";
        }
        if (message) {
            return "¿Qué mensaje quieres programar?";
        }
        return "¿Para qué día y hora quieres programar el mensaje?";
    }

    private AiEncryptedMessageSearchResponseDTO buildScheduledInvalidDate() {
        AiEncryptedMessageSearchResponseDTO response = new AiEncryptedMessageSearchResponseDTO();
        response.setSuccess(false);
        response.setCodigo("SCHEDULED_MESSAGE_INVALID_DATE");
        response.setMensaje("La fecha indicada ya ha pasado.");
        response.setTarget("SCHEDULED_MESSAGE_CREATE");
        response.setNeedsClarification(Boolean.FALSE);
        return response;
    }

    private AiEncryptedMessageSearchResponseDTO buildScheduledBackendEncryptionNotAvailable() {
        AiEncryptedMessageSearchResponseDTO response = new AiEncryptedMessageSearchResponseDTO();
        response.setSuccess(false);
        response.setCodigo("SCHEDULED_MESSAGE_BACKEND_ENCRYPTION_NOT_AVAILABLE");
        response.setMensaje("No se pudo crear el mensaje programado porque el backend aún no puede generar el payload E2E.");
        response.setTarget("SCHEDULED_MESSAGE_CREATE");
        response.setNeedsClarification(Boolean.FALSE);
        return response;
    }

    private ResolvedScheduledChat resolveScheduledChat(Long userId, String recipientName, String scheduledTargetType) {
        if (chatIndividualRepository == null || chatGrupalRepository == null) {
            return null;
        }
        String normalizedRecipient = normalizeSemanticText(recipientName);
        if (!hasText(normalizedRecipient)) {
            return null;
        }
        LinkedHashMap<Long, ResolvedScheduledChat> matches = new LinkedHashMap<>();
        boolean includeGroups = !"INDIVIDUAL".equals(normalizeUpper(scheduledTargetType));
        boolean includeIndividuals = !"GROUP".equals(normalizeUpper(scheduledTargetType));

        if (includeIndividuals) {
            List<ChatIndividualEntity> individualChats = chatIndividualRepository.findAllByUsuario1IdOrUsuario2Id(userId, userId);
            for (ChatIndividualEntity chat : individualChats) {
                if (chat == null || chat.isAdminDirect()) {
                    continue;
                }
                UsuarioEntity other = chat.getUsuario1() != null && userId.equals(chat.getUsuario1().getId())
                        ? chat.getUsuario2()
                        : chat.getUsuario1();
                String displayName = other == null ? null : other.getNombre();
                if (isNameMatch(normalizedRecipient, displayName)) {
                    Long recipientId = other == null ? null : other.getId();
                    matches.putIfAbsent(chat.getId(), new ResolvedScheduledChat(chat.getId(), recipientId, displayName, false));
                }
            }
        }

        if (includeGroups) {
            List<ChatGrupalEntity> groupChats = chatGrupalRepository.findAllByUsuariosId(userId);
            for (ChatGrupalEntity chat : groupChats) {
                if (chat == null || !chat.isActivo() || chat.isClosed()) {
                    continue;
                }
                if (isNameMatch(normalizedRecipient, chat.getNombreGrupo())) {
                    matches.putIfAbsent(chat.getId(), new ResolvedScheduledChat(chat.getId(), null, chat.getNombreGrupo(), true));
                }
            }
        }

        if (matches.isEmpty()) {
            return null;
        }
        if (matches.size() > 1) {
            return ResolvedScheduledChat.ambiguousResult();
        }
        return matches.values().iterator().next();
    }

    private boolean isNameMatch(String normalizedRecipient, String candidateName) {
        String normalizedCandidate = normalizeSemanticText(candidateName);
        if (!hasText(normalizedRecipient) || !hasText(normalizedCandidate)) {
            return false;
        }
        return normalizedCandidate.equals(normalizedRecipient)
                || normalizedCandidate.startsWith(normalizedRecipient + " ")
                || normalizedRecipient.startsWith(normalizedCandidate + " ");
    }

    private String toUtcIsoOrNull(String dateIso, String timezone) {
        if (!hasText(dateIso)) {
            return null;
        }
        ZoneId zone;
        try {
            zone = ZoneId.of(hasText(timezone) ? timezone : "Europe/Madrid");
        } catch (Exception ex) {
            zone = ZoneId.of("Europe/Madrid");
        }
        try {
            OffsetDateTime odt = OffsetDateTime.parse(dateIso);
            return odt.toInstant().toString();
        } catch (DateTimeParseException ignored) {
        }
        try {
            ZonedDateTime zdt = ZonedDateTime.parse(dateIso);
            return zdt.toInstant().toString();
        } catch (DateTimeParseException ignored) {
        }
        try {
            LocalDateTime ldt = LocalDateTime.parse(dateIso, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            return ldt.atZone(zone).toInstant().toString();
        } catch (DateTimeParseException ignored) {
            return null;
        }
    }

    private String resolveScheduledDateTimeFallback(String scheduledExpression, String consulta, String timezone) {
        String combined = ((scheduledExpression == null ? "" : scheduledExpression) + " " + (consulta == null ? "" : consulta)).trim();
        String normalized = normalizeSemanticText(combined);
        if (!hasText(normalized)) {
            return null;
        }
        ZoneId zone;
        try {
            zone = ZoneId.of(hasText(timezone) ? timezone : "Europe/Madrid");
        } catch (Exception ex) {
            zone = ZoneId.of("Europe/Madrid");
        }

        LocalDateTime now = LocalDateTime.now(zone);
        LocalDateTime relativeDateTime = resolveRelativeDateTimeFromText(normalized, now);
        if (relativeDateTime != null) {
            return relativeDateTime.atZone(zone).toOffsetDateTime().toString();
        }
        LocalDateTime baseDateTime = resolveBaseDateTimeFromText(normalized, now);
        if (baseDateTime == null) {
            return null;
        }
        int[] hm = extractHourMinute(normalized);
        if (hm == null) {
            return null;
        }
        return baseDateTime.withHour(hm[0]).withMinute(hm[1]).withSecond(0).withNano(0).atZone(zone).toOffsetDateTime().toString();
    }

    private LocalDateTime resolveBaseDateTimeFromText(String normalized, LocalDateTime now) {
        if (containsAny(normalized, "pasado manana")) {
            return now.plusDays(2);
        }
        if (containsAny(normalized, "manana")) {
            return now.plusDays(1);
        }
        if (containsAny(normalized, "hoy", "esta noche", "esta tarde")) {
            return now;
        }
        DayOfWeek targetDay = resolveWeekday(normalized);
        if (targetDay != null) {
            return now.with(TemporalAdjusters.nextOrSame(targetDay));
        }
        return null;
    }

    private LocalDateTime resolveRelativeDateTimeFromText(String normalized, LocalDateTime now) {
        Matcher minutesMatcher = Pattern.compile("dentro de\\s+(\\d{1,4})\\s+min").matcher(normalized);
        if (minutesMatcher.find()) {
            Integer value = parseBoundedInt(minutesMatcher.group(1), 1, 1440);
            if (value != null) {
                return now.plusMinutes(value).withSecond(0).withNano(0);
            }
        }
        Matcher hoursMatcher = Pattern.compile("dentro de\\s+(\\d{1,3})\\s+hora").matcher(normalized);
        if (hoursMatcher.find()) {
            Integer value = parseBoundedInt(hoursMatcher.group(1), 1, 168);
            if (value != null) {
                return now.plusHours(value).withSecond(0).withNano(0);
            }
        }
        return null;
    }

    private DayOfWeek resolveWeekday(String normalized) {
        if (containsAny(normalized, "lunes")) return DayOfWeek.MONDAY;
        if (containsAny(normalized, "martes")) return DayOfWeek.TUESDAY;
        if (containsAny(normalized, "miercoles", "miércoles")) return DayOfWeek.WEDNESDAY;
        if (containsAny(normalized, "jueves")) return DayOfWeek.THURSDAY;
        if (containsAny(normalized, "viernes")) return DayOfWeek.FRIDAY;
        if (containsAny(normalized, "sabado", "sábado")) return DayOfWeek.SATURDAY;
        if (containsAny(normalized, "domingo")) return DayOfWeek.SUNDAY;
        return null;
    }

    private int[] extractHourMinute(String normalized) {
        if (!hasText(normalized)) {
            return null;
        }
        Pattern hmPattern = Pattern.compile("a las\\s+(\\d{1,2})\\s*(?::|y)\\s*(\\d{1,2})");
        Matcher hmMatcher = hmPattern.matcher(normalized);
        if (hmMatcher.find()) {
            Integer hour = parseBoundedInt(hmMatcher.group(1), 0, 23);
            Integer minute = parseBoundedInt(hmMatcher.group(2), 0, 59);
            if (hour != null && minute != null) {
                return new int[]{hour, minute};
            }
        }
        Pattern hPattern = Pattern.compile("a las\\s+(\\d{1,2})(?!\\s*(?::|y))");
        Matcher hMatcher = hPattern.matcher(normalized);
        if (hMatcher.find()) {
            Integer hour = parseBoundedInt(hMatcher.group(1), 0, 23);
            if (hour == null) {
                return null;
            }
            int minute = 0;
            if (containsAny(normalized, "esta noche") && hour >= 1 && hour <= 11) {
                hour += 12;
            }
            return new int[]{hour, minute};
        }
        return null;
    }

    private Integer parseBoundedInt(String raw, int min, int max) {
        if (!hasText(raw)) {
            return null;
        }
        try {
            int value = Integer.parseInt(raw.trim());
            if (value < min || value > max) {
                return null;
            }
            return value;
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private boolean isPastDateTime(String utcIso) {
        try {
            return OffsetDateTime.parse(utcIso).isBefore(OffsetDateTime.now(ZoneId.of("UTC")));
        } catch (DateTimeParseException ex) {
            return true;
        }
    }

    private String normalizeForSearch(String text) {
        String normalized = normalizeSemanticText(text);
        return hasText(normalized) ? normalized.trim() : null;
    }

    private String safeTrim(String value) {
        return value == null ? null : value.trim();
    }

    private String resolveConsultaForAi(String consultaRaw) {
        if (!hasText(consultaRaw)) {
            return consultaRaw;
        }
        String trimmed = consultaRaw.trim();
        if (!looksLikeEncryptedPayload(trimmed)) {
            return consultaRaw;
        }
        if (aiEncryptedContextService == null) {
            return consultaRaw;
        }
        try {
            String decrypted = aiEncryptedContextService.decryptMessagePayload(trimmed);
            return hasText(decrypted) ? decrypted : null;
        } catch (RuntimeException ex) {
            LOGGER.warn("[AI][SMART_ACTION][DECRYPT_FALLBACK] errorClass={}", ex.getClass().getSimpleName());
            return null;
        }
    }

    private boolean looksLikeEncryptedPayload(String value) {
        return hasText(value)
                && value.startsWith("{")
                && value.contains("\"iv\"")
                && value.contains("\"ciphertext\"")
                && (value.contains("\"forEmisor\"") || value.contains("\"forReceptor\"") || value.contains("\"forAdmin\""));
    }

    private String buildEncryptedScheduledConfirmation(Long authUserId,
                                                       ResolvedScheduledChat resolvedChat,
                                                       String message,
                                                       String contenidoBusqueda,
                                                       String scheduledAtUtc,
                                                       String scheduledBatchId) {
        LinkedHashMap<String, Object> confirmation = new LinkedHashMap<>();
        confirmation.put("chatId", resolvedChat == null ? null : resolvedChat.chatId());
        confirmation.put("recipientId", resolvedChat == null ? null : resolvedChat.recipientId());
        confirmation.put("recipientName", resolvedChat == null ? null : resolvedChat.displayName());
        confirmation.put("senderName", resolveUserDisplayName(authUserId));
        confirmation.put("message", message);
        confirmation.put("contenidoBusqueda", contenidoBusqueda);
        confirmation.put("scheduledAt", scheduledAtUtc);
        confirmation.put("scheduledBatchId", scheduledBatchId);
        confirmation.put("status", "PENDING");
        try {
            String confirmationJson = OBJECT_MAPPER.writeValueAsString(confirmation);
            AiEncryptedResponseDTO encrypted = aiEncryptedContextService.encryptAiResponseForUser(confirmationJson, authUserId);
            if (encrypted == null || !encrypted.isSuccess() || !hasText(encrypted.getEncryptedPayload())) {
                return null;
            }
            return encrypted.getEncryptedPayload();
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("No se pudo serializar la confirmación programada", ex);
        }
    }

    private String buildScheduledE2EPayload(Long authUserId,
                                            ResolvedScheduledChat resolvedChat,
                                            String message,
                                            Instant scheduledAt) {
        if (authUserId == null || resolvedChat == null || resolvedChat.chatId() == null || !hasText(message) || scheduledAt == null) {
            return null;
        }
        UsuarioEntity emisor = usuarioRepository.findById(authUserId).orElse(null);
        if (emisor == null) {
            return null;
        }
        if (resolvedChat.group()) {
            ChatGrupalEntity group = chatGrupalRepository.findByIdWithUsuarios(resolvedChat.chatId()).orElse(null);
            if (group == null || !group.isActivo() || group.isClosed()) {
                return null;
            }
            List<UsuarioEntity> receptores = group.getUsuarios() == null ? List.of() : group.getUsuarios().stream()
                    .filter(Objects::nonNull)
                    .filter(u -> u.getId() != null && !u.getId().equals(authUserId))
                    .collect(Collectors.toList());
            return cifradorE2EMensajeProgramadoService.cifrarTextoGrupal(message, emisor, receptores).payloadJson();
        }
        if (resolvedChat.recipientId() == null) {
            return null;
        }
        UsuarioEntity receptor = usuarioRepository.findById(resolvedChat.recipientId()).orElse(null);
        if (receptor == null) {
            return null;
        }
        return cifradorE2EMensajeProgramadoService.cifrarTextoIndividual(message, emisor, receptor).payloadJson();
    }

    private static final class ResolvedScheduledChat {
        private final Long chatId;
        private final Long recipientId;
        private final String displayName;
        private final boolean group;
        private final boolean ambiguous;

        private ResolvedScheduledChat(Long chatId, Long recipientId, String displayName, boolean group, boolean ambiguous) {
            this.chatId = chatId;
            this.recipientId = recipientId;
            this.displayName = displayName;
            this.group = group;
            this.ambiguous = ambiguous;
        }

        private ResolvedScheduledChat(Long chatId, Long recipientId, String displayName, boolean group) {
            this(chatId, recipientId, displayName, group, false);
        }

        private static ResolvedScheduledChat ambiguousResult() {
            return new ResolvedScheduledChat(null, null, null, false, true);
        }

        private Long chatId() {
            return chatId;
        }

        private String displayName() {
            return displayName;
        }

        private Long recipientId() {
            return recipientId;
        }

        private boolean ambiguous() {
            return ambiguous;
        }

        private boolean group() {
            return group;
        }
    }
}
