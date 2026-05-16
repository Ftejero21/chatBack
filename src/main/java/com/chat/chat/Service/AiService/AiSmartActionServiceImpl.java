package com.chat.chat.Service.AiService;

import com.chat.chat.DTO.AiEncryptedMessageSearchRequestDTO;
import com.chat.chat.DTO.AiEncryptedMessageSearchResponseDTO;
import com.chat.chat.DTO.AiSearchIntentInternalRequestDTO;
import com.chat.chat.DTO.AiSearchIntentInternalResponseDTO;
import com.chat.chat.DTO.AiSmartActionRequestDTO;
import com.chat.chat.DTO.AiUiCustomizationResponseDTO;
import com.chat.chat.DTO.AiSearchProgressWS;
import com.chat.chat.DTO.UiCustomizationContextDTO;
import com.chat.chat.Repository.UsuarioRepository;
import com.chat.chat.Utils.Constantes;
import com.chat.chat.Utils.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.Locale;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

@Service
public class AiSmartActionServiceImpl implements AiSmartActionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AiSmartActionServiceImpl.class);
    private static final Set<String> SUPPORTED_TARGETS = Set.of(
            "MESSAGES",
            "COMPLAINTS_CREATED",
            "COMPLAINTS_RECEIVED",
            "MIXED",
            "SCHEDULED_MESSAGES",
            "UNREAD_MESSAGES",
            "OFFENSIVE_CONTENT_SEARCH",
            "APP_REPORT",
            "APP_REPORT_STATUS",
            "UI_CUSTOMIZATION"
    );
    private static final Set<String> ALLOWED_THEME_MODES = Set.of("LIGHT", "DARK");
    private static final Set<String> ALLOWED_UI_CONTEXT_SCOPES = Set.of("CHAT_LIST");
    private static final int MAX_UI_AREAS = 20;
    private static final int MAX_UI_PROPERTIES_PER_AREA = 20;
    private static final int MAX_UI_VALUE_LENGTH = 50;
    private static final Set<String> ALLOWED_UI_AREAS = Set.of(
            "CHAT_LIST_PANEL", "CHAT_LIST_HEADER", "CHAT_LIST_SEARCH", "CHAT_LIST_FILTERS", "CHAT_LIST_FILTER_BUTTONS",
            "CHAT_LIST_ITEM", "CHAT_LIST_ITEM_GROUP", "CHAT_LIST_ITEM_CHILDREN", "CHAT_LIST_PREVIEW",
            "CHAT_LIST_ITEM_PREVIEW", "CHAT_LIST_ITEM_DRAFT_PREVIEW", "CHAT_LIST_ITEM_AUDIO_PREVIEW",
            "CHAT_LIST_ITEM_IMAGE_PREVIEW", "CHAT_LIST_ITEM_FILE_PREVIEW", "CHAT_LIST_ITEM_BADGES",
            "CHAT_LIST_ITEM_ACTIONS_SCOPED", "CHAT_LIST_ITEM_STATUS_PILLS", "CHAT_LIST_ITEM_NAME_SCOPED",
            "CHAT_LIST_ITEM_GROUP_NAME", "CHAT_LIST_ITEM_GROUP_PREVIEW", "CHAT_LIST_ITEM_GROUP_DRAFT_PREVIEW",
            "CHAT_LIST_ITEM_GROUP_AUDIO_PREVIEW", "CHAT_LIST_ITEM_GROUP_IMAGE_PREVIEW", "CHAT_LIST_ITEM_GROUP_FILE_PREVIEW",
            "CHAT_LIST_ITEM_GROUP_BADGES", "CHAT_LIST_ITEM_GROUP_ACTIONS", "CHAT_LIST_ITEM_GROUP_STATUS_PILLS",
            "CHAT_LIST_GROUP_PILL", "CHAT_LIST_STATUS_PILLS", "CHAT_LIST_DRAFT_PREVIEW",
            "CHAT_LIST_AUDIO_PREVIEW", "CHAT_LIST_ITEM_ACTIONS", "CHAT_LIST_ITEM_ACTIVE", "CHAT_LIST_ITEM_UNREAD",
            "CHAT_LIST_FILTER_BUTTONS_ACTIVE", "CHAT_LIST_PIN_MENU", "CHAT_LIST_PIN_MENU_ITEM", "CHAT_LIST_PIN_TOGGLE",
            "CHAT_LIST_TITLE", "CHAT_LIST_HEADER_ACTIONS", "CHAT_LIST_SCROLL", "CHAT_LIST_AVATAR",
            "CHAT_LIST_ITEM_CONTENT", "CHAT_LIST_ITEM_NAME", "CHAT_LIST_IMAGE_PREVIEW", "CHAT_LIST_FILE_PREVIEW",
            "CHAT_LIST_ACTIONS_MENU", "CHAT_LIST_ACTIONS_MENU_ITEM"
    );
    private static final Set<String> ALLOWED_UI_PROPERTIES = Set.of(
            "BACKGROUND_COLOR", "TEXT_COLOR", "BORDER_COLOR", "BORDER_WIDTH", "BORDER_RADIUS", "FONT_SIZE", "HOVER_BACKGROUND_COLOR",
            "ICON_COLOR", "ACTIVE_BACKGROUND_COLOR", "ACTIVE_TEXT_COLOR", "BADGE_COLOR",
            "PREVIEW_SENDER_TEXT_COLOR", "LABEL_COLOR", "SEPARATOR_COLOR", "TIME_COLOR",
            "REPORTED_BACKGROUND_COLOR", "REPORTED_TEXT_COLOR", "BLOCKED_BACKGROUND_COLOR", "BLOCKED_TEXT_COLOR",
            "SHADOW_PRESET"
    );
    private static final int MAX_UI_HINTS_PER_PARENT = 20;

    private final SecurityUtils securityUtils;
    private final UsuarioRepository usuarioRepository;
    private final AiSearchIntentMicroserviceClient aiSearchIntentMicroserviceClient;
    private final AiEncryptedMessageSearchService aiEncryptedMessageSearchService;
    private final AiUiCustomizationValidationService aiUiCustomizationValidationService;
    private final SimpMessagingTemplate messagingTemplate;

    public AiSmartActionServiceImpl(SecurityUtils securityUtils,
                                    UsuarioRepository usuarioRepository,
                                    AiSearchIntentMicroserviceClient aiSearchIntentMicroserviceClient,
                                    AiEncryptedMessageSearchService aiEncryptedMessageSearchService,
                                    AiUiCustomizationValidationService aiUiCustomizationValidationService,
                                    SimpMessagingTemplate messagingTemplate) {
        this.securityUtils = securityUtils;
        this.usuarioRepository = usuarioRepository;
        this.aiSearchIntentMicroserviceClient = aiSearchIntentMicroserviceClient;
        this.aiEncryptedMessageSearchService = aiEncryptedMessageSearchService;
        this.aiUiCustomizationValidationService = aiUiCustomizationValidationService;
        this.messagingTemplate = messagingTemplate;
    }

    @Override
    public Object process(AiSmartActionRequestDTO request) {
        Long userId = securityUtils.getAuthenticatedUserId();
        if (userId == null) {
            return buildUiFailure("UI_CUSTOMIZATION_UNAUTHORIZED", "Usuario no autenticado");
        }

        String requestId = UUID.randomUUID().toString();
        String consulta = request == null ? null : request.getConsulta();
        LOGGER.info("[AI][SMART_ACTION] requestId={} consulta=\"{}\"", requestId, safe(consulta));

        AiSearchIntentInternalRequestDTO intentRequest = new AiSearchIntentInternalRequestDTO();
        intentRequest.setRequestId(requestId);
        intentRequest.setConsulta(consulta);
        intentRequest.setUsuarioActualNombre(resolveUserDisplayName(userId));
        UiCustomizationContextDTO sanitizedUiContext = sanitizeUiContext(requestId, request == null ? null : request.getUiContext());
        intentRequest.setUiContext(sanitizedUiContext);

        AiSearchIntentInternalResponseDTO intentResponse = aiSearchIntentMicroserviceClient.classifyIntent(requestId, intentRequest);
        String normalizedTarget = normalizeTarget(intentResponse == null ? null : intentResponse.getTarget());
        boolean forcedUiTarget = shouldForceUiCustomization(consulta);
        if (forcedUiTarget) {
            LOGGER.info("[AI][SMART_ACTION][UI_FORCE] reason=visual_intent_detected consulta=\"{}\"", safe(consulta));
            normalizedTarget = "UI_CUSTOMIZATION";
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
        }
        LOGGER.info("[AI][SMART_ACTION_INTENT] requestId={} target={} confidence={}",
                requestId, normalizedTarget, intentResponse == null ? null : intentResponse.getConfidence());

        AiEncryptedMessageSearchRequestDTO searchRequest = buildSearchRequest(request);
        if (intentResponse == null || !intentResponse.isSuccess() || !SUPPORTED_TARGETS.contains(normalizedTarget)) {
            LOGGER.info("[AI][SMART_ACTION_ROUTING] requestId={} branch=LEGACY_FALLBACK", requestId);
            Object response = aiEncryptedMessageSearchService.buscarMensajes(searchRequest);
            logResponse(requestId, response);
            return response;
        }

        intentResponse.setTarget(normalizedTarget);
        LOGGER.info("[AI][SMART_ACTION_ROUTING] requestId={} branch={}", requestId, normalizedTarget);

        Object response;
        if ("UI_CUSTOMIZATION".equals(normalizedTarget)) {
            notifyUiProgress(userId, requestId, "UI_CUSTOMIZATION_ANALYZING", "STARTED", resolveUiStartedMessage(intentResponse),
                    intentResponse.getAction(), intentResponse.getArea(), intentResponse.getProperty());
            notifyUiProgress(userId, requestId, "UI_CUSTOMIZATION_ANALYZING", "COMPLETED", "Interpretacion visual completada",
                    intentResponse.getAction(), intentResponse.getArea(), intentResponse.getProperty());
            notifyUiProgress(userId, requestId, "UI_CUSTOMIZATION_VALIDATING", "STARTED", "Validando cambios visuales...",
                    intentResponse.getAction(), intentResponse.getArea(), intentResponse.getProperty());
            try {
                response = aiUiCustomizationValidationService.validate(
                        requestId,
                        consulta,
                        intentResponse.getAction(),
                        intentResponse.getArea(),
                        intentResponse.getProperty(),
                        intentResponse.getValue(),
                        intentResponse.getValuePreset(),
                        intentResponse.getLabel(),
                        intentResponse.getConfidence(),
                        intentResponse.getColorIntent(),
                        intentResponse.getChanges(),
                        sanitizedUiContext
                );
                if (response instanceof AiUiCustomizationResponseDTO ui) {
                    int changesCount = ui.getChanges() == null ? 0 : ui.getChanges().size();
                    LOGGER.info("[AI][UI_CUSTOMIZATION_VALIDATE_RESULT] requestId={} consulta=\"{}\" action={} area={} property={} changesCount={} changes={}",
                            requestId, safe(consulta), ui.getAction(), ui.getArea(), ui.getProperty(), changesCount, ui.getChanges());
                }
                if (response instanceof AiUiCustomizationResponseDTO ui && ui.isSuccess()) {
                    notifyUiProgress(userId, requestId, "UI_CUSTOMIZATION_VALIDATING", "COMPLETED", "Validacion visual completada",
                            ui.getAction(), ui.getArea(), ui.getProperty());
                    notifyUiProgress(userId, requestId, "UI_CUSTOMIZATION_READY", "COMPLETED", resolveUiReadyMessage(ui.getAction()),
                            ui.getAction(), ui.getArea(), ui.getProperty());
                } else {
                    notifyUiProgress(userId, requestId, "UI_CUSTOMIZATION_FAILED", "FAILED", "No se pudo preparar el cambio visual",
                            intentResponse.getAction(), intentResponse.getArea(), intentResponse.getProperty());
                }
            } catch (RuntimeException ex) {
                notifyUiProgress(userId, requestId, "UI_CUSTOMIZATION_FAILED", "FAILED", "No se pudo preparar el cambio visual",
                        intentResponse.getAction(), intentResponse.getArea(), intentResponse.getProperty());
                throw ex;
            }
        } else {
            response = aiEncryptedMessageSearchService.buscarMensajes(searchRequest, requestId, intentResponse, true);
        }

        logResponse(requestId, response);
        return response;
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

    private UiCustomizationContextDTO sanitizeUiContext(String requestId, UiCustomizationContextDTO context) {
        if (context == null) {
            return null;
        }
        String scope = normalizeUpper(context.getScope());
        String themeMode = normalizeUpper(context.getThemeMode());
        Map<String, Map<String, String>> sanitizedStyles = sanitizeStylesMap(context.getCurrentStyles());
        Map<String, Map<String, String>> sanitizedComputed = sanitizeStylesMap(context.getComputedStyles());
        Map<String, List<String>> sanitizedHints = sanitizeGroupHints(context.getGroupExpansionHints());
        boolean validScope = ALLOWED_UI_CONTEXT_SCOPES.contains(scope);
        boolean validTheme = ALLOWED_THEME_MODES.contains(themeMode);
        if (sanitizedStyles.isEmpty() && sanitizedComputed.isEmpty() && sanitizedHints.isEmpty()) {
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
        if (context.getDomState() != null && !context.getDomState().isEmpty()) {
            sanitized.setDomState(new LinkedHashMap<>(context.getDomState()));
        }
        if (context.getAreaCatalog() != null && !context.getAreaCatalog().isEmpty()) {
            sanitized.setAreaCatalog(new LinkedHashMap<>(context.getAreaCatalog()));
        }
        LOGGER.info("[AI][UI_CONTEXT] requestId={} scope={} styles={} computed={} hints={}",
                requestId, sanitized.getScope(),
                sanitizedStyles.size(), sanitizedComputed.size(), sanitizedHints.size());
        LOGGER.info("[AI][UI_CONTEXT_VALIDATE] requestId={} validAreas={} ignoredAreas={}",
                requestId, sanitizedStyles.size() + sanitizedComputed.size(), 0);
        return sanitized;
    }

    private Map<String, Map<String, String>> sanitizeStylesMap(Map<String, Map<String, String>> source) {
        Map<String, Map<String, String>> sanitizedStyles = new LinkedHashMap<>();
        if (source == null || source.isEmpty()) {
            return sanitizedStyles;
        }
        for (Map.Entry<String, Map<String, String>> areaEntry : source.entrySet()) {
            if (sanitizedStyles.size() >= MAX_UI_AREAS) {
                break;
            }
            String area = normalizeUpper(areaEntry.getKey());
            if (!ALLOWED_UI_AREAS.contains(area) || areaEntry.getValue() == null) {
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

    private Map<String, List<String>> sanitizeGroupHints(Map<String, List<String>> source) {
        Map<String, List<String>> sanitized = new LinkedHashMap<>();
        if (source == null || source.isEmpty()) {
            return sanitized;
        }
        for (Map.Entry<String, List<String>> entry : source.entrySet()) {
            String parent = normalizeUpper(entry.getKey());
            if (!ALLOWED_UI_AREAS.contains(parent) || entry.getValue() == null || entry.getValue().isEmpty()) {
                continue;
            }
            List<String> children = new ArrayList<>();
            for (String child : entry.getValue()) {
                if (children.size() >= MAX_UI_HINTS_PER_PARENT) {
                    break;
                }
                String normalizedChild = normalizeUpper(child);
                if (ALLOWED_UI_AREAS.contains(normalizedChild)) {
                    children.add(normalizedChild);
                }
            }
            if (!children.isEmpty()) {
                sanitized.put(parent, children);
            }
        }
        return sanitized;
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
        boolean hasActionVerb = containsAny(normalized, "pon", "cambia", "modifica", "ajusta", "haz", "quita", "restaura", "aplica");
        boolean hasVisualWord = containsAny(normalized,
                "estilo", "estilos", "color", "colores", "fondo", "texto", "borde", "sombra", "tamano",
                "grande", "pequeno", "claro", "oscuro", "elegante", "tema",
                "blanco", "negro", "rojo", "azul", "verde", "morado", "violeta", "amarillo", "purpura");
        boolean hasUiArea = containsAny(normalized,
                "listado de chats", "lista de chats", "panel de chats", "zona izquierda",
                "filtros", "filtrado", "boton activo", "chat no leido", "chats no leidos",
                "preview", "previews", "imagen", "archivo", "desplegable", "menu de opciones",
                "badge", "contador", "etiqueta", "pill");
        return hasActionVerb && hasVisualWord && hasUiArea;
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
}
