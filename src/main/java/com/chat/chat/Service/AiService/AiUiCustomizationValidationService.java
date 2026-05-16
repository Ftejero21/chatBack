package com.chat.chat.Service.AiService;

import com.chat.chat.DTO.AiUiCustomizationResponseDTO;
import com.chat.chat.DTO.ColorIntentDTO;
import com.chat.chat.DTO.UiCustomizationContextDTO;
import com.chat.chat.DTO.UiCustomizationChangeDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class AiUiCustomizationValidationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AiUiCustomizationValidationService.class);
    private static final double MIN_CONFIDENCE = 0.75d;
    private static final String CLARIFICATION_MESSAGE = "Que zona quieres cambiar: fondo del chat, mensajes, listado de chats, barra lateral o popup IA?";

    private static final Set<String> ALLOWED_AREAS = Set.of(
            "MAIN_LAYOUT",
            "SIDEBAR_NAV",
            "SIDEBAR_NAV_ITEM",
            "SIDEBAR_NAV_ACTIVE_ITEM",
            "TOPBAR",
            "TOPBAR_PROFILE",
            "CHAT_LIST_PANEL",
            "CHAT_LIST_SEARCH",
            "CHAT_LIST_FILTERS",
            "CHAT_LIST_FILTER_BUTTONS",
            "CHAT_LIST_FILTER_BUTTONS_ACTIVE",
            "CHAT_LIST_ITEM",
            "CHAT_LIST_ITEM_GROUP",
            "CHAT_LIST_ITEM_PREVIEW",
            "CHAT_LIST_ITEM_DRAFT_PREVIEW",
            "CHAT_LIST_ITEM_AUDIO_PREVIEW",
            "CHAT_LIST_ITEM_IMAGE_PREVIEW",
            "CHAT_LIST_ITEM_FILE_PREVIEW",
            "CHAT_LIST_ITEM_BADGES",
            "CHAT_LIST_ITEM_ACTIONS_SCOPED",
            "CHAT_LIST_ITEM_STATUS_PILLS",
            "CHAT_LIST_ITEM_NAME_SCOPED",
            "CHAT_LIST_ITEM_GROUP_NAME",
            "CHAT_LIST_ITEM_GROUP_PREVIEW",
            "CHAT_LIST_ITEM_GROUP_AUDIO_PREVIEW",
            "CHAT_LIST_ITEM_GROUP_DRAFT_PREVIEW",
            "CHAT_LIST_ITEM_GROUP_IMAGE_PREVIEW",
            "CHAT_LIST_ITEM_GROUP_FILE_PREVIEW",
            "CHAT_LIST_ITEM_GROUP_BADGES",
            "CHAT_LIST_ITEM_GROUP_ACTIONS",
            "CHAT_LIST_ITEM_GROUP_STATUS_PILLS",
            "CHAT_LIST_ITEM_CHILDREN",
            "CHAT_LIST_PREVIEW",
            "CHAT_LIST_GROUP_PILL",
            "CHAT_LIST_BADGES",
            "CHAT_LIST_STATUS_PILLS",
            "CHAT_LIST_DRAFT_PREVIEW",
            "CHAT_LIST_AUDIO_PREVIEW",
            "CHAT_LIST_ITEM_ACTIONS",
            "CHAT_LIST_TITLE",
            "CHAT_LIST_HEADER_ACTIONS",
            "CHAT_LIST_SCROLL",
            "CHAT_LIST_AVATAR",
            "CHAT_LIST_ITEM_CONTENT",
            "CHAT_LIST_ITEM_NAME",
            "CHAT_LIST_IMAGE_PREVIEW",
            "CHAT_LIST_FILE_PREVIEW",
            "CHAT_LIST_ACTIONS_MENU",
            "CHAT_LIST_ACTIONS_MENU_ITEM",
            "CHAT_LIST_PIN_TOGGLE",
            "CHAT_LIST_PIN_MENU",
            "CHAT_LIST_PIN_MENU_ITEM",
            "CHAT_LIST_PIN_MENU_DANGER",
            "CHAT_LIST_PIN_MENU_REPORT",
            "CHAT_LIST_ITEM_ACTIVE",
            "CHAT_LIST_ITEM_UNREAD",
            "CHAT_LIST_HEADER",
            "CHAT_HEADER",
            "CHAT_HEADER_STATUS",
            "CHAT_MESSAGES_AREA",
            "MESSAGE_BUBBLES",
            "OWN_MESSAGE_BUBBLE",
            "OTHER_MESSAGE_BUBBLE",
            "GROUP_MESSAGE_BUBBLE",
            "MESSAGE_META",
            "MESSAGE_REACTIONS",
            "CHAT_COMPOSER",
            "CHAT_COMPOSER_TEXTAREA",
            "CHAT_COMPOSER_ACTIONS",
            "CHAT_COMPOSER_SEND_BUTTON",
            "COMPOSE_ACTIONS_POPUP",
            "MESSAGE_OPTIONS_DROPDOWN",
            "REPLY_BANNER",
            "AI_SEARCH_POPUP",
            "AI_SEARCH_RESULTS",
            "AI_ASK_POPUP",
            "GROUP_INFO_PANEL",
            "USER_INFO_PANEL",
            "STARRED_MESSAGES_PANEL",
            "PUBLIC_CHATS_PANEL",
            "CREATE_GROUP_MODAL",
            "REPORT_USER_POPUP",
            "POLL_COMPOSER",
            "SCHEDULE_MESSAGE_COMPOSER",
            "TEMPORARY_MESSAGE_POPUP",
            "MEDIA_PREVIEW"
    );

    private static final Set<String> ALLOWED_PROPERTIES = Set.of(
            "BACKGROUND_COLOR",
            "TEXT_COLOR",
            "BORDER_COLOR",
            "BORDER_WIDTH",
            "BORDER_RADIUS",
            "FONT_SIZE",
            "COLOR",
            "ICON_COLOR",
            "ACTIVE_BACKGROUND_COLOR",
            "ACTIVE_TEXT_COLOR",
            "ACTIVE_ICON_COLOR",
            "HOVER_BACKGROUND_COLOR",
            "PREVIEW_SENDER_TEXT_COLOR",
            "LABEL_COLOR",
            "SEPARATOR_COLOR",
            "TIME_COLOR",
            "REPORTED_BACKGROUND_COLOR",
            "REPORTED_TEXT_COLOR",
            "BLOCKED_BACKGROUND_COLOR",
            "BLOCKED_TEXT_COLOR",
            "PLACEHOLDER_COLOR",
            "BADGE_COLOR",
            "SEND_BUTTON_COLOR",
            "INPUT_BACKGROUND_COLOR",
            "CARD_BACKGROUND_COLOR",
            "HEADER_BACKGROUND_COLOR",
            "SHADOW_PRESET",
            "DENSITY",
            "OPACITY",
            "BLUR",
            "BACKGROUND_IMAGE"
    );
    private static final Set<String> ALLOWED_ACTIONS = Set.of("UPDATE_STYLE", "UPDATE_STYLE_GROUP", "UPDATE_STYLE_MULTI", "RESET_THEME");
    private static final int MAX_EXPLICIT_MULTI_CHANGES = 30;
    private static final int MAX_FINAL_MULTI_CHANGES = 80;
    private static final int MAX_THEME_MULTI_CHANGES = 120;

    private static final Set<String> COLOR_PROPERTIES = Set.of(
            "BACKGROUND_COLOR",
            "TEXT_COLOR",
            "BORDER_COLOR",
            "COLOR",
            "ICON_COLOR",
            "ACTIVE_BACKGROUND_COLOR",
            "ACTIVE_TEXT_COLOR",
            "ACTIVE_ICON_COLOR",
            "HOVER_BACKGROUND_COLOR",
            "PREVIEW_SENDER_TEXT_COLOR",
            "LABEL_COLOR",
            "SEPARATOR_COLOR",
            "TIME_COLOR",
            "REPORTED_BACKGROUND_COLOR",
            "REPORTED_TEXT_COLOR",
            "BLOCKED_BACKGROUND_COLOR",
            "BLOCKED_TEXT_COLOR",
            "PLACEHOLDER_COLOR",
            "BADGE_COLOR",
            "SEND_BUTTON_COLOR",
            "INPUT_BACKGROUND_COLOR",
            "CARD_BACKGROUND_COLOR",
            "HEADER_BACKGROUND_COLOR"
    );

    private static final Set<String> DENSITY_VALUES = Set.of("COMPACT", "NORMAL", "COMFORTABLE");
    private static final Map<String, String> BORDER_RADIUS_PRESETS = Map.of(
            "NONE", "4px",
            "SOFT", "12px",
            "NORMAL", "18px",
            "ROUNDED", "24px",
            "PILL", "999px"
    );
    private static final Set<String> BORDER_WIDTH_VALUES = Set.of("0px", "1px", "2px", "3px", "4px");
    private static final Map<String, String> FONT_SIZE_PRESETS = Map.of(
            "SMALL", "13px",
            "NORMAL", "14px",
            "LARGE", "16px",
            "XL", "18px"
    );
    private static final Set<Integer> SAFE_FONT_SIZES = Set.of(10, 11, 12, 13, 14, 16, 18, 20, 22, 24, 28, 32);
    private static final Set<String> SAFE_BORDER_RADIUS_VALUES = Set.of("0px", "4px", "8px", "12px", "16px", "18px", "24px", "32px", "999px");
    private static final Set<String> GROUP_ONLY_ALLOWED_AREAS = Set.of(
            "CHAT_LIST_ITEM_GROUP",
            "CHAT_LIST_GROUP_PILL",
            "CHAT_LIST_ITEM_GROUP_PREVIEW",
            "CHAT_LIST_ITEM_GROUP_AUDIO_PREVIEW",
            "CHAT_LIST_ITEM_GROUP_DRAFT_PREVIEW",
            "CHAT_LIST_ITEM_GROUP_IMAGE_PREVIEW",
            "CHAT_LIST_ITEM_GROUP_FILE_PREVIEW",
            "CHAT_LIST_ITEM_GROUP_BADGES",
            "CHAT_LIST_ITEM_GROUP_ACTIONS",
            "CHAT_LIST_ITEM_GROUP_STATUS_PILLS"
    );
    private static final Set<String> GROUP_ONLY_FORBIDDEN_GENERIC_AREAS = Set.of(
            "CHAT_LIST_PREVIEW",
            "CHAT_LIST_DRAFT_PREVIEW",
            "CHAT_LIST_AUDIO_PREVIEW",
            "CHAT_LIST_IMAGE_PREVIEW",
            "CHAT_LIST_FILE_PREVIEW",
            "CHAT_LIST_STATUS_PILLS",
            "CHAT_LIST_ITEM_ACTIONS",
            "CHAT_LIST_ITEM_ACTIVE",
            "CHAT_LIST_ITEM",
            "CHAT_LIST_PANEL",
            "CHAT_LIST_FILTERS",
            "CHAT_LIST_SEARCH",
            "CHAT_LIST_PIN_MENU"
    );
    private static final Set<String> INDIVIDUAL_ONLY_ALLOWED_AREAS = Set.of(
            "CHAT_LIST_ITEM",
            "CHAT_LIST_ITEM_PREVIEW",
            "CHAT_LIST_ITEM_DRAFT_PREVIEW",
            "CHAT_LIST_ITEM_AUDIO_PREVIEW",
            "CHAT_LIST_ITEM_IMAGE_PREVIEW",
            "CHAT_LIST_ITEM_FILE_PREVIEW",
            "CHAT_LIST_ITEM_BADGES",
            "CHAT_LIST_ITEM_ACTIONS_SCOPED",
            "CHAT_LIST_ITEM_STATUS_PILLS",
            "CHAT_LIST_ITEM_NAME_SCOPED"
    );
    private static final Map<String, String> SHADOW_PRESETS = Map.of(
            "NONE", "none",
            "SOFT", "0 4px 12px rgba(0,0,0,0.12)",
            "NORMAL", "0 8px 24px rgba(0,0,0,0.18)",
            "STRONG", "0 12px 32px rgba(0,0,0,0.28)"
    );
    private static final Map<String, String> BLUR_PRESETS = Map.of(
            "NONE", "0px",
            "SOFT", "4px",
            "NORMAL", "8px"
    );

    private static final Map<String, String> NAMED_COLORS = new HashMap<>();
    static {
        NAMED_COLORS.put("rojo", "#ef4444");
        NAMED_COLORS.put("verde", "#16a34a");
        NAMED_COLORS.put("verde oscuro", "#14532d");
        NAMED_COLORS.put("azul", "#2563eb");
        NAMED_COLORS.put("morado", "#7c3aed");
        NAMED_COLORS.put("blanco", "#ffffff");
        NAMED_COLORS.put("negro", "#111827");
        NAMED_COLORS.put("negro intenso", "#000000");
        NAMED_COLORS.put("negro puro", "#000000");
        NAMED_COLORS.put("negro total", "#000000");
        NAMED_COLORS.put("negro app", "#111827");
        NAMED_COLORS.put("negro suave", "#111827");
        NAMED_COLORS.put("oscuro", "#111827");
        NAMED_COLORS.put("gris oscuro", "#1f2937");
        NAMED_COLORS.put("gris", "#6b7280");
        NAMED_COLORS.put("amarillo", "#eab308");
        NAMED_COLORS.put("naranja", "#f97316");
        NAMED_COLORS.put("rosa", "#ec4899");
        NAMED_COLORS.put("red", "#ef4444");
        NAMED_COLORS.put("blue", "#2563eb");
        NAMED_COLORS.put("purple", "#7c3aed");
        NAMED_COLORS.put("pink", "#ec4899");
        NAMED_COLORS.put("white", "#ffffff");
        NAMED_COLORS.put("black", "#111827");
        NAMED_COLORS.put("gray", "#6b7280");
        NAMED_COLORS.put("grey", "#6b7280");
        NAMED_COLORS.put("green", "#16a34a");
        NAMED_COLORS.put("orange", "#f97316");
        NAMED_COLORS.put("yellow", "#eab308");
        NAMED_COLORS.put("cyan", "#06b6d4");
        NAMED_COLORS.put("teal", "#14b8a6");
        NAMED_COLORS.put("indigo", "#6366f1");
        NAMED_COLORS.put("brown", "#92400e");
    }

    public AiUiCustomizationResponseDTO validate(String requestId,
                                                 String consulta,
                                                 String action,
                                                 String area,
                                                 String property,
                                                 String value,
                                                 String valuePreset,
                                                 String label,
                                                 Double confidence,
                                                 ColorIntentDTO colorIntent,
                                                 List<UiCustomizationChangeDTO> inputChanges,
                                                 UiCustomizationContextDTO uiContext) {
        String normalizedAction = hasText(action) ? action : "UPDATE_STYLE";
        if (!ALLOWED_ACTIONS.contains(normalizedAction)) {
            return failure(action, area, property, value, valuePreset, label, confidence,
                    "UI_CUSTOMIZATION_NOT_ALLOWED", "La accion solicitada no esta permitida.");
        }
        action = normalizedAction;
        if ("UPDATE_STYLE".equals(action) && inputChanges != null && inputChanges.size() > 1) {
            action = "UPDATE_STYLE_MULTI";
        }
        Map<String, Map<String, String>> contextStyles = mergeContextStyles(uiContext);
        boolean relativeStyleRequest = isRelativeStyleRequest(consulta);
        if (relativeStyleRequest && (contextStyles == null || contextStyles.isEmpty())) {
            LOGGER.info("[AI][UI_CUSTOMIZATION_CONTEXT_USED] requestId={} used=false reason=NO_CONTEXT", requestId);
        }

        if (relativeStyleRequest && contextStyles != null && !contextStyles.isEmpty()) {
            LOGGER.info("[AI][UI_CUSTOMIZATION_CONTEXT_USED] requestId={} used=true reason=RELATIVE_STYLE", requestId);
            RelativeResolution resolution = resolveRelativeWithContext(requestId, consulta, action, area, property, value, contextStyles);
            if (resolution != null) {
                action = resolution.action();
                area = resolution.area();
                property = resolution.property();
                value = resolution.value();
                if (hasText(resolution.valuePreset())) {
                    valuePreset = resolution.valuePreset();
                }
                if (resolution.changes() != null) {
                    inputChanges = resolution.changes();
                }
            }
        }

        if ("UPDATE_STYLE".equals(action) && isGroupAudioPreviewScopedRequest(consulta)) {
            action = "UPDATE_STYLE_MULTI";
            inputChanges = buildGroupAudioPreviewScopedColorGroup(hasText(value) ? value : "#f97316");
            logRuleMatch("GROUP_AUDIO_PREVIEW_SCOPED", consulta, inputChanges);
            area = null;
            property = null;
            value = null;
        } else if ("UPDATE_STYLE".equals(action) && isIndividualAudioPreviewScopedRequest(consulta)) {
            action = "UPDATE_STYLE_MULTI";
            inputChanges = buildIndividualAudioPreviewScopedColorGroup(hasText(value) ? value : "#f97316");
            logRuleMatch("INDIVIDUAL_AUDIO_PREVIEW_SCOPED", consulta, inputChanges);
            area = null;
            property = null;
            value = null;
        } else if ("UPDATE_STYLE".equals(action) && isAudioPreviewRequest(consulta)) {
            area = "CHAT_LIST_AUDIO_PREVIEW";
            property = inferAudioPreviewProperty(consulta, property);
        }
        if ("TEXT_COLOR".equals(property) && isPxValue(value)) {
            property = "FONT_SIZE";
            if (!hasText(valuePreset)) {
                valuePreset = inferFontSizePreset(value);
            }
        }
        if ("UPDATE_STYLE".equals(action) && isGroupPillRequest(consulta)) {
            area = "CHAT_LIST_GROUP_PILL";
        }
        if ("UPDATE_STYLE".equals(action)) {
            area = resolveFiltersContainerAreaFromConsulta(consulta, area);
        }
        if ("UPDATE_STYLE".equals(action)) {
            area = resolveFilterButtonsAreaFromConsulta(consulta, area);
        }
        if ("UPDATE_STYLE".equals(action)) {
            area = resolveChatStateAreaFromConsulta(consulta, area);
        }
        if ("UPDATE_STYLE".equals(action)) {
            area = resolvePinMenuAreaFromConsulta(consulta, area);
            property = inferPinMenuProperty(consulta, property);
        }
        if (isGroupBadgesScopedRequest(consulta)) {
            String requestedBackgroundColor = resolveRequestedBackgroundColor(consulta);
            String requestedTextColor = resolveRequestedTextColor(consulta);
            String forcedBaseColor = hasText(requestedBackgroundColor)
                    ? requestedBackgroundColor
                    : resolveForcedBaseColor(consulta, value, inputChanges, "#fb7185");
            action = "UPDATE_STYLE_MULTI";
            inputChanges = buildGroupBadgesScopedColorGroup(forcedBaseColor, requestedTextColor);
            LOGGER.info("[AI][UI_CUSTOMIZATION_RULE_MATCH] rule=FORCE_GROUP_BADGES_SCOPED_COLORS consulta={} bg={} text={}",
                    safe(consulta), safe(forcedBaseColor), safe(requestedTextColor));
            area = null;
            property = null;
            value = null;
        }
        if (isGroupAudioPreviewScopedRequest(consulta)) {
            String forcedBaseColor = resolveForcedBaseColor(consulta, value, inputChanges, "#f97316");
            action = "UPDATE_STYLE_MULTI";
            inputChanges = buildGroupAudioPreviewScopedColorGroup(forcedBaseColor);
            logRuleMatch("FORCE_GROUP_AUDIO_PREVIEW_SCOPED", consulta, inputChanges);
            area = null;
            property = null;
            value = null;
        }
        if (isPinMenuWholeMenuRequest(consulta)) {
            String forcedBaseColor = resolveForcedBaseColor(consulta, value, inputChanges, "#7c3aed");
            inputChanges = completePinMenuThemeChanges(forcedBaseColor, inputChanges);
            logRuleMatch("FORCE_PIN_MENU_FULL_GROUP", consulta, inputChanges);
            action = "UPDATE_STYLE_MULTI";
            area = null;
            property = null;
            value = null;
        }
        if ("TEXT_COLOR".equals(property)
                && isPxValue(value)
                && isFontSizeIntent(consulta, label)) {
            property = "FONT_SIZE";
            if (!hasText(valuePreset)) {
                valuePreset = inferFontSizePreset(value);
            }
        }
        if ("UPDATE_STYLE".equals(action) && isUnreadBadgeRequest(consulta)) {
            area = "CHAT_LIST_BADGES";
        }
        if ("UPDATE_STYLE".equals(action) && isGroupBadgesScopedRequest(consulta)) {
            action = "UPDATE_STYLE_MULTI";
            inputChanges = buildGroupBadgesScopedColorGroup(hasText(value) ? value : "#7c3aed");
            logRuleMatch("GROUP_BADGES_SCOPED", consulta, inputChanges);
            area = null;
            property = null;
            value = null;
        }
        if ("UPDATE_STYLE".equals(action) && isIndividualPreviewTextRequest(consulta)) {
            area = "CHAT_LIST_ITEM_PREVIEW";
            property = "TEXT_COLOR";
            logRuleMatch("INDIVIDUAL_PREVIEW_TEXT", consulta, List.of(change("CHAT_LIST_ITEM_PREVIEW", "TEXT_COLOR", hasText(value) ? value : "")));
        }
        if ("UPDATE_STYLE".equals(action) && isIndividualFilePreviewRequest(consulta)) {
            action = "UPDATE_STYLE_MULTI";
            inputChanges = buildIndividualFilePreviewScopedColorGroup(hasText(value) ? value : "#2563eb");
            logRuleMatch("INDIVIDUAL_FILE_PREVIEW_SCOPED", consulta, inputChanges);
            area = null;
            property = null;
            value = null;
        }
        if ("BORDER_COLOR".equals(property) && isPxValue(value)) {
            property = "BORDER_WIDTH";
        }
        if ("TEXT_COLOR".equals(property) && looksLikeFontSizeValue(value) && isPreviewTextSizeRequest(consulta, area)) {
            property = "FONT_SIZE";
            valuePreset = inferFontSizePreset(value);
        }
        if ("UPDATE_STYLE".equals(action) && isRemoveBorderRequest(consulta)) {
            area = resolveReportedAreaFromConsulta(consulta, area);
            if (!hasText(property) || "BORDER_COLOR".equals(property) || "BORDER_WIDTH".equals(property)
                    || "NONE".equals(normalizePreset(valuePreset))) {
                property = "BORDER_WIDTH";
                value = "0px";
                valuePreset = "NONE";
            }
        }
        if ("UPDATE_STYLE".equals(action) && isSpecificListProperty(property) && !isScopedChatItemChildArea(area)) {
            area = resolveGroupAreaFromConsulta(consulta, area);
        }
        if ("UPDATE_STYLE".equals(action)
                && "CHAT_LIST_GROUP_PILL".equals(area)
                && "BACKGROUND_COLOR".equals(property)
                && isGroupPillRequest(consulta)) {
            action = "UPDATE_STYLE_GROUP";
        }
        if ("UPDATE_STYLE".equals(action)
                && ("CHAT_LIST_PIN_MENU".equals(area) || "CHAT_LIST_PIN_MENU_ITEM".equals(area))
                && "BACKGROUND_COLOR".equals(property)
                && isPinMenuWholeMenuRequest(consulta)) {
            action = "UPDATE_STYLE_GROUP";
            area = "CHAT_LIST_PIN_MENU";
        }
        if ("UPDATE_STYLE".equals(action)
                && "CHAT_LIST_AUDIO_PREVIEW".equals(area)
                && "BACKGROUND_COLOR".equals(property)
                && isAudioPreviewThemeRequest(consulta)) {
            action = "UPDATE_STYLE_GROUP";
        }
        if ("UPDATE_STYLE".equals(action)
                && shouldForceChatListPreview(consulta)
                && !isPreviewTextSizeRequest(consulta, area)
                && !looksLikeFontSizeValue(value)) {
            area = isStrictIndividualScope(consulta, area) ? "CHAT_LIST_ITEM_PREVIEW" : (isStrictGroupOnlyScope(consulta, area) ? "CHAT_LIST_ITEM_GROUP_PREVIEW" : "CHAT_LIST_PREVIEW");
            property = "TEXT_COLOR";
        }
        if ("UPDATE_STYLE".equals(action)
                && "BACKGROUND_COLOR".equals(property)
                && "CHAT_LIST_PANEL".equals(area)
                && isChatListPanelThemeRequest(consulta)) {
            action = "UPDATE_STYLE_GROUP";
            logRuleMatch("CHAT_LIST_PANEL_THEME", consulta, List.of(change("CHAT_LIST_PANEL", "BACKGROUND_COLOR", hasText(value) ? value : "")));
        }
        if ("UPDATE_STYLE".equals(action)
                && "BACKGROUND_COLOR".equals(property)
                && isBothIndividualAndGroupScopeRequest(consulta)) {
            action = "UPDATE_STYLE_MULTI";
            inputChanges = new ArrayList<>();
            inputChanges.add(change("CHAT_LIST_ITEM", "BACKGROUND_COLOR", normalizeColorText(value)));
            inputChanges.add(change("CHAT_LIST_ITEM_GROUP", "BACKGROUND_COLOR", normalizeColorText(value)));
            logRuleMatch("BOTH_INDIVIDUAL_AND_GROUP_ITEMS", consulta, inputChanges);
            area = null;
            property = null;
            value = null;
        }
        if ("UPDATE_STYLE".equals(action)
                && "BACKGROUND_COLOR".equals(property)
                && ("CHAT_LIST_ITEM".equals(area) || "CHAT_LIST_ITEM_GROUP".equals(area))
                && isSimpleChatItemsBackgroundRequest(consulta)) {
            action = "UPDATE_STYLE_MULTI";
            inputChanges = buildSimpleChatItemsBackgroundChanges(consulta, area, normalizeColorText(value));
            area = null;
            property = null;
            value = null;
        }
        if ("UPDATE_STYLE".equals(action)
                && "BACKGROUND_COLOR".equals(property)
                && hasText(area)
                && hasGroupExpansionHints(uiContext, area)) {
            action = "UPDATE_STYLE_GROUP";
        }
        if ("UPDATE_STYLE".equals(action)
                && "BACKGROUND_COLOR".equals(property)
                && ("CHAT_LIST_ITEM".equals(area) || "CHAT_LIST_ITEM_GROUP".equals(area))
                && isChatListVisualBlockRequest(consulta)
                && !isSimpleChatItemsBackgroundRequest(consulta)) {
            action = "UPDATE_STYLE_GROUP";
            area = resolveGroupAreaFromConsulta(consulta, area);
        }
        if ("UPDATE_STYLE".equals(action)
                && "BACKGROUND_COLOR".equals(property)
                && hasText(area)
                && isWholeBlockStyleRequest(consulta)) {
            action = "UPDATE_STYLE_GROUP";
        }

        String requestedProperty = property;
        String requestedValue = value;
        boolean propertySpecificColors = hasPropertySpecificColors(consulta);
        if (propertySpecificColors) {
            LOGGER.info("[AI][UI_CUSTOMIZATION_PROPERTY_COLORS] enabled=true bg={} text={}",
                    safe(resolveRequestedBackgroundColor(consulta)),
                    safe(resolveRequestedTextColor(consulta)));
        }
        NormalizedInput normalizedInput = normalizeGlobalInput(requestId, consulta, area, property, value, valuePreset);
        property = normalizedInput.property();
        value = normalizedInput.value();
        valuePreset = normalizedInput.valuePreset();
        AreaProperty normalizedAreaProperty = normalizeAreaProperty(area, property);
        area = normalizedAreaProperty.area();
        property = normalizedAreaProperty.property();

        double effectiveConfidence = normalizeUiConfidence(requestId, consulta, action, area, property, value, confidence);
        if ("RESET_THEME".equals(action)) {
            LOGGER.info("[AI][UI_CUSTOMIZATION_VALIDATE] requestId={} allowed=true area={} property={} value={}",
                    requestId, null, null, null);
            AiUiCustomizationResponseDTO response = baseResponse(action, null, null, null, null, label, confidence);
            response.setColorIntent(colorIntent);
            response.setSuccess(true);
            response.setCodigo("UI_CUSTOMIZATION_OK");
            response.setMensaje("Restauracion solicitada.");
            if (!hasText(response.getLabel())) {
                response.setLabel("Restaurar tema por defecto");
            }
            LOGGER.info("[AI][UI_CUSTOMIZATION_VALIDATE] requestId={} allowed=true action={}", requestId, action);
            return response;
        }

        if ("UPDATE_STYLE".equals(action)
                && isChatListPanelThemeRequest(consulta)
                && (!hasText(area) || !hasText(property) || !hasText(value))) {
            String inferredColor = resolveColorFromText(consulta);
            if (!hasText(inferredColor) && containsAny(normalizeSemanticText(consulta), "negro", "oscuro")) {
                inferredColor = "#111827";
            }
            if (!hasText(inferredColor) && containsAny(normalizeSemanticText(consulta), "blanco", "claro")) {
                inferredColor = "#ffffff";
            }
            if (hasText(inferredColor)) {
                action = "UPDATE_STYLE_GROUP";
                area = "CHAT_LIST_PANEL";
                property = "BACKGROUND_COLOR";
                value = inferredColor;
                logRuleMatch("CHAT_LIST_PANEL_FALLBACK", consulta, List.of(change("CHAT_LIST_PANEL", "BACKGROUND_COLOR", inferredColor)));
            }
        }

        if (isAmbiguousDropdownRequest(consulta)) {
            LOGGER.info("[AI][UI_CUSTOMIZATION_NEEDS_CLARIFICATION] reason=AMBIGUOUS_DROPDOWN consulta={}", safe(consulta));
            return failure(action, area, property, value, valuePreset, label, confidence,
                    "UI_CUSTOMIZATION_NEEDS_CLARIFICATION",
                    "¿Que desplegable quieres cambiar: el de opciones del chat, el de mensajes, perfil u otra zona?");
        }

        if (needsClarification(consulta, area, property, action)) {
            LOGGER.info("[AI][UI_CUSTOMIZATION_VALIDATE] requestId={} allowed=false reason=NEEDS_CLARIFICATION area={} property={} value={}",
                    requestId, area, property, safe(value));
            return failure(action, area, property, value, valuePreset, label, confidence,
                    "UI_CUSTOMIZATION_NEEDS_CLARIFICATION", CLARIFICATION_MESSAGE);
        }

        if (confidence == null || effectiveConfidence < 0.70d) {
            LOGGER.info("[AI][UI_CUSTOMIZATION_VALIDATE] requestId={} allowed=false reason=LOW_CONFIDENCE confidence={} area={} property={}",
                    requestId, effectiveConfidence, area, property);
            return failure(action, area, property, value, valuePreset, label, confidence,
                    "UI_CUSTOMIZATION_LOW_CONFIDENCE", "No estoy seguro del cambio visual solicitado.");
        }

        if (!"UPDATE_STYLE_MULTI".equals(action) && (!hasText(area) || !ALLOWED_AREAS.contains(area))) {
            LOGGER.info("[AI][UI_CUSTOMIZATION_VALIDATE] requestId={} allowed=false reason=AREA_NOT_ALLOWED area={} property={} value={}",
                    requestId, area, property, safe(value));
            return failure(action, area, property, value, valuePreset, label, confidence,
                    "UI_CUSTOMIZATION_NOT_ALLOWED", "Esa zona visual no esta permitida.");
        }

        if ("UPDATE_STYLE_MULTI".equals(action)) {
            boolean aiProvidedChanges = inputChanges != null && !inputChanges.isEmpty();
            List<UiCustomizationChangeDTO> explicitChanges = extractExplicitChanges(consulta, area, property, value, valuePreset, inputChanges);
            boolean autoRepairedWhiteElegant = false;
            boolean highVolumeThemeRequest = isHighVolumeThemeRequest(consulta, area, explicitChanges);
            int maxExplicitChanges = highVolumeThemeRequest ? MAX_THEME_MULTI_CHANGES : MAX_EXPLICIT_MULTI_CHANGES;
            int maxFinalChanges = highVolumeThemeRequest ? MAX_THEME_MULTI_CHANGES : MAX_FINAL_MULTI_CHANGES;
            if (explicitChanges.isEmpty()
                    && effectiveConfidence >= 0.70d
                    && isChatListPanelThemeRequest(consulta)
                    && isWhiteElegantThemeRequest(consulta)) {
                explicitChanges.addAll(buildWhiteElegantChatListThemeChanges());
                autoRepairedWhiteElegant = true;
                LOGGER.info("[AI][UI_MULTI_REPAIR] requestId={} reason=CHAT_LIST_WHITE_ELEGANT generatedChanges={}",
                        requestId, explicitChanges.size());
            }
            LOGGER.info("[AI][UI_MULTI_VALIDATE] requestId={} rootAreaIgnored=true changesCount={}", requestId, explicitChanges.size());
            LOGGER.info("[AI][UI_MULTI_EXPLICIT] requestId={} count={}", requestId, explicitChanges.size());
            if (!autoRepairedWhiteElegant && explicitChanges.size() > maxExplicitChanges) {
                return failure(action, area, property, value, valuePreset, label, confidence,
                        "UI_CUSTOMIZATION_TOO_MANY_CHANGES", "Demasiados cambios explicitos en la solicitud.");
            }

            List<UiCustomizationChangeDTO> generatedChanges = new ArrayList<>();
            if (!aiProvidedChanges) {
                for (UiCustomizationChangeDTO explicit : explicitChanges) {
                    if (isBlockThemeChange(explicit)) {
                        generatedChanges.addAll(buildStyleGroupChanges(consulta, explicit.getArea(), explicit.getProperty(), explicit.getValue()));
                    }
                }
                if (!generatedChanges.isEmpty()) {
                    LOGGER.info("[AI][UI_CUSTOMIZATION_REPAIR_STRUCTURE_ONLY] reason=GENERATED_FROM_EXPLICIT_CHANGES colorsPreserved=true");
                }
            }
            LOGGER.info("[AI][UI_MULTI_GENERATED] requestId={} count={}", requestId, generatedChanges.size());

            Map<String, UiCustomizationChangeDTO> merged = new LinkedHashMap<>();
            for (UiCustomizationChangeDTO generated : generatedChanges) {
                UiCustomizationChangeDTO normalized = normalizeGroupChange(generated);
                if (isValidGroupChange(normalized)) {
                    merged.put(changeKey(normalized), normalized);
                }
            }
            for (UiCustomizationChangeDTO explicit : explicitChanges) {
                UiCustomizationChangeDTO normalized = normalizeGroupChange(explicit);
                if (!isValidGroupChange(normalized)) {
                    LOGGER.info("[AI][UI_CUSTOMIZATION_VALUE_REJECTED] area={} property={} value={} reason=INVALID_GROUP_CHANGE",
                            safe(explicit == null ? null : explicit.getArea()),
                            safe(explicit == null ? null : explicit.getProperty()),
                            safe(explicit == null ? null : explicit.getValue()));
                    continue;
                }
                LOGGER.info("[AI][UI_CUSTOMIZATION_VALUE_PRESERVED] area={} property={} value={}",
                        normalized.getArea(), normalized.getProperty(), safe(normalized.getValue()));
                String key = changeKey(normalized);
                if (merged.containsKey(key)) {
                    UiCustomizationChangeDTO generated = merged.get(key);
                    LOGGER.info("[AI][UI_MULTI_CONFLICT_RESOLVED] requestId={} area={} property={} winner=EXPLICIT",
                            requestId, normalized.getArea(), normalized.getProperty());
                    LOGGER.info("[AI][UI_MULTI_CONFLICT_RESOLVED] requestId={} area={} property={} winner=EXPLICIT generated={} explicit={}",
                            requestId, normalized.getArea(), normalized.getProperty(), safe(generated == null ? null : generated.getValue()), safe(normalized.getValue()));
                }
                merged.put(key, normalized);
            }
            List<UiCustomizationChangeDTO> finalChanges = new ArrayList<>(merged.values());
            if (isStrictGroupOnlyScope(consulta, area)) {
                finalChanges = sanitizeGroupOnlyChanges(finalChanges);
            } else if (isStrictIndividualScope(consulta, area)) {
                finalChanges = sanitizeIndividualOnlyChanges(finalChanges);
            }
            LOGGER.info("[AI][UI_CUSTOMIZATION_VALIDATE_SCOPED] requestId={} scoped={} genericRejected={}",
                    requestId,
                    isStrictGroupOnlyScope(consulta, area) ? "GROUP_ONLY" : (isStrictIndividualScope(consulta, area) ? "INDIVIDUAL_ONLY" : "NONE"),
                    explicitChanges.size() - finalChanges.size());
            LOGGER.info("[AI][UI_MULTI_FINAL] requestId={} count={}", requestId, finalChanges.size());
            if (finalChanges.isEmpty()) {
                return failure(action, area, property, value, valuePreset, label, confidence,
                        "UI_CUSTOMIZATION_NEEDS_CLARIFICATION", CLARIFICATION_MESSAGE);
            }
            if (finalChanges.size() > maxFinalChanges) {
                return failure(action, area, property, value, valuePreset, label, confidence,
                        "UI_CUSTOMIZATION_TOO_MANY_CHANGES", "La solicitud genera demasiados cambios.");
            }

            AiUiCustomizationResponseDTO response = baseResponse(action, null, null, null, valuePreset, label, confidence);
            response.setColorIntent(colorIntent);
            response.setChanges(finalChanges);
            response.setSuccess(true);
            response.setCodigo("UI_CUSTOMIZATION_OK");
            response.setMensaje("Cambios visuales multiples interpretados correctamente.");
            LOGGER.info("[AI][UI_CUSTOMIZATION_VALIDATE] requestId={} inputChangesCount={} outputChangesCount={} normalized={} rejected={} reason={}",
                    requestId, explicitChanges.size(), finalChanges.size(), Boolean.TRUE.equals(response.getNormalized()), false, aiProvidedChanges ? "AI_VALUES_ACCEPTED" : "REPAIRED_TEMPLATE");
            LOGGER.info("[AI][UI_CUSTOMIZATION_VALIDATE] requestId={} allowed=true normalized={} repaired=false action={}",
                    requestId, Boolean.TRUE.equals(response.getNormalized()), action);
            return response;
        }

        if ("UPDATE_STYLE_GROUP".equals(action)) {
            if ((inputChanges == null || inputChanges.isEmpty())
                    && hasText(area)
                    && hasText(property)
                    && hasText(value)
                    && !"BACKGROUND_COLOR".equals(property)
                    && ALLOWED_AREAS.contains(area)
                    && ALLOWED_PROPERTIES.contains(property)) {
                action = "UPDATE_STYLE";
            }
        }

        if ("UPDATE_STYLE_GROUP".equals(action)) {
            String effectiveArea = resolveGroupAreaFromConsulta(consulta, area);
            if (!"BACKGROUND_COLOR".equals(property)
                    || (!"CHAT_LIST_PANEL".equals(effectiveArea)
                    && !"CHAT_LIST_FILTERS".equals(effectiveArea)
                    && !"CHAT_LIST_ITEM".equals(effectiveArea)
                    && !"CHAT_LIST_ITEM_GROUP".equals(effectiveArea)
                    && !"CHAT_LIST_AUDIO_PREVIEW".equals(effectiveArea)
                    && !"CHAT_LIST_GROUP_PILL".equals(effectiveArea)
                    && !"CHAT_LIST_PIN_MENU".equals(effectiveArea)
                    && !"CHAT_LIST_STATUS_PILLS".equals(effectiveArea)
                    && !"CHAT_LIST_SEARCH".equals(effectiveArea))) {
                return failure(action, area, property, value, valuePreset, label, confidence,
                        "UI_CUSTOMIZATION_NOT_ALLOWED", "El grupo solicitado no esta permitido.");
            }
            String resolvedBaseValue = resolveValue(property, value, valuePreset);
            if (!hasText(resolvedBaseValue)) {
                return failure(action, area, property, value, valuePreset, label, confidence,
                        "UI_CUSTOMIZATION_NOT_ALLOWED", "El valor solicitado no es valido.");
            }

            boolean repaired = inputChanges == null || inputChanges.isEmpty();
            List<UiCustomizationChangeDTO> candidateChanges = (inputChanges != null && !inputChanges.isEmpty())
                    ? new ArrayList<>(inputChanges)
                    : ("CHAT_LIST_PANEL".equals(effectiveArea)
                    ? buildChatListPanelColorGroup(consulta, resolvedBaseValue)
                    : "CHAT_LIST_FILTERS".equals(effectiveArea)
                    ? buildFiltersColorGroup(resolvedBaseValue)
                    : "CHAT_LIST_AUDIO_PREVIEW".equals(effectiveArea)
                    ? buildAudioPreviewColorGroup(resolvedBaseValue)
                    : "CHAT_LIST_GROUP_PILL".equals(effectiveArea)
                    ? buildGroupPillColorGroup(resolvedBaseValue)
                    : "CHAT_LIST_PIN_MENU".equals(effectiveArea)
                    ? buildPinMenuColorGroup(resolvedBaseValue)
                    : "CHAT_LIST_STATUS_PILLS".equals(effectiveArea)
                    ? buildStatusPillsColorGroup(resolvedBaseValue)
                    : "CHAT_LIST_SEARCH".equals(effectiveArea)
                    ? buildSearchColorGroup(resolvedBaseValue)
                    : buildChatListItemColorGroup(consulta, effectiveArea, resolvedBaseValue));
            if ("CHAT_LIST_ITEM_GROUP".equals(effectiveArea)) {
                List<UiCustomizationChangeDTO> sanitizedGroupOnly = sanitizeGroupOnlyChanges(candidateChanges);
                if (sanitizedGroupOnly.isEmpty()) {
                    sanitizedGroupOnly = sanitizeGroupOnlyChanges(buildChatListItemColorGroup(consulta, "CHAT_LIST_ITEM_GROUP", resolvedBaseValue));
                }
                candidateChanges = sanitizedGroupOnly;
            }
            if (isStrictIndividualScope(consulta, area)) {
                candidateChanges = sanitizeIndividualOnlyChanges(candidateChanges);
            }
            if (candidateChanges.isEmpty() && inputChanges != null) {
                candidateChanges = inputChanges;
                repaired = false;
            }
            int valid = 0;
            int invalid = 0;
            List<UiCustomizationChangeDTO> validated = new ArrayList<>();
            for (UiCustomizationChangeDTO change : candidateChanges) {
                if (isValidGroupChange(change)) {
                    UiCustomizationChangeDTO normalized = normalizeGroupChange(change);
                    validated.add(normalized);
                    LOGGER.info("[AI][UI_CUSTOMIZATION_VALUE_PRESERVED] area={} property={} value={}",
                            normalized.getArea(), normalized.getProperty(), safe(normalized.getValue()));
                    valid++;
                } else {
                    LOGGER.info("[AI][UI_CUSTOMIZATION_VALUE_REJECTED] area={} property={} value={} reason=INVALID_GROUP_CHANGE",
                            safe(change == null ? null : change.getArea()),
                            safe(change == null ? null : change.getProperty()),
                            safe(change == null ? null : change.getValue()));
                    invalid++;
                }
            }
            LOGGER.info("[AI][UI_CUSTOMIZATION_GROUP] requestId={} area={} baseProperty={} baseValue={} totalChanges={}",
                    requestId, effectiveArea, property, resolvedBaseValue, candidateChanges.size());
            if (repaired) {
                LOGGER.info("[AI][UI_GROUP_REPAIR] requestId={} area={} value={} generatedChanges={}",
                        requestId, effectiveArea, resolvedBaseValue, candidateChanges.size());
                LOGGER.info("[AI][UI_CUSTOMIZATION_REPAIR_STRUCTURE_ONLY] reason=GROUP_TEMPLATE_REPAIR colorsPreserved=true");
            }
            LOGGER.info("[AI][UI_CUSTOMIZATION_VALIDATE] requestId={} group=true validChanges={} invalidChanges={}",
                    requestId, valid, invalid);
            if (validated.isEmpty() || invalid > 0) {
                return failure(action, area, property, value, valuePreset, label, confidence,
                        "UI_CUSTOMIZATION_NOT_ALLOWED", "Algunos cambios del grupo no estan permitidos.");
            }
            AiUiCustomizationResponseDTO response = baseResponse(action, effectiveArea, property, resolvedBaseValue, valuePreset, label, confidence);
            response.setColorIntent(colorIntent);
            response.setChanges(validated);
            response.setSuccess(true);
            response.setCodigo("UI_CUSTOMIZATION_OK");
            response.setMensaje("Cambio visual de grupo interpretado correctamente.");
            applyNormalizationMetadata(response, normalizedInput, requestedProperty, requestedValue, resolvedBaseValue);
            LOGGER.info("[AI][UI_CUSTOMIZATION_VALIDATE_COLORS] requestId={} inputChanges={} outputChanges={} normalized={} reason={}",
                    requestId, candidateChanges.size(), validated.size(), Boolean.TRUE.equals(response.getNormalized()),
                    "SAFE_VALUES");
            LOGGER.info("[AI][UI_CUSTOMIZATION_VALIDATE] requestId={} inputChangesCount={} outputChangesCount={} normalized={} rejected={} reason={}",
                    requestId, inputChanges == null ? 0 : inputChanges.size(), validated.size(), Boolean.TRUE.equals(response.getNormalized()), invalid > 0, repaired ? "REPAIRED_TEMPLATE" : "AI_VALUES_ACCEPTED");
            LOGGER.info("[AI][UI_CUSTOMIZATION_VALIDATE] requestId={} allowed=true normalized={} repaired={}", requestId, Boolean.TRUE.equals(response.getNormalized()), repaired);
            LOGGER.info("[AI][UI_CUSTOMIZATION_VALIDATE] requestId={} allowed=true action={}", requestId, action);
            return response;
        }

        if (!hasText(property) || !ALLOWED_PROPERTIES.contains(property)) {
            LOGGER.info("[AI][UI_CUSTOMIZATION_VALIDATE] requestId={} allowed=false reason=PROPERTY_NOT_ALLOWED area={} property={} value={}",
                    requestId, area, property, safe(value));
            return failure(action, area, property, value, valuePreset, label, confidence,
                    "UI_CUSTOMIZATION_NOT_ALLOWED", "Esa propiedad visual no esta permitida.");
        }
        if (!isPropertyAllowedForArea(area, property)) {
            LOGGER.info("[AI][UI_CUSTOMIZATION_VALIDATE] requestId={} allowed=false reason=AREA_PROPERTY_NOT_ALLOWED area={} property={} value={}",
                    requestId, area, property, safe(value));
            return failure(action, area, property, value, valuePreset, label, confidence,
                    "UI_CUSTOMIZATION_NOT_ALLOWED", "La propiedad no esta permitida para esa area.");
        }

        if ("BACKGROUND_IMAGE".equals(property)) {
            LOGGER.info("[AI][UI_CUSTOMIZATION_VALIDATE] requestId={} allowed=false reason=SAFE_IMAGE_FLOW_REQUIRED area={} property={} value={}",
                    requestId, area, property, safe(value));
            return failure(action, area, property, value, valuePreset, label, confidence,
                    "UI_CUSTOMIZATION_NEEDS_SAFE_IMAGE_FLOW", "El cambio de imagen necesita un flujo seguro de imagen.");
        }

        String resolvedValue = resolveValue(property, value, valuePreset);
        if (!hasText(resolvedValue) && "UPDATE_STYLE".equals(action) && "BORDER_COLOR".equals(property)
                && isRemoveBorderRequest(consulta)) {
            resolvedValue = "transparent";
        }
        if (!hasText(resolvedValue) && "UPDATE_STYLE".equals(action) && "BORDER_WIDTH".equals(property)
                && isRemoveBorderRequest(consulta)) {
            resolvedValue = "0px";
        }
        if (!hasText(resolvedValue)) {
            LOGGER.info("[AI][UI_CUSTOMIZATION_VALUE_REJECTED] area={} property={} value={} reason=INVALID_VALUE",
                    safe(area), safe(property), safe(value));
            LOGGER.info("[AI][UI_CUSTOMIZATION_VALIDATE] requestId={} allowed=false reason=INVALID_VALUE area={} property={} value={}",
                    requestId, area, property, safe(value));
            return failure(action, area, property, value, valuePreset, label, confidence,
                    "UI_CUSTOMIZATION_NOT_ALLOWED", "El valor solicitado no es valido.");
        }
        LOGGER.info("[AI][UI_CUSTOMIZATION_VALUE_PRESERVED] area={} property={} value={}",
                area, property, safe(resolvedValue));

        LOGGER.info("[AI][UI_CUSTOMIZATION_VALIDATE] requestId={} allowed=true action={} area={} property={} value={}",
                requestId, action, area, property, resolvedValue);
        AiUiCustomizationResponseDTO response = baseResponse(action, area, property, resolvedValue, valuePreset, label, confidence);
        response.setColorIntent(colorIntent);
        response.setSuccess(true);
        response.setCodigo("UI_CUSTOMIZATION_OK");
        response.setMensaje("Cambio visual interpretado correctamente.");
        applyNormalizationMetadata(response, normalizedInput, requestedProperty, requestedValue, resolvedValue);
        LOGGER.info("[AI][UI_CUSTOMIZATION_VALIDATE_COLORS] requestId={} inputChanges={} outputChanges={} normalized={} reason={}",
                requestId, inputChanges == null ? 0 : inputChanges.size(), 1, Boolean.TRUE.equals(response.getNormalized()), "SAFE_VALUES");
        LOGGER.info("[AI][UI_CUSTOMIZATION_VALIDATE] requestId={} inputChangesCount={} outputChangesCount={} normalized={} rejected={} reason={}",
                requestId, inputChanges == null ? 0 : inputChanges.size(), 1, Boolean.TRUE.equals(response.getNormalized()), false, "AI_VALUE_ACCEPTED");
        LOGGER.info("[AI][UI_CUSTOMIZATION_VALIDATE] requestId={} allowed=true normalized={} repaired=false", requestId, Boolean.TRUE.equals(response.getNormalized()));
        LOGGER.info("[AI][UI_CUSTOMIZATION_VALIDATE] requestId={} allowed=true action={}", requestId, action);
        return response;
    }

    private boolean needsClarification(String consulta,
                                       String area,
                                       String property,
                                       String action) {
        if ("RESET_THEME".equals(action)) {
            return false;
        }
        if (isChatListPanelThemeRequest(consulta)) {
            return false;
        }
        String normalized = normalizeSemanticText(consulta);
        if (!hasText(normalized)) {
            return !hasText(area) || !hasText(property);
        }
        boolean genericIntent = containsAny(normalized,
                "ponlo mas bonito", "hazlo mas bonito", "cambia el estilo", "cambia estilo",
                "hazlo moderno", "hazlo mas moderno", "mejora los colores", "mejora el estilo",
                "ponlo bonito", "ponlo mejor", "mejora el tema", "quiero otro estilo");
        boolean concreteZone = containsAny(normalized,
                "chat", "mensaje", "mensajes", "burbuja", "burbujas", "barra lateral", "sidebar",
                "topbar", "cabecera", "header", "listado", "lista de chats", "popup", "panel",
                "buscador", "busqueda", "composer", "textarea", "send", "boton", "perfil",
                "reacciones", "reply", "grupo", "usuario", "publicos", "modal", "poll",
                "programado", "temporal", "preview", "preview media", "media");
        boolean concreteProperty = containsAny(normalized,
                "rojo", "verde", "azul", "morado", "blanco", "negro", "gris", "amarillo", "naranja", "rosa",
                "fondo", "color", "borde", "bordes", "radio", "redondo", "redondas", "cuadrado",
                "letra", "texto", "sombra", "shadow", "densidad", "opacidad", "blur", "desenfoque",
                "icono", "placeholder", "badge", "activo", "hover");
        if (genericIntent && (!concreteZone || !concreteProperty)) {
            return true;
        }
        return (!hasText(area) || !hasText(property)) && !concreteZone;
    }

    private boolean isAmbiguousDropdownRequest(String consulta) {
        String normalized = normalizeSemanticText(consulta);
        boolean mentionsMenu = containsAny(normalized, "desplegable", "menu", "menú", "opciones");
        boolean mentionsChatScope = containsAny(normalized,
                "del chat", "de chat", "item de chat", "listado de chats", "lista de chats", "del listado de chats");
        boolean mentionsOtherKnownScope = containsAny(normalized,
                "mensaje", "mensajes", "perfil", "ajustes", "ia", "popup");
        boolean mentionsToggleOnly = containsAny(normalized,
                "icono", "boton", "botón", "abre", "abrir", "tres puntos", "flecha");
        return mentionsMenu && !mentionsChatScope && !mentionsOtherKnownScope && !mentionsToggleOnly;
    }

    private AiUiCustomizationResponseDTO failure(String action,
                                                 String area,
                                                 String property,
                                                 String value,
                                                 String valuePreset,
                                                 String label,
                                                 Double confidence,
                                                 String codigo,
                                                 String mensaje) {
        AiUiCustomizationResponseDTO response = baseResponse(action, area, property, value, valuePreset, label, confidence);
        response.setSuccess(false);
        response.setCodigo(codigo);
        response.setMensaje(mensaje);
        return response;
    }

    private AiUiCustomizationResponseDTO baseResponse(String action,
                                                      String area,
                                                      String property,
                                                      String value,
                                                      String valuePreset,
                                                      String label,
                                                      Double confidence) {
        AiUiCustomizationResponseDTO response = new AiUiCustomizationResponseDTO();
        response.setTarget("UI_CUSTOMIZATION");
        response.setAction(action);
        response.setArea(area);
        response.setProperty(property);
        response.setValue(value);
        response.setValuePreset(valuePreset);
        response.setLabel(label);
        response.setConfidence(confidence);
        return response;
    }

    private List<UiCustomizationChangeDTO> buildChatListItemColorGroup(String consulta, String area, String baseColor) {
        String normalizedBase = normalizeColorText(baseColor);
        if (isSimpleChatItemsBackgroundRequest(consulta)) {
            return buildSimpleChatItemsBackgroundChanges(consulta, area, normalizedBase);
        }
        String textColor;
        String previewColor;
        String previewSenderColor;
        String borderColor;
        String hoverColor;
        String audioBgColor;
        String audioTextColor;
        String audioIconColor;
        String pillBgColor;
        String pillTextColor;
        if ("#ef4444".equals(normalizedBase)) {
            textColor = "#ffffff";
            previewColor = "#fee2e2";
            previewSenderColor = "#ffffff";
            borderColor = "#b91c1c";
            hoverColor = "#dc2626";
            audioBgColor = "#991b1b";
            audioTextColor = "#ffffff";
            audioIconColor = "#fecaca";
            pillBgColor = "#b91c1c";
            pillTextColor = "#ffffff";
        } else if ("#14532d".equals(normalizedBase)) {
            textColor = "#ffffff";
            previewColor = "#dcfce7";
            previewSenderColor = "#ffffff";
            borderColor = "#166534";
            hoverColor = "#166534";
            audioBgColor = "#14532d";
            audioTextColor = "#ffffff";
            audioIconColor = "#bbf7d0";
            pillBgColor = "#166534";
            pillTextColor = "#ffffff";
        } else if ("#16a34a".equals(normalizedBase)) {
            textColor = "#ffffff";
            previewColor = "#dcfce7";
            previewSenderColor = "#ffffff";
            borderColor = "#15803d";
            hoverColor = "#15803d";
            audioBgColor = "#14532d";
            audioTextColor = "#ffffff";
            audioIconColor = "#bbf7d0";
            pillBgColor = "#166534";
            pillTextColor = "#ffffff";
        } else if ("#7c3aed".equals(normalizedBase)) {
            textColor = "#ffffff";
            previewColor = "#ede9fe";
            previewSenderColor = "#ffffff";
            borderColor = "#6d28d9";
            hoverColor = "#6d28d9";
            audioBgColor = "#5b21b6";
            audioTextColor = "#ffffff";
            audioIconColor = "#ddd6fe";
            pillBgColor = "#6d28d9";
            pillTextColor = "#ffffff";
        } else if (isDarkColor(normalizedBase)) {
            textColor = "#ffffff";
            previewColor = "#cbd5e1";
            previewSenderColor = "#f9fafb";
            borderColor = "#334155";
            hoverColor = "#1f2937";
            audioBgColor = "#1f2937";
            audioTextColor = "#f9fafb";
            audioIconColor = "#93c5fd";
            pillBgColor = "#1f2937";
            pillTextColor = "#f9fafb";
        } else {
            textColor = "#111827";
            previewColor = "#64748b";
            previewSenderColor = "#111827";
            borderColor = "#e5e7eb";
            hoverColor = "#f1f5f9";
            audioBgColor = "#f1f5f9";
            audioTextColor = "#111827";
            audioIconColor = "#2563eb";
            pillBgColor = "#e5e7eb";
            pillTextColor = "#111827";
        }
        boolean groupOnly = "CHAT_LIST_ITEM_GROUP".equals(area) || isGroupListRequest(consulta);
        boolean individualOnly = "CHAT_LIST_ITEM".equals(area) && isIndividualListRequest(consulta);
        boolean allListChats = !groupOnly && !individualOnly;

        List<UiCustomizationChangeDTO> changes = new ArrayList<>();
        if (!groupOnly) {
            changes.add(change("CHAT_LIST_ITEM", "BACKGROUND_COLOR", normalizedBase));
            changes.add(change("CHAT_LIST_ITEM", "TEXT_COLOR", textColor));
            changes.add(change("CHAT_LIST_ITEM", "BORDER_COLOR", borderColor));
            changes.add(change("CHAT_LIST_ITEM", "HOVER_BACKGROUND_COLOR", hoverColor));
        }
        if (groupOnly || allListChats) {
            changes.add(change("CHAT_LIST_ITEM_GROUP", "BACKGROUND_COLOR", normalizedBase));
            changes.add(change("CHAT_LIST_ITEM_GROUP", "TEXT_COLOR", textColor));
            changes.add(change("CHAT_LIST_ITEM_GROUP", "BORDER_COLOR", borderColor));
            changes.add(change("CHAT_LIST_ITEM_GROUP", "HOVER_BACKGROUND_COLOR", hoverColor));
        }
        if (groupOnly || allListChats) {
            changes.add(change("CHAT_LIST_GROUP_PILL", "BACKGROUND_COLOR", pillBgColor));
            changes.add(change("CHAT_LIST_GROUP_PILL", "TEXT_COLOR", pillTextColor));
            changes.add(change("CHAT_LIST_GROUP_PILL", "BORDER_COLOR", borderColor));
        }
        if (!groupOnly) {
            changes.add(change("CHAT_LIST_PREVIEW", "TEXT_COLOR", previewColor));
            changes.add(change("CHAT_LIST_PREVIEW", "PREVIEW_SENDER_TEXT_COLOR", previewSenderColor));
            changes.add(change("CHAT_LIST_DRAFT_PREVIEW", "TEXT_COLOR", previewColor));
            changes.add(change("CHAT_LIST_DRAFT_PREVIEW", "LABEL_COLOR", textColor));
            changes.add(change("CHAT_LIST_AUDIO_PREVIEW", "BACKGROUND_COLOR", audioBgColor));
            changes.add(change("CHAT_LIST_AUDIO_PREVIEW", "TEXT_COLOR", audioTextColor));
            changes.add(change("CHAT_LIST_AUDIO_PREVIEW", "ICON_COLOR", audioIconColor));
            changes.add(change("CHAT_LIST_AUDIO_PREVIEW", "BORDER_COLOR", borderColor));
            changes.add(change("CHAT_LIST_STATUS_PILLS", "REPORTED_BACKGROUND_COLOR", pillBgColor));
            changes.add(change("CHAT_LIST_STATUS_PILLS", "REPORTED_TEXT_COLOR", pillTextColor));
            changes.add(change("CHAT_LIST_STATUS_PILLS", "BLOCKED_BACKGROUND_COLOR", pillBgColor));
            changes.add(change("CHAT_LIST_STATUS_PILLS", "BLOCKED_TEXT_COLOR", pillTextColor));
            changes.add(change("CHAT_LIST_ITEM_ACTIONS", "ICON_COLOR", audioIconColor));
            changes.add(change("CHAT_LIST_ITEM_ACTIONS", "HOVER_BACKGROUND_COLOR", hoverColor));
            changes.add(change("CHAT_LIST_ITEM_ACTIVE", "BACKGROUND_COLOR", "#2563eb"));
            changes.add(change("CHAT_LIST_ITEM_ACTIVE", "TEXT_COLOR", "#ffffff"));
        }
        return changes;
    }

    private List<UiCustomizationChangeDTO> buildSimpleChatItemsBackgroundChanges(String consulta, String area, String baseColor) {
        List<UiCustomizationChangeDTO> changes = new ArrayList<>();
        boolean mentionsBoth = mentionsIndividualAndGroup(consulta) || mentionsEachChat(consulta);
        if (mentionsBoth) {
            changes.add(change("CHAT_LIST_ITEM", "BACKGROUND_COLOR", baseColor));
            changes.add(change("CHAT_LIST_ITEM_GROUP", "BACKGROUND_COLOR", baseColor));
            return changes;
        }
        if ("CHAT_LIST_ITEM_GROUP".equals(area) || isGroupListRequest(consulta)) {
            changes.add(change("CHAT_LIST_ITEM_GROUP", "BACKGROUND_COLOR", baseColor));
            return changes;
        }
        changes.add(change("CHAT_LIST_ITEM", "BACKGROUND_COLOR", baseColor));
        return changes;
    }

    private boolean isSimpleChatItemsBackgroundRequest(String consulta) {
        String normalized = normalizeSemanticText(consulta);
        boolean mentionsChatItems = mentionsEachChat(consulta)
                || containsAny(normalized, "chat individual y grupal", "chats individuales y grupales",
                "cada chat grupal e individual", "los chats con fondo", "chats con fondo");
        boolean mentionsBackgroundIntent = containsAny(normalized, "fondo", "background", "color de fondo");
        boolean explicitFullBlock = containsAny(normalized,
                "todo el chat completo", "todo el item", "que combine", "incluyendo previews",
                "incluyendo etiquetas", "incluyendo audios", "incluyendo hijos", "todo");
        return mentionsChatItems && mentionsBackgroundIntent && !explicitFullBlock;
    }

    private boolean mentionsEachChat(String consulta) {
        String normalized = normalizeSemanticText(consulta);
        return containsAny(normalized, "cada chat", "cada uno de los chats");
    }

    private boolean mentionsIndividualAndGroup(String consulta) {
        String normalized = normalizeSemanticText(consulta);
        return containsAny(normalized,
                "chat individual y grupal", "chats individuales y grupales",
                "cada chat grupal e individual", "grupal y individual", "individual y grupal",
                "cada chat grupal y cada chat individual", "cada chat individual y cada chat grupal");
    }

    private List<UiCustomizationChangeDTO> sanitizeGroupOnlyChanges(List<UiCustomizationChangeDTO> changes) {
        List<UiCustomizationChangeDTO> sanitized = new ArrayList<>();
        if (changes == null) {
            return sanitized;
        }
        for (UiCustomizationChangeDTO change : changes) {
            if (change == null || !hasText(change.getArea())) {
                continue;
            }
            if (GROUP_ONLY_FORBIDDEN_GENERIC_AREAS.contains(change.getArea())) {
                continue;
            }
            if (GROUP_ONLY_ALLOWED_AREAS.contains(change.getArea())) {
                sanitized.add(change);
            }
        }
        return sanitized;
    }

    private List<UiCustomizationChangeDTO> sanitizeIndividualOnlyChanges(List<UiCustomizationChangeDTO> changes) {
        List<UiCustomizationChangeDTO> sanitized = new ArrayList<>();
        if (changes == null) {
            return sanitized;
        }
        for (UiCustomizationChangeDTO change : changes) {
            if (change == null || !hasText(change.getArea())) {
                continue;
            }
            if (INDIVIDUAL_ONLY_ALLOWED_AREAS.contains(change.getArea())) {
                sanitized.add(change);
            }
        }
        return sanitized;
    }

    private UiCustomizationChangeDTO change(String area, String property, String value) {
        UiCustomizationChangeDTO dto = new UiCustomizationChangeDTO();
        dto.setArea(area);
        dto.setProperty(property);
        dto.setValue(value);
        dto.setValuePreset(null);
        return dto;
    }

    private boolean isValidGroupChange(UiCustomizationChangeDTO change) {
        if (change == null || !hasText(change.getArea()) || !hasText(change.getProperty())) {
            return false;
        }
        if (!ALLOWED_AREAS.contains(change.getArea()) || !ALLOWED_PROPERTIES.contains(change.getProperty())) {
            return false;
        }
        if ("BACKGROUND_IMAGE".equals(change.getProperty())) {
            return false;
        }
        if (!isPropertyAllowedForArea(change.getArea(), change.getProperty())) {
            return false;
        }
        return hasText(resolveValue(change.getProperty(), change.getValue(), change.getValuePreset()));
    }

    private UiCustomizationChangeDTO normalizeGroupChange(UiCustomizationChangeDTO change) {
        AreaProperty normalizedAreaProperty = normalizeAreaProperty(change.getArea(), change.getProperty());
        UiCustomizationChangeDTO normalized = new UiCustomizationChangeDTO();
        normalized.setArea(normalizedAreaProperty.area());
        normalized.setProperty(normalizedAreaProperty.property());
        normalized.setValue(resolveValue(normalizedAreaProperty.property(), change.getValue(), change.getValuePreset()));
        normalized.setValuePreset(change.getValuePreset());
        return normalized;
    }

    private AreaProperty normalizeAreaProperty(String area, String property) {
        String normalizedArea = area;
        String normalizedProperty = property;
        if ("CHAT_HEADER".equals(normalizedArea)) {
            normalizedArea = "CHAT_LIST_HEADER";
        }
        if ("CHAT_LIST_FILTERS".equals(normalizedArea) && "SEND_BUTTON_COLOR".equals(normalizedProperty)) {
            normalizedProperty = "ICON_COLOR";
        }
        return new AreaProperty(normalizedArea, normalizedProperty);
    }

    private boolean isDarkColor(String hex) {
        if (!hasText(hex) || !hex.matches("^#[0-9a-f]{6}$")) {
            return false;
        }
        int r = Integer.parseInt(hex.substring(1, 3), 16);
        int g = Integer.parseInt(hex.substring(3, 5), 16);
        int b = Integer.parseInt(hex.substring(5, 7), 16);
        double luminance = (0.2126 * r + 0.7152 * g + 0.0722 * b) / 255.0;
        return luminance < 0.45;
    }

    private RelativeResolution resolveRelativeWithContext(String requestId,
                                                          String consulta,
                                                          String action,
                                                          String area,
                                                          String property,
                                                          String value,
                                                          Map<String, Map<String, String>> styles) {
        String normalized = normalizeSemanticText(consulta);
        if (!hasText(normalized)) {
            return null;
        }
        String effectiveArea = resolveRelativeArea(consulta, area, styles);
        if (!hasText(effectiveArea) || (!"CHAT_LIST_ITEM".equals(effectiveArea) && !"CHAT_LIST_ITEM_GROUP".equals(effectiveArea)) ) {
            effectiveArea = "CHAT_LIST_ITEM";
        }

        if (isRelativeFontSizeRequest(normalized)) {
            String fontArea = resolveFontSizeArea(consulta, area, styles);
            String currentFontSize = getStyleValue(styles, fontArea, "FONT_SIZE");
            if (!hasText(fontArea)) {
                return null;
            }
            int baseSize = parseFontPx(currentFontSize);
            if (baseSize <= 0) {
                baseSize = 14;
                currentFontSize = "14px";
            }
            String explicitPx = extractRequestedPx(consulta);
            Integer targetSize = resolveTargetFontSizePx(baseSize, explicitPx, normalized);
            if (targetSize == null) {
                return null;
            }
            int clamped = clampFontSize(targetSize);
            String targetFontSize = clamped + "px";
            if (!hasText(targetFontSize)) {
                return null;
            }
            LOGGER.info("[AI][UI_CONTEXT_USED] requestId={} area={} property={} currentValue={}",
                    requestId, fontArea, "FONT_SIZE", currentFontSize);
            LOGGER.info("[AI][UI_RELATIVE_FONT_SIZE] requestId={} from={} to={} reason={}",
                    requestId, currentFontSize, targetFontSize, relativeFontReason(normalized));
            if (targetSize != clamped) {
                LOGGER.info("[AI][UI_FONT_SIZE_CLAMP] requestId={} requested={}px clamped={}px",
                        requestId, targetSize, clamped);
            }
            String preset = inferFontSizePreset(targetFontSize);
            if (clamped == 32) {
                preset = "MAX";
            }
            return new RelativeResolution("UPDATE_STYLE", fontArea, "FONT_SIZE", targetFontSize, null, preset);
        }

        if (containsAny(normalized, "igual que los individuales", "grupos igual que los individuales")) {
            Map<String, String> source = styles.get("CHAT_LIST_ITEM");
            if (source == null || source.isEmpty()) return null;
            return copyAreaStylesAsGroup("CHAT_LIST_ITEM_GROUP", source);
        }
        if (containsAny(normalized, "igual que los grupos", "individuales igual que los grupos")) {
            Map<String, String> source = styles.get("CHAT_LIST_ITEM_GROUP");
            if (source == null || source.isEmpty()) return null;
            return copyAreaStylesAsGroup("CHAT_LIST_ITEM", source);
        }

        if (containsAny(normalized, "hover un poco mas fuerte", "hover mas fuerte")) {
            String currentHover = getStyleValue(styles, effectiveArea, "HOVER_BACKGROUND_COLOR");
            String currentBg = getStyleValue(styles, effectiveArea, "BACKGROUND_COLOR");
            String base = hasText(currentHover) ? currentHover : currentBg;
            if (!hasText(base)) {
                return null;
            }
            return new RelativeResolution("UPDATE_STYLE", effectiveArea, "HOVER_BACKGROUND_COLOR", lighten(base, 0.16), null, null);
        }

        if (containsAny(normalized, "mas claro", "un poco mas claro")) {
            String currentBg = getStyleValue(styles, effectiveArea, "BACKGROUND_COLOR");
            if (!hasText(currentBg)) {
                return null;
            }
            return new RelativeResolution("UPDATE_STYLE_GROUP", effectiveArea, "BACKGROUND_COLOR", lighten(currentBg, 0.16), null, null);
        }
        if (containsAny(normalized, "mas oscuro", "un poco mas oscuro")) {
            String currentBg = getStyleValue(styles, effectiveArea, "BACKGROUND_COLOR");
            if (!hasText(currentBg)) {
                return null;
            }
            return new RelativeResolution("UPDATE_STYLE_GROUP", effectiveArea, "BACKGROUND_COLOR", darken(currentBg, 0.16), null, null);
        }
        if (containsAny(normalized, "mas contraste", "mantener contraste", "dale mas contraste")) {
            String currentBg = getStyleValue(styles, effectiveArea, "BACKGROUND_COLOR");
            if (!hasText(currentBg)) {
                return null;
            }
            return new RelativeResolution("UPDATE_STYLE_GROUP", effectiveArea, "BACKGROUND_COLOR", currentBg, null, null);
        }
        return null;
    }

    private RelativeResolution copyAreaStylesAsGroup(String targetArea, Map<String, String> source) {
        String bg = getStyleValue(source, "BACKGROUND_COLOR");
        String txt = getStyleValue(source, "TEXT_COLOR");
        String border = getStyleValue(source, "BORDER_COLOR");
        String hover = getStyleValue(source, "HOVER_BACKGROUND_COLOR");
        if (!hasText(bg)) {
            return null;
        }
        List<UiCustomizationChangeDTO> changes = new ArrayList<>();
        changes.add(change(targetArea, "BACKGROUND_COLOR", bg));
        if (hasText(txt)) changes.add(change(targetArea, "TEXT_COLOR", txt));
        if (hasText(border)) changes.add(change(targetArea, "BORDER_COLOR", border));
        if (hasText(hover)) changes.add(change(targetArea, "HOVER_BACKGROUND_COLOR", hover));
        return new RelativeResolution("UPDATE_STYLE_GROUP", targetArea, "BACKGROUND_COLOR", bg, changes, null);
    }

    private String getStyleValue(Map<String, Map<String, String>> styles, String area, String property) {
        if (styles == null || !hasText(area) || !hasText(property)) {
            return null;
        }
        Map<String, String> props = styles.get(area);
        return props == null ? null : getStyleValue(props, property);
    }

    private String getStyleValue(Map<String, String> props, String property) {
        if (props == null || !hasText(property)) {
            return null;
        }
        String value = props.get(property);
        return hasText(value) ? value : null;
    }

    private boolean isRelativeStyleRequest(String consulta) {
        String normalized = normalizeSemanticText(consulta);
        return containsAny(normalized,
                "mas claro", "mas oscuro", "menos intenso", "mas fuerte", "igual que",
                "parecido a", "mantener contraste", "mas contraste", "como esta ahora");
    }

    private boolean isRelativeFontSizeRequest(String normalizedConsulta) {
        return containsAny(normalizedConsulta,
                "mas grande", "mucho mas grande", "aumenta bastante", "letra enorme",
                "un poco mas grande", "mas pequeno", "mucho mas pequeno", "un poco mas pequeno",
                "tamano", "letra", "fuente");
    }

    private String resolveRelativeArea(String consulta, String area, Map<String, Map<String, String>> styles) {
        if (hasText(area)) {
            return area;
        }
        if (styles != null && styles.size() == 1) {
            return styles.keySet().iterator().next();
        }
        String normalized = normalizeSemanticText(consulta);
        if (containsAny(normalized, "menu desplegable", "menu del chat", "desplegable")) {
            return "CHAT_LIST_PIN_MENU_ITEM";
        }
        return null;
    }

    private String resolveFontSizeArea(String consulta, String area, Map<String, Map<String, String>> styles) {
        String resolved = resolveRelativeArea(consulta, area, styles);
        if (hasText(resolved)) {
            return resolved;
        }
        return null;
    }

    private Integer resolveTargetFontSizePx(int baseSize, String explicitPx, String normalizedConsulta) {
        if (containsAny(normalizedConsulta, "multiplicalo por 2", "multiplicalo x2", "x2", "por 2")) {
            int result = baseSize * 2;
            if (hasText(explicitPx)) {
                int explicit = parseFontPx(explicitPx);
                if (explicit > 0) {
                    result = explicit * 2;
                }
            }
            return result;
        }
        if (hasText(explicitPx)) {
            int explicit = parseFontPx(explicitPx);
            return explicit > 0 ? explicit : null;
        }
        if (containsAny(normalizedConsulta, "mucho mas grande", "aumenta bastante", "letra enorme")) {
            return switch (baseSize) {
                case 12 -> 16; case 13 -> 18; case 14 -> 20; case 16 -> 22;
                case 18 -> 24; case 20 -> 28; case 22 -> 32; default -> 32;
            };
        }
        if (containsAny(normalizedConsulta, "un poco mas grande")) {
            return switch (baseSize) {
                case 12 -> 13; case 13 -> 14; case 14 -> 16; case 16 -> 18;
                default -> Math.min(32, baseSize + 2);
            };
        }
        if (containsAny(normalizedConsulta, "mucho mas pequeno")) {
            return switch (baseSize) {
                case 32 -> 22; case 28 -> 20; case 24 -> 18; case 22 -> 16;
                case 20 -> 14; case 18 -> 13; case 16 -> 12; default -> 12;
            };
        }
        if (containsAny(normalizedConsulta, "reducir", "mas pequeno", "un poco mas pequeno")) {
            return switch (baseSize) {
                case 32 -> 28; case 28 -> 24; case 24 -> 22; case 22 -> 20;
                case 20 -> 18; case 18 -> 16; case 16 -> 14; case 14 -> 13; case 13 -> 12;
                default -> Math.max(10, baseSize - 2);
            };
        }
        if (containsAny(normalizedConsulta, "aumentar", "mas grande")) {
            return switch (baseSize) {
                case 12 -> 13; case 13 -> 14; case 14 -> 16; case 16 -> 18;
                case 18 -> 20; case 20 -> 22; case 22 -> 24; case 24 -> 28; case 28 -> 32;
                default -> Math.min(32, baseSize + 2);
            };
        }
        return null;
    }

    private String relativeFontReason(String normalizedConsulta) {
        if (containsAny(normalizedConsulta, "multiplicalo por 2", "multiplicalo x2", "x2", "por 2")) return "MULTIPLY_X2";
        if (containsAny(normalizedConsulta, "aumentar", "mas grande")) return "INCREASE";
        if (containsAny(normalizedConsulta, "mucho mas grande", "aumenta bastante", "letra enorme")) return "MUCH_BIGGER";
        if (containsAny(normalizedConsulta, "un poco mas grande")) return "SLIGHTLY_BIGGER";
        if (containsAny(normalizedConsulta, "mucho mas pequeno")) return "MUCH_SMALLER";
        if (containsAny(normalizedConsulta, "mas pequeno", "un poco mas pequeno")) return "SMALLER";
        return "BIGGER";
    }

    private String extractRequestedPx(String consulta) {
        if (!hasText(consulta)) {
            return null;
        }
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("(\\d{1,2})\\s*px", java.util.regex.Pattern.CASE_INSENSITIVE)
                .matcher(consulta);
        if (matcher.find()) {
            return matcher.group(1) + "px";
        }
        return null;
    }

    private int parseFontPx(String value) {
        if (!hasText(value)) {
            return -1;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        if (!normalized.matches("^[0-9]{1,2}px$")) {
            return -1;
        }
        try {
            return Integer.parseInt(normalized.substring(0, normalized.length() - 2));
        } catch (NumberFormatException ex) {
            return -1;
        }
    }

    private int clampFontSize(int requested) {
        int clamped = Math.max(10, Math.min(32, requested));
        if (SAFE_FONT_SIZES.contains(clamped)) {
            return clamped;
        }
        int nearest = 10;
        int delta = Integer.MAX_VALUE;
        for (int candidate : SAFE_FONT_SIZES) {
            int currentDelta = Math.abs(candidate - clamped);
            if (currentDelta < delta) {
                nearest = candidate;
                delta = currentDelta;
            }
        }
        return nearest;
    }

    private String lighten(String hex, double amount) {
        return adjustColor(hex, Math.abs(amount));
    }

    private String darken(String hex, double amount) {
        return adjustColor(hex, -Math.abs(amount));
    }

    private String adjustColor(String hex, double amount) {
        String normalized = resolveColor(hex);
        if (!hasText(normalized) || !normalized.matches("^#[0-9a-f]{6}$")) {
            return hex;
        }
        int r = Integer.parseInt(normalized.substring(1, 3), 16);
        int g = Integer.parseInt(normalized.substring(3, 5), 16);
        int b = Integer.parseInt(normalized.substring(5, 7), 16);
        int nr = clamp((int) Math.round(r + (amount * 255)));
        int ng = clamp((int) Math.round(g + (amount * 255)));
        int nb = clamp((int) Math.round(b + (amount * 255)));
        return String.format(Locale.ROOT, "#%02x%02x%02x", nr, ng, nb);
    }

    private Map<String, Map<String, String>> mergeContextStyles(UiCustomizationContextDTO uiContext) {
        if (uiContext == null) {
            return null;
        }
        Map<String, Map<String, String>> current = uiContext.getCurrentStyles();
        Map<String, Map<String, String>> computed = uiContext.getComputedStyles();
        if ((current == null || current.isEmpty()) && (computed == null || computed.isEmpty())) {
            return null;
        }
        Map<String, Map<String, String>> merged = new LinkedHashMap<>();
        if (current != null) {
            for (Map.Entry<String, Map<String, String>> entry : current.entrySet()) {
                if (!hasText(entry.getKey()) || entry.getValue() == null || entry.getValue().isEmpty()) {
                    continue;
                }
                merged.put(entry.getKey(), new LinkedHashMap<>(entry.getValue()));
            }
        }
        if (computed != null) {
            for (Map.Entry<String, Map<String, String>> entry : computed.entrySet()) {
                if (!hasText(entry.getKey()) || entry.getValue() == null || entry.getValue().isEmpty()) {
                    continue;
                }
                Map<String, String> target = merged.computeIfAbsent(entry.getKey(), k -> new LinkedHashMap<>());
                for (Map.Entry<String, String> propEntry : entry.getValue().entrySet()) {
                    if (!target.containsKey(propEntry.getKey()) && hasText(propEntry.getValue())) {
                        target.put(propEntry.getKey(), propEntry.getValue());
                    }
                }
            }
        }
        return merged.isEmpty() ? null : merged;
    }

    private boolean hasGroupExpansionHints(UiCustomizationContextDTO uiContext, String area) {
        if (uiContext == null || !hasText(area) || uiContext.getGroupExpansionHints() == null) {
            return false;
        }
        List<String> children = uiContext.getGroupExpansionHints().get(area);
        if (children == null || children.isEmpty()) {
            return false;
        }
        for (String child : children) {
            if (ALLOWED_AREAS.contains(child)) {
                return true;
            }
        }
        return false;
    }

    private int clamp(int value) {
        return Math.max(0, Math.min(255, value));
    }

    private record RelativeResolution(String action, String area, String property, String value, List<UiCustomizationChangeDTO> changes, String valuePreset) {}

    private String resolveValue(String property, String rawValue, String valuePreset) {
        if (COLOR_PROPERTIES.contains(property)) {
            return resolveColor(rawValue);
        }
        return switch (property) {
            case "BORDER_RADIUS" -> resolvePresetOrPx(rawValue, valuePreset, BORDER_RADIUS_PRESETS, 0, 999);
            case "BORDER_WIDTH" -> resolveBorderWidth(rawValue, valuePreset);
            case "FONT_SIZE" -> resolveFontSize(rawValue, valuePreset);
            case "SHADOW_PRESET" -> resolvePreset(valuePreset, rawValue, SHADOW_PRESETS);
            case "DENSITY" -> resolveDensity(valuePreset, rawValue);
            case "OPACITY" -> resolveOpacity(rawValue);
            case "BLUR" -> resolvePresetOrPx(rawValue, valuePreset, BLUR_PRESETS, 0, 32);
            default -> null;
        };
    }

    private String resolveFontSize(String rawValue, String valuePreset) {
        String preset = resolvePreset(valuePreset, null, FONT_SIZE_PRESETS);
        if (preset != null) {
            return preset;
        }
        if (!hasText(rawValue)) {
            return null;
        }
        String normalized = rawValue.trim().toLowerCase(Locale.ROOT);
        if (!normalized.matches("^[0-9]{1,2}px$")) {
            return null;
        }
        int px = parseFontPx(normalized);
        if (px <= 0) {
            return null;
        }
        return SAFE_FONT_SIZES.contains(px) ? normalized : null;
    }

    private String resolveColor(String raw) {
        if (!hasText(raw)) {
            return null;
        }
        if (containsDangerousCss(raw)) {
            return null;
        }
        String normalized = normalizeColorText(raw);
        if (NAMED_COLORS.containsKey(normalized)) {
            return NAMED_COLORS.get(normalized);
        }
        if ("transparent".equals(normalized)) {
            return "transparent";
        }
        return normalized.matches("^#[0-9a-f]{3}$") || normalized.matches("^#[0-9a-f]{6}$")
                ? normalized
                : null;
    }

    private String resolveBorderWidth(String rawValue, String valuePreset) {
        String normalizedPreset = normalizePreset(valuePreset);
        if ("NONE".equals(normalizedPreset)) {
            return "0px";
        }
        if (!hasText(rawValue)) {
            return null;
        }
        String normalized = rawValue.trim().toLowerCase(Locale.ROOT);
        return BORDER_WIDTH_VALUES.contains(normalized) ? normalized : null;
    }

    private boolean isPxValue(String rawValue) {
        if (!hasText(rawValue)) {
            return false;
        }
        return rawValue.trim().toLowerCase(Locale.ROOT).matches("^[0-9]+px$");
    }

    private String resolvePresetOrPx(String rawValue,
                                     String valuePreset,
                                     Map<String, String> presets,
                                     int minPx,
                                     int maxPx) {
        String preset = resolvePreset(valuePreset, null, presets);
        if (preset != null) {
            return preset;
        }
        if (!matchesPxValue(rawValue, minPx, maxPx)) {
            return null;
        }
        String normalized = rawValue.trim().toLowerCase(Locale.ROOT);
        if (presets == BORDER_RADIUS_PRESETS && !SAFE_BORDER_RADIUS_VALUES.contains(normalized)) {
            int requested = parseFontPx(normalized);
            int[] allowed = new int[]{0, 4, 8, 12, 16, 18, 24, 32, 999};
            int nearest = allowed[0];
            int delta = Integer.MAX_VALUE;
            for (int a : allowed) {
                int d = Math.abs(a - requested);
                if (d < delta) {
                    delta = d;
                    nearest = a;
                }
            }
            return nearest + "px";
        }
        return normalized;
    }

    private String resolvePreset(String valuePreset, String rawValue, Map<String, String> presets) {
        String normalizedPreset = normalizePreset(valuePreset);
        if ("MEDIUM".equals(normalizedPreset)) {
            normalizedPreset = "NORMAL";
        }
        if (normalizedPreset != null && presets.containsKey(normalizedPreset)) {
            return presets.get(normalizedPreset);
        }
        String normalizedValue = normalizePreset(rawValue);
        if ("MEDIUM".equals(normalizedValue)) {
            normalizedValue = "NORMAL";
        }
        if (normalizedValue != null && presets.containsKey(normalizedValue)) {
            return presets.get(normalizedValue);
        }
        return null;
    }

    private String resolveDensity(String valuePreset, String rawValue) {
        String candidate = normalizePreset(valuePreset);
        if (candidate == null) {
            candidate = normalizePreset(rawValue);
        }
        return candidate != null && DENSITY_VALUES.contains(candidate) ? candidate : null;
    }

    private String resolveOpacity(String rawValue) {
        if (!hasText(rawValue)) {
            return null;
        }
        String normalized = rawValue.trim().replace(',', '.');
        try {
            double value = Double.parseDouble(normalized);
            if (value < 0.1d || value > 1.0d) {
                return null;
            }
            if (Math.abs(value - Math.rint(value)) < 0.000001d) {
                return String.format(Locale.ROOT, "%.1f", value);
            }
            return normalized;
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private boolean matchesPxValue(String raw, int min, int max) {
        if (!hasText(raw)) {
            return false;
        }
        String normalized = raw.trim().toLowerCase(Locale.ROOT);
        if (!normalized.endsWith("px")) {
            return false;
        }
        try {
            int value = Integer.parseInt(normalized.substring(0, normalized.length() - 2));
            return value >= min && value <= max;
        } catch (NumberFormatException ex) {
            return false;
        }
    }

    private String normalizeColorText(String raw) {
        String normalized = normalizeSemanticText(raw);
        if (normalized.equals("negrointenso") || normalized.equals("negropuro") || normalized.equals("negrototal")) {
            return "negro intenso";
        }
        if (normalized.equals("negroapp") || normalized.equals("negrosuave")) {
            return "negro app";
        }
        if (normalized.equals("verdeoscuro")) {
            return "verde oscuro";
        }
        if (normalized.equals("grisoscuro")) {
            return "gris oscuro";
        }
        return normalized;
    }

    private String normalizePreset(String value) {
        if (!hasText(value)) {
            return null;
        }
        return value.trim().toUpperCase(Locale.ROOT).replace(' ', '_');
    }

    private String normalizeSemanticText(String value) {
        if (!hasText(value)) {
            return "";
        }
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
        normalized = normalized.toLowerCase(Locale.ROOT);
        normalized = normalized.replaceAll("[^a-z0-9#\\s]", " ");
        return normalized.replaceAll("\\s+", " ").trim();
    }

    private boolean containsAny(String text, String... fragments) {
        if (!hasText(text)) {
            return false;
        }
        for (String fragment : fragments) {
            if (hasText(fragment) && text.contains(fragment)) {
                return true;
            }
        }
        return false;
    }

    private boolean shouldForceChatListPreview(String consulta) {
        String normalized = normalizeSemanticText(consulta);
        if (isAudioPreviewRequest(consulta)) {
            return false;
        }
        return containsAny(normalized,
                "ultimo mensaje", "texto del ultimo mensaje", "vista previa", "preview",
                "mensaje que sale debajo del nombre", "ultimo mensaje mostrado en cada chat");
    }

    private boolean isAudioPreviewRequest(String consulta) {
        String normalized = normalizeSemanticText(consulta);
        return containsAny(normalized,
                "preview de audio", "audio del listado", "nota de voz del listado",
                "mensaje de audio en el listado", "audio compacto", "audio preview",
                "vista previa de audio", "icono del audio", "texto del audio",
                "duracion del audio", "borde del audio", "audio");
    }

    private String inferAudioPreviewProperty(String consulta, String currentProperty) {
        String normalized = normalizeSemanticText(consulta);
        if (containsAny(normalized, "icono del audio", "icono de la nota de voz", "icono")) {
            return "ICON_COLOR";
        }
        if (containsAny(normalized, "separador del audio", "separador")) {
            return "SEPARATOR_COLOR";
        }
        if (containsAny(normalized, "tiempo del audio", "duracion del audio", "duracion", "time")) {
            return "TIME_COLOR";
        }
        if (containsAny(normalized, "texto tu del audio", "label del audio", "me label")) {
            return "LABEL_COLOR";
        }
        if (containsAny(normalized, "borde del audio", "borde")) {
            return "BORDER_COLOR";
        }
        if (containsAny(normalized, "texto del audio", "texto")) {
            return "TEXT_COLOR";
        }
        if ("TEXT_COLOR".equals(currentProperty) || "ICON_COLOR".equals(currentProperty)
                || "BORDER_COLOR".equals(currentProperty) || "BACKGROUND_COLOR".equals(currentProperty)
                || "LABEL_COLOR".equals(currentProperty) || "SEPARATOR_COLOR".equals(currentProperty)
                || "TIME_COLOR".equals(currentProperty)) {
            return currentProperty;
        }
        return "BACKGROUND_COLOR";
    }

    private boolean isAudioPreviewThemeRequest(String consulta) {
        String normalized = normalizeSemanticText(consulta);
        return isAudioPreviewRequest(consulta)
                && !containsAny(normalized,
                "icono del audio", "icono de la nota de voz", "icono",
                "texto del audio", "texto tu del audio", "label del audio", "me label",
                "duracion del audio", "tiempo del audio", "time",
                "separador del audio", "separador", "borde del audio", "borde");
    }

    private List<UiCustomizationChangeDTO> buildAudioPreviewColorGroup(String baseColor) {
        String normalizedBase = normalizeColorText(baseColor);
        String textColor;
        String iconColor;
        String borderColor;
        String labelColor;
        String separatorColor;
        String timeColor;
        if ("#7c3aed".equals(normalizedBase)) {
            textColor = "#ffffff"; iconColor = "#ddd6fe"; borderColor = "#6d28d9";
            labelColor = "#ffffff"; separatorColor = "#ddd6fe"; timeColor = "#f5f3ff";
        } else if ("#ef4444".equals(normalizedBase)) {
            textColor = "#ffffff"; iconColor = "#fecaca"; borderColor = "#b91c1c";
            labelColor = "#ffffff"; separatorColor = "#fee2e2"; timeColor = "#fff1f2";
        } else if ("#16a34a".equals(normalizedBase) || "#14532d".equals(normalizedBase)) {
            textColor = "#ffffff"; iconColor = "#bbf7d0"; borderColor = "#166534";
            labelColor = "#ffffff"; separatorColor = "#dcfce7"; timeColor = "#f0fdf4";
        } else if ("#2563eb".equals(normalizedBase)) {
            textColor = "#ffffff"; iconColor = "#bfdbfe"; borderColor = "#1d4ed8";
            labelColor = "#ffffff"; separatorColor = "#dbeafe"; timeColor = "#eff6ff";
        } else if ("#111827".equals(normalizedBase) || isDarkColor(normalizedBase)) {
            textColor = "#f9fafb"; iconColor = "#93c5fd"; borderColor = "#334155";
            labelColor = "#ffffff"; separatorColor = "#94a3b8"; timeColor = "#cbd5e1";
        } else {
            textColor = "#111827"; iconColor = "#2563eb"; borderColor = "#e5e7eb";
            labelColor = "#111827"; separatorColor = "#64748b"; timeColor = "#334155";
        }
        List<UiCustomizationChangeDTO> changes = new ArrayList<>();
        changes.add(change("CHAT_LIST_AUDIO_PREVIEW", "BACKGROUND_COLOR", normalizedBase));
        changes.add(change("CHAT_LIST_AUDIO_PREVIEW", "TEXT_COLOR", textColor));
        changes.add(change("CHAT_LIST_AUDIO_PREVIEW", "ICON_COLOR", iconColor));
        changes.add(change("CHAT_LIST_AUDIO_PREVIEW", "BORDER_COLOR", borderColor));
        changes.add(change("CHAT_LIST_AUDIO_PREVIEW", "LABEL_COLOR", labelColor));
        changes.add(change("CHAT_LIST_AUDIO_PREVIEW", "SEPARATOR_COLOR", separatorColor));
        changes.add(change("CHAT_LIST_AUDIO_PREVIEW", "TIME_COLOR", timeColor));
        return changes;
    }

    private List<UiCustomizationChangeDTO> buildGroupAudioPreviewScopedColorGroup(String baseColor) {
        String normalizedBase = normalizeColorText(baseColor);
        String textColor;
        String iconColor;
        if ("#7c3aed".equals(normalizedBase)) {
            textColor = "#ffffff";
            iconColor = "#ddd6fe";
            normalizedBase = "#5b21b6";
        } else if ("#ef4444".equals(normalizedBase)) {
            textColor = "#ffffff";
            iconColor = "#fecaca";
        } else if ("#16a34a".equals(normalizedBase) || "#14532d".equals(normalizedBase)) {
            textColor = "#ffffff";
            iconColor = "#bbf7d0";
        } else if ("#111827".equals(normalizedBase) || isDarkColor(normalizedBase)) {
            textColor = "#f9fafb";
            iconColor = "#93c5fd";
        } else {
            textColor = "#111827";
            iconColor = "#2563eb";
        }
        List<UiCustomizationChangeDTO> changes = new ArrayList<>();
        changes.add(change("CHAT_LIST_ITEM_GROUP_AUDIO_PREVIEW", "BACKGROUND_COLOR", normalizedBase));
        changes.add(change("CHAT_LIST_ITEM_GROUP_AUDIO_PREVIEW", "TEXT_COLOR", textColor));
        changes.add(change("CHAT_LIST_ITEM_GROUP_AUDIO_PREVIEW", "ICON_COLOR", iconColor));
        return changes;
    }

    private List<UiCustomizationChangeDTO> buildIndividualAudioPreviewScopedColorGroup(String baseColor) {
        String normalizedBase = normalizeColorText(baseColor);
        String textColor;
        String iconColor;
        if ("#f97316".equals(normalizedBase)) {
            textColor = "#7c2d12";
            iconColor = "#ea580c";
            normalizedBase = "#ffedd5";
        } else if ("#7c3aed".equals(normalizedBase)) {
            textColor = "#5b21b6";
            iconColor = "#7c3aed";
            normalizedBase = "#ede9fe";
        } else if (isDarkColor(normalizedBase)) {
            textColor = "#f9fafb";
            iconColor = "#93c5fd";
        } else {
            textColor = "#111827";
            iconColor = "#2563eb";
        }
        List<UiCustomizationChangeDTO> changes = new ArrayList<>();
        changes.add(change("CHAT_LIST_ITEM_AUDIO_PREVIEW", "BACKGROUND_COLOR", normalizedBase));
        changes.add(change("CHAT_LIST_ITEM_AUDIO_PREVIEW", "TEXT_COLOR", textColor));
        changes.add(change("CHAT_LIST_ITEM_AUDIO_PREVIEW", "ICON_COLOR", iconColor));
        return changes;
    }

    private List<UiCustomizationChangeDTO> buildGroupFilePreviewScopedColorGroup(String baseColor) {
        String normalizedBase = normalizeColorText(baseColor);
        String backgroundColor;
        String textColor;
        String iconColor;
        if ("#2563eb".equals(normalizedBase)) {
            backgroundColor = "#dbeafe";
            textColor = "#1e3a8a";
            iconColor = "#2563eb";
        } else if ("#7c3aed".equals(normalizedBase)) {
            backgroundColor = "#ede9fe";
            textColor = "#5b21b6";
            iconColor = "#7c3aed";
        } else if (isDarkColor(normalizedBase)) {
            backgroundColor = "#1f2937";
            textColor = "#f9fafb";
            iconColor = "#93c5fd";
        } else {
            backgroundColor = "#f1f5f9";
            textColor = "#111827";
            iconColor = normalizedBase;
        }
        List<UiCustomizationChangeDTO> changes = new ArrayList<>();
        changes.add(change("CHAT_LIST_ITEM_GROUP_FILE_PREVIEW", "BACKGROUND_COLOR", backgroundColor));
        changes.add(change("CHAT_LIST_ITEM_GROUP_FILE_PREVIEW", "TEXT_COLOR", textColor));
        changes.add(change("CHAT_LIST_ITEM_GROUP_FILE_PREVIEW", "ICON_COLOR", iconColor));
        return changes;
    }

    private List<UiCustomizationChangeDTO> buildGroupBadgesScopedColorGroup(String baseColor) {
        return buildGroupBadgesScopedColorGroup(baseColor, null);
    }

    private List<UiCustomizationChangeDTO> buildGroupBadgesScopedColorGroup(String baseColor, String explicitTextColor) {
        String normalizedBase = normalizeColorText(baseColor);
        String textColor = hasText(explicitTextColor)
                ? normalizeColorText(explicitTextColor)
                : (isDarkColor(normalizedBase) || "#2563eb".equals(normalizedBase) || "#7c3aed".equals(normalizedBase))
                ? "#ffffff" : "#111827";
        List<UiCustomizationChangeDTO> changes = new ArrayList<>();
        changes.add(change("CHAT_LIST_ITEM_GROUP_BADGES", "BACKGROUND_COLOR", normalizedBase));
        changes.add(change("CHAT_LIST_ITEM_GROUP_BADGES", "TEXT_COLOR", textColor));
        return changes;
    }

    private List<UiCustomizationChangeDTO> buildIndividualFilePreviewScopedColorGroup(String baseColor) {
        String normalizedBase = normalizeColorText(baseColor);
        String backgroundColor;
        String textColor;
        String iconColor;
        if ("#2563eb".equals(normalizedBase)) {
            backgroundColor = "#dbeafe";
            textColor = "#1e3a8a";
            iconColor = "#2563eb";
        } else if ("#7c3aed".equals(normalizedBase)) {
            backgroundColor = "#ede9fe";
            textColor = "#5b21b6";
            iconColor = "#7c3aed";
        } else if (isDarkColor(normalizedBase)) {
            backgroundColor = "#1f2937";
            textColor = "#f9fafb";
            iconColor = "#93c5fd";
        } else {
            backgroundColor = "#f1f5f9";
            textColor = "#111827";
            iconColor = normalizedBase;
        }
        List<UiCustomizationChangeDTO> changes = new ArrayList<>();
        changes.add(change("CHAT_LIST_ITEM_FILE_PREVIEW", "BACKGROUND_COLOR", backgroundColor));
        changes.add(change("CHAT_LIST_ITEM_FILE_PREVIEW", "TEXT_COLOR", textColor));
        changes.add(change("CHAT_LIST_ITEM_FILE_PREVIEW", "ICON_COLOR", iconColor));
        return changes;
    }

    private List<UiCustomizationChangeDTO> buildGroupPillColorGroup(String baseColor) {
        String normalizedBase = normalizeColorText(baseColor);
        String textColor;
        String borderColor;
        if ("#ef4444".equals(normalizedBase)) {
            textColor = "#ffffff";
            borderColor = "#b91c1c";
        } else if ("#16a34a".equals(normalizedBase) || "#14532d".equals(normalizedBase)) {
            textColor = "#ffffff";
            borderColor = "#15803d";
        } else if ("#7c3aed".equals(normalizedBase)) {
            textColor = "#ffffff";
            borderColor = "#6d28d9";
        } else if ("#111827".equals(normalizedBase) || isDarkColor(normalizedBase)) {
            textColor = "#f9fafb";
            borderColor = "#334155";
        } else {
            textColor = "#111827";
            borderColor = "#e5e7eb";
        }
        List<UiCustomizationChangeDTO> changes = new ArrayList<>();
        changes.add(change("CHAT_LIST_GROUP_PILL", "BACKGROUND_COLOR", normalizedBase));
        changes.add(change("CHAT_LIST_GROUP_PILL", "TEXT_COLOR", textColor));
        changes.add(change("CHAT_LIST_GROUP_PILL", "BORDER_COLOR", borderColor));
        return changes;
    }

    private List<UiCustomizationChangeDTO> buildPinMenuColorGroup(String baseColor) {
        String normalizedBase = normalizeColorText(baseColor);
        String textColor;
        String borderColor;
        String itemBg;
        String itemText;
        String itemIcon;
        String itemHover;
        String reportIcon;
        String reportHover;
        String dangerIcon;
        String dangerHover;
        if ("#ef4444".equals(normalizedBase)) {
            textColor = "#ffffff"; borderColor = "#b91c1c";
            itemBg = "#ef4444"; itemText = "#ffffff"; itemIcon = "#fee2e2"; itemHover = "#dc2626";
            reportIcon = "#fed7aa"; reportHover = "#c2410c";
            dangerIcon = "#fecaca"; dangerHover = "#991b1b";
        } else if ("#16a34a".equals(normalizedBase) || "#14532d".equals(normalizedBase)) {
            textColor = "#ffffff"; borderColor = "#166534";
            itemBg = "#14532d"; itemText = "#ffffff"; itemIcon = "#bbf7d0"; itemHover = "#166534";
            reportIcon = "#fed7aa"; reportHover = "#c2410c";
            dangerIcon = "#fecaca"; dangerHover = "#991b1b";
        } else if ("#7c3aed".equals(normalizedBase)) {
            textColor = "#ffffff"; borderColor = "#6d28d9";
            itemBg = "#7c3aed"; itemText = "#ffffff"; itemIcon = "#ddd6fe"; itemHover = "#6d28d9";
            reportIcon = "#fed7aa"; reportHover = "#c2410c";
            dangerIcon = "#fecaca"; dangerHover = "#991b1b";
        } else if ("#111827".equals(normalizedBase) || isDarkColor(normalizedBase)) {
            textColor = "#f9fafb"; borderColor = "#334155";
            itemBg = "#111827"; itemText = "#f9fafb"; itemIcon = "#cbd5e1"; itemHover = "#1f2937";
            reportIcon = "#fb923c"; reportHover = "#7c2d12";
            dangerIcon = "#f87171"; dangerHover = "#7f1d1d";
        } else {
            textColor = "#111827"; borderColor = "#e5e7eb";
            itemBg = normalizedBase; itemText = "#111827"; itemIcon = "#2563eb"; itemHover = "#f1f5f9";
            reportIcon = "#c2410c"; reportHover = "#ffedd5";
            dangerIcon = "#b91c1c"; dangerHover = "#fee2e2";
        }
        List<UiCustomizationChangeDTO> changes = new ArrayList<>();
        changes.add(change("CHAT_LIST_PIN_MENU", "BACKGROUND_COLOR", normalizedBase));
        changes.add(change("CHAT_LIST_PIN_MENU", "TEXT_COLOR", textColor));
        changes.add(change("CHAT_LIST_PIN_MENU", "BORDER_COLOR", borderColor));
        changes.add(change("CHAT_LIST_PIN_MENU_ITEM", "BACKGROUND_COLOR", itemBg));
        changes.add(change("CHAT_LIST_PIN_MENU_ITEM", "TEXT_COLOR", itemText));
        changes.add(change("CHAT_LIST_PIN_MENU_ITEM", "ICON_COLOR", itemIcon));
        changes.add(change("CHAT_LIST_PIN_MENU_ITEM", "HOVER_BACKGROUND_COLOR", itemHover));
        changes.add(change("CHAT_LIST_PIN_MENU_REPORT", "TEXT_COLOR", itemText));
        changes.add(change("CHAT_LIST_PIN_MENU_REPORT", "ICON_COLOR", reportIcon));
        changes.add(change("CHAT_LIST_PIN_MENU_REPORT", "HOVER_BACKGROUND_COLOR", reportHover));
        changes.add(change("CHAT_LIST_PIN_MENU_DANGER", "TEXT_COLOR", itemText));
        changes.add(change("CHAT_LIST_PIN_MENU_DANGER", "ICON_COLOR", dangerIcon));
        changes.add(change("CHAT_LIST_PIN_MENU_DANGER", "HOVER_BACKGROUND_COLOR", dangerHover));
        return changes;
    }

    private List<UiCustomizationChangeDTO> buildFiltersColorGroup(String baseColor) {
        String normalizedBase = normalizeColorText(baseColor);
        String textColor = isDarkColor(normalizedBase) || "#ef4444".equals(normalizedBase) || "#7c3aed".equals(normalizedBase) ? "#ffffff" : "#111827";
        String borderColor = "#ef4444".equals(normalizedBase) ? "#b91c1c"
                : "#7c3aed".equals(normalizedBase) ? "#6d28d9"
                : "#16a34a".equals(normalizedBase) || "#14532d".equals(normalizedBase) ? "#166534"
                : isDarkColor(normalizedBase) ? "#334155" : "#e5e7eb";
        List<UiCustomizationChangeDTO> changes = new ArrayList<>();
        changes.add(change("CHAT_LIST_FILTERS", "BACKGROUND_COLOR", normalizedBase));
        changes.add(change("CHAT_LIST_FILTER_BUTTONS", "BACKGROUND_COLOR", normalizedBase));
        changes.add(change("CHAT_LIST_FILTER_BUTTONS", "TEXT_COLOR", textColor));
        changes.add(change("CHAT_LIST_FILTER_BUTTONS", "BORDER_COLOR", borderColor));
        changes.add(change("CHAT_LIST_FILTER_BUTTONS_ACTIVE", "BACKGROUND_COLOR", normalizedBase));
        changes.add(change("CHAT_LIST_FILTER_BUTTONS_ACTIVE", "TEXT_COLOR", textColor));
        changes.add(change("CHAT_LIST_FILTER_BUTTONS_ACTIVE", "BORDER_COLOR", borderColor));
        return changes;
    }

    private List<UiCustomizationChangeDTO> buildStatusPillsColorGroup(String baseColor) {
        String normalizedBase = normalizeColorText(baseColor);
        String textColor = isDarkColor(normalizedBase) || "#ef4444".equals(normalizedBase) || "#7c3aed".equals(normalizedBase) ? "#ffffff" : "#111827";
        String borderColor = "#ef4444".equals(normalizedBase) ? "#b91c1c"
                : "#7c3aed".equals(normalizedBase) ? "#6d28d9"
                : "#16a34a".equals(normalizedBase) || "#14532d".equals(normalizedBase) ? "#166534"
                : isDarkColor(normalizedBase) ? "#334155" : "#e5e7eb";
        List<UiCustomizationChangeDTO> changes = new ArrayList<>();
        changes.add(change("CHAT_LIST_STATUS_PILLS", "REPORTED_BACKGROUND_COLOR", normalizedBase));
        changes.add(change("CHAT_LIST_STATUS_PILLS", "REPORTED_TEXT_COLOR", textColor));
        changes.add(change("CHAT_LIST_STATUS_PILLS", "BLOCKED_BACKGROUND_COLOR", normalizedBase));
        changes.add(change("CHAT_LIST_STATUS_PILLS", "BLOCKED_TEXT_COLOR", textColor));
        changes.add(change("CHAT_LIST_STATUS_PILLS", "BORDER_COLOR", borderColor));
        return changes;
    }

    private List<UiCustomizationChangeDTO> buildSearchColorGroup(String baseColor) {
        String normalizedBase = normalizeColorText(baseColor);
        String textColor = isDarkColor(normalizedBase) || "#ef4444".equals(normalizedBase) || "#7c3aed".equals(normalizedBase) ? "#ffffff" : "#111827";
        String borderColor = "#ef4444".equals(normalizedBase) ? "#b91c1c"
                : "#7c3aed".equals(normalizedBase) ? "#6d28d9"
                : "#16a34a".equals(normalizedBase) || "#14532d".equals(normalizedBase) ? "#166534"
                : isDarkColor(normalizedBase) ? "#334155" : "#e5e7eb";
        List<UiCustomizationChangeDTO> changes = new ArrayList<>();
        changes.add(change("CHAT_LIST_SEARCH", "BACKGROUND_COLOR", normalizedBase));
        changes.add(change("CHAT_LIST_SEARCH", "TEXT_COLOR", textColor));
        changes.add(change("CHAT_LIST_SEARCH", "BORDER_COLOR", borderColor));
        changes.add(change("CHAT_LIST_SEARCH", "PLACEHOLDER_COLOR", isDarkColor(normalizedBase) ? "#cbd5e1" : "#64748b"));
        return changes;
    }

    private List<UiCustomizationChangeDTO> buildStyleGroupChanges(String consulta, String area, String property, String value) {
        String effectiveArea = resolveGroupAreaFromConsulta(consulta, area);
        if (!"BACKGROUND_COLOR".equals(property) || !hasText(value)) {
            return List.of();
        }
        return switch (effectiveArea) {
            case "CHAT_LIST_PANEL" -> buildChatListPanelColorGroup(consulta, value);
            case "CHAT_LIST_FILTERS" -> buildFiltersColorGroup(value);
            case "CHAT_LIST_AUDIO_PREVIEW" -> buildAudioPreviewColorGroup(value);
            case "CHAT_LIST_GROUP_PILL" -> buildGroupPillColorGroup(value);
            case "CHAT_LIST_PIN_MENU" -> buildPinMenuColorGroup(value);
            case "CHAT_LIST_STATUS_PILLS" -> buildStatusPillsColorGroup(value);
            case "CHAT_LIST_SEARCH" -> buildSearchColorGroup(value);
            case "CHAT_LIST_ITEM", "CHAT_LIST_ITEM_GROUP" -> buildChatListItemColorGroup(consulta, effectiveArea, value);
            default -> List.of();
        };
    }

    private boolean isBlockThemeChange(UiCustomizationChangeDTO change) {
        if (change == null || !hasText(change.getArea()) || !hasText(change.getProperty())) {
            return false;
        }
        if (!"BACKGROUND_COLOR".equals(change.getProperty())) {
            return false;
        }
        return Set.of("CHAT_LIST_PANEL", "CHAT_LIST_ITEM", "CHAT_LIST_ITEM_GROUP", "CHAT_LIST_AUDIO_PREVIEW", "CHAT_LIST_GROUP_PILL",
                "CHAT_LIST_PIN_MENU", "CHAT_LIST_STATUS_PILLS", "CHAT_LIST_SEARCH", "CHAT_LIST_FILTERS").contains(change.getArea());
    }

    private List<UiCustomizationChangeDTO> buildChatListPanelColorGroup(String consulta, String baseColor) {
        String normalizedBase = normalizeColorText(baseColor);
        String panelText = isDarkColor(normalizedBase) || "#ef4444".equals(normalizedBase) || "#7c3aed".equals(normalizedBase) || "#14532d".equals(normalizedBase)
                ? "#f9fafb" : "#111827";
        String panelBorder = "#ef4444".equals(normalizedBase) ? "#b91c1c"
                : "#7c3aed".equals(normalizedBase) ? "#6d28d9"
                : "#16a34a".equals(normalizedBase) || "#14532d".equals(normalizedBase) ? "#166534"
                : isDarkColor(normalizedBase) ? "#334155" : "#e5e7eb";
        List<UiCustomizationChangeDTO> changes = new ArrayList<>();
        String filterActiveBg = (("#ffffff".equals(normalizedBase) || !isDarkColor(normalizedBase))) ? "#2563eb" : normalizedBase;
        String filterActiveText = "#2563eb".equals(filterActiveBg) ? "#ffffff" : panelText;
        String filterActiveBorder = "#2563eb".equals(filterActiveBg) ? "#2563eb" : panelBorder;
        changes.add(change("CHAT_LIST_PANEL", "BACKGROUND_COLOR", normalizedBase));
        changes.add(change("CHAT_LIST_PANEL", "TEXT_COLOR", panelText));
        changes.add(change("CHAT_LIST_PANEL", "BORDER_COLOR", panelBorder));
        changes.add(change("CHAT_LIST_HEADER", "BACKGROUND_COLOR", normalizedBase));
        changes.add(change("CHAT_LIST_HEADER", "TEXT_COLOR", panelText));
        changes.addAll(buildSearchColorGroup(normalizedBase));
        changes.add(change("CHAT_LIST_FILTERS", "BACKGROUND_COLOR", normalizedBase));
        changes.add(change("CHAT_LIST_FILTERS", "BORDER_COLOR", panelBorder));
        changes.add(change("CHAT_LIST_FILTER_BUTTONS", "BACKGROUND_COLOR", normalizedBase));
        changes.add(change("CHAT_LIST_FILTER_BUTTONS", "TEXT_COLOR", panelText));
        changes.add(change("CHAT_LIST_FILTER_BUTTONS", "BORDER_COLOR", panelBorder));
        changes.add(change("CHAT_LIST_FILTER_BUTTONS_ACTIVE", "BACKGROUND_COLOR", filterActiveBg));
        changes.add(change("CHAT_LIST_FILTER_BUTTONS_ACTIVE", "TEXT_COLOR", filterActiveText));
        changes.add(change("CHAT_LIST_FILTER_BUTTONS_ACTIVE", "BORDER_COLOR", filterActiveBorder));
        changes.addAll(buildChatListItemColorGroup(consulta, "CHAT_LIST_ITEM", normalizedBase));
        changes.addAll(buildPinMenuColorGroup(normalizedBase));
        changes.add(change("CHAT_LIST_ITEM_UNREAD", "BACKGROUND_COLOR", normalizedBase));
        changes.add(change("CHAT_LIST_ITEM_UNREAD", "TEXT_COLOR", panelText));
        changes.add(change("CHAT_LIST_ITEM_UNREAD", "BORDER_COLOR", panelBorder));
        changes.add(change("CHAT_LIST_ITEM_UNREAD", "HOVER_BACKGROUND_COLOR", isDarkColor(normalizedBase) ? "#1f2937" : "#f1f5f9"));
        changes.add(change("CHAT_LIST_IMAGE_PREVIEW", "BACKGROUND_COLOR", normalizedBase));
        changes.add(change("CHAT_LIST_IMAGE_PREVIEW", "TEXT_COLOR", panelText));
        changes.add(change("CHAT_LIST_IMAGE_PREVIEW", "BORDER_COLOR", panelBorder));
        changes.add(change("CHAT_LIST_FILE_PREVIEW", "BACKGROUND_COLOR", normalizedBase));
        changes.add(change("CHAT_LIST_FILE_PREVIEW", "TEXT_COLOR", panelText));
        changes.add(change("CHAT_LIST_FILE_PREVIEW", "ICON_COLOR", panelText));
        changes.add(change("CHAT_LIST_FILE_PREVIEW", "BORDER_COLOR", panelBorder));
        changes.add(change("CHAT_LIST_ACTIONS_MENU", "BACKGROUND_COLOR", normalizedBase));
        changes.add(change("CHAT_LIST_ACTIONS_MENU", "TEXT_COLOR", panelText));
        changes.add(change("CHAT_LIST_ACTIONS_MENU", "BORDER_COLOR", panelBorder));
        changes.add(change("CHAT_LIST_ACTIONS_MENU_ITEM", "BACKGROUND_COLOR", normalizedBase));
        changes.add(change("CHAT_LIST_ACTIONS_MENU_ITEM", "TEXT_COLOR", panelText));
        changes.add(change("CHAT_LIST_ACTIONS_MENU_ITEM", "ICON_COLOR", panelText));
        changes.add(change("CHAT_LIST_ACTIONS_MENU_ITEM", "HOVER_BACKGROUND_COLOR", isDarkColor(normalizedBase) ? "#1f2937" : "#f1f5f9"));
        changes.add(change("CHAT_LIST_BADGES", "BACKGROUND_COLOR", panelBorder));
        changes.add(change("CHAT_LIST_BADGES", "TEXT_COLOR", panelText));
        changes.add(change("CHAT_LIST_PIN_TOGGLE", "ICON_COLOR", panelText));
        return changes;
    }

    private List<UiCustomizationChangeDTO> extractExplicitChanges(String consulta,
                                                                  String area,
                                                                  String property,
                                                                  String value,
                                                                  String valuePreset,
                                                                  List<UiCustomizationChangeDTO> inputChanges) {
        List<UiCustomizationChangeDTO> explicit = new ArrayList<>();
        if (inputChanges != null && !inputChanges.isEmpty()) {
            explicit.addAll(inputChanges);
        } else if (hasText(area) && hasText(property) && hasText(value)) {
            UiCustomizationChangeDTO one = new UiCustomizationChangeDTO();
            one.setArea(area);
            one.setProperty(property);
            one.setValue(value);
            one.setValuePreset(valuePreset);
            explicit.add(one);
        } else {
            explicit.addAll(parseDeterministicMultiChanges(consulta));
            if (!explicit.isEmpty()) {
                LOGGER.info("[AI][UI_MULTI_REPAIR] generatedChanges={}", explicit.size());
            }
        }
        return repairScopedExplicitChanges(consulta, explicit);
    }

    private List<UiCustomizationChangeDTO> repairScopedExplicitChanges(String consulta, List<UiCustomizationChangeDTO> explicit) {
        if (explicit == null || explicit.isEmpty()) {
            return explicit;
        }
        if (isIndividualPreviewTextRequest(consulta)) {
            boolean repaired = false;
            for (UiCustomizationChangeDTO change : explicit) {
                if (change != null && "TEXT_COLOR".equals(change.getProperty())
                        && Set.of("CHAT_LIST_ITEM", "CHAT_LIST_PREVIEW", "CHAT_LIST_ITEM_GROUP_PREVIEW").contains(change.getArea())) {
                    change.setArea("CHAT_LIST_ITEM_PREVIEW");
                    repaired = true;
                }
            }
            if (repaired) {
                logRuleMatch("REPAIR_INDIVIDUAL_PREVIEW_TEXT", consulta, explicit);
            }
        }
        if (isBothIndividualAndGroupScopeRequest(consulta)) {
            String itemColor = null;
            boolean hasItem = false;
            boolean hasGroup = false;
            for (UiCustomizationChangeDTO change : explicit) {
                if (change == null || !"BACKGROUND_COLOR".equals(change.getProperty())) continue;
                if ("CHAT_LIST_ITEM".equals(change.getArea())) {
                    hasItem = true;
                    itemColor = change.getValue();
                }
                if ("CHAT_LIST_ITEM_GROUP".equals(change.getArea())) {
                    hasGroup = true;
                }
            }
            if (hasItem && !hasGroup && hasText(itemColor)) {
                explicit.add(change("CHAT_LIST_ITEM_GROUP", "BACKGROUND_COLOR", itemColor));
                logRuleMatch("REPAIR_BOTH_INDIVIDUAL_AND_GROUP_ITEMS", consulta, explicit);
            }
        }
        if (isGroupBadgesScopedRequest(consulta) && isIncompleteScopedGroup(explicit, "CHAT_LIST_ITEM_GROUP_BADGES", Set.of("BACKGROUND_COLOR", "TEXT_COLOR"))) {
            String base = firstExplicitColor(explicit);
            List<UiCustomizationChangeDTO> repaired = buildGroupBadgesScopedColorGroup(hasText(base) ? base : "#7c3aed");
            logRuleMatch("REPAIR_GROUP_BADGES_SCOPED", consulta, repaired);
            return repaired;
        }
        if (isGroupAudioPreviewScopedRequest(consulta) && isIncompleteScopedGroup(explicit, "CHAT_LIST_ITEM_GROUP_AUDIO_PREVIEW", Set.of("BACKGROUND_COLOR", "TEXT_COLOR", "ICON_COLOR"))) {
            String base = firstExplicitColor(explicit);
            List<UiCustomizationChangeDTO> repaired = buildGroupAudioPreviewScopedColorGroup(hasText(base) ? base : "#f97316");
            logRuleMatch("REPAIR_GROUP_AUDIO_PREVIEW_SCOPED", consulta, repaired);
            return repaired;
        }
        if (isIndividualAudioPreviewScopedRequest(consulta) && explicit.stream().noneMatch(c -> c != null && "CHAT_LIST_ITEM_AUDIO_PREVIEW".equals(c.getArea()))) {
            String base = firstExplicitColor(explicit);
            List<UiCustomizationChangeDTO> repaired = buildIndividualAudioPreviewScopedColorGroup(hasText(base) ? base : "#f97316");
            logRuleMatch("REPAIR_INDIVIDUAL_AUDIO_PREVIEW_SCOPED", consulta, repaired);
            return repaired;
        }
        if (isIndividualFilePreviewRequest(consulta) && explicit.stream().noneMatch(c -> c != null && "CHAT_LIST_ITEM_FILE_PREVIEW".equals(c.getArea()))) {
            String base = firstExplicitColor(explicit);
            List<UiCustomizationChangeDTO> repaired = buildIndividualFilePreviewScopedColorGroup(hasText(base) ? base : "#2563eb");
            logRuleMatch("REPAIR_INDIVIDUAL_FILE_PREVIEW_SCOPED", consulta, repaired);
            return repaired;
        }
        if (isChatListPanelThemeRequest(consulta) && isIncompleteChatListTheme(explicit)) {
            String base = firstExplicitColor(explicit);
            if (!hasText(base)) {
                base = resolveColorFromText(consulta);
            }
            if (hasText(base)) {
                List<UiCustomizationChangeDTO> repaired = new ArrayList<>(buildChatListPanelColorGroup(consulta, base));
                repaired.addAll(explicit);
                logRuleMatch("REPAIR_CHAT_LIST_PANEL_THEME", consulta, repaired);
                return repaired;
            }
        }
        return explicit;
    }

    private String firstExplicitColor(List<UiCustomizationChangeDTO> changes) {
        if (changes == null) {
            return null;
        }
        for (UiCustomizationChangeDTO change : changes) {
            if (change != null && COLOR_PROPERTIES.contains(change.getProperty()) && hasText(change.getValue())) {
                return change.getValue();
            }
        }
        return null;
    }

    private void logRuleMatch(String rule, String consulta, List<UiCustomizationChangeDTO> changes) {
        LOGGER.info("[AI][UI_CUSTOMIZATION_RULE_MATCH] rule={} consulta={} changesCount={}",
                rule,
                safe(consulta),
                changes == null ? 0 : changes.size());
    }

    private List<UiCustomizationChangeDTO> completePinMenuThemeChanges(String baseColor,
                                                                       List<UiCustomizationChangeDTO> inputChanges) {
        List<UiCustomizationChangeDTO> template = buildPinMenuColorGroup(baseColor);
        Map<String, UiCustomizationChangeDTO> merged = new LinkedHashMap<>();
        for (UiCustomizationChangeDTO change : template) {
            merged.put(changeKey(change), change);
        }
        if (inputChanges != null) {
            for (UiCustomizationChangeDTO explicit : inputChanges) {
                if (explicit == null || !hasText(explicit.getArea()) || !hasText(explicit.getProperty()) || !hasText(explicit.getValue())) {
                    continue;
                }
                merged.put(changeKey(explicit), explicit);
                LOGGER.info("[AI][UI_CUSTOMIZATION_COLOR_PRESERVE] area={} property={} value={}",
                        explicit.getArea(), explicit.getProperty(), safe(explicit.getValue()));
            }
        }
        return new ArrayList<>(merged.values());
    }

    private String resolveForcedBaseColor(String consulta,
                                          String value,
                                          List<UiCustomizationChangeDTO> inputChanges,
                                          String fallbackColor) {
        String explicit = firstExplicitColor(inputChanges);
        if (hasText(explicit)) {
            return explicit;
        }
        String resolved = resolveColorFromText(value);
        if (hasText(resolved)) {
            return resolved;
        }
        resolved = resolveColorFromText(consulta);
        if (hasText(resolved)) {
            return resolved;
        }
        return fallbackColor;
    }

    private boolean isIncompleteScopedGroup(List<UiCustomizationChangeDTO> changes,
                                            String area,
                                            Set<String> requiredProperties) {
        if (changes == null || changes.isEmpty()) {
            return true;
        }
        Set<String> found = new HashSet<>();
        for (UiCustomizationChangeDTO change : changes) {
            if (change != null && area.equals(change.getArea()) && hasText(change.getProperty())) {
                found.add(change.getProperty());
            }
        }
        return !found.containsAll(requiredProperties);
    }

    private boolean isIncompletePinMenuTheme(List<UiCustomizationChangeDTO> changes) {
        return isIncompleteScopedGroup(changes, "CHAT_LIST_PIN_MENU", Set.of("BACKGROUND_COLOR", "TEXT_COLOR", "BORDER_COLOR"))
                || isIncompleteScopedGroup(changes, "CHAT_LIST_PIN_MENU_ITEM", Set.of("BACKGROUND_COLOR", "TEXT_COLOR", "ICON_COLOR", "HOVER_BACKGROUND_COLOR"))
                || isIncompleteScopedGroup(changes, "CHAT_LIST_PIN_MENU_REPORT", Set.of("TEXT_COLOR", "ICON_COLOR", "HOVER_BACKGROUND_COLOR"))
                || isIncompleteScopedGroup(changes, "CHAT_LIST_PIN_MENU_DANGER", Set.of("TEXT_COLOR", "ICON_COLOR", "HOVER_BACKGROUND_COLOR"));
    }

    private boolean isIncompleteChatListTheme(List<UiCustomizationChangeDTO> changes) {
        if (changes == null || changes.isEmpty()) {
            return true;
        }
        boolean hasPanelBg = false;
        boolean hasPanelText = false;
        boolean hasItemText = false;
        boolean hasGroupText = false;
        boolean hasPreviewText = false;
        boolean hasBadgeText = false;
        boolean hasPinMenuText = false;
        for (UiCustomizationChangeDTO change : changes) {
            if (change == null) continue;
            if ("CHAT_LIST_PANEL".equals(change.getArea()) && "BACKGROUND_COLOR".equals(change.getProperty())) hasPanelBg = true;
            if ("CHAT_LIST_PANEL".equals(change.getArea()) && "TEXT_COLOR".equals(change.getProperty())) hasPanelText = true;
            if ("CHAT_LIST_ITEM".equals(change.getArea()) && "TEXT_COLOR".equals(change.getProperty())) hasItemText = true;
            if ("CHAT_LIST_ITEM_GROUP".equals(change.getArea()) && "TEXT_COLOR".equals(change.getProperty())) hasGroupText = true;
            if ("CHAT_LIST_PREVIEW".equals(change.getArea()) && "TEXT_COLOR".equals(change.getProperty())) hasPreviewText = true;
            if ("CHAT_LIST_BADGES".equals(change.getArea()) && "TEXT_COLOR".equals(change.getProperty())) hasBadgeText = true;
            if ("CHAT_LIST_PIN_MENU".equals(change.getArea()) && "TEXT_COLOR".equals(change.getProperty())) hasPinMenuText = true;
        }
        return !(hasPanelBg && hasPanelText && hasItemText && hasGroupText && hasPreviewText && hasBadgeText && hasPinMenuText);
    }

    private List<UiCustomizationChangeDTO> parseDeterministicMultiChanges(String consulta) {
        List<UiCustomizationChangeDTO> parsed = new ArrayList<>();
        String normalized = normalizeSemanticText(consulta);
        if (!hasText(normalized)) {
            return parsed;
        }
        String inferredColor = resolveColorFromText(consulta);
        if (isBothIndividualAndGroupScopeRequest(consulta) && hasText(inferredColor)) {
            parsed.add(change("CHAT_LIST_ITEM", "BACKGROUND_COLOR", inferredColor));
            parsed.add(change("CHAT_LIST_ITEM_GROUP", "BACKGROUND_COLOR", inferredColor));
            logRuleMatch("PARSE_BOTH_INDIVIDUAL_AND_GROUP_ITEMS", consulta, parsed);
            return parsed;
        }
        if (isChatListPanelThemeRequest(consulta) && hasText(inferredColor)) {
            parsed.addAll(buildChatListPanelColorGroup(consulta, inferredColor));
            logRuleMatch("PARSE_CHAT_LIST_PANEL_THEME", consulta, parsed);
            return parsed;
        }
        boolean strictGroupOnlyScope = isStrictGroupOnlyScope(consulta, null);
        boolean strictIndividualOnlyScope = isStrictIndividualScope(consulta, null);
        boolean mentionsFilePreview = containsAny(normalized, "archivos", "archivo", "preview de archivos", "file preview");
        boolean mentionsAudioPreview = containsAny(normalized, "preview de audio", "audio preview", "nota de voz", "audio");
        boolean mentionsImagePreview = containsAny(normalized, "preview de imagen", "image preview", "imagen", "imagenes");
        boolean mentionsGroupBadges = containsAny(normalized, "badges", "badge", "contador", "mensajes sin leer", "no leidos");
        if (isGroupAudioPreviewScopedRequest(consulta) && hasText(inferredColor)) {
            parsed.addAll(buildGroupAudioPreviewScopedColorGroup(inferredColor));
            logRuleMatch("PARSE_GROUP_AUDIO_PREVIEW_SCOPED", consulta, parsed);
            return parsed;
        }
        if (isIndividualAudioPreviewScopedRequest(consulta) && hasText(inferredColor)) {
            parsed.addAll(buildIndividualAudioPreviewScopedColorGroup(inferredColor));
            logRuleMatch("PARSE_INDIVIDUAL_AUDIO_PREVIEW_SCOPED", consulta, parsed);
            return parsed;
        }
        if (isGroupBadgesScopedRequest(consulta) && hasText(inferredColor)) {
            parsed.addAll(buildGroupBadgesScopedColorGroup(inferredColor));
            logRuleMatch("PARSE_GROUP_BADGES_SCOPED", consulta, parsed);
            return parsed;
        }
        if (isIndividualFilePreviewRequest(consulta) && hasText(inferredColor)) {
            parsed.addAll(buildIndividualFilePreviewScopedColorGroup(inferredColor));
            logRuleMatch("PARSE_INDIVIDUAL_FILE_PREVIEW_SCOPED", consulta, parsed);
            return parsed;
        }
        if (isIndividualPreviewTextRequest(consulta) && hasText(inferredColor)) {
            parsed.add(change("CHAT_LIST_ITEM_PREVIEW", "TEXT_COLOR", inferredColor));
            logRuleMatch("PARSE_INDIVIDUAL_PREVIEW_TEXT", consulta, parsed);
            return parsed;
        }
        String headerBg = firstColorAfter(normalized, "encabezado de la lista de los chats", "encabezado de la lista", "encabezado del listado", "header del listado");
        if (hasText(headerBg)) {
            parsed.add(change("CHAT_LIST_HEADER", "BACKGROUND_COLOR", headerBg));
        }
        String headerText = firstColorAfter(normalized, "texto del titulo", "texto del titulo", "titulo");
        if (hasText(headerText)) {
            parsed.add(change("CHAT_LIST_HEADER", "TEXT_COLOR", headerText));
        }
        String chatListBg = firstColorAfter(normalized, "fondo del listado de chats", "fondo de los chats", "fondo del chat");
        if (hasText(chatListBg)) {
            parsed.add(change("CHAT_LIST_ITEM", "BACKGROUND_COLOR", chatListBg));
        }
        String chatsText = firstColorAfter(normalized, "texto de los chats", "texto del chat", "texto chats");
        if (hasText(chatsText)) {
            parsed.add(change("CHAT_LIST_ITEM", "TEXT_COLOR", chatsText));
        }
        String previewText = firstColorAfter(normalized, "ultimo mensaje", "último mensaje", "preview", "vista previa");
        if (hasText(previewText) && !(strictGroupOnlyScope && (mentionsFilePreview || mentionsAudioPreview || mentionsImagePreview))) {
            parsed.add(change(strictGroupOnlyScope ? "CHAT_LIST_ITEM_GROUP_PREVIEW" : (strictIndividualOnlyScope ? "CHAT_LIST_ITEM_PREVIEW" : "CHAT_LIST_PREVIEW"), "TEXT_COLOR", previewText));
        }
        String audioPreviewColor = firstColorAfter(normalized, "preview de audio", "audio preview", "nota de voz", "audio");
        if (strictGroupOnlyScope && hasText(audioPreviewColor) && mentionsAudioPreview) {
            parsed.addAll(buildGroupAudioPreviewScopedColorGroup(audioPreviewColor));
        } else if (hasText(audioPreviewColor) && mentionsAudioPreview) {
            parsed.add(change("CHAT_LIST_AUDIO_PREVIEW", "BACKGROUND_COLOR", audioPreviewColor));
        }
        String filePreviewColor = firstColorAfter(normalized, "preview de archivos", "preview de archivo", "file preview", "archivos", "archivo");
        if (strictGroupOnlyScope && hasText(filePreviewColor) && mentionsFilePreview) {
            parsed.addAll(buildGroupFilePreviewScopedColorGroup(filePreviewColor));
        } else if (strictIndividualOnlyScope && hasText(filePreviewColor) && mentionsFilePreview) {
            parsed.addAll(buildIndividualFilePreviewScopedColorGroup(filePreviewColor));
        }
        String groupBadgesColor = firstColorAfter(normalized, "badges", "badge", "contador", "mensajes sin leer", "no leidos");
        if (strictGroupOnlyScope && hasText(groupBadgesColor) && mentionsGroupBadges) {
            parsed.addAll(buildGroupBadgesScopedColorGroup(groupBadgesColor));
        }
        String searchBg = firstColorAfter(normalized, "buscador");
        if (hasText(searchBg)) {
            parsed.add(change("CHAT_LIST_SEARCH", "BACKGROUND_COLOR", searchBg));
        }
        String searchBorder = firstColorAfter(normalized, "borde del buscador", "border del buscador", "buscador");
        if (hasText(searchBorder) && containsAny(normalized, "borde", "border")) {
            parsed.add(change("CHAT_LIST_SEARCH", "BORDER_COLOR", searchBorder));
        }
        String filtersBg = firstColorAfter(normalized, "fondo del filtro de los chats", "zona de filtros", "contenedor de filtros", "fondo de los filtros");
        if (hasText(filtersBg)) {
            parsed.add(change("CHAT_LIST_FILTERS", "BACKGROUND_COLOR", filtersBg));
        }
        String filterButtonsActiveBg = firstColorAfter(normalized,
                "boton activo de filtros", "boton active de filtros", "active de filtros",
                "filtro activo", "filtro seleccionado", "filtro marcado", "seleccionado de filtros", "marcado de filtros");
        if (hasText(filterButtonsActiveBg) && containsAny(normalized, "fondo", "background", "color")) {
            parsed.add(change("CHAT_LIST_FILTER_BUTTONS_ACTIVE", "BACKGROUND_COLOR", filterButtonsActiveBg));
        }
        String filterButtonsBg = firstColorAfter(normalized, "botones de los filtros", "buttons de los filtros", "filtros individuales");
        if (hasText(filterButtonsBg) && containsAny(normalized, "fondo", "background")) {
            parsed.add(change("CHAT_LIST_FILTER_BUTTONS", "BACKGROUND_COLOR", filterButtonsBg));
        }
        String filterButtonsText = firstColorAfter(normalized, "texto de los botones de filtros", "texto de los botones", "buttons de los filtros");
        if (hasText(filterButtonsText) && containsAny(normalized, "texto")) {
            parsed.add(change("CHAT_LIST_FILTER_BUTTONS", "TEXT_COLOR", filterButtonsText));
        }
        String filterButtonsHover = firstColorAfter(normalized, "hover de los botones de filtros", "hover botones de filtros");
        if (hasText(filterButtonsHover)) {
            parsed.add(change("CHAT_LIST_FILTER_BUTTONS", "HOVER_BACKGROUND_COLOR", filterButtonsHover));
        }
        String pinMenuBg = firstColorAfter(normalized, "desplegable", "menu de opciones", "menu del chat");
        if (hasText(pinMenuBg)) {
            parsed.add(change("CHAT_LIST_PIN_MENU", "BACKGROUND_COLOR", pinMenuBg));
        }
        return parsed;
    }

    private String firstColorAfter(String normalized, String... anchors) {
        for (String anchor : anchors) {
            int idx = normalized.indexOf(anchor);
            if (idx < 0) {
                continue;
            }
            String tail = normalized.substring(idx);
            String resolved = resolveColorFromText(tail);
            if (hasText(resolved)) {
                return resolved;
            }
        }
        return null;
    }

    private String resolveRequestedBackgroundColor(String consulta) {
        String normalized = normalizeSemanticText(consulta);
        return firstColorAfter(normalized, "fondo", "background", "color de fondo");
    }

    private String resolveRequestedTextColor(String consulta) {
        String normalized = normalizeSemanticText(consulta);
        return firstColorAfter(normalized, "texto", "letra", "color del texto");
    }

    private boolean hasPropertySpecificColors(String consulta) {
        String normalized = normalizeSemanticText(consulta);
        boolean hasBg = hasText(resolveRequestedBackgroundColor(consulta));
        boolean hasTextColor = hasText(resolveRequestedTextColor(consulta));
        return (hasBg && hasTextColor)
                || containsAny(normalized,
                "fondo ", "background ", "color de fondo ", "texto ", "letra ", "color del texto ");
    }

    private String resolveColorFromText(String text) {
        if (!hasText(text)) {
            return null;
        }
        String normalized = normalizeSemanticText(text);
        int bestIndex = Integer.MAX_VALUE;
        String bestColor = null;
        for (Map.Entry<String, String> entry : NAMED_COLORS.entrySet()) {
            int idx = normalized.indexOf(entry.getKey());
            if (idx >= 0 && idx < bestIndex) {
                bestIndex = idx;
                bestColor = entry.getValue();
            }
        }
        return bestColor;
    }

    private String changeKey(UiCustomizationChangeDTO change) {
        return (change.getArea() == null ? "" : change.getArea()) + "|" + (change.getProperty() == null ? "" : change.getProperty());
    }

    private boolean isChatListVisualBlockRequest(String consulta) {
        String normalized = normalizeSemanticText(consulta);
        return containsAny(normalized,
                "chats de la izquierda", "cada chat", "conversaciones del listado",
                "tarjetas de chat", "chats del listado", "los chats", "grupos del listado",
                "conversaciones grupales", "chats grupales", "conversaciones privadas",
                "chats privados", "chats individuales", "chats uno a uno");
    }

    private boolean isChatListPanelThemeRequest(String consulta) {
        String normalized = normalizeSemanticText(consulta);
        return containsAny(normalized,
                "fondo del listado", "listado de chats", "lista de chats", "panel de chats",
                "zona izquierda de chats", "bloque de chats", "todos los estilos del listado", "estilos visuales del listado");
    }

    private boolean isWhiteElegantThemeRequest(String consulta) {
        String normalized = normalizeSemanticText(consulta);
        return containsAny(normalized,
                "blanco elegante", "tema blanco", "estilo blanco", "todo en blanco");
    }

    private boolean isHighVolumeThemeRequest(String consulta, String area, List<UiCustomizationChangeDTO> explicitChanges) {
        String normalized = normalizeSemanticText(consulta);
        if (containsAny(normalized,
                "todo el listado", "todos los estilos", "tema completo", "listado de chats", "panel de chats",
                "zona izquierda", "que combine", "incluyendo filtros", "incluyendo chats no leidos",
                "incluyendo previews", "incluyendo desplegable")) {
            return true;
        }
        if ("CHAT_LIST_PANEL".equals(area) || "CHAT_LIST_FILTERS".equals(area) || "CHAT_LIST_PIN_MENU".equals(area)) {
            return true;
        }
        if (explicitChanges != null) {
            for (UiCustomizationChangeDTO change : explicitChanges) {
                if (isBlockThemeChange(change)) {
                    return true;
                }
            }
        }
        return false;
    }

    private List<UiCustomizationChangeDTO> buildWhiteElegantChatListThemeChanges() {
        List<UiCustomizationChangeDTO> changes = new ArrayList<>();
        changes.add(change("CHAT_LIST_PANEL", "BACKGROUND_COLOR", "#ffffff"));
        changes.add(change("CHAT_LIST_PANEL", "TEXT_COLOR", "#111827"));
        changes.add(change("CHAT_LIST_HEADER", "BACKGROUND_COLOR", "#ffffff"));
        changes.add(change("CHAT_LIST_SEARCH", "BACKGROUND_COLOR", "#ffffff"));
        changes.add(change("CHAT_LIST_SEARCH", "TEXT_COLOR", "#111827"));
        changes.add(change("CHAT_LIST_SEARCH", "BORDER_COLOR", "#e5e7eb"));
        changes.add(change("CHAT_LIST_FILTERS", "BACKGROUND_COLOR", "#ffffff"));
        changes.add(change("CHAT_LIST_FILTERS", "BORDER_COLOR", "#e5e7eb"));
        changes.add(change("CHAT_LIST_FILTER_BUTTONS", "BACKGROUND_COLOR", "#ffffff"));
        changes.add(change("CHAT_LIST_FILTER_BUTTONS", "TEXT_COLOR", "#111827"));
        changes.add(change("CHAT_LIST_FILTER_BUTTONS", "BORDER_COLOR", "#e5e7eb"));
        changes.add(change("CHAT_LIST_FILTER_BUTTONS_ACTIVE", "BACKGROUND_COLOR", "#2563eb"));
        changes.add(change("CHAT_LIST_FILTER_BUTTONS_ACTIVE", "TEXT_COLOR", "#ffffff"));
        changes.add(change("CHAT_LIST_FILTER_BUTTONS_ACTIVE", "BORDER_COLOR", "#2563eb"));
        changes.add(change("CHAT_LIST_ITEM", "BACKGROUND_COLOR", "#ffffff"));
        changes.add(change("CHAT_LIST_ITEM", "TEXT_COLOR", "#111827"));
        changes.add(change("CHAT_LIST_ITEM", "BORDER_COLOR", "#e5e7eb"));
        changes.add(change("CHAT_LIST_ITEM", "HOVER_BACKGROUND_COLOR", "#f1f5f9"));
        changes.add(change("CHAT_LIST_ITEM_ACTIVE", "BACKGROUND_COLOR", "#2563eb"));
        changes.add(change("CHAT_LIST_ITEM_ACTIVE", "TEXT_COLOR", "#ffffff"));
        changes.add(change("CHAT_LIST_ITEM_UNREAD", "BACKGROUND_COLOR", "#f8fafc"));
        changes.add(change("CHAT_LIST_ITEM_UNREAD", "TEXT_COLOR", "#111827"));
        changes.add(change("CHAT_LIST_ITEM_UNREAD", "BORDER_COLOR", "#cbd5e1"));
        changes.add(change("CHAT_LIST_ITEM_GROUP", "BACKGROUND_COLOR", "#ffffff"));
        changes.add(change("CHAT_LIST_ITEM_GROUP", "TEXT_COLOR", "#111827"));
        changes.add(change("CHAT_LIST_PREVIEW", "TEXT_COLOR", "#64748b"));
        changes.add(change("CHAT_LIST_DRAFT_PREVIEW", "TEXT_COLOR", "#64748b"));
        changes.add(change("CHAT_LIST_AUDIO_PREVIEW", "BACKGROUND_COLOR", "#f1f5f9"));
        changes.add(change("CHAT_LIST_AUDIO_PREVIEW", "TEXT_COLOR", "#111827"));
        changes.add(change("CHAT_LIST_IMAGE_PREVIEW", "BACKGROUND_COLOR", "#f1f5f9"));
        changes.add(change("CHAT_LIST_IMAGE_PREVIEW", "TEXT_COLOR", "#111827"));
        changes.add(change("CHAT_LIST_FILE_PREVIEW", "BACKGROUND_COLOR", "#f1f5f9"));
        changes.add(change("CHAT_LIST_FILE_PREVIEW", "TEXT_COLOR", "#111827"));
        changes.add(change("CHAT_LIST_FILE_PREVIEW", "ICON_COLOR", "#2563eb"));
        changes.add(change("CHAT_LIST_BADGES", "BACKGROUND_COLOR", "#2563eb"));
        changes.add(change("CHAT_LIST_BADGES", "TEXT_COLOR", "#ffffff"));
        changes.add(change("CHAT_LIST_GROUP_PILL", "BACKGROUND_COLOR", "#e5e7eb"));
        changes.add(change("CHAT_LIST_GROUP_PILL", "TEXT_COLOR", "#111827"));
        changes.add(change("CHAT_LIST_ITEM_ACTIONS", "ICON_COLOR", "#2563eb"));
        changes.add(change("CHAT_LIST_PIN_TOGGLE", "ICON_COLOR", "#111827"));
        changes.add(change("CHAT_LIST_PIN_MENU", "BACKGROUND_COLOR", "#ffffff"));
        changes.add(change("CHAT_LIST_PIN_MENU", "TEXT_COLOR", "#111827"));
        changes.add(change("CHAT_LIST_PIN_MENU", "BORDER_COLOR", "#e5e7eb"));
        changes.add(change("CHAT_LIST_PIN_MENU_ITEM", "BACKGROUND_COLOR", "#ffffff"));
        changes.add(change("CHAT_LIST_PIN_MENU_ITEM", "TEXT_COLOR", "#111827"));
        changes.add(change("CHAT_LIST_PIN_MENU_ITEM", "HOVER_BACKGROUND_COLOR", "#f1f5f9"));
        changes.add(change("CHAT_LIST_PIN_MENU_REPORT", "TEXT_COLOR", "#2563eb"));
        changes.add(change("CHAT_LIST_PIN_MENU_DANGER", "TEXT_COLOR", "#dc2626"));
        return changes;
    }

    private boolean isRemoveBorderRequest(String consulta) {
        String normalized = normalizeSemanticText(consulta);
        return containsAny(normalized,
                "quitar borde", "sin borde", "elimina el borde", "quita el contorno", "sin contorno");
    }

    private String resolveReportedAreaFromConsulta(String consulta, String area) {
        String normalized = normalizeSemanticText(consulta);
        if (containsAny(normalized,
                "denunciados", "reportados", "etiqueta reportado", "items denunciados", "chat denunciado")) {
            return "CHAT_LIST_STATUS_PILLS";
        }
        return area;
    }

    private String resolveFilterButtonsAreaFromConsulta(String consulta, String currentArea) {
        String normalized = normalizeSemanticText(consulta);
        boolean mentionsFilters = containsAny(normalized,
                "filtro", "filtros", "boton de filtro", "botones de filtros", "filtros de chats", "filtro de chats");
        if (!mentionsFilters) {
            return currentArea;
        }
        boolean mentionsActiveState = containsAny(normalized,
                "activo", "active", "seleccionado", "marcado");
        if (mentionsActiveState) {
            return "CHAT_LIST_FILTER_BUTTONS_ACTIVE";
        }
        return currentArea;
    }

    private String resolveFiltersContainerAreaFromConsulta(String consulta, String currentArea) {
        String normalized = normalizeSemanticText(consulta);
        if (containsAny(normalized,
                "zona de filtros", "contenedor de filtros", "filtros de chats", "filtrado de chats", "fondo de filtros")) {
            boolean mentionsActiveState = containsAny(normalized, "activo", "active", "seleccionado", "marcado");
            boolean mentionsButtons = containsAny(normalized, "boton", "botones");
            if (!mentionsActiveState && !mentionsButtons) {
                return "CHAT_LIST_FILTERS";
            }
        }
        return currentArea;
    }

    private String resolveChatStateAreaFromConsulta(String consulta, String currentArea) {
        String normalized = normalizeSemanticText(consulta);
        if (containsAny(normalized, "chat seleccionado", "chat activo", "item seleccionado")) {
            return "CHAT_LIST_ITEM_ACTIVE";
        }
        if (containsAny(normalized, "chat no leido", "no leidos completos", "fila sin leer")) {
            return "CHAT_LIST_ITEM_UNREAD";
        }
        return currentArea;
    }

    private String resolvePinMenuAreaFromConsulta(String consulta, String currentArea) {
        String normalized = normalizeSemanticText(consulta);
        if (containsAny(normalized,
                "icono del desplegable", "icono de opciones", "boton que abre el menu",
                "boton que abre el menu del chat", "icono de los tres puntos", "tres puntos del chat", "flecha del desplegable")) {
            return "CHAT_LIST_PIN_TOGGLE";
        }
        if (isPinMenuWholeMenuRequest(consulta)) {
            if (containsAny(normalized, "opcion de eliminar", "opciones de eliminar", "danger")) {
                return "CHAT_LIST_PIN_MENU_DANGER";
            }
            if (containsAny(normalized, "opcion de denunciar", "opciones de denunciar", "report")) {
                return "CHAT_LIST_PIN_MENU_REPORT";
            }
            if (containsAny(normalized, "iconos del menu", "icono del menu", "texto del menu", "botones del menu")) {
                return "CHAT_LIST_PIN_MENU_ITEM";
            }
            return "CHAT_LIST_PIN_MENU";
        }
        if (containsAny(normalized,
                "icono del desplegable abierto", "icono activo del desplegable", "cuando esta abierto ponlo")) {
            return "CHAT_LIST_PIN_TOGGLE";
        }
        if (containsAny(normalized,
                "icono de opciones del chat", "boton de opciones del chat")) {
            return "CHAT_LIST_PIN_TOGGLE";
        }
        if (containsAny(normalized, "opciones de eliminar", "opcion de eliminar", "danger")) {
            return "CHAT_LIST_PIN_MENU_DANGER";
        }
        if (containsAny(normalized, "opcion de denunciar", "opciones de denunciar", "report")) {
            return "CHAT_LIST_PIN_MENU_REPORT";
        }
        return currentArea;
    }

    private String inferPinMenuProperty(String consulta, String currentProperty) {
        String normalized = normalizeSemanticText(consulta);
        if (containsAny(normalized, "icono activo", "desplegable abierto")) {
            return "ACTIVE_ICON_COLOR";
        }
        if (containsAny(normalized, "fondo", "background")) {
            if ("CHAT_LIST_PIN_TOGGLE".equals(resolvePinMenuAreaFromConsulta(consulta, "")) && containsAny(normalized, "abierto", "activo")) {
                return "ACTIVE_BACKGROUND_COLOR";
            }
            return "BACKGROUND_COLOR";
        }
        if (containsAny(normalized, "icono", "iconos", "tres puntos")) {
            return "ICON_COLOR";
        }
        if (containsAny(normalized, "texto")) {
            return "TEXT_COLOR";
        }
        if (containsAny(normalized, "hover")) {
            return "HOVER_BACKGROUND_COLOR";
        }
        return currentProperty;
    }

    private boolean isPinMenuWholeMenuRequest(String consulta) {
        String normalized = normalizeSemanticText(consulta);
        boolean mentionsMenu = containsAny(normalized,
                "desplegable", "menu", "menú", "opciones", "contenido del desplegable");
        boolean mentionsChatScope = containsAny(normalized,
                "del chat", "de chat", "item de chat", "listado de chats", "lista de chats", "del listado de chats");
        boolean mentionsToggleOnly = containsAny(normalized,
                "icono", "boton", "botón", "abre", "abrir", "tres puntos", "flecha");
        return mentionsMenu && mentionsChatScope && !mentionsToggleOnly;
    }

    private boolean isWholeBlockStyleRequest(String consulta) {
        String normalized = normalizeSemanticText(consulta);
        return containsAny(normalized,
                "y el resto", "todo lo demas", "que combine", "sus estilos tambien",
                "todo el desplegable", "todo el bloque");
    }

    private boolean isGroupPillRequest(String consulta) {
        String normalized = normalizeSemanticText(consulta);
        if (containsAny(normalized, "chats grupales", "grupos del listado", "conversaciones grupales", "chat grupal completo")) {
            return false;
        }
        return containsAny(normalized,
                "etiqueta de grupo", "pill de grupo", "la etiqueta que pone grupo", "etiqueta que pone grupo");
    }

    private boolean isUnreadBadgeRequest(String consulta) {
        String normalized = normalizeSemanticText(consulta);
        if (containsAny(normalized, "chat no leido completo", "chats no leidos en", "chat no leido en")) {
            return false;
        }
        return containsAny(normalized,
                "contador de mensajes sin leer", "numero de mensajes sin leer", "badge de no leidos",
                "contador no leido");
    }

    private boolean isGroupBadgesScopedRequest(String consulta) {
        String normalized = normalizeSemanticText(consulta);
        return isGroupListRequest(consulta)
                && containsAny(normalized,
                "badges", "badge", "contador", "mensajes sin leer", "no leidos", "no leídos");
    }

    private boolean isIndividualPreviewTextRequest(String consulta) {
        String normalized = normalizeSemanticText(consulta);
        return isIndividualListRequest(consulta)
                && containsAny(normalized,
                "ultimo texto", "ultimo mensaje", "preview", "vista previa");
    }

    private boolean isIndividualFilePreviewRequest(String consulta) {
        String normalized = normalizeSemanticText(consulta);
        return isIndividualListRequest(consulta)
                && containsAny(normalized,
                "previews de archivos", "preview de archivos", "preview de archivo", "file preview", "archivos", "archivo");
    }

    private boolean isIndividualAudioPreviewScopedRequest(String consulta) {
        String normalized = normalizeSemanticText(consulta);
        return isIndividualListRequest(consulta)
                && containsAny(normalized,
                "preview de audio", "previews de audio", "audio preview", "audios", "audio", "nota de voz");
    }

    private boolean isGroupAudioPreviewScopedRequest(String consulta) {
        String normalized = normalizeSemanticText(consulta);
        return isGroupListRequest(consulta)
                && containsAny(normalized,
                "preview de audio", "previews de audio", "audio preview", "audios", "audio", "nota de voz");
    }

    private boolean isBothIndividualAndGroupScopeRequest(String consulta) {
        String normalized = normalizeSemanticText(consulta);
        return containsAny(normalized,
                "cada chat grupal y cada chat individual",
                "cada chat individual y cada chat grupal",
                "chat grupal y chat individual",
                "chat individual y chat grupal")
                && containsAny(normalized, "fondo", "background", "color de fondo", "con un color");
    }

    private boolean isPreviewTextSizeRequest(String consulta, String area) {
        String normalized = normalizeSemanticText(consulta);
        if ("CHAT_LIST_PREVIEW".equals(area)) {
            return true;
        }
        return containsAny(normalized,
                "texto del ultimo mensaje", "ultimo mensaje", "preview", "vista previa",
                "haz mas pequeno el texto del ultimo mensaje", "haz mas grande el preview",
                "reduce la letra del ultimo mensaje");
    }

    private boolean looksLikeFontSizeValue(String value) {
        if (!hasText(value)) {
            return false;
        }
        return Set.of("12px", "13px", "14px", "16px", "18px", "20px").contains(value.trim().toLowerCase(Locale.ROOT));
    }

    private String inferFontSizePreset(String value) {
        String normalized = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "12px" -> "SMALL";
            case "13px" -> "SMALL";
            case "14px" -> "NORMAL";
            case "16px" -> "LARGE";
            case "18px" -> "XL";
            case "20px" -> "XL";
            default -> null;
        };
    }

    private boolean isFontSizeIntent(String consulta, String label) {
        String combined = (consulta == null ? "" : consulta) + " " + (label == null ? "" : label);
        String normalized = normalizeSemanticText(combined);
        return containsAny(normalized,
                "tamano", "letra", "fuente", "texto mas grande", "texto mas pequeno",
                "aumentar texto", "reducir texto", "agrandar texto", "encoger texto");
    }

    private boolean isSpecificListProperty(String property) {
        if (!hasText(property)) {
            return false;
        }
        return Set.of(
                "HOVER_BACKGROUND_COLOR",
                "TEXT_COLOR",
                "BORDER_COLOR",
                "ICON_COLOR",
                "BADGE_COLOR",
                "PREVIEW_SENDER_TEXT_COLOR",
                "LABEL_COLOR",
                "TIME_COLOR",
                "SEPARATOR_COLOR",
                "ACTIVE_BACKGROUND_COLOR",
                "ACTIVE_TEXT_COLOR"
        ).contains(property);
    }

    private boolean isScopedChatItemChildArea(String area) {
        if (!hasText(area)) {
            return false;
        }
        return Set.of(
                "CHAT_LIST_ITEM_PREVIEW",
                "CHAT_LIST_ITEM_DRAFT_PREVIEW",
                "CHAT_LIST_ITEM_AUDIO_PREVIEW",
                "CHAT_LIST_ITEM_IMAGE_PREVIEW",
                "CHAT_LIST_ITEM_FILE_PREVIEW",
                "CHAT_LIST_ITEM_BADGES",
                "CHAT_LIST_ITEM_ACTIONS_SCOPED",
                "CHAT_LIST_ITEM_STATUS_PILLS",
                "CHAT_LIST_ITEM_NAME_SCOPED",
                "CHAT_LIST_ITEM_GROUP_PREVIEW",
                "CHAT_LIST_ITEM_GROUP_DRAFT_PREVIEW",
                "CHAT_LIST_ITEM_GROUP_AUDIO_PREVIEW",
                "CHAT_LIST_ITEM_GROUP_IMAGE_PREVIEW",
                "CHAT_LIST_ITEM_GROUP_FILE_PREVIEW",
                "CHAT_LIST_ITEM_GROUP_BADGES",
                "CHAT_LIST_ITEM_GROUP_ACTIONS",
                "CHAT_LIST_ITEM_GROUP_STATUS_PILLS",
                "CHAT_LIST_ITEM_GROUP_NAME"
        ).contains(area);
    }

    private boolean isGroupListRequest(String consulta) {
        String normalized = normalizeSemanticText(consulta);
        return containsAny(normalized,
                "chat grupal", "chats grupales", "grupos del listado", "conversaciones grupales",
                "grupos de la lista");
    }

    private boolean isStrictGroupOnlyScope(String consulta, String area) {
        if ("CHAT_LIST_ITEM_GROUP".equals(area) && !isIndividualListRequest(consulta)) {
            return true;
        }
        String normalized = normalizeSemanticText(consulta);
        return !isIndividualListRequest(consulta) && containsAny(normalized,
                "solo chats grupales",
                "solo grupos",
                "chats grupales completos",
                "chat grupal completo",
                "dentro de los chats grupales",
                "dentro de chats grupales",
                "de los chats grupales",
                "de chats grupales",
                "en chats grupales",
                "chat grupal",
                "chats grupales");
    }

    private boolean isStrictIndividualScope(String consulta, String area) {
        if ("CHAT_LIST_ITEM".equals(area) && !isStrictGroupOnlyScope(consulta, area) && !isGroupListRequest(consulta)) {
            return true;
        }
        String normalized = normalizeSemanticText(consulta);
        return !isGroupListRequest(consulta) && containsAny(normalized,
                "chats individuales",
                "chat individual",
                "de los chats individuales",
                "de chats individuales",
                "dentro de chats individuales",
                "en chats individuales");
    }

    private boolean enforceContrastForReadableText(List<UiCustomizationChangeDTO> changes) {
        if (changes == null || changes.isEmpty()) {
            return false;
        }
        Map<String, String> backgrounds = new HashMap<>();
        boolean adjusted = false;
        for (UiCustomizationChangeDTO change : changes) {
            if (change != null && "BACKGROUND_COLOR".equals(change.getProperty()) && hasText(change.getValue())) {
                backgrounds.put(change.getArea(), change.getValue());
            }
        }
        for (UiCustomizationChangeDTO change : changes) {
            if (change == null || !"TEXT_COLOR".equals(change.getProperty())) {
                continue;
            }
            String bg = backgrounds.get(change.getArea());
            if (!hasText(bg) || !hasText(change.getValue())) {
                continue;
            }
            String bgResolved = resolveColor(bg);
            String txtResolved = resolveColor(change.getValue());
            if (!hasText(bgResolved) || !hasText(txtResolved) || "transparent".equals(bgResolved)) {
                continue;
            }
            boolean bgDark = isDarkColor(expandHex(bgResolved));
            boolean txtDark = isDarkColor(expandHex(txtResolved));
            if (bgDark == txtDark) {
                change.setValue(bgDark ? "#ffffff" : "#111827");
                adjusted = true;
            }
        }
        return adjusted;
    }

    private String expandHex(String color) {
        if (color == null) return null;
        String c = color.toLowerCase(Locale.ROOT);
        if (c.matches("^#[0-9a-f]{3}$")) {
            return "#" + c.charAt(1) + c.charAt(1) + c.charAt(2) + c.charAt(2) + c.charAt(3) + c.charAt(3);
        }
        return c;
    }

    private boolean isIndividualListRequest(String consulta) {
        String normalized = normalizeSemanticText(consulta);
        return containsAny(normalized,
                "chat individual", "chats individuales", "chats privados", "conversaciones privadas",
                "chats uno a uno");
    }

    private String resolveGroupAreaFromConsulta(String consulta, String area) {
        if (isGroupListRequest(consulta)) {
            return "CHAT_LIST_ITEM_GROUP";
        }
        if (isIndividualListRequest(consulta)) {
            return "CHAT_LIST_ITEM";
        }
        return hasText(area) ? area : "CHAT_LIST_ITEM";
    }

    private NormalizedInput normalizeGlobalInput(String requestId,
                                                 String consulta,
                                                 String area,
                                                 String property,
                                                 String value,
                                                 String valuePreset) {
        String p = property;
        String v = value;
        String preset = valuePreset;
        String reason = null;
        String maxAllowed = null;
        String minAllowed = null;

        if ("TEXT_COLOR".equals(p) && isPxValue(v) && isFontSizeIntent(consulta, null)) {
            p = "FONT_SIZE";
            if (!hasText(preset)) {
                preset = inferFontSizePreset(v);
            }
            reason = "TEXT_COLOR_TO_FONT_SIZE";
        }
        if ("BORDER_COLOR".equals(p) && isPxValue(v)) {
            p = "BORDER_WIDTH";
            reason = "BORDER_COLOR_TO_BORDER_WIDTH";
        }
        if ("BORDER_WIDTH".equals(p) && isPxValue(v)) {
            int requested = parseFontPx(v);
            if (requested > 4) {
                v = "4px";
                preset = "MAX";
                reason = "BORDER_WIDTH_MAX_CLAMP";
                maxAllowed = "4px";
            } else if (requested < 0) {
                v = "0px";
                preset = "MIN";
                reason = "BORDER_WIDTH_MIN_CLAMP";
                minAllowed = "0px";
            }
        }
        if ("FONT_SIZE".equals(p) && isPxValue(v)) {
            int requested = parseFontPx(v);
            int clamped = clampFontSize(requested);
            String applied = clamped + "px";
            if (!applied.equals(v.trim().toLowerCase(Locale.ROOT))) {
                v = applied;
                preset = clamped == 32 ? "MAX" : clamped == 10 ? "MIN" : inferFontSizePreset(v);
                reason = clamped == 32 ? "FONT_SIZE_MAX_CLAMP" : "FONT_SIZE_NORMALIZE";
                if (clamped == 32) maxAllowed = "32px";
                if (clamped == 10) minAllowed = "10px";
                LOGGER.info("[AI][UI_VALUE_CLAMP] requestId={} property=FONT_SIZE requested={} applied={}", requestId, safe(value), v);
            }
        }
        if ("BORDER_RADIUS".equals(p) && isPxValue(v)) {
            int requested = parseFontPx(v);
            if (requested < 0) {
                v = "0px";
                reason = "BORDER_RADIUS_MIN_CLAMP";
            } else if (requested > 999) {
                v = "999px";
                preset = "PILL";
                reason = "BORDER_RADIUS_MAX_CLAMP";
            } else {
                int[] allowed = new int[]{0, 4, 8, 12, 16, 18, 24, 32, 999};
                int nearest = allowed[0];
                int delta = Integer.MAX_VALUE;
                for (int a : allowed) {
                    int d = Math.abs(a - requested);
                    if (d < delta) {
                        delta = d;
                        nearest = a;
                    }
                }
                String applied = nearest + "px";
                if (!applied.equals(v.trim().toLowerCase(Locale.ROOT))) {
                    v = applied;
                    reason = "BORDER_RADIUS_NORMALIZE";
                }
            }
        }
        if (("BACKGROUND_COLOR".equals(p) || "TEXT_COLOR".equals(p) || "BORDER_COLOR".equals(p))
                && hasText(v)
                && containsAny(normalizeSemanticText(v), "sin color", "transparente", "transparent")) {
            v = "transparent";
            reason = "COLOR_TO_TRANSPARENT";
        }

        if (reason != null) {
            LOGGER.info("[AI][UI_VALUE_NORMALIZE] requestId={} area={} property={} requested={} applied={} reason={}",
                    requestId, area, property, safe(value), safe(v), reason);
        }
        return new NormalizedInput(p, v, preset, reason, maxAllowed, minAllowed);
    }

    private void applyNormalizationMetadata(AiUiCustomizationResponseDTO response,
                                            NormalizedInput normalizedInput,
                                            String requestedProperty,
                                            String requestedValue,
                                            String appliedValue) {
        boolean normalized = normalizedInput != null && normalizedInput.reason() != null;
        response.setNormalized(normalized);
        if (!normalized) {
            return;
        }
        response.setNormalizationReason(normalizedInput.reason());
        response.setRequestedValue(requestedValue);
        response.setAppliedValue(appliedValue);
        response.setMaxAllowedValue(normalizedInput.maxAllowedValue());
        response.setMinAllowedValue(normalizedInput.minAllowedValue());
        if ("FONT_SIZE_MAX_CLAMP".equals(normalizedInput.reason())) {
            response.setValuePreset("MAX");
        }
        if ("FONT_SIZE_MIN_CLAMP".equals(normalizedInput.reason())) {
            response.setValuePreset("MIN");
        }
    }

    private double normalizeUiConfidence(String requestId,
                                         String consulta,
                                         String action,
                                         String area,
                                         String property,
                                         String value,
                                         Double rawConfidence) {
        double raw = rawConfidence == null ? 0.0d : rawConfidence;
        boolean structureValid = "UI_CUSTOMIZATION".equals("UI_CUSTOMIZATION")
                && hasText(action)
                && ALLOWED_ACTIONS.contains(action)
                && hasText(area)
                && ALLOWED_AREAS.contains(area)
                && hasText(property)
                && ALLOWED_PROPERTIES.contains(property)
                && hasText(resolveValue(property, value, null));
        String normalized = normalizeSemanticText(consulta);
        boolean clearStyleRequest = containsAny(normalized,
                "desplegable", "menu", "opciones", "estilos", "color",
                "purpura", "morado", "rojo", "negro", "verde", "azul");
        double effective = raw;
        if (structureValid && clearStyleRequest && raw >= 0.70d && raw < MIN_CONFIDENCE) {
            effective = 0.75d;
            LOGGER.info("[AI][UI_CONFIDENCE_NORMALIZED] requestId={} raw={} effective={} reason=STRUCTURE_VALID_AND_CLEAR_STYLE_REQUEST",
                    requestId, raw, effective);
        }
        return effective;
    }

    private boolean containsDangerousCss(String value) {
        String normalized = value == null ? "" : value.toLowerCase(Locale.ROOT);
        return normalized.contains("url(")
                || normalized.contains("expression(")
                || normalized.contains("var(")
                || normalized.contains("calc(")
                || normalized.contains("javascript:")
                || normalized.contains("data:")
                || normalized.contains("linear-gradient(")
                || normalized.contains("{")
                || normalized.contains("}")
                || normalized.contains(";");
    }

    private String resolveRequestedColorFamily(String consulta, ColorIntentDTO colorIntent) {
        String family = colorIntent == null ? null : normalizeSemanticText(colorIntent.getFamily());
        String requestedText = colorIntent == null ? null : normalizeSemanticText(colorIntent.getRequestedText());
        String normalized = normalizeSemanticText((consulta == null ? "" : consulta) + " " + (requestedText == null ? "" : requestedText));
        if (containsAny(normalized, "purpura", "morado", "violeta")) return "PURPLE";
        if (containsAny(normalized, "azul")) return "BLUE";
        if (containsAny(normalized, "rojo")) return "RED";
        if (containsAny(normalized, "verde")) return "GREEN";
        if (containsAny(normalized, "rosa")) return "PINK";
        if (containsAny(normalized, "blanco")) return "WHITE";
        if (containsAny(normalized, "negro")) return "BLACK";
        if (hasText(family)) return family.toUpperCase(Locale.ROOT);
        return null;
    }

    private UiCustomizationChangeDTO applyColorIntentToChange(UiCustomizationChangeDTO source, String family) {
        if (source == null) return null;
        UiCustomizationChangeDTO copy = new UiCustomizationChangeDTO();
        copy.setArea(source.getArea());
        copy.setProperty(source.getProperty());
        copy.setValue(source.getValue());
        copy.setValuePreset(source.getValuePreset());
        copy.setValue(alignColorValueWithIntent(copy.getProperty(), copy.getValue(), family));
        return copy;
    }

    private String alignColorValueWithIntent(String property, String value, String family) {
        if (!hasText(family) || !COLOR_PROPERTIES.contains(property)) {
            return value;
        }
        String resolved = resolveColor(value);
        if (!hasText(resolved) || "transparent".equals(resolved)) {
            return value;
        }
        String canonical = switch (family) {
            case "PURPLE" -> "#7c3aed";
            case "BLUE" -> "#2563eb";
            case "RED" -> "#ef4444";
            case "GREEN" -> "#16a34a";
            case "PINK" -> "#ec4899";
            case "WHITE" -> "#ffffff";
            case "BLACK" -> "#111827";
            default -> null;
        };
        if (!hasText(canonical)) {
            return value;
        }
        if (isSimilarColorFamily(resolved, canonical)) {
            return value;
        }
        return canonical;
    }

    private boolean isSimilarColorFamily(String one, String two) {
        String a = expandHex(one);
        String b = expandHex(two);
        if (!hasText(a) || !hasText(b) || !a.matches("^#[0-9a-f]{6}$") || !b.matches("^#[0-9a-f]{6}$")) {
            return false;
        }
        int ar = Integer.parseInt(a.substring(1, 3), 16);
        int ag = Integer.parseInt(a.substring(3, 5), 16);
        int ab = Integer.parseInt(a.substring(5, 7), 16);
        int br = Integer.parseInt(b.substring(1, 3), 16);
        int bg = Integer.parseInt(b.substring(3, 5), 16);
        int bb = Integer.parseInt(b.substring(5, 7), 16);
        int da = Math.max(ar, Math.max(ag, ab));
        int db = Math.max(br, Math.max(bg, bb));
        if (da == ar && db == br) return true;
        if (da == ag && db == bg) return true;
        if (da == ab && db == bb) return true;
        return Math.abs(ar - br) + Math.abs(ag - bg) + Math.abs(ab - bb) < 90;
    }

    private boolean isPropertyAllowedForArea(String area, String property) {
        if (!hasText(area) || !hasText(property)) {
            return false;
        }
        if ("CHAT_LIST_ITEM_GROUP_PREVIEW".equals(area)) {
            return Set.of("TEXT_COLOR", "PREVIEW_SENDER_TEXT_COLOR", "FONT_SIZE").contains(property);
        }
        if ("CHAT_LIST_ITEM_GROUP_AUDIO_PREVIEW".equals(area)) {
            return Set.of("BACKGROUND_COLOR", "TEXT_COLOR", "ICON_COLOR", "BORDER_COLOR", "LABEL_COLOR", "SEPARATOR_COLOR", "TIME_COLOR").contains(property);
        }
        if ("CHAT_LIST_ITEM_GROUP_FILE_PREVIEW".equals(area)) {
            return Set.of("BACKGROUND_COLOR", "TEXT_COLOR", "ICON_COLOR", "BORDER_COLOR").contains(property);
        }
        if ("CHAT_LIST_ITEM_GROUP_IMAGE_PREVIEW".equals(area)) {
            return Set.of("BACKGROUND_COLOR", "TEXT_COLOR", "ICON_COLOR", "BORDER_COLOR").contains(property);
        }
        if ("CHAT_LIST_ITEM_GROUP_DRAFT_PREVIEW".equals(area)) {
            return Set.of("TEXT_COLOR", "LABEL_COLOR").contains(property);
        }
        if ("CHAT_LIST_ITEM_GROUP_BADGES".equals(area)) {
            return Set.of("BACKGROUND_COLOR", "TEXT_COLOR", "BORDER_COLOR", "BADGE_COLOR").contains(property);
        }
        if ("CHAT_LIST_ITEM_GROUP_ACTIONS".equals(area)) {
            return Set.of("ICON_COLOR", "HOVER_BACKGROUND_COLOR", "TEXT_COLOR").contains(property);
        }
        if ("CHAT_LIST_ITEM_GROUP_STATUS_PILLS".equals(area)) {
            return Set.of("REPORTED_BACKGROUND_COLOR", "REPORTED_TEXT_COLOR", "BLOCKED_BACKGROUND_COLOR", "BLOCKED_TEXT_COLOR", "BORDER_COLOR").contains(property);
        }
        return true;
    }

    private record AreaProperty(String area, String property) {}

    private record NormalizedInput(String property, String value, String valuePreset, String reason, String maxAllowedValue, String minAllowedValue) {}

    private String safe(String value) {
        return value == null ? "" : value.replaceAll("\\s+", " ").trim();
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
