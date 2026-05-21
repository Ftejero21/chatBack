package com.chat.chat.Service.AiService;

import com.chat.chat.DTO.AiUiCustomizationResponseDTO;
import com.chat.chat.DTO.ColorIntentDTO;
import com.chat.chat.DTO.UiCustomizationScopeDTO;
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
    private static final String DEFAULT_CLARIFICATION_MESSAGE = "\u00BFPuedes especificar un poco m\u00E1s el cambio visual?";
    private static final String COLOR_VALUE_MISSING_MESSAGE = "\u00BFQu\u00E9 color quieres aplicar?";
    private static final String SIZE_VALUE_MISSING_MESSAGE = "\u00BFQu\u00E9 tama\u00F1o quieres aplicar?";
    private static final String AMBIGUOUS_DROPDOWN_CLARIFICATION_MESSAGE = "\u00BFQu\u00E9 desplegable quieres cambiar: el de opciones del chat, mensajes, perfil u otra zona?";
    private static final String AMBIGUOUS_ICON_CLARIFICATION_MESSAGE = "\u00BFQu\u00E9 iconos quieres cambiar: los de la barra lateral, listado de chats, mensajes u otra zona?";
    private static final String AMBIGUOUS_BORDER_CLARIFICATION_MESSAGE = "\u00BFQu\u00E9 parte del borde quieres cambiar: color, grosor o redondeo?";
    private static final String AMBIGUOUS_AREA_CLARIFICATION_MESSAGE = "\u00BFQu\u00E9 zona quieres cambiar exactamente?";

    private static final Set<String> ALLOWED_AREAS = Set.of(
            "MAIN_LAYOUT",
            "SIDEBAR_NAV",
            "SIDEBAR_NAV_PANEL",
            "SIDEBAR_NAV_GROUP",
            "SIDEBAR_NAV_BOTTOM",
            "SIDEBAR_NAV_ITEM",
            "SIDEBAR_NAV_ITEM_ACTIVE",
            "SIDEBAR_NAV_ACTIVE_ITEM",
            "SIDEBAR_NAV_ACTIVE_INDICATOR",
            "SIDEBAR_NAV_LOGO",
            "SIDEBAR_NAV_ICON",
            "SIDEBAR_NAV_ICON_ACTIVE",
            "SIDEBAR_NAV_AI_ICON",
            "SIDEBAR_NAV_TOOLTIP",
            "SIDEBAR_NAV_AVATAR",
            "SIDEBAR_NAV_NOTIF_BADGE",
            "SIDEBAR_NAV_SETTINGS",
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
            "CHAT_LIST_HEADER_ICON_BUTTON",
            "CHAT_LIST_HEADER_ICON",
            "CHAT_LIST_HEADER_MENU",
            "CHAT_LIST_HEADER_MENU_ITEM",
            "CHAT_LIST_SCROLL",
            "CHAT_LIST_AVATAR",
            "CHAT_LIST_STATUS_DOT",
            "CHAT_LIST_ITEM_CONTENT",
            "CHAT_LIST_ITEM_NAME",
            "CHAT_LIST_NAME",
            "CHAT_LIST_ITEM_DATE",
            "CHAT_LIST_IMAGE_PREVIEW",
            "CHAT_LIST_FILE_PREVIEW",
            "CHAT_LIST_STICKER_PREVIEW",
            "CHAT_LIST_ITEM_STICKER_PREVIEW",
            "CHAT_LIST_ITEM_GROUP_STICKER_PREVIEW",
            "CHAT_LIST_MUTED_INDICATOR",
            "CHAT_LIST_FAVORITE_INDICATOR",
            "CHAT_LIST_CLOSED_INDICATOR",
            "CHAT_LIST_EMPTY_STATE",
            "CHAT_LIST_PUBLIC_PANEL",
            "CHAT_LIST_PUBLIC_CARD",
            "CHAT_LIST_ACTIONS_MENU",
            "CHAT_LIST_ACTIONS_MENU_ITEM",
            "CHAT_LIST_PIN_TOGGLE",
            "CHAT_LIST_PIN_MENU",
            "CHAT_LIST_PIN_MENU_ITEM",
            "CHAT_LIST_PIN_MENU_DANGER",
            "CHAT_LIST_PIN_MENU_REPORT",
            "CHAT_LIST_ITEM_ACTIVE",
            "CHAT_LIST_ITEM_GROUP_ACTIVE",
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
            "WIDTH",
            "HEIGHT",
            "GAP",
            "BORDER_RADIUS",
            "FONT_SIZE",
            "FONT_WEIGHT",
            "COLOR",
            "ICON_COLOR",
            "ACTIVE_BACKGROUND_COLOR",
            "ACTIVE_TEXT_COLOR",
            "ACTIVE_ICON_COLOR",
            "HOVER_BACKGROUND_COLOR",
            "HOVER_TEXT_COLOR",
            "HOVER_ICON_COLOR",
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
            "SHADOW",
            "SHADOW_PRESET",
            "DENSITY",
            "OPACITY",
            "BLUR",
            "BACKGROUND_IMAGE"
    );
    private static final Set<String> ALLOWED_ACTIONS = Set.of("UPDATE_STYLE", "UPDATE_STYLE_GROUP", "UPDATE_STYLE_MULTI", "RESET_THEME");
    private static final int MAX_EXPLICIT_MULTI_CHANGES = 300;
    private static final int MAX_FINAL_MULTI_CHANGES = 300;
    private static final int MAX_THEME_MULTI_CHANGES = 300;

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
            "HOVER_TEXT_COLOR",
            "HOVER_ICON_COLOR",
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
    private static final Set<String> SIZE_PROPERTIES = Set.of(
            "FONT_SIZE",
            "BORDER_WIDTH",
            "BORDER_RADIUS",
            "WIDTH",
            "HEIGHT",
            "GAP"
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
    private static final Set<String> WIDTH_VALUES = Set.of("1px", "2px", "3px", "4px", "6px", "8px");
    private static final Set<String> HEIGHT_VALUES = Set.of("4px", "6px", "8px", "10px", "12px", "14px", "16px", "18px", "20px", "24px");
    private static final Set<String> GAP_VALUES = Set.of("0px", "2px", "4px", "6px", "8px", "10px", "12px", "16px");
    private static final Set<String> FONT_WEIGHT_VALUES = Set.of("400", "500", "600", "700");
    private static final Set<Integer> SAFE_FONT_SIZES = Set.of(10, 11, 12, 13, 14, 16, 18, 20, 22, 24, 28, 32);
    private static final Set<String> SAFE_BORDER_RADIUS_VALUES = Set.of("0px", "4px", "8px", "12px", "16px", "18px", "24px", "32px", "999px");
    private static final Set<String> GROUP_ONLY_ALLOWED_AREAS = Set.of(
            "CHAT_LIST_ITEM_GROUP",
            "CHAT_LIST_ITEM_GROUP_ACTIVE",
            "CHAT_LIST_GROUP_PILL",
            "CHAT_LIST_ITEM_GROUP_PREVIEW",
            "CHAT_LIST_ITEM_GROUP_AUDIO_PREVIEW",
            "CHAT_LIST_ITEM_GROUP_DRAFT_PREVIEW",
            "CHAT_LIST_ITEM_GROUP_IMAGE_PREVIEW",
            "CHAT_LIST_ITEM_GROUP_FILE_PREVIEW",
            "CHAT_LIST_ITEM_GROUP_STICKER_PREVIEW",
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
            "CHAT_LIST_ITEM_STICKER_PREVIEW",
            "CHAT_LIST_ITEM_BADGES",
            "CHAT_LIST_ITEM_ACTIONS_SCOPED",
            "CHAT_LIST_ITEM_STATUS_PILLS",
            "CHAT_LIST_ITEM_NAME_SCOPED"
    );
    private static final Set<String> PARENT_AREAS_DEFAULT_HARMONY = Set.of(
            "CHAT_LIST_PANEL",
            "CHAT_LIST_HEADER",
            "CHAT_LIST_ITEM",
            "CHAT_LIST_ITEM_ACTIVE",
            "CHAT_LIST_ITEM_GROUP",
            "CHAT_LIST_ITEM_GROUP_ACTIVE",
            "CHAT_LIST_SEARCH",
            "CHAT_LIST_FILTERS",
            "CHAT_LIST_PIN_MENU",
            "SIDEBAR_NAV_PANEL",
            "SIDEBAR_NAV_ITEM",
            "SIDEBAR_NAV_ITEM_ACTIVE",
            "SIDEBAR_NAV_TOOLTIP",
            "CHAT_LIST_PUBLIC_PANEL",
            "CHAT_LIST_PUBLIC_CARD"
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
        return validate(requestId, consulta, action, area, property, value, valuePreset, label, confidence,
                colorIntent, null, null, null, null, inputChanges, uiContext);
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
                                                 UiCustomizationScopeDTO scope,
                                                 Boolean needsClarification,
                                                 String clarificationReason,
                                                 String clarificationQuestion,
                                                 List<UiCustomizationChangeDTO> inputChanges,
                                                 UiCustomizationContextDTO uiContext) {
        String semantic = semanticSource(consulta, label);
        logScopeResult(scope);
        String resolvedAreaFromScope = resolveAreaFromScope(scope);
        String normalizedAction = hasText(action) ? action : "UPDATE_STYLE";
        if (isGroupActiveChatRequest(semantic)) {
            List<UiCustomizationChangeDTO> repairedChanges = buildGroupActiveScopedChanges(semantic);
            LOGGER.info("[AI][UI_CUSTOMIZATION_GROUP_ACTIVE_CHANGES] changesCount={}", repairedChanges.size());
            if (repairedChanges.size() > 1) {
                normalizedAction = "UPDATE_STYLE_MULTI";
                inputChanges = repairedChanges;
                area = null;
                property = null;
                value = null;
                valuePreset = null;
                needsClarification = Boolean.FALSE;
            } else if (repairedChanges.size() == 1) {
                UiCustomizationChangeDTO single = repairedChanges.get(0);
                normalizedAction = "UPDATE_STYLE";
                area = single.getArea();
                property = single.getProperty();
                value = single.getValue();
                valuePreset = single.getValuePreset();
                inputChanges = null;
                needsClarification = Boolean.FALSE;
            } else {
                normalizedAction = "UPDATE_STYLE";
                area = "CHAT_LIST_ITEM_GROUP_ACTIVE";
                needsClarification = Boolean.FALSE;
            }
            LOGGER.info("[AI][UI_CUSTOMIZATION_CLARIFICATION_SKIPPED] reason=GROUP_ACTIVE_ITEM semantic={}", safe(semantic));
        }
        if (hasText(resolvedAreaFromScope)) {
            LOGGER.info("[AI][UI_SCOPE_AREA_RESOLVED] area={}", safe(resolvedAreaFromScope));
            if (!hasText(area)) {
                area = resolvedAreaFromScope;
            }
            inputChanges = applyResolvedAreaToChanges(inputChanges, resolvedAreaFromScope);
        }
        SidebarRepair sidebarRepair = repairSidebarNavIntent(semantic, normalizedAction, area, property, value, valuePreset, inputChanges, needsClarification);
        normalizedAction = sidebarRepair.action();
        area = sidebarRepair.area();
        property = sidebarRepair.property();
        value = sidebarRepair.value();
        valuePreset = sidebarRepair.valuePreset();
        inputChanges = sidebarRepair.changes();
        needsClarification = sidebarRepair.needsClarification();
        boolean aiAskedClarification = Boolean.TRUE.equals(needsClarification) || "NEEDS_CLARIFICATION".equals(normalizedAction);
        boolean hasResolvableArea = hasText(normalizeArea(area))
                || hasText(resolvedAreaFromScope)
                || hasValidChanges(inputChanges)
                || hasResolvableVisualPayload(property, value, inputChanges);
        LOGGER.info("[AI][UI_CLARIFICATION_AI_DECISION] needsClarification={} reason={} question={}",
                aiAskedClarification, safe(clarificationReason), safe(clarificationQuestion));
        LOGGER.info("[AI][UI_CLARIFICATION_AI_DECISION] reason={} question={}",
                safe(clarificationReason), safe(clarificationQuestion));
        if (aiAskedClarification && hasResolvableArea) {
            LOGGER.info("[AI][UI_CLARIFICATION_IGNORED] reason=RESOLVABLE_AREA_OR_CHANGES area={} changesCount={}",
                    safe(hasText(area) ? area : resolvedAreaFromScope),
                    inputChanges == null ? 0 : inputChanges.size());
            normalizedAction = resolveActionFromPayload(property, value, inputChanges);
            if (!hasText(area) && hasText(resolvedAreaFromScope)) {
                area = resolvedAreaFromScope;
            }
            needsClarification = Boolean.FALSE;
        } else if (aiAskedClarification) {
            LOGGER.info("[AI][UI_CLARIFICATION_ACCEPTED] reason=AI_REQUESTED_AND_UNRESOLVABLE");
            return failure(action, area, property, value, valuePreset, label, confidence,
                    "UI_CUSTOMIZATION_NEEDS_CLARIFICATION",
                    resolveClarificationMessage(clarificationQuestion, clarificationReason),
                    clarificationReason,
                    clarificationQuestion);
        }
        if (!ALLOWED_ACTIONS.contains(normalizedAction)) {
            return failure(action, area, property, value, valuePreset, label, confidence,
                    "UI_CUSTOMIZATION_NOT_ALLOWED", "La accion solicitada no esta permitida.");
        }
        action = normalizedAction;
        if ("UPDATE_STYLE".equals(action) && inputChanges != null && inputChanges.size() > 1) {
            action = "UPDATE_STYLE_MULTI";
        }
        if (isGroupActiveChatRequest(semantic)) {
            LOGGER.info("[AI][UI_CUSTOMIZATION_CLARIFICATION_SKIPPED] reason=GROUP_ACTIVE_ITEM consulta={}", safe(semantic));
            if (!hasText(area) || "CHAT_LIST_ITEM_ACTIVE".equals(area)) {
                LOGGER.info("[AI][UI_CUSTOMIZATION_AREA_REPAIRED] from={} to=CHAT_LIST_ITEM_GROUP_ACTIVE reason=GROUP_ACTIVE_SCOPE",
                        safe(area));
                area = "CHAT_LIST_ITEM_GROUP_ACTIVE";
            }
            if ("UPDATE_STYLE_MULTI".equals(action) && (inputChanges == null || inputChanges.isEmpty())) {
                List<UiCustomizationChangeDTO> repairedChanges = buildGroupActiveScopedChanges(semantic);
                LOGGER.info("[AI][UI_CUSTOMIZATION_GROUP_ACTIVE_CHANGES] changesCount={}", repairedChanges.size());
                if (!repairedChanges.isEmpty()) {
                    inputChanges = repairedChanges;
                    logRuleMatch("GROUP_ACTIVE_ITEM", semantic, repairedChanges);
                }
            }
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
        boolean scopedPinMenuRequest = isScopedPinMenuRequest(consulta) || isPinMenuArea(resolvedAreaFromScope);
        boolean fullChatListThemeRequest = scopedPinMenuRequest ? false : isFullChatListThemeRequest(consulta);
        if (fullChatListThemeRequest) {
            LOGGER.info("[AI][UI_CUSTOMIZATION_FULL_THEME] detected=true consulta={}", safe(consulta));
        }
        if (scopedPinMenuRequest) {
            LOGGER.info("[AI][UI_CUSTOMIZATION_FULL_THEME_SKIPPED] reason=SCOPED_PIN_MENU consulta={}", safe(consulta));
        }
        if (fullChatListThemeRequest && mentionsDropdownKeywords(consulta)) {
            LOGGER.info("[AI][UI_CUSTOMIZATION_CLARIFICATION_SKIPPED] reason=FULL_CHAT_LIST_THEME consulta={}", safe(consulta));
        }
        LOGGER.info("[AI][UI_CLARIFICATION_BACKEND_NOT_ALLOWED] reason=DETERMINISTIC_AMBIGUITY_DISABLED");

        if ("UPDATE_STYLE".equals(action) && !fullChatListThemeRequest && isGroupAudioPreviewScopedRequest(consulta)) {
            action = "UPDATE_STYLE_MULTI";
            inputChanges = buildGroupAudioPreviewScopedColorGroup(hasText(value) ? value : "#f97316");
            logRuleMatch("GROUP_AUDIO_PREVIEW_SCOPED", consulta, inputChanges);
            area = null;
            property = null;
            value = null;
        } else if ("UPDATE_STYLE".equals(action) && !fullChatListThemeRequest && isIndividualAudioPreviewScopedRequest(consulta)) {
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
            area = resolveChatStateAreaFromConsulta(semantic, area);
        }
        if ("UPDATE_STYLE".equals(action)) {
            area = resolveChatListSpecificAreaFromConsulta(consulta, area);
            property = resolveChatListSpecificPropertyFromConsulta(consulta, area, property);
        }
        if ("UPDATE_STYLE".equals(action)) {
            area = resolveSidebarAreaFromConsulta(consulta, area);
            property = resolveSidebarPropertyFromConsulta(consulta, area, property);
        }
        if ("UPDATE_STYLE".equals(action) && isSidebarRequest(normalizeSemanticText(consulta))) {
            List<UiCustomizationChangeDTO> sidebarChanges = parseDeterministicMultiChanges(consulta);
            if (sidebarChanges.size() > 1 && areAllSidebarChanges(sidebarChanges)) {
                action = "UPDATE_STYLE_MULTI";
                inputChanges = sidebarChanges;
                area = null;
                property = null;
                value = null;
            }
        }
        if ("UPDATE_STYLE".equals(action) && isGroupActiveChatRequest(semantic) && !hasText(property)) {
            String normalized = normalizeSemanticText(semantic);
            if (containsAny(normalized, "texto", "letra")) {
                property = "TEXT_COLOR";
            } else if (containsAny(normalized, "borde", "contorno", "border")) {
                property = containsAny(normalized, "sin borde", "quita el borde", "quitar borde", "elimina el borde", "sin contorno")
                        ? "BORDER_WIDTH" : "BORDER_COLOR";
            } else if (containsAny(normalized, "fondo", "background")) {
                property = "BACKGROUND_COLOR";
            }
        }
        if ("UPDATE_STYLE".equals(action) && isGroupActiveChatRequest(semantic) && !hasText(value) && hasText(property)) {
            if ("BORDER_WIDTH".equals(property) && isRemoveBorderRequest(consulta)) {
                value = "0px";
            } else {
                String inferredValue = resolveColorFromText(semantic);
                if (hasText(inferredValue)) {
                    value = inferredValue;
                }
            }
        }
        if ("UPDATE_STYLE".equals(action)) {
            area = resolvePinMenuAreaFromConsulta(consulta, area);
            property = inferPinMenuProperty(consulta, property);
        }
        if ("UPDATE_STYLE".equals(action)) {
            area = resolveChatItemDateAreaFromConsulta(consulta, area);
            if ("CHAT_LIST_ITEM_DATE".equals(area) && !hasText(property)) {
                property = "TEXT_COLOR";
            }
        }
        if (hasText(resolvedAreaFromScope) && !hasText(area)) {
            area = resolvedAreaFromScope;
        }
        if (!fullChatListThemeRequest && isGroupBadgesScopedRequest(consulta)) {
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
        } else if (fullChatListThemeRequest && isGroupBadgesScopedRequest(consulta)) {
            LOGGER.info("[AI][UI_CUSTOMIZATION_SCOPED_RULE_SKIPPED] rule=GROUP_BADGES_SCOPED reason=FULL_CHAT_LIST_THEME");
        }
        if (!fullChatListThemeRequest && isGroupAudioPreviewScopedRequest(consulta)) {
            String forcedBaseColor = resolveForcedBaseColor(consulta, value, inputChanges, "#f97316");
            action = "UPDATE_STYLE_MULTI";
            inputChanges = buildGroupAudioPreviewScopedColorGroup(forcedBaseColor);
            logRuleMatch("FORCE_GROUP_AUDIO_PREVIEW_SCOPED", consulta, inputChanges);
            area = null;
            property = null;
            value = null;
        } else if (fullChatListThemeRequest && isGroupAudioPreviewScopedRequest(consulta)) {
            LOGGER.info("[AI][UI_CUSTOMIZATION_SCOPED_RULE_SKIPPED] rule=GROUP_AUDIO_PREVIEW_SCOPED reason=FULL_CHAT_LIST_THEME");
        }
        if (!fullChatListThemeRequest && isControlledPinMenuRequest(consulta)) {
            String forcedBaseColor = resolveForcedBaseColor(consulta, value, inputChanges, "#7c3aed");
            List<UiCustomizationChangeDTO> beforeFilter = inputChanges == null ? List.of() : inputChanges;
            List<UiCustomizationChangeDTO> pinOnly = filterPinMenuChanges(inputChanges);
            LOGGER.info("[AI][UI_CUSTOMIZATION_PIN_MENU_FILTER] before={} after={}", beforeFilter.size(), pinOnly.size());
            inputChanges = sanitizePinMenuOnlyChanges(completePinMenuThemeChanges(forcedBaseColor, pinOnly));
            logRuleMatch("PIN_MENU_FULL_GROUP", consulta, inputChanges);
            action = "UPDATE_STYLE_MULTI";
            area = null;
            property = null;
            value = null;
        } else if (fullChatListThemeRequest && isControlledPinMenuRequest(consulta)) {
            LOGGER.info("[AI][UI_CUSTOMIZATION_SCOPED_RULE_SKIPPED] rule=PIN_MENU_FULL_GROUP reason=FULL_CHAT_LIST_THEME");
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
        if ("UPDATE_STYLE".equals(action) && !fullChatListThemeRequest && isGroupBadgesScopedRequest(consulta)) {
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
        if ("UPDATE_STYLE".equals(action) && !fullChatListThemeRequest && isIndividualFilePreviewRequest(consulta)) {
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
                && isControlledPinMenuRequest(consulta)) {
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
                && !fullChatListThemeRequest
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
        if ("UPDATE_STYLE".equals(action) && isFullSidebarThemeRequest(consulta)) {
            String sidebarBaseColor = hasText(value) ? normalizeColorText(value) : resolveFullSidebarThemeBaseColor(consulta);
            if (hasText(sidebarBaseColor)) {
                action = "UPDATE_STYLE_MULTI";
                inputChanges = buildSidebarNavThemeChanges(sidebarBaseColor);
                logRuleMatch("SIDEBAR_NAV_THEME", consulta, inputChanges);
                area = null;
                property = null;
                value = null;
            }
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
            String inferredColor = resolveFullChatListThemeBaseColor(consulta);
            if (hasText(inferredColor)) {
                action = "UPDATE_STYLE_GROUP";
                area = "CHAT_LIST_PANEL";
                property = "BACKGROUND_COLOR";
                value = inferredColor;
                logRuleMatch("CHAT_LIST_PANEL_FALLBACK", consulta, List.of(change("CHAT_LIST_PANEL", "BACKGROUND_COLOR", inferredColor)));
            }
        }

        if (needsClarificationByMissingData(area, property, value, inputChanges, action, resolvedAreaFromScope)) {
            String effectiveClarificationReason = inferClarificationReason(
                    clarificationReason,
                    area,
                    property,
                    value,
                    inputChanges,
                    action,
                    resolvedAreaFromScope
            );
            LOGGER.info("[AI][UI_CUSTOMIZATION_VALIDATE] requestId={} allowed=false reason=NEEDS_CLARIFICATION area={} property={} value={}",
                    requestId, area, property, safe(value));
            return failure(action, area, property, value, valuePreset, label, confidence,
                    "UI_CUSTOMIZATION_NEEDS_CLARIFICATION",
                    resolveClarificationMessage(clarificationQuestion, effectiveClarificationReason),
                    effectiveClarificationReason,
                    clarificationQuestion);
        }

        if (confidence == null || effectiveConfidence < 0.70d) {
            LOGGER.info("[AI][UI_CUSTOMIZATION_VALIDATE] requestId={} allowed=false reason=LOW_CONFIDENCE confidence={} area={} property={}",
                    requestId, effectiveConfidence, area, property);
            return failure(action, area, property, value, valuePreset, label, confidence,
                    "UI_CUSTOMIZATION_LOW_CONFIDENCE", "No estoy seguro del cambio visual solicitado.");
        }

        ActionNormalization actionNormalization = normalizeSingleAreaGroupAction(action, area, property, value, inputChanges);
        if (actionNormalization.normalized()) {
            action = actionNormalization.action();
            area = actionNormalization.area();
            property = actionNormalization.property();
            value = actionNormalization.value();
            inputChanges = actionNormalization.changes();
        }

        area = normalizeArea(area);
        property = normalizeProperty(property);

        if (!"UPDATE_STYLE_MULTI".equals(action) && (!hasText(area) || !ALLOWED_AREAS.contains(area))) {
            LOGGER.info("[AI][UI_CUSTOMIZATION_VALIDATE] requestId={} allowed=false reason=AREA_NOT_ALLOWED area={} property={} value={}",
                    requestId, area, property, safe(value));
            return failure(action, area, property, value, valuePreset, label, confidence,
                    "UI_CUSTOMIZATION_NOT_ALLOWED", "Esa zona visual no esta permitida.");
        }

        if ("UPDATE_STYLE_MULTI".equals(action)) {
            boolean aiProvidedChanges = inputChanges != null && !inputChanges.isEmpty();
            List<UiCustomizationChangeDTO> explicitChanges = extractExplicitChanges(consulta, label, area, property, value, valuePreset, inputChanges);
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
            if (explicitChanges.isEmpty()
                    && effectiveConfidence >= 0.70d
                    && isFullChatListThemeRequest(consulta)) {
                String inferredColor = resolveFullChatListThemeBaseColor(consulta);
                if (hasText(inferredColor)) {
                    explicitChanges.addAll(buildChatListPanelColorGroup(consulta, inferredColor));
                    LOGGER.info("[AI][UI_CUSTOMIZATION_CLARIFICATION_SKIPPED] reason=FULL_CHAT_LIST_THEME");
                    LOGGER.info("[AI][UI_CUSTOMIZATION_GLOBAL_THEME_CHANGES] changesCount={}", explicitChanges.size());
                }
            }
            if (explicitChanges.isEmpty()
                    && effectiveConfidence >= 0.70d
                    && isFullSidebarThemeRequest(consulta)) {
                String inferredColor = resolveFullSidebarThemeBaseColor(consulta);
                if (hasText(inferredColor)) {
                    explicitChanges.addAll(buildSidebarNavThemeChanges(inferredColor));
                    LOGGER.info("[AI][UI_CUSTOMIZATION_CLARIFICATION_SKIPPED] reason=FULL_SIDEBAR_THEME");
                    LOGGER.info("[AI][UI_CUSTOMIZATION_GLOBAL_THEME_CHANGES] changesCount={}", explicitChanges.size());
                }
            }
            LOGGER.info("[AI][UI_MULTI_VALIDATE] requestId={} rootAreaIgnored=true changesCount={}", requestId, explicitChanges.size());
            LOGGER.info("[AI][UI_MULTI_EXPLICIT] requestId={} count={}", requestId, explicitChanges.size());
            LOGGER.info("[AI][UI_CUSTOMIZATION_MULTI_LIMIT] limit={} changesCount={}", maxFinalChanges, explicitChanges.size());
            if (isScopedPinMenuRequest(consulta)) {
                int before = explicitChanges.size();
                explicitChanges = sanitizePinMenuOnlyChanges(explicitChanges);
                LOGGER.info("[AI][UI_CUSTOMIZATION_PIN_MENU_FILTER] before={} after={}", before, explicitChanges.size());
            }
            boolean controlledPinMenuGroup = isControlledPinMenuRequest(consulta) || isControlledPinMenuChanges(explicitChanges);
            if (controlledPinMenuGroup && explicitChanges.size() > maxExplicitChanges) {
                LOGGER.info("[AI][UI_CUSTOMIZATION_TOO_MANY_CHANGES_SKIPPED] reason=CONTROLLED_PIN_MENU_GROUP changesCount={}", explicitChanges.size());
            }
            if (controlledPinMenuGroup) {
                LOGGER.info("[AI][UI_CUSTOMIZATION_VALIDATE_CONTROLLED_GROUP] group=PIN_MENU_FULL_GROUP valid=true");
            }
            if (!autoRepairedWhiteElegant && !controlledPinMenuGroup && explicitChanges.size() > maxExplicitChanges) {
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
            if (isScopedPinMenuRequest(consulta)) {
                finalChanges = sanitizePinMenuOnlyChanges(finalChanges);
            }
            if (isStrictGroupOnlyScope(consulta, area)) {
                finalChanges = sanitizeGroupOnlyChanges(finalChanges);
            } else if (isStrictIndividualScope(consulta, area)) {
                finalChanges = sanitizeIndividualOnlyChanges(finalChanges);
            }
            finalChanges = completeParentAreaChildrenForHarmony(consulta, area, finalChanges);
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
                        "UI_CUSTOMIZATION_NEEDS_CLARIFICATION",
                        resolveClarificationMessage(clarificationQuestion, clarificationReason),
                        clarificationReason,
                        clarificationQuestion);
            }
            if (finalChanges.size() > maxFinalChanges) {
                return failure(action, area, property, value, valuePreset, label, confidence,
                        "UI_CUSTOMIZATION_TOO_MANY_CHANGES", "La solicitud genera demasiados cambios.");
            }
            LOGGER.info("[AI][UI_CUSTOMIZATION_MULTI_ALLOWED] reason=PARENT_HARMONY");

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

        if (shouldExpandParentAreaByDefaultHarmony(action, area, property, consulta, inputChanges)) {
            List<UiCustomizationChangeDTO> candidateChanges = buildParentAreaDefaultHarmonyChanges(area, property, resolvedValue);
            if (!candidateChanges.isEmpty()) {
                List<UiCustomizationChangeDTO> validated = new ArrayList<>();
                for (UiCustomizationChangeDTO change : candidateChanges) {
                    if (isValidGroupChange(change)) {
                        validated.add(normalizeGroupChange(change));
                    }
                }
                if (!validated.isEmpty()) {
                    LOGGER.info("[AI][UI_CUSTOMIZATION_PARENT_EXPANSION] area={} explicitSingle=false before={} after={}",
                            safe(area), 1, validated.size());
                    LOGGER.info("[AI][UI_CUSTOMIZATION_COMPLETED_CHILDREN] area={} added={}",
                            safe(area), Math.max(0, validated.size() - 1));
                    AiUiCustomizationResponseDTO response = baseResponse("UPDATE_STYLE_MULTI", area, property, resolvedValue, valuePreset, label, confidence);
                    response.setColorIntent(colorIntent);
                    response.setChanges(validated);
                    response.setSuccess(true);
                    response.setCodigo("UI_CUSTOMIZATION_OK");
                    response.setMensaje("Cambio visual interpretado correctamente con armonia en hijos.");
                    applyNormalizationMetadata(response, normalizedInput, requestedProperty, requestedValue, resolvedValue);
                    return response;
                }
            }
        }

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

    private boolean needsClarificationByMissingData(String area,
                                                    String property,
                                                    String value,
                                                    List<UiCustomizationChangeDTO> changes,
                                                    String action,
                                                    String resolvedAreaFromScope) {
        if ("RESET_THEME".equals(action)) {
            return false;
        }
        if ("UPDATE_STYLE_MULTI".equals(action)) {
            return !hasValidChanges(changes);
        }
        String effectiveArea = hasText(area) ? normalizeArea(area) : normalizeArea(resolvedAreaFromScope);
        if (!hasText(effectiveArea)) {
            return true;
        }
        return !hasText(property) || !hasText(value);
    }

    private boolean shouldExpandParentAreaByDefaultHarmony(String action,
                                                           String area,
                                                           String property,
                                                           String consulta,
                                                           List<UiCustomizationChangeDTO> changes) {
        if (!"UPDATE_STYLE".equals(action)) {
            return false;
        }
        if (changes != null && !changes.isEmpty()) {
            return false;
        }
        String normalizedArea = normalizeArea(area);
        String normalizedProperty = normalizeProperty(property);
        if (!hasText(normalizedArea) || !PARENT_AREAS_DEFAULT_HARMONY.contains(normalizedArea)) {
            return false;
        }
        if (!hasText(normalizedProperty) || !COLOR_PROPERTIES.contains(normalizedProperty)) {
            return false;
        }
        if (hasExplicitSinglePropertyIntent(consulta)) {
            LOGGER.info("[AI][UI_CUSTOMIZATION_PARENT_EXPANSION_SKIP] reason=EXPLICIT_SINGLE_PROPERTY");
            return false;
        }
        return true;
    }

    private boolean hasExplicitSinglePropertyIntent(String consulta) {
        String normalized = normalizeSemanticText(consulta);
        return containsAny(normalized,
                "solo cambia", "unicamente cambia", "solamente cambia", "cambia solo", "quiero cambiar solo",
                "solo el fondo", "solo el texto", "solo el borde", "solo el icono", "solo esa propiedad",
                "unicamente el fondo", "unicamente el texto", "unicamente el borde", "unicamente el icono",
                "solamente el fondo", "solamente el texto", "solamente el borde", "solamente el icono",
                "no toques nada mas", "no cambies nada mas", "sin cambiar nada mas", "manten el resto igual", "deja lo demas igual",
                "sin cambiar el resto", "sin tocar los hijos", "no cambies los hijos", "no cambies los elementos internos",
                "aplica unicamente ese cambio", "no armonices", "sin armonizar", "sin combinar",
                "aplica solo ese cambio");
    }

    private List<UiCustomizationChangeDTO> buildParentAreaDefaultHarmonyChanges(String area, String property, String bgColor) {
        String normalizedArea = normalizeArea(area);
        String baseBg = hasText(bgColor) ? bgColor : "#2563eb";
        List<UiCustomizationChangeDTO> changes = new ArrayList<>(buildParentAreaExpansionTemplate(null, normalizedArea, baseBg));
        changes.add(change(normalizedArea, property, baseBg));
        return deduplicateChangesPreservingLast(changes);
    }

    private List<UiCustomizationChangeDTO> completeParentAreaChildrenForHarmony(String consulta,
                                                                                String area,
                                                                                List<UiCustomizationChangeDTO> changes) {
        if (changes == null || changes.isEmpty()) {
            return changes;
        }
        if (hasExplicitSinglePropertyIntent(consulta)) {
            LOGGER.info("[AI][UI_CUSTOMIZATION_PARENT_EXPANSION_SKIP] reason=EXPLICIT_SINGLE_PROPERTY");
            return changes;
        }
        String expansionArea = resolveParentExpansionArea(consulta, area, changes);
        if (!hasText(expansionArea)) {
            LOGGER.info("[AI][UI_CUSTOMIZATION_PARENT_EXPANSION_SKIP] reason=NO_PARENT_AREA");
            return changes;
        }
        int before = changes.size();
        String baseColor = resolveParentExpansionBaseColor(consulta, expansionArea, changes);
        List<UiCustomizationChangeDTO> template = buildParentAreaExpansionTemplate(consulta, expansionArea, baseColor);
        if (template.isEmpty()) {
            return changes;
        }
        Map<String, UiCustomizationChangeDTO> merged = new LinkedHashMap<>();
        for (UiCustomizationChangeDTO templateChange : template) {
            UiCustomizationChangeDTO normalized = normalizeGroupChange(templateChange);
            if (isValidGroupChange(normalized)) {
                merged.put(changeKey(normalized), normalized);
            }
        }
        for (UiCustomizationChangeDTO existing : changes) {
            UiCustomizationChangeDTO normalized = normalizeGroupChange(existing);
            if (isValidGroupChange(normalized)) {
                merged.put(changeKey(normalized), normalized);
            }
        }
        List<UiCustomizationChangeDTO> completed = new ArrayList<>(merged.values());
        LOGGER.info("[AI][UI_CUSTOMIZATION_PARENT_EXPANSION] area={} explicitSingle=false before={} after={}",
                expansionArea, before, completed.size());
        LOGGER.info("[AI][UI_CUSTOMIZATION_COMPLETED_CHILDREN] area={} added={}",
                expansionArea, Math.max(0, completed.size() - before));
        return completed;
    }

    private String resolveParentExpansionArea(String consulta,
                                             String area,
                                             List<UiCustomizationChangeDTO> changes) {
        String normalizedArea = normalizeArea(area);
        LOGGER.info("[AI][UI_CUSTOMIZATION_PARENT_EXPANSION] area={} normalizedArea={}", safe(area), safe(normalizedArea));
        if (hasText(normalizedArea) && PARENT_AREAS_DEFAULT_HARMONY.contains(normalizedArea)) {
            return normalizedArea;
        }
        if (changes != null) {
            for (UiCustomizationChangeDTO change : changes) {
                if (change == null) {
                    LOGGER.info("[AI][UI_CUSTOMIZATION_PARENT_EXPANSION_SKIP] reason=NULL_CHANGE_AREA");
                    continue;
                }
                String changeArea = normalizeArea(change == null ? null : change.getArea());
                if (!hasText(changeArea)) {
                    LOGGER.info("[AI][UI_CUSTOMIZATION_PARENT_EXPANSION_SKIP] reason=NULL_CHANGE_AREA");
                    continue;
                }
                if (PARENT_AREAS_DEFAULT_HARMONY.contains(changeArea)) {
                    return changeArea;
                }
            }
        }
        String normalized = normalizeSemanticText(consulta);
        if (containsAny(normalized, "todo el listado de chats", "listado de chats completo", "panel de chats", "zona de chats", "lista de conversaciones", "todo el bloque de chats")) {
            return "CHAT_LIST_PANEL";
        }
        if (containsAny(normalized, "chat grupal activo", "grupo seleccionado", "grupo activo", "chat grupal seleccionado")) {
            return "CHAT_LIST_ITEM_GROUP_ACTIVE";
        }
        if (containsAny(normalized, "chat activo", "chat seleccionado", "chat individual seleccionado", "chat individual activo")) {
            return "CHAT_LIST_ITEM_ACTIVE";
        }
        if (containsAny(normalized, "chats grupales", "grupos", "todos los grupos", "chats de grupo")) {
            return "CHAT_LIST_ITEM_GROUP";
        }
        if (containsAny(normalized, "chats individuales", "chat individual", "conversaciones individuales", "todos los chats individuales")) {
            return "CHAT_LIST_ITEM";
        }
        if (containsAny(normalized, "buscador de chats", "buscador")) {
            return "CHAT_LIST_SEARCH";
        }
        if (containsAny(normalized, "filtros", "botones de filtros", "filtro de chats")) {
            return "CHAT_LIST_FILTERS";
        }
        if (containsAny(normalized, "desplegable de opciones del chat", "menu de opciones del chat", "opciones del chat")) {
            return "CHAT_LIST_PIN_MENU";
        }
        if (containsAny(normalized, "barra lateral", "sidebar", "menu izquierdo", "menu lateral")) {
            return "SIDEBAR_NAV_PANEL";
        }
        if (containsAny(normalized, "encabezado de chats", "header de chats", "cabecera de chats")) {
            return "CHAT_LIST_HEADER";
        }
        return null;
    }

    private String resolveParentExpansionBaseColor(String consulta,
                                                   String expansionArea,
                                                   List<UiCustomizationChangeDTO> changes) {
        if (changes != null) {
            for (UiCustomizationChangeDTO change : changes) {
                String normalizedProperty = normalizeProperty(change == null ? null : change.getProperty());
                if (change == null || !hasText(normalizedProperty) || !COLOR_PROPERTIES.contains(normalizedProperty) || !hasText(change.getValue())) {
                    continue;
                }
                if (expansionArea.equals(normalizeArea(change.getArea()))) {
                    return change.getValue();
                }
            }
            for (UiCustomizationChangeDTO change : changes) {
                String normalizedProperty = normalizeProperty(change == null ? null : change.getProperty());
                if (change != null && hasText(normalizedProperty) && COLOR_PROPERTIES.contains(normalizedProperty) && hasText(change.getValue())) {
                    return change.getValue();
                }
            }
        }
        String inferred = resolveColorFromText(consulta);
        return hasText(inferred) ? inferred : "#2563eb";
    }

    private List<UiCustomizationChangeDTO> buildParentAreaExpansionTemplate(String consulta,
                                                                            String area,
                                                                            String baseColor) {
        String normalizedArea = normalizeArea(area);
        if (!hasText(normalizedArea)) {
            return List.of();
        }
        return switch (normalizedArea) {
            case "CHAT_LIST_PANEL" -> buildChatListPanelColorGroup(consulta, baseColor);
            case "CHAT_LIST_ITEM" -> buildCompleteChatListItemChildren(baseColor);
            case "CHAT_LIST_ITEM_GROUP" -> buildCompleteChatListGroupChildren(baseColor);
            case "CHAT_LIST_ITEM_ACTIVE" -> buildCompleteChatListActiveChildren(baseColor);
            case "CHAT_LIST_ITEM_GROUP_ACTIVE" -> buildCompleteChatListGroupActiveChildren(baseColor);
            case "CHAT_LIST_SEARCH" -> buildSearchColorGroup(baseColor);
            case "CHAT_LIST_FILTERS" -> buildFiltersColorGroup(baseColor);
            case "CHAT_LIST_PIN_MENU" -> buildPinMenuColorGroup(baseColor);
            case "SIDEBAR_NAV_PANEL" -> buildSidebarNavThemeChanges(baseColor);
            case "CHAT_LIST_HEADER" -> buildChatListHeaderColorGroup(baseColor);
            default -> List.of();
        };
    }

    private List<UiCustomizationChangeDTO> buildCompleteChatListItemChildren(String baseColor) {
        String normalizedBase = normalizeColorText(baseColor);
        String textColor = choosePrimaryTextColor(normalizedBase);
        String secondaryText = chooseSecondaryTextColor(normalizedBase);
        String borderColor = chooseBorderColor(normalizedBase);
        List<UiCustomizationChangeDTO> changes = new ArrayList<>(buildChatListItemColorGroup(null, "CHAT_LIST_ITEM", normalizedBase));
        changes.add(change("CHAT_LIST_ITEM_NAME", "TEXT_COLOR", textColor));
        changes.add(change("CHAT_LIST_ITEM_NAME_SCOPED", "TEXT_COLOR", textColor));
        changes.add(change("CHAT_LIST_ITEM_PREVIEW", "TEXT_COLOR", secondaryText));
        changes.add(change("CHAT_LIST_ITEM_DRAFT_PREVIEW", "TEXT_COLOR", secondaryText));
        changes.add(change("CHAT_LIST_ITEM_BADGES", "BACKGROUND_COLOR", borderColor));
        changes.add(change("CHAT_LIST_ITEM_BADGES", "TEXT_COLOR", textColor));
        changes.add(change("CHAT_LIST_ITEM_ACTIONS", "ICON_COLOR", textColor));
        changes.add(change("CHAT_LIST_ITEM_ACTIONS_SCOPED", "ICON_COLOR", textColor));
        changes.add(change("CHAT_LIST_ITEM_STATUS_PILLS", "REPORTED_BACKGROUND_COLOR", borderColor));
        changes.add(change("CHAT_LIST_ITEM_STATUS_PILLS", "REPORTED_TEXT_COLOR", textColor));
        changes.add(change("CHAT_LIST_ITEM_STATUS_PILLS", "BLOCKED_BACKGROUND_COLOR", borderColor));
        changes.add(change("CHAT_LIST_ITEM_STATUS_PILLS", "BLOCKED_TEXT_COLOR", textColor));
        changes.add(change("CHAT_LIST_ITEM_DATE", "TEXT_COLOR", secondaryText));
        changes.add(change("CHAT_LIST_PIN_TOGGLE", "ICON_COLOR", textColor));
        changes.add(change("CHAT_LIST_MUTED_INDICATOR", "ICON_COLOR", textColor));
        changes.add(change("CHAT_LIST_FAVORITE_INDICATOR", "ICON_COLOR", textColor));
        changes.add(change("CHAT_LIST_CLOSED_INDICATOR", "TEXT_COLOR", secondaryText));
        return deduplicateChangesPreservingLast(changes);
    }

    private List<UiCustomizationChangeDTO> buildCompleteChatListGroupChildren(String baseColor) {
        String normalizedBase = normalizeColorText(baseColor);
        String textColor = choosePrimaryTextColor(normalizedBase);
        String secondaryText = chooseSecondaryTextColor(normalizedBase);
        String borderColor = chooseBorderColor(normalizedBase);
        List<UiCustomizationChangeDTO> changes = new ArrayList<>(buildChatListItemColorGroup("chats grupales", "CHAT_LIST_ITEM_GROUP", normalizedBase));
        changes.add(change("CHAT_LIST_ITEM_GROUP_NAME", "TEXT_COLOR", textColor));
        changes.add(change("CHAT_LIST_ITEM_GROUP_PREVIEW", "TEXT_COLOR", secondaryText));
        changes.add(change("CHAT_LIST_ITEM_GROUP_DRAFT_PREVIEW", "TEXT_COLOR", secondaryText));
        changes.add(change("CHAT_LIST_ITEM_GROUP_BADGES", "BACKGROUND_COLOR", borderColor));
        changes.add(change("CHAT_LIST_ITEM_GROUP_BADGES", "TEXT_COLOR", textColor));
        changes.add(change("CHAT_LIST_ITEM_GROUP_ACTIONS", "ICON_COLOR", textColor));
        changes.add(change("CHAT_LIST_ITEM_GROUP_STATUS_PILLS", "REPORTED_BACKGROUND_COLOR", borderColor));
        changes.add(change("CHAT_LIST_ITEM_GROUP_STATUS_PILLS", "REPORTED_TEXT_COLOR", textColor));
        changes.add(change("CHAT_LIST_ITEM_GROUP_STATUS_PILLS", "BLOCKED_BACKGROUND_COLOR", borderColor));
        changes.add(change("CHAT_LIST_ITEM_GROUP_STATUS_PILLS", "BLOCKED_TEXT_COLOR", textColor));
        changes.add(change("CHAT_LIST_GROUP_PILL", "BACKGROUND_COLOR", borderColor));
        changes.add(change("CHAT_LIST_GROUP_PILL", "TEXT_COLOR", textColor));
        changes.add(change("CHAT_LIST_ITEM_DATE", "TEXT_COLOR", secondaryText));
        changes.add(change("CHAT_LIST_PIN_TOGGLE", "ICON_COLOR", textColor));
        changes.add(change("CHAT_LIST_MUTED_INDICATOR", "ICON_COLOR", textColor));
        changes.add(change("CHAT_LIST_FAVORITE_INDICATOR", "ICON_COLOR", textColor));
        changes.add(change("CHAT_LIST_CLOSED_INDICATOR", "TEXT_COLOR", secondaryText));
        return deduplicateChangesPreservingLast(changes);
    }

    private List<UiCustomizationChangeDTO> buildCompleteChatListActiveChildren(String baseColor) {
        String normalizedBase = normalizeColorText(baseColor);
        String textColor = choosePrimaryTextColor(normalizedBase);
        String secondaryText = chooseSecondaryTextColor(normalizedBase);
        String borderColor = chooseBorderColor(normalizedBase);
        List<UiCustomizationChangeDTO> changes = new ArrayList<>();
        changes.add(change("CHAT_LIST_ITEM_ACTIVE", "BACKGROUND_COLOR", normalizedBase));
        changes.add(change("CHAT_LIST_ITEM_ACTIVE", "TEXT_COLOR", textColor));
        changes.add(change("CHAT_LIST_ITEM_ACTIVE", "BORDER_COLOR", borderColor));
        changes.add(change("CHAT_LIST_ITEM_NAME", "TEXT_COLOR", textColor));
        changes.add(change("CHAT_LIST_ITEM_NAME_SCOPED", "TEXT_COLOR", textColor));
        changes.add(change("CHAT_LIST_ITEM_PREVIEW", "TEXT_COLOR", secondaryText));
        changes.add(change("CHAT_LIST_ITEM_DATE", "TEXT_COLOR", secondaryText));
        changes.add(change("CHAT_LIST_PIN_TOGGLE", "ICON_COLOR", textColor));
        changes.add(change("CHAT_LIST_BADGES", "BACKGROUND_COLOR", borderColor));
        changes.add(change("CHAT_LIST_BADGES", "TEXT_COLOR", textColor));
        changes.add(change("CHAT_LIST_MUTED_INDICATOR", "ICON_COLOR", textColor));
        changes.add(change("CHAT_LIST_FAVORITE_INDICATOR", "ICON_COLOR", textColor));
        return changes;
    }

    private List<UiCustomizationChangeDTO> buildCompleteChatListGroupActiveChildren(String baseColor) {
        String normalizedBase = normalizeColorText(baseColor);
        String textColor = choosePrimaryTextColor(normalizedBase);
        String secondaryText = chooseSecondaryTextColor(normalizedBase);
        String borderColor = chooseBorderColor(normalizedBase);
        List<UiCustomizationChangeDTO> changes = new ArrayList<>();
        changes.add(change("CHAT_LIST_ITEM_GROUP_ACTIVE", "BACKGROUND_COLOR", normalizedBase));
        changes.add(change("CHAT_LIST_ITEM_GROUP_ACTIVE", "TEXT_COLOR", textColor));
        changes.add(change("CHAT_LIST_ITEM_GROUP_ACTIVE", "BORDER_COLOR", borderColor));
        changes.add(change("CHAT_LIST_ITEM_GROUP_NAME", "TEXT_COLOR", textColor));
        changes.add(change("CHAT_LIST_ITEM_GROUP_PREVIEW", "TEXT_COLOR", secondaryText));
        changes.add(change("CHAT_LIST_ITEM_DATE", "TEXT_COLOR", secondaryText));
        changes.add(change("CHAT_LIST_GROUP_PILL", "BACKGROUND_COLOR", borderColor));
        changes.add(change("CHAT_LIST_GROUP_PILL", "TEXT_COLOR", textColor));
        changes.add(change("CHAT_LIST_ITEM_GROUP_BADGES", "BACKGROUND_COLOR", borderColor));
        changes.add(change("CHAT_LIST_ITEM_GROUP_BADGES", "TEXT_COLOR", textColor));
        changes.add(change("CHAT_LIST_PIN_TOGGLE", "ICON_COLOR", textColor));
        changes.add(change("CHAT_LIST_MUTED_INDICATOR", "ICON_COLOR", textColor));
        changes.add(change("CHAT_LIST_FAVORITE_INDICATOR", "ICON_COLOR", textColor));
        return changes;
    }

    private List<UiCustomizationChangeDTO> buildChatListHeaderColorGroup(String baseColor) {
        String normalizedBase = normalizeColorText(baseColor);
        String textColor = choosePrimaryTextColor(normalizedBase);
        String borderColor = chooseBorderColor(normalizedBase);
        List<UiCustomizationChangeDTO> changes = new ArrayList<>();
        changes.add(change("CHAT_LIST_HEADER", "BACKGROUND_COLOR", normalizedBase));
        changes.add(change("CHAT_LIST_HEADER", "TEXT_COLOR", textColor));
        changes.add(change("CHAT_LIST_TITLE", "TEXT_COLOR", textColor));
        changes.add(change("CHAT_LIST_HEADER_ACTIONS", "ICON_COLOR", textColor));
        changes.add(change("CHAT_LIST_HEADER_ICON_BUTTON", "BACKGROUND_COLOR", normalizedBase));
        changes.add(change("CHAT_LIST_HEADER_ICON_BUTTON", "ICON_COLOR", textColor));
        changes.add(change("CHAT_LIST_HEADER_ICON", "ICON_COLOR", textColor));
        changes.add(change("CHAT_LIST_HEADER_MENU", "BACKGROUND_COLOR", normalizedBase));
        changes.add(change("CHAT_LIST_HEADER_MENU", "TEXT_COLOR", textColor));
        changes.add(change("CHAT_LIST_HEADER_MENU", "BORDER_COLOR", borderColor));
        changes.add(change("CHAT_LIST_HEADER_MENU_ITEM", "TEXT_COLOR", textColor));
        changes.add(change("CHAT_LIST_HEADER_MENU_ITEM", "ICON_COLOR", textColor));
        changes.add(change("CHAT_LIST_ACTIONS_MENU", "BACKGROUND_COLOR", normalizedBase));
        changes.add(change("CHAT_LIST_ACTIONS_MENU", "TEXT_COLOR", textColor));
        changes.add(change("CHAT_LIST_ACTIONS_MENU", "BORDER_COLOR", borderColor));
        changes.add(change("CHAT_LIST_ACTIONS_MENU_ITEM", "TEXT_COLOR", textColor));
        changes.add(change("CHAT_LIST_ACTIONS_MENU_ITEM", "ICON_COLOR", textColor));
        return deduplicateChangesPreservingLast(changes);
    }

    private String choosePrimaryTextColor(String baseColor) {
        String normalizedBase = normalizeColorText(baseColor);
        return isDarkColor(normalizedBase) || "#ef4444".equals(normalizedBase) || "#7c3aed".equals(normalizedBase) || "#14532d".equals(normalizedBase)
                ? "#ffffff" : "#111827";
    }

    private String chooseSecondaryTextColor(String baseColor) {
        return isDarkColor(normalizeColorText(baseColor)) ? "#e5e7eb" : "#64748b";
    }

    private String chooseBorderColor(String baseColor) {
        String normalizedBase = normalizeColorText(baseColor);
        if ("#ef4444".equals(normalizedBase)) {
            return "#b91c1c";
        }
        if ("#7c3aed".equals(normalizedBase)) {
            return "#6d28d9";
        }
        if ("#16a34a".equals(normalizedBase) || "#14532d".equals(normalizedBase)) {
            return "#166534";
        }
        return isDarkColor(normalizedBase) ? "#334155" : "#e5e7eb";
    }

    private List<UiCustomizationChangeDTO> deduplicateChangesPreservingLast(List<UiCustomizationChangeDTO> changes) {
        Map<String, UiCustomizationChangeDTO> deduped = new LinkedHashMap<>();
        if (changes == null) {
            return new ArrayList<>();
        }
        for (UiCustomizationChangeDTO change : changes) {
            UiCustomizationChangeDTO normalized = normalizeGroupChange(change);
            if (isValidGroupChange(normalized)) {
                deduped.put(changeKey(normalized), normalized);
            }
        }
        return new ArrayList<>(deduped.values());
    }

    private boolean hasValidChanges(List<UiCustomizationChangeDTO> changes) {
        if (changes == null || changes.isEmpty()) {
            return false;
        }
        for (UiCustomizationChangeDTO change : changes) {
            if (change == null) {
                continue;
            }
            String changeArea = normalizeArea(change.getArea());
            String changeProperty = normalizeProperty(change.getProperty());
            String changeValue = change.getValue();
            if (hasText(changeArea) && ALLOWED_AREAS.contains(changeArea)
                    && hasText(changeProperty) && hasText(changeValue)) {
                return true;
            }
        }
        return false;
    }

    private boolean mentionsDropdownKeywords(String consulta) {
        String normalized = normalizeSemanticText(consulta);
        return containsAny(normalized, "desplegable", "menu", "opciones");
    }

    private String inferClarificationReason(String clarificationReason,
                                            String area,
                                            String property,
                                            String value,
                                            List<UiCustomizationChangeDTO> changes,
                                            String action,
                                            String resolvedAreaFromScope) {
        if (hasText(clarificationReason)) {
            return clarificationReason;
        }
        if (!needsClarificationByMissingData(area, property, value, changes, action, resolvedAreaFromScope)) {
            return clarificationReason;
        }
        String effectiveArea = hasText(area) ? normalizeArea(area) : normalizeArea(resolvedAreaFromScope);
        String normalizedProperty = normalizeProperty(property);
        if (!hasText(effectiveArea)) {
            return "AREA_SCOPE_AMBIGUOUS";
        }
        if (hasText(normalizedProperty) && !hasText(value) && (changes == null || changes.isEmpty())) {
            if (COLOR_PROPERTIES.contains(normalizedProperty)) {
                return "COLOR_VALUE_MISSING";
            }
            if (SIZE_PROPERTIES.contains(normalizedProperty)) {
                return "SIZE_VALUE_MISSING";
            }
        }
        return clarificationReason;
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
        return failure(action, area, property, value, valuePreset, label, confidence, codigo, mensaje, null, null);
    }

    private AiUiCustomizationResponseDTO failure(String action,
                                                 String area,
                                                 String property,
                                                 String value,
                                                 String valuePreset,
                                                 String label,
                                                 Double confidence,
                                                 String codigo,
                                                 String mensaje,
                                                 String clarificationReason,
                                                 String clarificationQuestion) {
        String responseAction = "UI_CUSTOMIZATION_NEEDS_CLARIFICATION".equals(codigo) ? "NEEDS_CLARIFICATION" : action;
        AiUiCustomizationResponseDTO response = baseResponse(responseAction, area, property, value, valuePreset, label, confidence);
        response.setSuccess(false);
        response.setCodigo(codigo);
        if ("UI_CUSTOMIZATION_NEEDS_CLARIFICATION".equals(codigo)) {
            String resolvedClarificationMessage = resolveClarificationMessage(clarificationQuestion, clarificationReason);
            response.setMensaje(resolvedClarificationMessage);
            response.setNeedsClarification(Boolean.TRUE);
            response.setClarificationReason(clarificationReason);
            response.setClarificationQuestion(resolvedClarificationMessage);
            response.setArea(null);
            response.setProperty(null);
            response.setValue(null);
            response.setChanges(null);
            LOGGER.info("[AI][UI_CLARIFICATION_MESSAGE_RESOLVED] message={}", safe(resolvedClarificationMessage));
            LOGGER.info("[AI][UI_CLARIFICATION_CONTEXTUAL] label={} reason={}", safe(label), safe(clarificationReason));
        } else {
            response.setMensaje(mensaje);
        }
        return response;
    }

    private String resolveClarificationMessage(String clarificationQuestion,
                                              String clarificationReason) {
        if (hasText(clarificationQuestion)) {
            return clarificationQuestion;
        }
        if ("COLOR_VALUE_MISSING".equalsIgnoreCase(safe(clarificationReason))) {
            return COLOR_VALUE_MISSING_MESSAGE;
        }
        if ("SIZE_VALUE_MISSING".equalsIgnoreCase(safe(clarificationReason))) {
            return SIZE_VALUE_MISSING_MESSAGE;
        }
        if ("DROPDOWN_SCOPE_AMBIGUOUS".equalsIgnoreCase(safe(clarificationReason))) {
            return AMBIGUOUS_DROPDOWN_CLARIFICATION_MESSAGE;
        }
        if ("ICON_SCOPE_AMBIGUOUS".equalsIgnoreCase(safe(clarificationReason))) {
            return AMBIGUOUS_ICON_CLARIFICATION_MESSAGE;
        }
        if ("BORDER_PROPERTY_AMBIGUOUS".equalsIgnoreCase(safe(clarificationReason))) {
            return AMBIGUOUS_BORDER_CLARIFICATION_MESSAGE;
        }
        if ("AREA_SCOPE_AMBIGUOUS".equalsIgnoreCase(safe(clarificationReason))) {
            return AMBIGUOUS_AREA_CLARIFICATION_MESSAGE;
        }
        return DEFAULT_CLARIFICATION_MESSAGE;
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
        String normalizedArea = normalizeArea(area);
        String normalizedProperty = normalizeProperty(property);
        if ("CHAT_HEADER".equals(normalizedArea)) {
            normalizedArea = "CHAT_LIST_HEADER";
        }
        if ("CHAT_LIST_FILTERS".equals(normalizedArea) && "SEND_BUTTON_COLOR".equals(normalizedProperty)) {
            normalizedProperty = "ICON_COLOR";
        }
        return new AreaProperty(normalizedArea, normalizedProperty);
    }

    private String normalizeArea(String area) {
        if (!hasText(area)) {
            return area;
        }
        return switch (area) {
            case "SIDEBAR_NAV" -> "SIDEBAR_NAV_PANEL";
            case "SIDEBAR_NAV_ACTIVE_ITEM" -> "SIDEBAR_NAV_ITEM_ACTIVE";
            case "CHAT_LIST_NAME" -> "CHAT_LIST_ITEM_NAME";
            case "CHAT_LIST_STATUS_INDICATOR" -> "CHAT_LIST_STATUS_DOT";
            default -> area;
        };
    }

    private String normalizeProperty(String property) {
        if (!hasText(property)) {
            return property;
        }
        return "SHADOW".equals(property) ? "SHADOW_PRESET" : property;
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
            case "WIDTH" -> resolveWidth(rawValue);
            case "HEIGHT" -> resolveHeight(rawValue);
            case "GAP" -> resolveGap(rawValue);
            case "FONT_SIZE" -> resolveFontSize(rawValue, valuePreset);
            case "FONT_WEIGHT" -> resolveFontWeight(rawValue);
            case "SHADOW_PRESET", "SHADOW" -> resolvePreset(valuePreset, rawValue, SHADOW_PRESETS);
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

    private String resolveWidth(String rawValue) {
        if (!hasText(rawValue)) {
            return null;
        }
        String normalized = rawValue.trim().toLowerCase(Locale.ROOT);
        return WIDTH_VALUES.contains(normalized) ? normalized : null;
    }

    private String resolveHeight(String rawValue) {
        if (!hasText(rawValue)) {
            return null;
        }
        String normalized = rawValue.trim().toLowerCase(Locale.ROOT);
        return HEIGHT_VALUES.contains(normalized) ? normalized : null;
    }

    private String resolveGap(String rawValue) {
        if (!hasText(rawValue)) {
            return null;
        }
        String normalized = rawValue.trim().toLowerCase(Locale.ROOT);
        return GAP_VALUES.contains(normalized) ? normalized : null;
    }

    private String resolveFontWeight(String rawValue) {
        if (!hasText(rawValue)) {
            return null;
        }
        String normalized = rawValue.trim();
        return FONT_WEIGHT_VALUES.contains(normalized) ? normalized : null;
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
        changes.add(change("CHAT_LIST_FILTERS", "TEXT_COLOR", textColor));
        changes.add(change("CHAT_LIST_FILTERS", "BORDER_COLOR", borderColor));
        changes.add(change("CHAT_LIST_FILTER_BUTTONS", "BACKGROUND_COLOR", normalizedBase));
        changes.add(change("CHAT_LIST_FILTER_BUTTONS", "TEXT_COLOR", textColor));
        changes.add(change("CHAT_LIST_FILTER_BUTTONS", "BORDER_COLOR", borderColor));
        changes.add(change("CHAT_LIST_FILTER_BUTTONS", "HOVER_BACKGROUND_COLOR", isDarkColor(normalizedBase) ? "#1f2937" : "#f1f5f9"));
        changes.add(change("CHAT_LIST_FILTER_BUTTONS_ACTIVE", "BACKGROUND_COLOR", normalizedBase));
        changes.add(change("CHAT_LIST_FILTER_BUTTONS_ACTIVE", "TEXT_COLOR", textColor));
        changes.add(change("CHAT_LIST_FILTER_BUTTONS_ACTIVE", "BORDER_COLOR", borderColor));
        changes.add(change("CHAT_LIST_FILTER_BUTTONS_ACTIVE", "ACTIVE_BACKGROUND_COLOR", normalizedBase));
        changes.add(change("CHAT_LIST_FILTER_BUTTONS_ACTIVE", "ACTIVE_TEXT_COLOR", textColor));
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
        changes.add(change("CHAT_LIST_SEARCH", "BORDER_RADIUS", "18px"));
        return changes;
    }

    private List<UiCustomizationChangeDTO> buildStyleGroupChanges(String consulta, String area, String property, String value) {
        String effectiveArea = resolveGroupAreaFromConsulta(consulta, area);
        if (!"BACKGROUND_COLOR".equals(property) || !hasText(value)) {
            return List.of();
        }
        return switch (effectiveArea) {
            case "CHAT_LIST_PANEL" -> buildChatListPanelColorGroup(consulta, value);
            case "SIDEBAR_NAV_PANEL" -> buildSidebarNavThemeChanges(value);
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
        return Set.of("CHAT_LIST_PANEL", "SIDEBAR_NAV_PANEL", "CHAT_LIST_ITEM", "CHAT_LIST_ITEM_GROUP", "CHAT_LIST_AUDIO_PREVIEW", "CHAT_LIST_GROUP_PILL",
                "CHAT_LIST_PIN_MENU", "CHAT_LIST_STATUS_PILLS", "CHAT_LIST_SEARCH", "CHAT_LIST_FILTERS").contains(change.getArea());
    }

    private List<UiCustomizationChangeDTO> buildSidebarNavThemeChanges(String baseColor) {
        String normalizedBase = normalizeColorText(baseColor);
        String panelText = isDarkColor(normalizedBase) || "#2563eb".equals(normalizedBase) || "#7c3aed".equals(normalizedBase)
                ? "#f9fafb" : "#111827";
        String borderColor = isDarkColor(normalizedBase) ? "#334155" : "#d1d5db";
        String itemBackground = isDarkColor(normalizedBase) ? "#1f2937" : "#f8fafc";
        String itemHoverBackground = isDarkColor(normalizedBase) ? "#374151" : "#e5e7eb";
        String accent = "#ffffff".equals(normalizedBase) ? "#2563eb"
                : isDarkColor(normalizedBase) ? "#3b82f6"
                : "#1d4ed8";
        String tooltipBackground = isDarkColor(normalizedBase) ? "#0f172a" : "#ffffff";
        List<UiCustomizationChangeDTO> changes = new ArrayList<>();
        changes.add(change("SIDEBAR_NAV_PANEL", "BACKGROUND_COLOR", normalizedBase));
        changes.add(change("SIDEBAR_NAV_PANEL", "TEXT_COLOR", panelText));
        changes.add(change("SIDEBAR_NAV_GROUP", "BACKGROUND_COLOR", normalizedBase));
        changes.add(change("SIDEBAR_NAV_GROUP", "TEXT_COLOR", panelText));
        changes.add(change("SIDEBAR_NAV_BOTTOM", "BACKGROUND_COLOR", normalizedBase));
        changes.add(change("SIDEBAR_NAV_BOTTOM", "TEXT_COLOR", panelText));
        changes.add(change("SIDEBAR_NAV_ITEM", "BACKGROUND_COLOR", itemBackground));
        changes.add(change("SIDEBAR_NAV_ITEM", "ICON_COLOR", panelText));
        changes.add(change("SIDEBAR_NAV_ITEM", "TEXT_COLOR", panelText));
        changes.add(change("SIDEBAR_NAV_ITEM", "HOVER_BACKGROUND_COLOR", itemHoverBackground));
        changes.add(change("SIDEBAR_NAV_ITEM_ACTIVE", "BACKGROUND_COLOR", accent));
        changes.add(change("SIDEBAR_NAV_ITEM_ACTIVE", "ICON_COLOR", "#ffffff"));
        changes.add(change("SIDEBAR_NAV_ITEM_ACTIVE", "TEXT_COLOR", "#ffffff"));
        changes.add(change("SIDEBAR_NAV_ACTIVE_INDICATOR", "BACKGROUND_COLOR", accent));
        changes.add(change("SIDEBAR_NAV_ICON", "ICON_COLOR", panelText));
        changes.add(change("SIDEBAR_NAV_ICON_ACTIVE", "ICON_COLOR", "#ffffff"));
        changes.add(change("SIDEBAR_NAV_AI_ICON", "ICON_COLOR", panelText));
        changes.add(change("SIDEBAR_NAV_TOOLTIP", "BACKGROUND_COLOR", tooltipBackground));
        changes.add(change("SIDEBAR_NAV_TOOLTIP", "TEXT_COLOR", isDarkColor(tooltipBackground) ? "#f9fafb" : "#111827"));
        changes.add(change("SIDEBAR_NAV_LOGO", "BACKGROUND_COLOR", accent));
        changes.add(change("SIDEBAR_NAV_LOGO", "TEXT_COLOR", "#ffffff"));
        changes.add(change("SIDEBAR_NAV_AVATAR", "BORDER_COLOR", borderColor));
        changes.add(change("SIDEBAR_NAV_SETTINGS", "ICON_COLOR", panelText));
        changes.add(change("SIDEBAR_NAV_NOTIF_BADGE", "BACKGROUND_COLOR", accent));
        changes.add(change("SIDEBAR_NAV_NOTIF_BADGE", "TEXT_COLOR", "#ffffff"));
        return changes;
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
        changes.add(change("CHAT_LIST_TITLE", "TEXT_COLOR", panelText));
        changes.add(change("CHAT_LIST_SCROLL", "BACKGROUND_COLOR", normalizedBase));
        changes.addAll(buildSearchColorGroup(normalizedBase));
        changes.add(change("CHAT_LIST_FILTERS", "BACKGROUND_COLOR", normalizedBase));
        changes.add(change("CHAT_LIST_FILTERS", "BORDER_COLOR", panelBorder));
        changes.add(change("CHAT_LIST_FILTER_BUTTONS", "BACKGROUND_COLOR", normalizedBase));
        changes.add(change("CHAT_LIST_FILTER_BUTTONS", "TEXT_COLOR", panelText));
        changes.add(change("CHAT_LIST_FILTER_BUTTONS", "BORDER_COLOR", panelBorder));
        changes.add(change("CHAT_LIST_FILTER_BUTTONS_ACTIVE", "BACKGROUND_COLOR", filterActiveBg));
        changes.add(change("CHAT_LIST_FILTER_BUTTONS_ACTIVE", "TEXT_COLOR", filterActiveText));
        changes.add(change("CHAT_LIST_FILTER_BUTTONS_ACTIVE", "BORDER_COLOR", filterActiveBorder));
        changes.add(change("CHAT_LIST_HEADER_ACTIONS", "ICON_COLOR", panelText));
        changes.add(change("CHAT_LIST_HEADER_ICON_BUTTON", "BACKGROUND_COLOR", normalizedBase));
        changes.add(change("CHAT_LIST_HEADER_ICON_BUTTON", "ICON_COLOR", panelText));
        changes.add(change("CHAT_LIST_HEADER_MENU", "BACKGROUND_COLOR", normalizedBase));
        changes.add(change("CHAT_LIST_HEADER_MENU", "TEXT_COLOR", panelText));
        changes.add(change("CHAT_LIST_HEADER_MENU", "BORDER_COLOR", panelBorder));
        changes.add(change("CHAT_LIST_HEADER_MENU_ITEM", "TEXT_COLOR", panelText));
        changes.add(change("CHAT_LIST_HEADER_MENU_ITEM", "ICON_COLOR", panelText));
        List<UiCustomizationChangeDTO> itemThemeChanges = buildChatListItemColorGroup(consulta, "CHAT_LIST_ITEM", normalizedBase);
        changes.addAll(itemThemeChanges);
        changes.addAll(buildCompleteChatListActiveChildren(normalizedBase));
        changes.addAll(buildCompleteChatListGroupActiveChildren(normalizedBase));
        changes.addAll(buildCompleteChatListItemChildren(normalizedBase));
        changes.addAll(buildCompleteChatListGroupChildren(normalizedBase));
        changes.addAll(buildPinMenuColorGroup(normalizedBase));
        changes.add(change("CHAT_LIST_ITEM_UNREAD", "BACKGROUND_COLOR", normalizedBase));
        changes.add(change("CHAT_LIST_ITEM_UNREAD", "TEXT_COLOR", panelText));
        changes.add(change("CHAT_LIST_ITEM_UNREAD", "BORDER_COLOR", panelBorder));
        changes.add(change("CHAT_LIST_ITEM_UNREAD", "HOVER_BACKGROUND_COLOR", isDarkColor(normalizedBase) ? "#1f2937" : "#f1f5f9"));
        changes.add(change("CHAT_LIST_AVATAR", "BORDER_COLOR", panelBorder));
        changes.add(change("CHAT_LIST_STATUS_DOT", "BACKGROUND_COLOR", filterActiveBg));
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
        changes.add(change("CHAT_LIST_MUTED_INDICATOR", "ICON_COLOR", panelText));
        changes.add(change("CHAT_LIST_FAVORITE_INDICATOR", "ICON_COLOR", panelText));
        changes.add(change("CHAT_LIST_CLOSED_INDICATOR", "TEXT_COLOR", panelText));
        String previewText = findChangeValue(itemThemeChanges, "CHAT_LIST_PREVIEW", "TEXT_COLOR", panelText);
        String previewSenderText = findChangeValue(itemThemeChanges, "CHAT_LIST_PREVIEW", "PREVIEW_SENDER_TEXT_COLOR", panelText);
        String audioBackground = findChangeValue(itemThemeChanges, "CHAT_LIST_AUDIO_PREVIEW", "BACKGROUND_COLOR", normalizedBase);
        String audioText = findChangeValue(itemThemeChanges, "CHAT_LIST_AUDIO_PREVIEW", "TEXT_COLOR", panelText);
        String audioIcon = findChangeValue(itemThemeChanges, "CHAT_LIST_AUDIO_PREVIEW", "ICON_COLOR", panelText);
        String audioBorder = findChangeValue(itemThemeChanges, "CHAT_LIST_AUDIO_PREVIEW", "BORDER_COLOR", panelBorder);
        changes.add(change("CHAT_LIST_ITEM_PREVIEW", "TEXT_COLOR", previewText));
        changes.add(change("CHAT_LIST_ITEM_PREVIEW", "PREVIEW_SENDER_TEXT_COLOR", previewSenderText));
        changes.add(change("CHAT_LIST_ITEM_GROUP_PREVIEW", "TEXT_COLOR", previewText));
        changes.add(change("CHAT_LIST_ITEM_GROUP_PREVIEW", "PREVIEW_SENDER_TEXT_COLOR", previewSenderText));
        changes.add(change("CHAT_LIST_ITEM_NAME_SCOPED", "TEXT_COLOR", panelText));
        changes.add(change("CHAT_LIST_ITEM_GROUP_NAME", "TEXT_COLOR", panelText));
        changes.addAll(buildIndividualAudioPreviewScopedColorGroup(audioBackground));
        changes.addAll(buildGroupAudioPreviewScopedColorGroup(audioBackground));
        changes.add(change("CHAT_LIST_ITEM_AUDIO_PREVIEW", "BORDER_COLOR", audioBorder));
        changes.add(change("CHAT_LIST_ITEM_GROUP_AUDIO_PREVIEW", "BORDER_COLOR", audioBorder));
        changes.add(change("CHAT_LIST_ITEM_AUDIO_PREVIEW", "LABEL_COLOR", audioText));
        changes.add(change("CHAT_LIST_ITEM_GROUP_AUDIO_PREVIEW", "LABEL_COLOR", audioText));
        changes.add(change("CHAT_LIST_ITEM_AUDIO_PREVIEW", "SEPARATOR_COLOR", audioText));
        changes.add(change("CHAT_LIST_ITEM_GROUP_AUDIO_PREVIEW", "SEPARATOR_COLOR", audioText));
        changes.add(change("CHAT_LIST_ITEM_AUDIO_PREVIEW", "TIME_COLOR", audioText));
        changes.add(change("CHAT_LIST_ITEM_GROUP_AUDIO_PREVIEW", "TIME_COLOR", audioText));
        changes.addAll(buildIndividualFilePreviewScopedColorGroup(normalizedBase));
        changes.addAll(buildGroupFilePreviewScopedColorGroup(normalizedBase));
        changes.add(change("CHAT_LIST_ITEM_IMAGE_PREVIEW", "BACKGROUND_COLOR", normalizedBase));
        changes.add(change("CHAT_LIST_ITEM_IMAGE_PREVIEW", "TEXT_COLOR", panelText));
        changes.add(change("CHAT_LIST_ITEM_IMAGE_PREVIEW", "BORDER_COLOR", panelBorder));
        changes.add(change("CHAT_LIST_ITEM_GROUP_IMAGE_PREVIEW", "BACKGROUND_COLOR", normalizedBase));
        changes.add(change("CHAT_LIST_ITEM_GROUP_IMAGE_PREVIEW", "TEXT_COLOR", panelText));
        changes.add(change("CHAT_LIST_ITEM_GROUP_IMAGE_PREVIEW", "BORDER_COLOR", panelBorder));
        return deduplicateChangesPreservingLast(changes);
    }

    private String findChangeValue(List<UiCustomizationChangeDTO> changes,
                                   String area,
                                   String property,
                                   String fallback) {
        if (changes == null) {
            return fallback;
        }
        for (UiCustomizationChangeDTO change : changes) {
            if (change != null
                    && area.equals(change.getArea())
                    && property.equals(change.getProperty())
                    && hasText(change.getValue())) {
                return change.getValue();
            }
        }
        return fallback;
    }

    private List<UiCustomizationChangeDTO> extractExplicitChanges(String consulta,
                                                                  String label,
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
            explicit.addAll(parseDeterministicMultiChanges(semanticSource(consulta, label)));
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
        if (!isFullChatListThemeRequest(consulta) && isGroupBadgesScopedRequest(consulta) && isIncompleteScopedGroup(explicit, "CHAT_LIST_ITEM_GROUP_BADGES", Set.of("BACKGROUND_COLOR", "TEXT_COLOR"))) {
            String base = firstExplicitColor(explicit);
            List<UiCustomizationChangeDTO> repaired = buildGroupBadgesScopedColorGroup(hasText(base) ? base : "#7c3aed");
            logRuleMatch("REPAIR_GROUP_BADGES_SCOPED", consulta, repaired);
            return repaired;
        }
        if (!isFullChatListThemeRequest(consulta) && isGroupAudioPreviewScopedRequest(consulta) && isIncompleteScopedGroup(explicit, "CHAT_LIST_ITEM_GROUP_AUDIO_PREVIEW", Set.of("BACKGROUND_COLOR", "TEXT_COLOR", "ICON_COLOR"))) {
            String base = firstExplicitColor(explicit);
            List<UiCustomizationChangeDTO> repaired = buildGroupAudioPreviewScopedColorGroup(hasText(base) ? base : "#f97316");
            logRuleMatch("REPAIR_GROUP_AUDIO_PREVIEW_SCOPED", consulta, repaired);
            return repaired;
        }
        if (!isFullChatListThemeRequest(consulta) && isIndividualAudioPreviewScopedRequest(consulta) && explicit.stream().noneMatch(c -> c != null && "CHAT_LIST_ITEM_AUDIO_PREVIEW".equals(c.getArea()))) {
            String base = firstExplicitColor(explicit);
            List<UiCustomizationChangeDTO> repaired = buildIndividualAudioPreviewScopedColorGroup(hasText(base) ? base : "#f97316");
            logRuleMatch("REPAIR_INDIVIDUAL_AUDIO_PREVIEW_SCOPED", consulta, repaired);
            return repaired;
        }
        if (!isFullChatListThemeRequest(consulta) && isIndividualFilePreviewRequest(consulta) && explicit.stream().noneMatch(c -> c != null && "CHAT_LIST_ITEM_FILE_PREVIEW".equals(c.getArea()))) {
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

    private boolean isControlledPinMenuChanges(List<UiCustomizationChangeDTO> changes) {
        if (changes == null || changes.isEmpty()) {
            return false;
        }
        for (UiCustomizationChangeDTO change : changes) {
            if (change == null || !hasText(change.getArea())) {
                return false;
            }
            if (!Set.of("CHAT_LIST_PIN_MENU", "CHAT_LIST_PIN_MENU_ITEM", "CHAT_LIST_PIN_MENU_REPORT", "CHAT_LIST_PIN_MENU_DANGER").contains(change.getArea())) {
                return false;
            }
        }
        return true;
    }

    private boolean isControlledPinMenuRequest(String consulta) {
        if (isPinMenuWholeMenuRequest(consulta)) {
            return true;
        }
        String normalized = normalizeSemanticText(consulta);
        return containsAny(normalized,
                "menu desplegable del listado de chats",
                "desplegable del listado de chats",
                "menu de opciones del chat",
                "desplegable de opciones del chat",
                "opciones del chat",
                "contenido del desplegable del chat");
    }

    private boolean isScopedPinMenuRequest(String consulta) {
        String normalized = normalizeSemanticText(consulta);
        return containsAny(normalized,
                "menu desplegable del listado de chats",
                "desplegable del listado de chats",
                "menu de opciones del chat",
                "desplegable de opciones del chat",
                "opciones del chat",
                "contenido del desplegable del chat");
    }

    private List<UiCustomizationChangeDTO> filterPinMenuChanges(List<UiCustomizationChangeDTO> changes) {
        List<UiCustomizationChangeDTO> filtered = new ArrayList<>();
        if (changes == null) {
            return filtered;
        }
        for (UiCustomizationChangeDTO change : changes) {
            if (change == null || !hasText(change.getArea())) {
                continue;
            }
            if (Set.of("CHAT_LIST_PIN_MENU", "CHAT_LIST_PIN_MENU_ITEM", "CHAT_LIST_PIN_MENU_REPORT", "CHAT_LIST_PIN_MENU_DANGER").contains(change.getArea())) {
                filtered.add(change);
            }
        }
        return filtered;
    }

    private List<UiCustomizationChangeDTO> sanitizePinMenuOnlyChanges(List<UiCustomizationChangeDTO> changes) {
        return filterPinMenuChanges(changes);
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
        if (isGroupActiveChatRequest(consulta)) {
            List<UiCustomizationChangeDTO> groupActiveChanges = buildGroupActiveScopedChanges(consulta);
            if (!groupActiveChanges.isEmpty()) {
                logRuleMatch("GROUP_ACTIVE_ITEM", consulta, groupActiveChanges);
                return groupActiveChanges;
            }
        }
        String inferredColor = resolveColorFromText(consulta);
        if (isFullSidebarThemeRequest(consulta)) {
            String sidebarBase = resolveFullSidebarThemeBaseColor(consulta);
            if (hasText(sidebarBase)) {
                parsed.addAll(buildSidebarNavThemeChanges(sidebarBase));
                logRuleMatch("PARSE_SIDEBAR_NAV_THEME", consulta, parsed);
                return parsed;
            }
        }
        if (isSidebarRequest(normalized)) {
            String sidebarPanelBg = firstColorAfter(normalized,
                    "barra lateral", "sidebar", "barra izquierda", "menu lateral", "menu de la izquierda", "navegacion lateral", "panel lateral izquierdo");
            if (hasText(sidebarPanelBg) && containsAny(normalized, "fondo", "background", "modo oscuro", "modo claro", "dark", "light")) {
                parsed.add(change("SIDEBAR_NAV_PANEL", "BACKGROUND_COLOR", sidebarPanelBg));
            }
            String sidebarIconColor = firstColorAfter(normalized,
                    "iconos de la barra lateral", "iconos del menu lateral", "iconos de la sidebar", "iconos blancos", "iconos", "icono");
            if (hasText(sidebarIconColor) && containsAny(normalized, "iconos de la barra lateral", "iconos del menu lateral", "iconos de la sidebar", "barra lateral", "sidebar")) {
                parsed.add(change("SIDEBAR_NAV_ICON", "ICON_COLOR", sidebarIconColor));
            }
            String sidebarItemBg = firstColorAfter(normalized,
                    "botones de la barra lateral", "items de la barra lateral", "opciones de la barra lateral", "iconos con fondo");
            if (hasText(sidebarItemBg) && containsAny(normalized, "botones de la barra lateral", "items de la barra lateral", "opciones de la barra lateral", "iconos con fondo")) {
                parsed.add(change("SIDEBAR_NAV_ITEM", "BACKGROUND_COLOR", sidebarItemBg));
            }
            String sidebarActiveBg = firstColorAfter(normalized,
                    "boton activo de la barra lateral", "icono activo de la barra lateral", "seccion activa", "item seleccionado de la sidebar", "el activo");
            if (hasText(sidebarActiveBg) && containsAny(normalized, "activo", "seleccionado")) {
                parsed.add(change("SIDEBAR_NAV_ITEM_ACTIVE", "BACKGROUND_COLOR", sidebarActiveBg));
            }
            String tooltipBg = firstColorAfter(normalized, "tooltip de la barra lateral", "texto emergente de la sidebar", "etiqueta al pasar el raton");
            if (hasText(tooltipBg) && containsAny(normalized, "tooltip", "texto emergente", "etiqueta")) {
                parsed.add(change("SIDEBAR_NAV_TOOLTIP", "BACKGROUND_COLOR", tooltipBg));
            }
            String tooltipText = firstColorAfter(normalized, "texto", "letra", "color del texto", "color de texto");
            if (hasText(tooltipText) && containsAny(normalized, "tooltip", "texto emergente", "etiqueta") && containsAny(normalized, "texto", "letra")) {
                parsed.add(change("SIDEBAR_NAV_TOOLTIP", "TEXT_COLOR", tooltipText));
            }
            String logoBg = firstColorAfter(normalized, "logo n", "logo de inicio", "n de la barra lateral", "icono nexo de inicio");
            if (hasText(logoBg) && containsAny(normalized, "logo n", "logo de inicio", "n de la barra lateral", "icono nexo de inicio")) {
                parsed.add(change("SIDEBAR_NAV_LOGO", "BACKGROUND_COLOR", logoBg));
            }
            String logoText = firstColorAfter(normalized, "texto", "letra", "color del texto", "color de texto");
            if (hasText(logoText) && containsAny(normalized, "logo n", "logo de inicio", "n de la barra lateral") && containsAny(normalized, "texto", "letra", "blanco", "negro")) {
                parsed.add(change("SIDEBAR_NAV_LOGO", "TEXT_COLOR", logoText));
            }
            String settingsIcon = firstColorAfter(normalized, "ajustes de la barra lateral", "boton de ajustes", "icono de ajustes");
            if (hasText(settingsIcon) && containsAny(normalized, "ajustes", "boton de ajustes", "icono de ajustes")) {
                parsed.add(change("SIDEBAR_NAV_SETTINGS", "ICON_COLOR", settingsIcon));
            }
            String notifBadgeColor = firstColorAfter(normalized,
                    "contador de notificaciones", "badge de notificaciones", "burbuja de notificaciones", "notificaciones de la barra lateral");
            if (hasText(notifBadgeColor) && containsAny(normalized, "contador de notificaciones", "badge de notificaciones", "burbuja de notificaciones")) {
                parsed.add(change("SIDEBAR_NAV_NOTIF_BADGE",
                        containsAny(normalized, "texto", "numero", "numeros", "letra") ? "TEXT_COLOR" : "BACKGROUND_COLOR",
                        notifBadgeColor));
            }
            if (!parsed.isEmpty()) {
                logRuleMatch("PARSE_SIDEBAR_NAV", consulta, parsed);
                return parsed;
            }
        }
        if (!isFullChatListThemeRequest(consulta) && isBothIndividualAndGroupScopeRequest(consulta) && hasText(inferredColor)) {
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
        if (!isFullChatListThemeRequest(consulta) && isGroupAudioPreviewScopedRequest(consulta) && hasText(inferredColor)) {
            parsed.addAll(buildGroupAudioPreviewScopedColorGroup(inferredColor));
            logRuleMatch("PARSE_GROUP_AUDIO_PREVIEW_SCOPED", consulta, parsed);
            return parsed;
        }
        if (!isFullChatListThemeRequest(consulta) && isIndividualAudioPreviewScopedRequest(consulta) && hasText(inferredColor)) {
            parsed.addAll(buildIndividualAudioPreviewScopedColorGroup(inferredColor));
            logRuleMatch("PARSE_INDIVIDUAL_AUDIO_PREVIEW_SCOPED", consulta, parsed);
            return parsed;
        }
        if (!isFullChatListThemeRequest(consulta) && isGroupBadgesScopedRequest(consulta) && hasText(inferredColor)) {
            parsed.addAll(buildGroupBadgesScopedColorGroup(inferredColor));
            logRuleMatch("PARSE_GROUP_BADGES_SCOPED", consulta, parsed);
            return parsed;
        }
        if (!isFullChatListThemeRequest(consulta) && isIndividualFilePreviewRequest(consulta) && hasText(inferredColor)) {
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

    private List<UiCustomizationChangeDTO> buildGroupActiveScopedChanges(String consulta) {
        List<UiCustomizationChangeDTO> changes = new ArrayList<>();
        String normalized = normalizeSemanticText(consulta);
        String background = firstColorAfter(normalized, "fondo", "background", "color de fondo");
        String text = firstColorAfter(normalized, "texto", "textos", "letra", "letras", "color del texto", "color de texto");
        String border = firstColorAfter(normalized, "borde", "bordes", "contorno", "border", "color del borde");

        if (hasText(background)) {
            changes.add(change("CHAT_LIST_ITEM_GROUP_ACTIVE", "BACKGROUND_COLOR", background));
        }
        if (hasText(text)) {
            changes.add(change("CHAT_LIST_ITEM_GROUP_ACTIVE", "TEXT_COLOR", text));
        }
        if (containsAny(normalized, "sin borde", "quita el borde", "quitar borde", "elimina el borde", "sin contorno")) {
            changes.add(change("CHAT_LIST_ITEM_GROUP_ACTIVE", "BORDER_WIDTH", "0px"));
        } else if (hasText(border)) {
            changes.add(change("CHAT_LIST_ITEM_GROUP_ACTIVE", "BORDER_COLOR", border));
        }
        return changes;
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
        if (isScopedPinMenuRequest(consulta)) {
            return false;
        }
        String normalized = normalizeSemanticText(consulta);
        return containsAny(normalized,
                "fondo del listado", "listado de chats", "lista de chats", "panel de chats",
                "zona izquierda de chats", "bloque de chats", "todos los estilos del listado", "estilos visuales del listado");
    }

    private boolean isFullChatListThemeRequest(String consulta) {
        if (isScopedPinMenuRequest(consulta)) {
            return false;
        }
        String normalized = normalizeSemanticText(consulta);
        boolean baseListTheme = containsAny(normalized,
                "todo el listado", "todo el listado de chats", "listado de chats completo",
                "todo el panel de chats", "panel de chats completo", "panel de chats",
                "lista de chats", "lista de chats completa", "todo la lista de chats",
                "theme completo", "tema completo", "aplica modo claro al listado de chats completo");
        boolean coverage = containsAny(normalized,
                "modo claro", "modo oscuro", "incluyendo encabezado", "incluyendo iconos",
                "incluyendo buscador", "incluyendo filtros", "incluyendo chats individuales",
                "incluyendo chats grupales", "incluyendo previews", "incluyendo audios",
                "incluyendo imagenes", "incluyendo imágenes", "incluyendo archivos",
                "incluyendo contadores", "incluyendo badges", "incluyendo desplegables",
                "incluyendo chat activo", "incluyendo chats no leidos", "incluyendo chats no leídos",
                "incluyendo nombres");
        return baseListTheme || (isChatListPanelThemeRequest(consulta) && coverage);
    }

    private boolean isFullSidebarThemeRequest(String consulta) {
        String normalized = normalizeSemanticText(consulta);
        boolean baseSidebarTheme = containsAny(normalized,
                "toda la barra lateral", "barra lateral completa", "sidebar completa", "todo el sidebar",
                "todo el menu lateral", "todo el menu de la barra lateral", "tema azul moderno a toda la sidebar");
        boolean visualTheme = containsAny(normalized,
                "modo oscuro", "modo claro", "dark mode", "light mode", "tema", "estilo", "moderno", "elegante");
        return baseSidebarTheme || (isSidebarRequest(normalized) && visualTheme && containsAny(normalized, "toda", "completa", "todo"));
    }

    private String resolveFullChatListThemeBaseColor(String consulta) {
        String inferredColor = resolveColorFromText(consulta);
        String normalized = normalizeSemanticText(consulta);
        if (!hasText(inferredColor) && containsAny(normalized, "negro", "oscuro", "modo oscuro")) {
            inferredColor = "#111827";
        }
        if (!hasText(inferredColor) && containsAny(normalized, "blanco", "claro", "modo claro")) {
            inferredColor = "#ffffff";
        }
        return inferredColor;
    }

    private String resolveFullSidebarThemeBaseColor(String consulta) {
        String inferredColor = resolveColorFromText(consulta);
        String normalized = normalizeSemanticText(consulta);
        if (!hasText(inferredColor) && containsAny(normalized, "negro", "oscuro", "modo oscuro", "dark mode")) {
            inferredColor = "#111827";
        }
        if (!hasText(inferredColor) && containsAny(normalized, "blanco", "claro", "modo claro", "light mode")) {
            inferredColor = "#ffffff";
        }
        if (!hasText(inferredColor) && containsAny(normalized, "azul")) {
            inferredColor = "#2563eb";
        }
        return inferredColor;
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
                "toda la barra lateral", "sidebar completa", "menu lateral completo",
                "incluyendo previews", "incluyendo desplegable")) {
            return true;
        }
        if ("CHAT_LIST_PANEL".equals(area) || "CHAT_LIST_FILTERS".equals(area) || "CHAT_LIST_PIN_MENU".equals(area)
                || "SIDEBAR_NAV_PANEL".equals(normalizeArea(area))) {
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
        if (isGroupActiveChatRequest(consulta)) {
            LOGGER.info("[AI][UI_CUSTOMIZATION_RULE_MATCH] rule=GROUP_ACTIVE_ITEM area=CHAT_LIST_ITEM_GROUP_ACTIVE consulta={}", safe(consulta));
            return "CHAT_LIST_ITEM_GROUP_ACTIVE";
        }
        if (containsAny(normalized, "chat seleccionado", "chat activo", "item seleccionado")) {
            return "CHAT_LIST_ITEM_ACTIVE";
        }
        if (containsAny(normalized, "chat no leido", "no leidos completos", "fila sin leer")) {
            return "CHAT_LIST_ITEM_UNREAD";
        }
        return currentArea;
    }

    private String resolveChatListSpecificAreaFromConsulta(String consulta, String currentArea) {
        String normalized = normalizeSemanticText(consulta);
        if (containsAny(normalized, "estado vacio del listado", "pantalla sin chats", "mensaje de no hay chats", "sin chats")) {
            return "CHAT_LIST_EMPTY_STATE";
        }
        if (containsAny(normalized, "seccion de chats publicos", "panel de comunidades", "listado de chats publicos")) {
            return "CHAT_LIST_PUBLIC_PANEL";
        }
        if (containsAny(normalized, "tarjeta de chat publico", "tarjetas de chats publicos", "card de comunidad", "boton unirme")) {
            return "CHAT_LIST_PUBLIC_CARD";
        }
        if (containsAny(normalized, "punto de estado del listado", "indicador conectado", "indicador ausente", "estado online del chat", "puntito verde de conectado")) {
            return "CHAT_LIST_STATUS_DOT";
        }
        if (containsAny(normalized, "estrella de favorito", "indicador favorito", "icono de chat favorito")) {
            return "CHAT_LIST_FAVORITE_INDICATOR";
        }
        if (containsAny(normalized, "icono de silenciado", "indicador de chat silenciado", "campana tachada del listado")) {
            return "CHAT_LIST_MUTED_INDICATOR";
        }
        if (containsAny(normalized, "candado de chat cerrado", "indicador de chat cerrado", "texto de chat cerrado")) {
            return "CHAT_LIST_CLOSED_INDICATOR";
        }
        if (containsAny(normalized, "preview de sticker", "sticker del ultimo mensaje", "icono de sticker del listado", "texto sticker en el listado")) {
            if (isStrictGroupOnlyScope(consulta, currentArea)) {
                return "CHAT_LIST_ITEM_GROUP_STICKER_PREVIEW";
            }
            if (isStrictIndividualScope(consulta, currentArea)) {
                return "CHAT_LIST_ITEM_STICKER_PREVIEW";
            }
            return "CHAT_LIST_STICKER_PREVIEW";
        }
        if (containsAny(normalized, "nombre del chat", "texto del nombre del chat", "nombre de la conversacion", "nombre del grupo")) {
            if (isGroupActiveChatRequest(consulta)) {
                return "CHAT_LIST_ITEM_GROUP_ACTIVE";
            }
            if (containsAny(normalized, "seleccionado", "activo")) {
                return containsAny(normalized, "grupo", "grupal") ? "CHAT_LIST_ITEM_GROUP_ACTIVE" : "CHAT_LIST_ITEM_ACTIVE";
            }
            return "CHAT_LIST_ITEM_NAME";
        }
        return currentArea;
    }

    private String resolveChatListSpecificPropertyFromConsulta(String consulta, String area, String currentProperty) {
        String normalizedProperty = normalizeProperty(currentProperty);
        if (hasText(normalizedProperty)) {
            return normalizedProperty;
        }
        String normalizedArea = normalizeArea(area);
        if (!hasText(normalizedArea) || !normalizedArea.startsWith("CHAT_LIST")) {
            return normalizedProperty;
        }
        String normalized = normalizeSemanticText(consulta);
        if (isRemoveBorderRequest(consulta)) {
            return "BORDER_WIDTH";
        }
        if (containsAny(normalized, "grosor", "ancho", "width")) {
            return "WIDTH";
        }
        if (containsAny(normalized, "alto", "height")) {
            return "HEIGHT";
        }
        if (containsAny(normalized, "separacion", "espaciado", "gap")) {
            return "GAP";
        }
        if (containsAny(normalized, "opacidad", "opacity")) {
            return "OPACITY";
        }
        if (containsAny(normalized, "sombra", "shadow")) {
            return "SHADOW_PRESET";
        }
        if (containsAny(normalized, "negrita", "peso", "font weight")) {
            return "FONT_WEIGHT";
        }
        if (containsAny(normalized, "tamano", "tamaño", "fuente", "letra")) {
            return "FONT_SIZE";
        }
        if (containsAny(normalized, "borde", "contorno", "border")) {
            return "BORDER_COLOR";
        }
        if (containsAny(normalized, "icono", "estrella", "campana", "candado")) {
            return "ICON_COLOR";
        }
        if (containsAny(normalized, "texto", "nombre", "letra", "hora", "fecha")) {
            return "TEXT_COLOR";
        }
        if ("CHAT_LIST_STATUS_DOT".equals(normalizedArea)) {
            return "BACKGROUND_COLOR";
        }
        if ("CHAT_LIST_PUBLIC_CARD".equals(normalizedArea) && containsAny(normalized, "hover")) {
            return "HOVER_BACKGROUND_COLOR";
        }
        if (Set.of("CHAT_LIST_ITEM_NAME", "CHAT_LIST_ITEM_ACTIVE", "CHAT_LIST_ITEM_GROUP_ACTIVE").contains(normalizedArea)) {
            return "TEXT_COLOR";
        }
        if (Set.of("CHAT_LIST_MUTED_INDICATOR", "CHAT_LIST_FAVORITE_INDICATOR", "CHAT_LIST_CLOSED_INDICATOR").contains(normalizedArea)) {
            return "ICON_COLOR";
        }
        if (Set.of("CHAT_LIST_STICKER_PREVIEW", "CHAT_LIST_ITEM_STICKER_PREVIEW", "CHAT_LIST_ITEM_GROUP_STICKER_PREVIEW").contains(normalizedArea)) {
            return "TEXT_COLOR";
        }
        if (Set.of("CHAT_LIST_EMPTY_STATE", "CHAT_LIST_PUBLIC_PANEL", "CHAT_LIST_PUBLIC_CARD").contains(normalizedArea)) {
            return "BACKGROUND_COLOR";
        }
        return normalizedProperty;
    }

    private String resolveSidebarAreaFromConsulta(String consulta, String currentArea) {
        String normalized = normalizeSemanticText(consulta);
        if (!isSidebarRequest(normalized) && !isSidebarArea(currentArea)) {
            return currentArea;
        }
        if (containsAny(normalized, "contador de notificaciones", "badge de notificaciones", "burbuja de notificaciones", "notificaciones de la barra lateral")) {
            LOGGER.info("[AI][UI_CUSTOMIZATION_RULE_MATCH] rule=SIDEBAR_NAV_NOTIF_BADGE consulta={}", safe(consulta));
            return "SIDEBAR_NAV_NOTIF_BADGE";
        }
        if (containsAny(normalized, "ajustes de la barra lateral", "boton de ajustes", "icono de ajustes")) {
            LOGGER.info("[AI][UI_CUSTOMIZATION_RULE_MATCH] rule=SIDEBAR_NAV_SETTINGS consulta={}", safe(consulta));
            return "SIDEBAR_NAV_SETTINGS";
        }
        if (containsAny(normalized, "indicador activo", "linea activa", "marca activa", "puntito activo")) {
            LOGGER.info("[AI][UI_CUSTOMIZATION_RULE_MATCH] rule=SIDEBAR_NAV_ACTIVE_INDICATOR consulta={}", safe(consulta));
            return "SIDEBAR_NAV_ACTIVE_INDICATOR";
        }
        if (containsAny(normalized, "tooltip de la barra lateral", "texto emergente de la sidebar", "etiqueta al pasar el raton")) {
            LOGGER.info("[AI][UI_CUSTOMIZATION_RULE_MATCH] rule=SIDEBAR_NAV_TOOLTIP consulta={}", safe(consulta));
            return "SIDEBAR_NAV_TOOLTIP";
        }
        if (containsAny(normalized, "avatar de la barra lateral", "foto de perfil de la barra lateral", "perfil de la barra lateral")) {
            LOGGER.info("[AI][UI_CUSTOMIZATION_RULE_MATCH] rule=SIDEBAR_NAV_AVATAR consulta={}", safe(consulta));
            return "SIDEBAR_NAV_AVATAR";
        }
        if (containsAny(normalized, "nexo ia", "icono de nexo ia", "boton de nexo ia", "icono svg de nexo ia")) {
            LOGGER.info("[AI][UI_CUSTOMIZATION_RULE_MATCH] rule=SIDEBAR_NAV_AI_ICON consulta={}", safe(consulta));
            return "SIDEBAR_NAV_AI_ICON";
        }
        if (containsAny(normalized, "logo n", "logo de inicio", "n de la barra lateral", "icono nexo de inicio")) {
            LOGGER.info("[AI][UI_CUSTOMIZATION_RULE_MATCH] rule=SIDEBAR_NAV_LOGO consulta={}", safe(consulta));
            return "SIDEBAR_NAV_LOGO";
        }
        if (containsAny(normalized, "parte inferior de la barra lateral", "zona inferior de la sidebar", "perfil y ajustes")) {
            LOGGER.info("[AI][UI_CUSTOMIZATION_RULE_MATCH] rule=SIDEBAR_NAV_BOTTOM consulta={}", safe(consulta));
            return "SIDEBAR_NAV_BOTTOM";
        }
        if (containsAny(normalized, "grupo superior", "iconos superiores", "zona superior de la barra lateral")) {
            LOGGER.info("[AI][UI_CUSTOMIZATION_RULE_MATCH] rule=SIDEBAR_NAV_GROUP consulta={}", safe(consulta));
            return "SIDEBAR_NAV_GROUP";
        }
        if (containsAny(normalized, "iconos activos", "iconos seleccionados")) {
            LOGGER.info("[AI][UI_CUSTOMIZATION_RULE_MATCH] rule=SIDEBAR_NAV_ICON_ACTIVE consulta={}", safe(consulta));
            return "SIDEBAR_NAV_ICON_ACTIVE";
        }
        if (containsAny(normalized, "boton activo de la barra lateral", "icono activo de la barra lateral", "seccion activa", "item seleccionado de la sidebar")) {
            LOGGER.info("[AI][UI_CUSTOMIZATION_RULE_MATCH] rule=SIDEBAR_NAV_ITEM_ACTIVE consulta={}", safe(consulta));
            return "SIDEBAR_NAV_ITEM_ACTIVE";
        }
        if (containsAny(normalized, "botones de la barra lateral", "items de la barra lateral", "opciones de la barra lateral", "iconos con fondo")) {
            LOGGER.info("[AI][UI_CUSTOMIZATION_RULE_MATCH] rule=SIDEBAR_NAV_ITEM consulta={}", safe(consulta));
            return "SIDEBAR_NAV_ITEM";
        }
        if (containsAny(normalized, "iconos de la barra lateral", "iconos del menu lateral", "iconos de la sidebar")) {
            LOGGER.info("[AI][UI_CUSTOMIZATION_RULE_MATCH] rule=SIDEBAR_NAV_ICON consulta={}", safe(consulta));
            return "SIDEBAR_NAV_ICON";
        }
        if (containsAny(normalized, "barra lateral", "sidebar", "barra izquierda", "menu lateral", "menu de la izquierda", "navegacion lateral", "panel lateral izquierdo")) {
            LOGGER.info("[AI][UI_CUSTOMIZATION_RULE_MATCH] rule=SIDEBAR_NAV_PANEL consulta={}", safe(consulta));
            return "SIDEBAR_NAV_PANEL";
        }
        return normalizeArea(currentArea);
    }

    private String resolveSidebarPropertyFromConsulta(String consulta, String area, String currentProperty) {
        String normalizedProperty = normalizeProperty(currentProperty);
        if (hasText(normalizedProperty)) {
            return normalizedProperty;
        }
        if (!isSidebarArea(area)) {
            return normalizedProperty;
        }
        String normalized = normalizeSemanticText(consulta);
        if (isRemoveBorderRequest(consulta)) {
            return "BORDER_WIDTH";
        }
        if (containsAny(normalized, "ancho", "width")) {
            return "WIDTH";
        }
        if (containsAny(normalized, "opacidad", "opacity")) {
            return "OPACITY";
        }
        if (containsAny(normalized, "sombra", "shadow")) {
            return "SHADOW_PRESET";
        }
        if (containsAny(normalized, "tamano", "tamaño", "fuente", "letra") && "SIDEBAR_NAV_LOGO".equals(normalizeArea(area))) {
            return "FONT_SIZE";
        }
        if (containsAny(normalized, "borde", "contorno", "border")) {
            return "BORDER_COLOR";
        }
        if (containsAny(normalized, "texto", "letra", "label")) {
            return "TEXT_COLOR";
        }
        if ("SIDEBAR_NAV_ICON".equals(normalizeArea(area))
                || "SIDEBAR_NAV_ICON_ACTIVE".equals(normalizeArea(area))
                || "SIDEBAR_NAV_SETTINGS".equals(normalizeArea(area))) {
            return "ICON_COLOR";
        }
        if ("SIDEBAR_NAV_ACTIVE_INDICATOR".equals(normalizeArea(area))) {
            return "BACKGROUND_COLOR";
        }
        if ("SIDEBAR_NAV_AVATAR".equals(normalizeArea(area)) && containsAny(normalized, "avatar", "perfil")) {
            return "BACKGROUND_COLOR";
        }
        if ("SIDEBAR_NAV_NOTIF_BADGE".equals(normalizeArea(area))) {
            if (containsAny(normalized, "texto", "numero", "numeros", "letra")) {
                return "TEXT_COLOR";
            }
            if (containsAny(normalized, "borde", "contorno", "border")) {
                return "BORDER_COLOR";
            }
            return "BACKGROUND_COLOR";
        }
        if ("SIDEBAR_NAV_TOOLTIP".equals(normalizeArea(area)) && containsAny(normalized, "tooltip", "texto emergente", "etiqueta")) {
            return containsAny(normalized, "texto", "letra") ? "TEXT_COLOR" : "BACKGROUND_COLOR";
        }
        return "BACKGROUND_COLOR";
    }

    private boolean isSidebarRequest(String normalized) {
        return isSidebarNavRequest(normalized)
                || containsAny(normalized, "panel lateral izquierdo", "menu de la izquierda", "iconos superiores", "perfil y ajustes", "logo n", "nexo ia");
    }

    private boolean isSidebarArea(String area) {
        String normalizedArea = normalizeArea(area);
        return normalizedArea != null && normalizedArea.startsWith("SIDEBAR_NAV");
    }

    private SidebarRepair repairSidebarNavIntent(String semantic,
                                                 String action,
                                                 String area,
                                                 String property,
                                                 String value,
                                                 String valuePreset,
                                                 List<UiCustomizationChangeDTO> inputChanges,
                                                 Boolean needsClarification) {
        if (!isSidebarNavRequest(semantic)) {
            return new SidebarRepair(action, area, property, value, valuePreset, inputChanges, needsClarification);
        }
        LOGGER.info("[AI][UI_CUSTOMIZATION_CLARIFICATION_SKIPPED] reason=SIDEBAR_NAV_SCOPE semantic={}", safe(semantic));
        List<UiCustomizationChangeDTO> sidebarChanges = parseDeterministicMultiChanges(semantic);
        if (areAllSidebarChanges(sidebarChanges) && sidebarChanges.size() > 1) {
            return new SidebarRepair("UPDATE_STYLE_MULTI", null, null, null, null, sidebarChanges, Boolean.FALSE);
        }

        String repairedArea = resolveSidebarAreaFromConsulta(semantic, area);
        if (hasText(repairedArea) && !repairedArea.equals(normalizeArea(area))) {
            LOGGER.info("[AI][UI_CUSTOMIZATION_AREA_REPAIRED] from={} to={} reason=SIDEBAR_NAV_SCOPE",
                    safe(area), safe(repairedArea));
        }
        String repairedProperty = resolveSidebarPropertyFromConsulta(semantic, repairedArea, property);
        if (hasText(repairedArea) && hasText(repairedProperty)) {
            LOGGER.info("[AI][UI_CUSTOMIZATION_RULE_MATCH] rule={} property={}", safe(repairedArea), safe(repairedProperty));
        }
        String repairedValue = value;
        if (!hasText(repairedValue)
                && Set.of("BACKGROUND_COLOR", "TEXT_COLOR", "BORDER_COLOR", "ICON_COLOR", "HOVER_BACKGROUND_COLOR").contains(normalizeProperty(repairedProperty))) {
            repairedValue = resolveColorFromText(semantic);
        }
        String repairedAction = action;
        boolean resolvedSidebarPayload = hasText(repairedArea) && hasText(repairedProperty) && hasText(repairedValue);
        if ((Boolean.TRUE.equals(needsClarification) || "NEEDS_CLARIFICATION".equals(normalizeUpper(action))) && resolvedSidebarPayload) {
            repairedAction = "UPDATE_STYLE";
        }
        return new SidebarRepair(repairedAction, repairedArea, repairedProperty, repairedValue, valuePreset, inputChanges,
                resolvedSidebarPayload ? Boolean.FALSE : needsClarification);
    }

    private boolean isSidebarNavRequest(String semantic) {
        String normalized = normalizeSemanticText(semantic);
        return containsAny(normalized,
                "barra lateral", "sidebar", "barra izquierda", "menu lateral", "menu de la izquierda", "panel lateral",
                "panel de la barra lateral", "navegacion lateral", "sidebar nav");
    }

    private boolean areAllSidebarChanges(List<UiCustomizationChangeDTO> changes) {
        if (changes == null || changes.isEmpty()) {
            return false;
        }
        for (UiCustomizationChangeDTO change : changes) {
            if (change == null || !isSidebarArea(change.getArea())) {
                return false;
            }
        }
        return true;
    }

    private record SidebarRepair(String action,
                                 String area,
                                 String property,
                                 String value,
                                 String valuePreset,
                                 List<UiCustomizationChangeDTO> changes,
                                 Boolean needsClarification) {}

    private boolean isGroupActiveChatRequest(String consulta) {
        String normalized = normalizeSemanticText(consulta);
        boolean mentionsGroup = containsAny(normalized, "grupal", "grupales", "grupo", "grupos");
        boolean mentionsActive = containsAny(normalized,
                "activo", "activos", "seleccionado", "seleccionados", "pulsado", "pulsados", "marcado", "marcados");
        boolean mentionsChat = containsAny(normalized, "chat", "chats", "item", "items", "fila", "filas");
        return mentionsGroup && mentionsActive && (mentionsChat || mentionsGroup);
    }

    private String semanticSource(String consulta, String label) {
        String q = consulta == null ? "" : consulta;
        String l = label == null ? "" : label;
        return (q + " " + l).trim();
    }

    private String resolvePinMenuAreaFromConsulta(String consulta, String currentArea) {
        String normalized = normalizeSemanticText(consulta);
        if (containsAny(normalized,
                "desplegable de mensajes", "menu de mensajes", "menu del mensaje",
                "opciones de mensajes", "opciones del mensaje", "menu de opciones de mensajes")) {
            return "MESSAGE_OPTIONS_DROPDOWN";
        }
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

    private String resolveChatItemDateAreaFromConsulta(String consulta, String currentArea) {
        String normalized = normalizeSemanticText(consulta);
        if (containsAny(normalized,
                "fecha del ultimo mensaje",
                "hora del ultimo mensaje",
                "ultimafecha",
                "fecha del chat",
                "hora del chat",
                "timestamp del listado de chats",
                "fecha que aparece a la derecha del chat",
                "hora que aparece a la derecha del chat")) {
            return "CHAT_LIST_ITEM_DATE";
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
        if (isFullChatListThemeRequest(consulta)) {
            return false;
        }
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
        if (isFullChatListThemeRequest(consulta)) {
            return false;
        }
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
        String p = normalizeProperty(property);
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
        if ("WIDTH".equals(p) && isPxValue(v)) {
            int requested = parseFontPx(v);
            int[] allowed = new int[]{1, 2, 3, 4, 6, 8};
            int nearest = allowed[0];
            int delta = Integer.MAX_VALUE;
            for (int candidate : allowed) {
                int distance = Math.abs(candidate - requested);
                if (distance < delta) {
                    delta = distance;
                    nearest = candidate;
                }
            }
            String applied = nearest + "px";
            if (!applied.equals(v.trim().toLowerCase(Locale.ROOT))) {
                v = applied;
                reason = "WIDTH_NORMALIZE";
                maxAllowed = "8px";
                minAllowed = "1px";
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
        String normalizedArea = normalizeArea(area);
        String normalizedProperty = normalizeProperty(property);
        if ("SIDEBAR_NAV_PANEL".equals(normalizedArea)) {
            return Set.of("BACKGROUND_COLOR", "TEXT_COLOR", "BORDER_COLOR", "BORDER_WIDTH", "BORDER_RADIUS", "SHADOW_PRESET").contains(normalizedProperty);
        }
        if ("SIDEBAR_NAV_GROUP".equals(normalizedArea)) {
            return Set.of("BACKGROUND_COLOR", "BORDER_COLOR").contains(normalizedProperty);
        }
        if ("SIDEBAR_NAV_BOTTOM".equals(normalizedArea)) {
            return Set.of("BACKGROUND_COLOR", "BORDER_COLOR").contains(normalizedProperty);
        }
        if ("SIDEBAR_NAV_ITEM".equals(normalizedArea)) {
            return Set.of("BACKGROUND_COLOR", "TEXT_COLOR", "ICON_COLOR", "BORDER_COLOR", "BORDER_WIDTH", "BORDER_RADIUS",
                    "HOVER_BACKGROUND_COLOR", "HOVER_TEXT_COLOR", "HOVER_ICON_COLOR").contains(normalizedProperty);
        }
        if ("SIDEBAR_NAV_ITEM_ACTIVE".equals(normalizedArea)) {
            return Set.of("BACKGROUND_COLOR", "TEXT_COLOR", "ICON_COLOR", "BORDER_COLOR", "BORDER_WIDTH", "BORDER_RADIUS").contains(normalizedProperty);
        }
        if ("SIDEBAR_NAV_ACTIVE_INDICATOR".equals(normalizedArea)) {
            return Set.of("BACKGROUND_COLOR", "BORDER_COLOR", "WIDTH").contains(normalizedProperty);
        }
        if ("SIDEBAR_NAV_LOGO".equals(normalizedArea)) {
            return Set.of("BACKGROUND_COLOR", "TEXT_COLOR", "BORDER_COLOR", "BORDER_RADIUS", "FONT_SIZE", "SHADOW_PRESET").contains(normalizedProperty);
        }
        if ("SIDEBAR_NAV_ICON".equals(normalizedArea)) {
            return Set.of("ICON_COLOR", "TEXT_COLOR").contains(normalizedProperty);
        }
        if ("SIDEBAR_NAV_ICON_ACTIVE".equals(normalizedArea)) {
            return Set.of("ICON_COLOR", "TEXT_COLOR").contains(normalizedProperty);
        }
        if ("SIDEBAR_NAV_AI_ICON".equals(normalizedArea)) {
            return Set.of("BACKGROUND_COLOR", "BORDER_COLOR", "BORDER_RADIUS", "OPACITY").contains(normalizedProperty);
        }
        if ("SIDEBAR_NAV_TOOLTIP".equals(normalizedArea)) {
            return Set.of("BACKGROUND_COLOR", "TEXT_COLOR", "BORDER_COLOR", "BORDER_RADIUS", "SHADOW_PRESET").contains(normalizedProperty);
        }
        if ("SIDEBAR_NAV_AVATAR".equals(normalizedArea)) {
            return Set.of("BACKGROUND_COLOR", "TEXT_COLOR", "BORDER_COLOR", "BORDER_WIDTH", "BORDER_RADIUS").contains(normalizedProperty);
        }
        if ("SIDEBAR_NAV_NOTIF_BADGE".equals(normalizedArea)) {
            return Set.of("BACKGROUND_COLOR", "TEXT_COLOR", "BORDER_COLOR", "BORDER_WIDTH", "BORDER_RADIUS", "FONT_SIZE", "FONT_WEIGHT").contains(normalizedProperty);
        }
        if ("SIDEBAR_NAV_SETTINGS".equals(normalizedArea)) {
            return Set.of("BACKGROUND_COLOR", "TEXT_COLOR", "ICON_COLOR", "HOVER_BACKGROUND_COLOR").contains(normalizedProperty);
        }
        if ("CHAT_LIST_ITEM".equals(normalizedArea) || "CHAT_LIST_ITEM_GROUP".equals(normalizedArea) || "CHAT_LIST_ITEM_UNREAD".equals(normalizedArea)) {
            return Set.of("BACKGROUND_COLOR", "TEXT_COLOR", "BORDER_COLOR", "BORDER_WIDTH", "BORDER_RADIUS", "HOVER_BACKGROUND_COLOR").contains(normalizedProperty);
        }
        if ("CHAT_LIST_ITEM_ACTIVE".equals(normalizedArea)) {
            return Set.of("BACKGROUND_COLOR", "TEXT_COLOR", "BORDER_COLOR", "BORDER_WIDTH", "BORDER_RADIUS", "HOVER_BACKGROUND_COLOR").contains(normalizedProperty);
        }
        if ("CHAT_LIST_ITEM_GROUP_ACTIVE".equals(normalizedArea)) {
            return Set.of("BACKGROUND_COLOR", "TEXT_COLOR", "BORDER_COLOR", "BORDER_WIDTH", "BORDER_RADIUS", "HOVER_BACKGROUND_COLOR").contains(normalizedProperty);
        }
        if ("CHAT_LIST_ITEM_NAME".equals(normalizedArea) || "CHAT_LIST_ITEM_NAME_SCOPED".equals(normalizedArea) || "CHAT_LIST_ITEM_GROUP_NAME".equals(normalizedArea)) {
            return Set.of("TEXT_COLOR", "FONT_SIZE", "FONT_WEIGHT").contains(normalizedProperty);
        }
        if ("CHAT_LIST_STATUS_DOT".equals(normalizedArea)) {
            return Set.of("BACKGROUND_COLOR", "BORDER_COLOR", "WIDTH", "HEIGHT", "OPACITY").contains(normalizedProperty);
        }
        if ("CHAT_LIST_ITEM_GROUP_PREVIEW".equals(normalizedArea)) {
            return Set.of("TEXT_COLOR", "PREVIEW_SENDER_TEXT_COLOR", "FONT_SIZE").contains(normalizedProperty);
        }
        if ("CHAT_LIST_ITEM_GROUP_AUDIO_PREVIEW".equals(normalizedArea)) {
            return Set.of("BACKGROUND_COLOR", "TEXT_COLOR", "ICON_COLOR", "BORDER_COLOR", "LABEL_COLOR", "SEPARATOR_COLOR", "TIME_COLOR").contains(normalizedProperty);
        }
        if ("CHAT_LIST_ITEM_GROUP_FILE_PREVIEW".equals(normalizedArea)) {
            return Set.of("BACKGROUND_COLOR", "TEXT_COLOR", "ICON_COLOR", "BORDER_COLOR").contains(normalizedProperty);
        }
        if ("CHAT_LIST_ITEM_GROUP_IMAGE_PREVIEW".equals(normalizedArea)) {
            return Set.of("BACKGROUND_COLOR", "TEXT_COLOR", "ICON_COLOR", "BORDER_COLOR").contains(normalizedProperty);
        }
        if ("CHAT_LIST_STICKER_PREVIEW".equals(normalizedArea)
                || "CHAT_LIST_ITEM_STICKER_PREVIEW".equals(normalizedArea)
                || "CHAT_LIST_ITEM_GROUP_STICKER_PREVIEW".equals(normalizedArea)) {
            return Set.of("BACKGROUND_COLOR", "TEXT_COLOR", "ICON_COLOR", "BORDER_COLOR", "BORDER_RADIUS", "FONT_SIZE", "OPACITY", "GAP").contains(normalizedProperty);
        }
        if ("CHAT_LIST_ITEM_GROUP_DRAFT_PREVIEW".equals(normalizedArea)) {
            return Set.of("TEXT_COLOR", "LABEL_COLOR").contains(normalizedProperty);
        }
        if ("CHAT_LIST_ITEM_GROUP_BADGES".equals(normalizedArea)) {
            return Set.of("BACKGROUND_COLOR", "TEXT_COLOR", "BORDER_COLOR", "BADGE_COLOR").contains(normalizedProperty);
        }
        if ("CHAT_LIST_ITEM_GROUP_ACTIONS".equals(normalizedArea)) {
            return Set.of("ICON_COLOR", "HOVER_BACKGROUND_COLOR", "TEXT_COLOR").contains(normalizedProperty);
        }
        if ("CHAT_LIST_ITEM_GROUP_STATUS_PILLS".equals(normalizedArea)) {
            return Set.of("REPORTED_BACKGROUND_COLOR", "REPORTED_TEXT_COLOR", "BLOCKED_BACKGROUND_COLOR", "BLOCKED_TEXT_COLOR", "BORDER_COLOR").contains(normalizedProperty);
        }
        if ("CHAT_LIST_MUTED_INDICATOR".equals(normalizedArea)
                || "CHAT_LIST_FAVORITE_INDICATOR".equals(normalizedArea)
                || "CHAT_LIST_CLOSED_INDICATOR".equals(normalizedArea)) {
            return Set.of("BACKGROUND_COLOR", "TEXT_COLOR", "ICON_COLOR", "BORDER_COLOR", "BORDER_RADIUS").contains(normalizedProperty);
        }
        if ("CHAT_LIST_EMPTY_STATE".equals(normalizedArea) || "CHAT_LIST_PUBLIC_PANEL".equals(normalizedArea)) {
            return Set.of("BACKGROUND_COLOR", "TEXT_COLOR", "BORDER_COLOR", "BORDER_RADIUS", "SHADOW_PRESET").contains(normalizedProperty);
        }
        if ("CHAT_LIST_PUBLIC_CARD".equals(normalizedArea)) {
            return Set.of("BACKGROUND_COLOR", "TEXT_COLOR", "BORDER_COLOR", "BORDER_RADIUS", "HOVER_BACKGROUND_COLOR").contains(normalizedProperty);
        }
        if ("CHAT_LIST_ITEM_DATE".equals(normalizedArea)) {
            return Set.of("TEXT_COLOR", "FONT_SIZE", "FONT_WEIGHT").contains(normalizedProperty);
        }
        if ("CHAT_LIST_HEADER".equals(normalizedArea)) {
            return Set.of("BACKGROUND_COLOR", "TEXT_COLOR", "ICON_COLOR", "BORDER_COLOR", "BORDER_RADIUS", "SHADOW_PRESET").contains(normalizedProperty);
        }
        if ("CHAT_LIST_TITLE".equals(normalizedArea)) {
            return Set.of("TEXT_COLOR", "FONT_SIZE", "FONT_WEIGHT", "ICON_COLOR").contains(normalizedProperty);
        }
        if ("CHAT_LIST_HEADER_ACTIONS".equals(normalizedArea)) {
            return Set.of("BACKGROUND_COLOR", "TEXT_COLOR", "ICON_COLOR", "GAP", "BORDER_COLOR").contains(normalizedProperty);
        }
        if ("CHAT_LIST_HEADER_ICON_BUTTON".equals(normalizedArea)) {
            return Set.of("BACKGROUND_COLOR", "TEXT_COLOR", "ICON_COLOR", "BORDER_COLOR", "BORDER_RADIUS", "HOVER_BACKGROUND_COLOR", "ACTIVE_BACKGROUND_COLOR", "SHADOW_PRESET").contains(normalizedProperty);
        }
        if ("CHAT_LIST_HEADER_ICON".equals(normalizedArea)) {
            return Set.of("ICON_COLOR", "TEXT_COLOR", "FONT_SIZE").contains(normalizedProperty);
        }
        if ("CHAT_LIST_HEADER_MENU".equals(normalizedArea)) {
            return Set.of("BACKGROUND_COLOR", "TEXT_COLOR", "BORDER_COLOR", "BORDER_RADIUS", "SHADOW_PRESET").contains(normalizedProperty);
        }
        if ("CHAT_LIST_HEADER_MENU_ITEM".equals(normalizedArea)) {
            return Set.of("BACKGROUND_COLOR", "TEXT_COLOR", "ICON_COLOR", "HOVER_BACKGROUND_COLOR", "BORDER_RADIUS", "FONT_SIZE").contains(normalizedProperty);
        }
        return true;
    }

    private record AreaProperty(String area, String property) {}

    private record NormalizedInput(String property, String value, String valuePreset, String reason, String maxAllowedValue, String minAllowedValue) {}

    private void logScopeResult(UiCustomizationScopeDTO scope) {
        if (scope == null) {
            return;
        }
        LOGGER.info("[AI][UI_SCOPE_RESULT] module={} element={} chatType={} state={} subElement={}",
                safe(scope.getModule()),
                safe(scope.getElement()),
                safe(scope.getChatType()),
                safe(scope.getState()),
                safe(scope.getSubElement()));
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

    private List<UiCustomizationChangeDTO> applyResolvedAreaToChanges(List<UiCustomizationChangeDTO> inputChanges, String resolvedArea) {
        if (!hasText(resolvedArea) || inputChanges == null || inputChanges.isEmpty()) {
            return inputChanges;
        }
        List<UiCustomizationChangeDTO> normalized = new ArrayList<>();
        for (UiCustomizationChangeDTO change : inputChanges) {
            if (change == null) {
                continue;
            }
            UiCustomizationChangeDTO copy = new UiCustomizationChangeDTO();
            copy.setArea(hasText(change.getArea()) ? change.getArea() : resolvedArea);
            copy.setProperty(change.getProperty());
            copy.setValue(change.getValue());
            copy.setValuePreset(change.getValuePreset());
            normalized.add(copy);
        }
        return normalized.isEmpty() ? inputChanges : normalized;
    }

    private boolean hasResolvableVisualPayload(String property,
                                              String value,
                                              List<UiCustomizationChangeDTO> inputChanges) {
        return (inputChanges != null && !inputChanges.isEmpty())
                || (hasText(property) && hasText(value));
    }

    private String resolveActionFromPayload(String property,
                                            String value,
                                            List<UiCustomizationChangeDTO> inputChanges) {
        if (inputChanges != null && inputChanges.size() > 1) {
            return "UPDATE_STYLE_MULTI";
        }
        if (inputChanges != null && inputChanges.size() == 1) {
            return "UPDATE_STYLE";
        }
        return hasText(property) && hasText(value) ? "UPDATE_STYLE" : "UPDATE_STYLE";
    }

    private boolean isPinMenuArea(String area) {
        return Set.of("CHAT_LIST_PIN_MENU", "CHAT_LIST_PIN_MENU_ITEM", "CHAT_LIST_PIN_MENU_REPORT", "CHAT_LIST_PIN_MENU_DANGER")
                .contains(normalizeUpper(area));
    }

    private String normalizeUpper(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    private record ActionNormalization(String action,
                                       String area,
                                       String property,
                                       String value,
                                       List<UiCustomizationChangeDTO> changes,
                                       boolean normalized) {}

    private ActionNormalization normalizeSingleAreaGroupAction(String action,
                                                               String area,
                                                               String property,
                                                               String value,
                                                               List<UiCustomizationChangeDTO> changes) {
        if (!"UPDATE_STYLE_GROUP".equals(action)) {
            return new ActionNormalization(action, area, property, value, changes, false);
        }

        boolean hasChanges = changes != null && !changes.isEmpty();
        if (!hasChanges && isSimpleAreaPropertyValue(area, property, value)) {
            LOGGER.info("[AI][UI_CUSTOMIZATION_ACTION_NORMALIZED] from=UPDATE_STYLE_GROUP to=UPDATE_STYLE reason=SINGLE_AREA_PROPERTY_VALUE area={} property={}",
                    safe(area), safe(property));
            return new ActionNormalization("UPDATE_STYLE", area, property, value, changes, true);
        }

        if (!hasChanges) {
            return new ActionNormalization(action, area, property, value, changes, false);
        }

        List<UiCustomizationChangeDTO> normalizedChanges = new ArrayList<>();
        String scopedArea = hasText(area) ? area : null;
        for (UiCustomizationChangeDTO change : changes) {
            UiCustomizationChangeDTO normalizedChange = normalizeGroupChange(change);
            if (!isValidGroupChange(normalizedChange)) {
                return new ActionNormalization(action, area, property, value, changes, false);
            }
            if (scopedArea == null) {
                scopedArea = normalizedChange.getArea();
            }
            if (!scopedArea.equals(normalizedChange.getArea())) {
                return new ActionNormalization(action, area, property, value, changes, false);
            }
            normalizedChanges.add(normalizedChange);
        }

        if (scopedArea != null && !ALLOWED_AREAS.contains(scopedArea)) {
            return new ActionNormalization(action, area, property, value, changes, false);
        }

        LOGGER.info("[AI][UI_CUSTOMIZATION_ACTION_NORMALIZED] from=UPDATE_STYLE_GROUP to=UPDATE_STYLE_MULTI reason=SINGLE_AREA_VALID_CHANGES area={} property={}",
                safe(scopedArea), safe(property));
        return new ActionNormalization("UPDATE_STYLE_MULTI", null, null, null, normalizedChanges, true);
    }

    private boolean isSimpleAreaPropertyValue(String area, String property, String value) {
        if (!hasText(area) || !hasText(property) || !hasText(value)) {
            return false;
        }
        if (!ALLOWED_AREAS.contains(area) || !ALLOWED_PROPERTIES.contains(property)) {
            return false;
        }
        if (!isPropertyAllowedForArea(area, property)) {
            return false;
        }
        return hasText(resolveValue(property, value, null));
    }

    private String safe(String value) {
        return value == null ? "" : value.replaceAll("\\s+", " ").trim();
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}



