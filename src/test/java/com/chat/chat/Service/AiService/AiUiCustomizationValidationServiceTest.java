package com.chat.chat.Service.AiService;

import com.chat.chat.DTO.AiUiCustomizationResponseDTO;
import com.chat.chat.DTO.UiCustomizationScopeDTO;
import com.chat.chat.DTO.UiCustomizationChangeDTO;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AiUiCustomizationValidationServiceTest {

    private final AiUiCustomizationValidationService service = new AiUiCustomizationValidationService();

    @Test
    void validate_acceptsAllowedColorCustomization() {
        AiUiCustomizationResponseDTO response = service.validate(
                "req-1",
                "pon rojo el fondo del chat",
                "UPDATE_STYLE",
                "CHAT_MESSAGES_AREA",
                "BACKGROUND_COLOR",
                "#ef4444",
                null,
                "Cambiar fondo del chat a rojo",
                0.94d,
                null,
                null,
                null
        );

        assertTrue(response.isSuccess());
        assertEquals("UI_CUSTOMIZATION_OK", response.getCodigo());
        assertEquals("#ef4444", response.getValue());
    }

    @Test
    void validate_resolvesAreaFromStructuredScope() {
        UiCustomizationScopeDTO scope = new UiCustomizationScopeDTO();
        scope.setModule("CHAT_LIST");
        scope.setElement("CHAT_ITEM");
        scope.setChatType("GROUP");
        scope.setState("ACTIVE");
        List<UiCustomizationChangeDTO> changes = List.of(
                areaLessChange("BACKGROUND_COLOR", "#d8b4fe"),
                areaLessChange("TEXT_COLOR", "#ffffff"),
                areaLessChange("BORDER_COLOR", "#facc15")
        );

        AiUiCustomizationResponseDTO response = service.validate(
                "req-scope-1",
                "Pon el chat grupal seleccionado con fondo morado, texto blanco y borde amarillo",
                "NEEDS_CLARIFICATION",
                null,
                null,
                null,
                null,
                "Cambiar chat grupal seleccionado",
                0.95d,
                null,
                scope,
                true,
                "AREA_SCOPE_PENDING",
                "¿Qué zona quieres cambiar?",
                changes,
                null
        );

        assertTrue(response.isSuccess());
        assertEquals("UPDATE_STYLE_MULTI", response.getAction());
        assertTrue(response.getChanges().stream().allMatch(c -> "CHAT_LIST_ITEM_GROUP_ACTIVE".equals(c.getArea())));
    }

    @Test
    void validate_repairsPluralGroupActiveBeforeClarification() {
        AiUiCustomizationResponseDTO response = service.validate(
                "req-scope-2",
                "Poner fondo azul muy suave en chats grupales activos y textos en rojo",
                "NEEDS_CLARIFICATION",
                null,
                null,
                null,
                null,
                "Fondo azul muy suave y texto rojo en chats grupales activos",
                0.95d,
                null,
                null,
                true,
                "AREA_SCOPE_PENDING",
                "¿Qué zona quieres cambiar?",
                null,
                null
        );

        assertTrue(response.isSuccess());
        assertEquals("UPDATE_STYLE_MULTI", response.getAction());
        assertTrue(response.getChanges().stream().anyMatch(c ->
                "CHAT_LIST_ITEM_GROUP_ACTIVE".equals(c.getArea()) && "BACKGROUND_COLOR".equals(c.getProperty())));
        assertTrue(response.getChanges().stream().anyMatch(c ->
                "CHAT_LIST_ITEM_GROUP_ACTIVE".equals(c.getArea()) && "TEXT_COLOR".equals(c.getProperty())));
    }

    @Test
    void validate_groupOnlyFilterPreservesGroupActiveChanges() {
        UiCustomizationChangeDTO background = new UiCustomizationChangeDTO();
        background.setArea("CHAT_LIST_ITEM_GROUP_ACTIVE");
        background.setProperty("BACKGROUND_COLOR");
        background.setValue("#2563eb");
        UiCustomizationChangeDTO text = new UiCustomizationChangeDTO();
        text.setArea("CHAT_LIST_ITEM_GROUP_ACTIVE");
        text.setProperty("TEXT_COLOR");
        text.setValue("#ef4444");

        AiUiCustomizationResponseDTO response = service.validate(
                "req-scope-3",
                "Poner fondo azul muy suave en chats grupales activos y textos en rojo",
                "UPDATE_STYLE_MULTI",
                null,
                null,
                null,
                null,
                "Fondo azul muy suave y texto rojo en chats grupales activos",
                0.95d,
                null,
                null,
                false,
                null,
                null,
                List.of(background, text),
                null
        );

        assertTrue(response.isSuccess());
        assertEquals("UPDATE_STYLE_MULTI", response.getAction());
        assertEquals(2, response.getChanges().size());
        assertTrue(response.getChanges().stream().allMatch(c -> "CHAT_LIST_ITEM_GROUP_ACTIVE".equals(c.getArea())));
    }

    private UiCustomizationChangeDTO areaLessChange(String property, String value) {
        UiCustomizationChangeDTO change = new UiCustomizationChangeDTO();
        change.setProperty(property);
        change.setValue(value);
        return change;
    }

    @Test
    void validate_rejectsLowConfidenceCustomization() {
        AiUiCustomizationResponseDTO response = service.validate(
                "req-2",
                "pon rojo el fondo del chat",
                "UPDATE_STYLE",
                "CHAT_MESSAGES_AREA",
                "BACKGROUND_COLOR",
                "#ef4444",
                null,
                "Cambiar fondo del chat a rojo",
                0.60d,
                null,
                null,
                null
        );

        assertFalse(response.isSuccess());
        assertEquals("UI_CUSTOMIZATION_LOW_CONFIDENCE", response.getCodigo());
    }

    @Test
    void validate_requestsClarificationForGenericPrompt() {
        AiUiCustomizationResponseDTO response = service.validate(
                "req-3",
                "ponlo mas bonito",
                "UPDATE_STYLE",
                null,
                null,
                null,
                null,
                "Cambiar estilo",
                0.92d,
                null,
                null,
                null
        );

        assertFalse(response.isSuccess());
        assertEquals("UI_CUSTOMIZATION_NEEDS_CLARIFICATION", response.getCodigo());
    }

    @Test
    void validate_blocksBackgroundImageWithoutSafeFlow() {
        AiUiCustomizationResponseDTO response = service.validate(
                "req-4",
                "pon una imagen en el fondo del chat",
                "UPDATE_STYLE",
                "CHAT_MESSAGES_AREA",
                "BACKGROUND_IMAGE",
                "gradient.png",
                null,
                "Poner imagen de fondo",
                0.96d,
                null,
                null,
                null
        );

        assertFalse(response.isSuccess());
        assertEquals("UI_CUSTOMIZATION_NEEDS_SAFE_IMAGE_FLOW", response.getCodigo());
    }

    @Test
    void validate_buildsChatListGroupWhenBackgroundRequested() {
        AiUiCustomizationResponseDTO response = service.validate(
                "req-5",
                "pon cada chat del listado en negro",
                "UPDATE_STYLE_GROUP",
                "CHAT_LIST_ITEM",
                "BACKGROUND_COLOR",
                "#111827",
                null,
                "Cambiar los chats del listado a negro",
                0.94d,
                null,
                null,
                null
        );

        assertTrue(response.isSuccess());
        assertEquals("UI_CUSTOMIZATION_OK", response.getCodigo());
        assertEquals("UPDATE_STYLE_GROUP", response.getAction());
        assertEquals(7, response.getChanges().size());
    }

    @Test
    void validate_groupOnlyScopeDoesNotExpandToGenericChildren() {
        AiUiCustomizationResponseDTO response = service.validate(
                "req-6",
                "cambia solo los chats grupales completos a morado elegante",
                "UPDATE_STYLE_GROUP",
                "CHAT_LIST_ITEM_GROUP",
                "BACKGROUND_COLOR",
                "#7c3aed",
                null,
                "Cambiar chats grupales completos a morado elegante",
                0.94d,
                null,
                null,
                null
        );

        assertTrue(response.isSuccess());
        assertEquals("UI_CUSTOMIZATION_OK", response.getCodigo());
        assertEquals("CHAT_LIST_ITEM_GROUP", response.getArea());
        assertEquals(7, response.getChanges().size());

        Set<String> forbiddenAreas = Set.of(
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
        assertTrue(response.getChanges().stream().noneMatch(c -> forbiddenAreas.contains(c.getArea())));
    }

    @Test
    void validate_groupOnlyPreviewAndAudioUseScopedAreas() {
        AiUiCustomizationResponseDTO response = service.validate(
                "req-7",
                "Cambia solo el último mensaje y los previews de audio dentro de los chats grupales a morado elegante",
                "UPDATE_STYLE_MULTI",
                "CHAT_LIST_ITEM_GROUP",
                null,
                null,
                null,
                "Cambiar previews dentro de chats grupales",
                0.95d,
                null,
                null,
                null
        );

        assertTrue(response.isSuccess());
        assertEquals("UI_CUSTOMIZATION_OK", response.getCodigo());
        assertEquals("UPDATE_STYLE_MULTI", response.getAction());
        assertTrue(response.getChanges().stream().anyMatch(c ->
                "CHAT_LIST_ITEM_GROUP_PREVIEW".equals(c.getArea()) && "TEXT_COLOR".equals(c.getProperty()) && "#7c3aed".equals(c.getValue())));
        assertTrue(response.getChanges().stream().anyMatch(c ->
                "CHAT_LIST_ITEM_GROUP_AUDIO_PREVIEW".equals(c.getArea()) && "BACKGROUND_COLOR".equals(c.getProperty()) && "#5b21b6".equals(c.getValue())));
        assertTrue(response.getChanges().stream().anyMatch(c ->
                "CHAT_LIST_ITEM_GROUP_AUDIO_PREVIEW".equals(c.getArea()) && "TEXT_COLOR".equals(c.getProperty()) && "#ffffff".equals(c.getValue())));
        assertTrue(response.getChanges().stream().anyMatch(c ->
                "CHAT_LIST_ITEM_GROUP_AUDIO_PREVIEW".equals(c.getArea()) && "ICON_COLOR".equals(c.getProperty()) && "#ddd6fe".equals(c.getValue())));
        assertTrue(response.getChanges().stream().noneMatch(c -> "CHAT_LIST_PREVIEW".equals(c.getArea())));
        assertTrue(response.getChanges().stream().noneMatch(c -> "CHAT_LIST_AUDIO_PREVIEW".equals(c.getArea())));
    }

    @Test
    void validate_groupOnlyFilePreviewAndBadgesUseScopedAreas() {
        AiUiCustomizationResponseDTO response = service.validate(
                "req-8",
                "Cambia solo los previews de archivos y las badges dentro de los chats grupales a un estilo azul elegante",
                "UPDATE_STYLE_MULTI",
                "CHAT_LIST_ITEM_GROUP",
                null,
                null,
                null,
                "Cambiar preview de archivos y badges en grupales",
                0.95d,
                null,
                null,
                null
        );

        assertTrue(response.isSuccess());
        assertEquals("UI_CUSTOMIZATION_OK", response.getCodigo());
        assertEquals("UPDATE_STYLE_MULTI", response.getAction());
        assertTrue(response.getChanges().stream().anyMatch(c ->
                "CHAT_LIST_ITEM_GROUP_FILE_PREVIEW".equals(c.getArea()) && "BACKGROUND_COLOR".equals(c.getProperty()) && "#dbeafe".equals(c.getValue())));
        assertTrue(response.getChanges().stream().anyMatch(c ->
                "CHAT_LIST_ITEM_GROUP_FILE_PREVIEW".equals(c.getArea()) && "TEXT_COLOR".equals(c.getProperty()) && "#1e3a8a".equals(c.getValue())));
        assertTrue(response.getChanges().stream().anyMatch(c ->
                "CHAT_LIST_ITEM_GROUP_FILE_PREVIEW".equals(c.getArea()) && "ICON_COLOR".equals(c.getProperty()) && "#2563eb".equals(c.getValue())));
        assertTrue(response.getChanges().stream().anyMatch(c ->
                "CHAT_LIST_ITEM_GROUP_BADGES".equals(c.getArea()) && "BACKGROUND_COLOR".equals(c.getProperty()) && "#2563eb".equals(c.getValue())));
        assertTrue(response.getChanges().stream().anyMatch(c ->
                "CHAT_LIST_ITEM_GROUP_BADGES".equals(c.getArea()) && "TEXT_COLOR".equals(c.getProperty()) && "#ffffff".equals(c.getValue())));
        assertTrue(response.getChanges().stream().noneMatch(c -> "CHAT_LIST_ITEM_GROUP_PREVIEW".equals(c.getArea())));
        assertTrue(response.getChanges().stream().noneMatch(c -> "CHAT_LIST_FILE_PREVIEW".equals(c.getArea())));
        assertTrue(response.getChanges().stream().noneMatch(c -> "CHAT_LIST_BADGES".equals(c.getArea())));
    }

    @Test
    void validate_fullChatListThemeIncludesCriticalScopedAreas() {
        AiUiCustomizationResponseDTO response = service.validate(
                "req-8b",
                "Pon todo el listado de chats en negro elegante, incluyendo encabezado, iconos, buscador, filtros, chats individuales, chats grupales, previews, contadores, badges y desplegables",
                "UPDATE_STYLE_GROUP",
                "CHAT_LIST_PANEL",
                "BACKGROUND_COLOR",
                "#111827",
                null,
                "Tema completo del listado de chats",
                0.95d,
                null,
                null,
                null
        );

        assertTrue(response.isSuccess());
        assertTrue(response.getChanges().stream().anyMatch(c -> "CHAT_LIST_ITEM".equals(c.getArea())));
        assertTrue(response.getChanges().stream().anyMatch(c -> "CHAT_LIST_ITEM_ACTIVE".equals(c.getArea())));
        assertTrue(response.getChanges().stream().anyMatch(c -> "CHAT_LIST_PREVIEW".equals(c.getArea())));
        assertTrue(response.getChanges().stream().anyMatch(c -> "CHAT_LIST_ITEM_PREVIEW".equals(c.getArea())));
        assertTrue(response.getChanges().stream().anyMatch(c -> "CHAT_LIST_ITEM_GROUP_PREVIEW".equals(c.getArea())));
        assertTrue(response.getChanges().stream().anyMatch(c -> "CHAT_LIST_AUDIO_PREVIEW".equals(c.getArea())));
        assertTrue(response.getChanges().stream().anyMatch(c -> "CHAT_LIST_ITEM_AUDIO_PREVIEW".equals(c.getArea())));
        assertTrue(response.getChanges().stream().anyMatch(c -> "CHAT_LIST_ITEM_GROUP_AUDIO_PREVIEW".equals(c.getArea())));
        assertTrue(response.getChanges().stream().anyMatch(c -> "CHAT_LIST_ITEM_NAME_SCOPED".equals(c.getArea())));
        assertTrue(response.getChanges().stream().anyMatch(c -> "CHAT_LIST_ITEM_GROUP_NAME".equals(c.getArea())));
        assertTrue(response.getChanges().stream().anyMatch(c -> "CHAT_LIST_HEADER_ACTIONS".equals(c.getArea()) && "ICON_COLOR".equals(c.getProperty())));
    }

    @Test
    void validate_fullChatListThemeWithClaroDoesNotNeedClarification() {
        AiUiCustomizationResponseDTO response = service.validate(
                "req-8c",
                "Pon todo el listado de chats en modo claro elegante, incluyendo encabezado, iconos, buscador, filtros, chats individuales, chats grupales, chat activo, chats no leídos, nombres, previews, audios, imágenes, archivos, contadores, badges, menú superior y desplegables de opciones del chat",
                "UPDATE_STYLE_MULTI",
                null,
                null,
                null,
                null,
                "Tema completo del listado claro",
                0.95d,
                null,
                null,
                null
        );

        assertTrue(response.isSuccess());
        assertEquals("UI_CUSTOMIZATION_OK", response.getCodigo());
        assertEquals("UPDATE_STYLE_MULTI", response.getAction());
        assertTrue(response.getChanges() != null && !response.getChanges().isEmpty());
        assertTrue(response.getChanges().stream().anyMatch(c -> "CHAT_LIST_PANEL".equals(c.getArea())));
        assertTrue(response.getChanges().stream().anyMatch(c -> "CHAT_LIST_ITEM".equals(c.getArea())));
        assertTrue(response.getChanges().stream().anyMatch(c -> "CHAT_LIST_ITEM_GROUP".equals(c.getArea())));
        assertTrue(response.getChanges().stream().anyMatch(c -> "CHAT_LIST_PREVIEW".equals(c.getArea())));
        assertTrue(response.getChanges().stream().anyMatch(c -> "CHAT_LIST_PIN_MENU_REPORT".equals(c.getArea())));
        assertTrue(response.getChanges().stream().anyMatch(c -> "CHAT_LIST_PIN_MENU_DANGER".equals(c.getArea())));
    }

    @Test
    void validate_forcesGroupBadgesScopedChanges() {
        AiUiCustomizationResponseDTO response = service.validate(
                "req-9",
                "Cambia el contador de no leídos en grupos a fondo azul y texto rosa",
                "UPDATE_STYLE",
                "CHAT_LIST_ITEM_GROUP",
                "TEXT_COLOR",
                null,
                null,
                "Cambiar badges de chats grupales",
                0.95d,
                null,
                null,
                null
        );

        assertTrue(response.isSuccess());
        assertEquals("UPDATE_STYLE_MULTI", response.getAction());
        assertTrue(response.getChanges().stream().anyMatch(c ->
                "CHAT_LIST_ITEM_GROUP_BADGES".equals(c.getArea()) && "BACKGROUND_COLOR".equals(c.getProperty()) && "#2563eb".equals(c.getValue())));
        assertTrue(response.getChanges().stream().anyMatch(c ->
                "CHAT_LIST_ITEM_GROUP_BADGES".equals(c.getArea()) && "TEXT_COLOR".equals(c.getProperty()) && "#ec4899".equals(c.getValue())));
        assertTrue(response.getChanges().stream().anyMatch(c ->
                "CHAT_LIST_ITEM_GROUP_BADGES".equals(c.getArea()) && "BACKGROUND_COLOR".equals(c.getProperty())));
        assertTrue(response.getChanges().stream().anyMatch(c ->
                "CHAT_LIST_ITEM_GROUP_BADGES".equals(c.getArea()) && "TEXT_COLOR".equals(c.getProperty())));
        String bg = response.getChanges().stream().filter(c ->
                "CHAT_LIST_ITEM_GROUP_BADGES".equals(c.getArea()) && "BACKGROUND_COLOR".equals(c.getProperty())).findFirst().orElseThrow().getValue();
        String text = response.getChanges().stream().filter(c ->
                "CHAT_LIST_ITEM_GROUP_BADGES".equals(c.getArea()) && "TEXT_COLOR".equals(c.getProperty())).findFirst().orElseThrow().getValue();
        assertFalse(bg.equals(text));
        assertTrue(response.getChanges().stream().noneMatch(c -> Set.of("CHAT_LIST_ITEM_GROUP", "CHAT_LIST_GROUP_PILL", "CHAT_LIST_BADGES", "CHAT_LIST_ITEM").contains(c.getArea())));
    }

    @Test
    void validate_forcesGroupAudioPreviewScopedChanges() {
        AiUiCustomizationResponseDTO response = service.validate(
                "req-10",
                "Pon los previews de audio de los chats grupales en naranja suave",
                "UPDATE_STYLE",
                "CHAT_LIST_ITEM_GROUP",
                "TEXT_COLOR",
                "#f97316",
                null,
                "Cambiar audio previews grupales",
                0.95d,
                null,
                null,
                null
        );

        assertTrue(response.isSuccess());
        assertEquals("UPDATE_STYLE_MULTI", response.getAction());
        assertTrue(response.getChanges().stream().anyMatch(c ->
                "CHAT_LIST_ITEM_GROUP_AUDIO_PREVIEW".equals(c.getArea()) && "BACKGROUND_COLOR".equals(c.getProperty()) && "#ffedd5".equals(c.getValue())));
        assertTrue(response.getChanges().stream().anyMatch(c ->
                "CHAT_LIST_ITEM_GROUP_AUDIO_PREVIEW".equals(c.getArea()) && "TEXT_COLOR".equals(c.getProperty()) && "#7c2d12".equals(c.getValue())));
        assertTrue(response.getChanges().stream().anyMatch(c ->
                "CHAT_LIST_ITEM_GROUP_AUDIO_PREVIEW".equals(c.getArea()) && "ICON_COLOR".equals(c.getProperty()) && "#ea580c".equals(c.getValue())));
        assertTrue(response.getChanges().stream().noneMatch(c -> Set.of("CHAT_LIST_ITEM_GROUP", "CHAT_LIST_GROUP_PILL", "CHAT_LIST_AUDIO_PREVIEW").contains(c.getArea())));
    }

    @Test
    void validate_forcesFullPinMenuGroupWhenIncompleteChangesArrive() {
        UiCustomizationChangeDTO incomplete = new UiCustomizationChangeDTO();
        incomplete.setArea("CHAT_LIST_PIN_MENU");
        incomplete.setProperty("BACKGROUND_COLOR");
        incomplete.setValue("#7c3aed");

        AiUiCustomizationResponseDTO response = service.validate(
                "req-11",
                "Pon el desplegable de opciones del chat en púrpura fuerte y que combine",
                "UPDATE_STYLE_GROUP",
                "CHAT_LIST_PIN_MENU",
                "BACKGROUND_COLOR",
                "#7c3aed",
                null,
                "Cambiar desplegable completo",
                0.95d,
                null,
                List.of(incomplete),
                null
        );

        assertTrue(response.isSuccess());
        assertEquals("UPDATE_STYLE_MULTI", response.getAction());
        assertTrue(response.getChanges().stream().anyMatch(c ->
                "CHAT_LIST_PIN_MENU".equals(c.getArea()) && "BACKGROUND_COLOR".equals(c.getProperty()) && "#7c3aed".equals(c.getValue())));
        assertTrue(response.getChanges().stream().anyMatch(c ->
                "CHAT_LIST_PIN_MENU".equals(c.getArea()) && "TEXT_COLOR".equals(c.getProperty()) && "#ffffff".equals(c.getValue())));
        assertTrue(response.getChanges().stream().anyMatch(c ->
                "CHAT_LIST_PIN_MENU".equals(c.getArea()) && "BORDER_COLOR".equals(c.getProperty()) && "#6d28d9".equals(c.getValue())));
        assertTrue(response.getChanges().stream().anyMatch(c ->
                "CHAT_LIST_PIN_MENU_ITEM".equals(c.getArea()) && "ICON_COLOR".equals(c.getProperty()) && "#ddd6fe".equals(c.getValue())));
        assertTrue(response.getChanges().stream().anyMatch(c ->
                "CHAT_LIST_PIN_MENU_REPORT".equals(c.getArea()) && "ICON_COLOR".equals(c.getProperty()) && "#fed7aa".equals(c.getValue())));
        assertTrue(response.getChanges().stream().anyMatch(c ->
                "CHAT_LIST_PIN_MENU_DANGER".equals(c.getArea()) && "HOVER_BACKGROUND_COLOR".equals(c.getProperty()) && "#991b1b".equals(c.getValue())));
    }

    @Test
    void validate_ambiguousDropdownNeedsClarification() {
        AiUiCustomizationResponseDTO response = service.validate(
                "req-12",
                "Cambia el color del desplegable a azul",
                "UPDATE_STYLE",
                null,
                null,
                null,
                null,
                "Cambiar desplegable ambiguo",
                0.95d,
                null,
                null,
                null
        );

        assertFalse(response.isSuccess());
        assertEquals("UI_CUSTOMIZATION_NEEDS_CLARIFICATION", response.getCodigo());
        assertEquals("NEEDS_CLARIFICATION", response.getAction());
        assertTrue(response.getChanges() == null);
    }

    @Test
    void validate_dropdownChatIconMapsToPinToggle() {
        AiUiCustomizationResponseDTO response = service.validate(
                "req-13",
                "Cambia el icono del desplegable del chat a azul",
                "UPDATE_STYLE",
                null,
                null,
                "#2563eb",
                null,
                "Cambiar icono del desplegable del chat",
                0.95d,
                null,
                null,
                null
        );

        assertTrue(response.isSuccess());
        assertEquals("CHAT_LIST_PIN_TOGGLE", response.getArea());
        assertEquals("ICON_COLOR", response.getProperty());
    }

    @Test
    void validate_chatOptionsMenuResolvesToFullPinMenu() {
        AiUiCustomizationResponseDTO response = service.validate(
                "req-14",
                "Cambia el menú de opciones del chat a azul",
                "UPDATE_STYLE",
                null,
                null,
                "#2563eb",
                null,
                "Cambiar menu de opciones del chat",
                0.95d,
                null,
                null,
                null
        );

        assertTrue(response.isSuccess());
        assertEquals("UPDATE_STYLE_MULTI", response.getAction());
        assertTrue(response.getChanges().stream().anyMatch(c -> "CHAT_LIST_PIN_MENU_REPORT".equals(c.getArea()) && "TEXT_COLOR".equals(c.getProperty())));
        assertTrue(response.getChanges().stream().anyMatch(c -> "CHAT_LIST_PIN_MENU_DANGER".equals(c.getArea()) && "TEXT_COLOR".equals(c.getProperty())));
    }

    @Test
    void validate_ambiguousDropdownOverridesAiPinMenuArea() {
        AiUiCustomizationResponseDTO response = service.validate(
                "req-15",
                "Cambia el color del desplegable a azul",
                "UPDATE_STYLE",
                "CHAT_LIST_PIN_MENU",
                "BACKGROUND_COLOR",
                "#2563eb",
                null,
                "Cambiar desplegable ambiguo",
                0.95d,
                null,
                null,
                null
        );

        assertFalse(response.isSuccess());
        assertEquals("UI_CUSTOMIZATION_NEEDS_CLARIFICATION", response.getCodigo());
        assertEquals("NEEDS_CLARIFICATION", response.getAction());
        assertEquals(null, response.getArea());
        assertEquals(null, response.getProperty());
        assertEquals(null, response.getValue());
    }

    @Test
    void validate_normalizesActiveChatSingleGroupActionToUpdateStyle() {
        AiUiCustomizationResponseDTO response = service.validate(
                "req-16",
                "Pon el chat activo con fondo blanco",
                "UPDATE_STYLE_GROUP",
                "CHAT_LIST_ITEM_ACTIVE",
                "BACKGROUND_COLOR",
                "#ffffff",
                null,
                "Cambiar chat activo",
                0.95d,
                null,
                null,
                null
        );

        assertTrue(response.isSuccess());
        assertEquals("UI_CUSTOMIZATION_OK", response.getCodigo());
        assertEquals("UPDATE_STYLE", response.getAction());
        assertEquals("CHAT_LIST_ITEM_ACTIVE", response.getArea());
        assertEquals("BACKGROUND_COLOR", response.getProperty());
        assertEquals("#ffffff", response.getValue());
    }

    @Test
    void validate_normalizesActiveChatGroupActionWithChangesToUpdateStyleMulti() {
        UiCustomizationChangeDTO background = new UiCustomizationChangeDTO();
        background.setArea("CHAT_LIST_ITEM_ACTIVE");
        background.setProperty("BACKGROUND_COLOR");
        background.setValue("#ffffff");

        UiCustomizationChangeDTO text = new UiCustomizationChangeDTO();
        text.setArea("CHAT_LIST_ITEM_ACTIVE");
        text.setProperty("TEXT_COLOR");
        text.setValue("#111827");

        UiCustomizationChangeDTO border = new UiCustomizationChangeDTO();
        border.setArea("CHAT_LIST_ITEM_ACTIVE");
        border.setProperty("BORDER_WIDTH");
        border.setValue("0px");

        AiUiCustomizationResponseDTO response = service.validate(
                "req-17",
                "Pon el chat activo con fondo blanco, texto negro y sin borde",
                "UPDATE_STYLE_GROUP",
                "CHAT_LIST_ITEM_ACTIVE",
                "BACKGROUND_COLOR",
                "#ffffff",
                null,
                "Cambiar chat activo completo",
                0.95d,
                null,
                List.of(background, text, border),
                null
        );

        assertTrue(response.isSuccess());
        assertEquals("UI_CUSTOMIZATION_OK", response.getCodigo());
        assertEquals("UPDATE_STYLE_MULTI", response.getAction());
        assertTrue(response.getChanges().stream().anyMatch(c ->
                "CHAT_LIST_ITEM_ACTIVE".equals(c.getArea()) && "BACKGROUND_COLOR".equals(c.getProperty()) && "#ffffff".equals(c.getValue())));
        assertTrue(response.getChanges().stream().anyMatch(c ->
                "CHAT_LIST_ITEM_ACTIVE".equals(c.getArea()) && "TEXT_COLOR".equals(c.getProperty()) && "#111827".equals(c.getValue())));
        assertTrue(response.getChanges().stream().anyMatch(c ->
                "CHAT_LIST_ITEM_ACTIVE".equals(c.getArea()) && "BORDER_WIDTH".equals(c.getProperty()) && "0px".equals(c.getValue())));
    }

    @Test
    void validate_messageDropdownDoesNotResolveToChatPinMenu() {
        AiUiCustomizationResponseDTO response = service.validate(
                "req-18",
                "Cambia el desplegable de mensajes a azul",
                "UPDATE_STYLE",
                "CHAT_LIST_PIN_MENU",
                "BACKGROUND_COLOR",
                "#2563eb",
                null,
                "Cambiar desplegable de mensajes",
                0.95d,
                null,
                null,
                null
        );

        assertTrue(response.isSuccess());
        assertEquals("MESSAGE_OPTIONS_DROPDOWN", response.getArea());
        assertEquals("BACKGROUND_COLOR", response.getProperty());
        assertEquals("#2563eb", response.getValue());
    }

    @Test
    void validate_groupActiveChatMapsToScopedArea() {
        AiUiCustomizationResponseDTO response = service.validate(
                "req-19",
                "Cambiar fondo del chat grupal activo a verde",
                "UPDATE_STYLE",
                null,
                "BACKGROUND_COLOR",
                "#16a34a",
                null,
                "Cambiar fondo chat grupal activo",
                0.95d,
                null,
                null,
                null
        );

        assertTrue(response.isSuccess());
        assertEquals("UI_CUSTOMIZATION_OK", response.getCodigo());
        assertEquals("UPDATE_STYLE", response.getAction());
        assertEquals("CHAT_LIST_ITEM_GROUP_ACTIVE", response.getArea());
        assertEquals("BACKGROUND_COLOR", response.getProperty());
        assertEquals("#16a34a", response.getValue());
    }

    @Test
    void validate_pinMenuBackgroundFromChatListBuildsFullGroup() {
        AiUiCustomizationResponseDTO response = service.validate(
                "req-20",
                "Cambiar fondo del menú desplegable del listado de chats a azul",
                "UPDATE_STYLE",
                null,
                "BACKGROUND_COLOR",
                "#2563eb",
                null,
                "Cambiar fondo del menu desplegable",
                0.95d,
                null,
                null,
                null
        );

        assertTrue(response.isSuccess());
        assertEquals("UI_CUSTOMIZATION_OK", response.getCodigo());
        assertEquals("UPDATE_STYLE_MULTI", response.getAction());
        assertTrue(response.getChanges().stream().anyMatch(c -> "CHAT_LIST_PIN_MENU".equals(c.getArea())));
        assertTrue(response.getChanges().stream().anyMatch(c -> "CHAT_LIST_PIN_MENU_ITEM".equals(c.getArea())));
        assertTrue(response.getChanges().stream().anyMatch(c -> "CHAT_LIST_PIN_MENU_REPORT".equals(c.getArea())));
        assertTrue(response.getChanges().stream().anyMatch(c -> "CHAT_LIST_PIN_MENU_DANGER".equals(c.getArea())));
    }

    @Test
    void validate_groupActiveSelectedMultiDoesNotNeedClarification() {
        AiUiCustomizationResponseDTO response = service.validate(
                "req-21",
                "Pon el chat grupal seleccionado con fondo morado, texto blanco y borde amarillo",
                "UPDATE_STYLE_MULTI",
                null,
                null,
                null,
                null,
                "Cambiar chat grupal seleccionado: fondo morado, texto blanco, borde amarillo",
                0.95d,
                null,
                null,
                null
        );

        assertTrue(response.isSuccess());
        assertEquals("UI_CUSTOMIZATION_OK", response.getCodigo());
        assertEquals("UPDATE_STYLE_MULTI", response.getAction());
        assertTrue(response.getChanges().stream().anyMatch(c ->
                "CHAT_LIST_ITEM_GROUP_ACTIVE".equals(c.getArea()) && "BACKGROUND_COLOR".equals(c.getProperty())));
        assertTrue(response.getChanges().stream().anyMatch(c ->
                "CHAT_LIST_ITEM_GROUP_ACTIVE".equals(c.getArea()) && "TEXT_COLOR".equals(c.getProperty())));
        assertTrue(response.getChanges().stream().anyMatch(c ->
                "CHAT_LIST_ITEM_GROUP_ACTIVE".equals(c.getArea()) && "BORDER_COLOR".equals(c.getProperty())));
    }

    @Test
    void validate_groupActiveRemoveBorderMapsToBorderWidthZero() {
        AiUiCustomizationResponseDTO response = service.validate(
                "req-22",
                "Quita el borde del grupo seleccionado",
                "UPDATE_STYLE",
                null,
                null,
                null,
                null,
                "Quitar borde grupo seleccionado",
                0.95d,
                null,
                null,
                null
        );

        assertTrue(response.isSuccess());
        assertEquals("CHAT_LIST_ITEM_GROUP_ACTIVE", response.getArea());
        assertEquals("BORDER_WIDTH", response.getProperty());
        assertEquals("0px", response.getValue());
    }

    @Test
    void validate_groupActiveRepairsFromLabelWhenActionNeedsClarification() {
        AiUiCustomizationResponseDTO response = service.validate(
                "req-23",
                "Ponlo así",
                "NEEDS_CLARIFICATION",
                null,
                null,
                null,
                null,
                "Cambiar chat grupal seleccionado: fondo morado, texto blanco, borde amarillo",
                0.95d,
                null,
                null,
                null
        );

        assertTrue(response.isSuccess());
        assertEquals("UI_CUSTOMIZATION_OK", response.getCodigo());
        assertEquals("UPDATE_STYLE_MULTI", response.getAction());
        assertTrue(response.getChanges().stream().allMatch(c -> "CHAT_LIST_ITEM_GROUP_ACTIVE".equals(c.getArea())));
    }

    @Test
    void validate_scopedPinMenuRequestDoesNotGenerateFullChatListThemeAreas() {
        AiUiCustomizationResponseDTO response = service.validate(
                "req-24",
                "Cambiar fondo del menú desplegable del listado de chats a azul",
                "UPDATE_STYLE_MULTI",
                null,
                null,
                null,
                null,
                "Cambiar fondo del menú desplegable del listado de chats a azul",
                0.95d,
                null,
                null,
                null
        );

        assertTrue(response.isSuccess());
        assertEquals("UPDATE_STYLE_MULTI", response.getAction());
        assertTrue(response.getChanges().stream().allMatch(c -> Set.of(
                "CHAT_LIST_PIN_MENU",
                "CHAT_LIST_PIN_MENU_ITEM",
                "CHAT_LIST_PIN_MENU_REPORT",
                "CHAT_LIST_PIN_MENU_DANGER"
        ).contains(c.getArea())));
    }
    @Test
    void validate_activeIndividualScopeFallsBackToChatListItemActive() {
        UiCustomizationScopeDTO scope = new UiCustomizationScopeDTO();
        scope.setModule("CHAT_LIST");
        scope.setElement("CHAT_ITEM");
        scope.setChatType("INDIVIDUAL");
        scope.setState("ACTIVE");

        AiUiCustomizationResponseDTO response = service.validate(
                "req-25",
                "cuando tengamos seleccionado un chat individual tiene que tener el texto del nombre a naranja",
                "UPDATE_STYLE",
                null,
                "TEXT_COLOR",
                "#f97316",
                null,
                "Nombre del chat individual seleccionado en naranja",
                0.95d,
                null,
                scope,
                false,
                null,
                null,
                null,
                null
        );

        assertTrue(response.isSuccess());
        assertEquals("CHAT_LIST_ITEM_ACTIVE", response.getArea());
        assertEquals("TEXT_COLOR", response.getProperty());
        assertEquals("#f97316", response.getValue());
    }

    @Test
    void validate_lastMessageTimeMapsToChatListItemDate() {
        AiUiCustomizationResponseDTO response = service.validate(
                "req-26",
                "cambia la hora del ultimo mensaje a gris",
                "UPDATE_STYLE",
                null,
                null,
                "#6b7280",
                null,
                "Cambiar hora del ultimo mensaje a gris",
                0.95d,
                null,
                null,
                null
        );

        assertTrue(response.isSuccess());
        assertEquals("CHAT_LIST_ITEM_DATE", response.getArea());
        assertEquals("TEXT_COLOR", response.getProperty());
        assertEquals("#6b7280", response.getValue());
    }

    @Test
    void validate_sidebarNotificationBadgeMapsCorrectly() {
        AiUiCustomizationResponseDTO response = service.validate(
                "req-27",
                "pon el contador de notificaciones de la barra lateral en rojo",
                "UPDATE_STYLE",
                null,
                null,
                "#ef4444",
                null,
                "Cambiar contador de notificaciones de la barra lateral a rojo",
                0.95d,
                null,
                null,
                null
        );

        assertTrue(response.isSuccess());
        assertEquals("SIDEBAR_NAV_NOTIF_BADGE", response.getArea());
        assertEquals("BACKGROUND_COLOR", response.getProperty());
        assertEquals("#ef4444", response.getValue());
    }

    @Test
    void validate_sidebarBackgroundAndIconsUseSeparateAreas() {
        AiUiCustomizationResponseDTO response = service.validate(
                "req-28",
                "cambia la barra lateral a fondo rojo e iconos blancos",
                "UPDATE_STYLE",
                null,
                null,
                null,
                null,
                "Cambiar barra lateral a fondo rojo e iconos blancos",
                0.95d,
                null,
                null,
                null
        );

        assertTrue(response.isSuccess());
        assertEquals("UPDATE_STYLE_MULTI", response.getAction());
        assertTrue(response.getChanges().stream().anyMatch(c ->
                "SIDEBAR_NAV_PANEL".equals(c.getArea()) && "BACKGROUND_COLOR".equals(c.getProperty()) && "#ef4444".equals(c.getValue())));
        assertTrue(response.getChanges().stream().anyMatch(c ->
                "SIDEBAR_NAV_ICON".equals(c.getArea()) && "ICON_COLOR".equals(c.getProperty()) && "#ffffff".equals(c.getValue())));
    }

    @Test
    void validate_menuDeLaIzquierdaMapsToSidebarPanel() {
        AiUiCustomizationResponseDTO response = service.validate(
                "req-29",
                "cambia el menu de la izquierda a rojo",
                "UPDATE_STYLE",
                null,
                null,
                "#ef4444",
                null,
                "Cambiar menu de la izquierda a rojo",
                0.95d,
                null,
                null,
                null
        );

        assertTrue(response.isSuccess());
        assertEquals("SIDEBAR_NAV_PANEL", response.getArea());
        assertEquals("BACKGROUND_COLOR", response.getProperty());
        assertEquals("#ef4444", response.getValue());
    }

    @Test
    void validate_chatNameMapsToChatListItemName() {
        AiUiCustomizationResponseDTO response = service.validate(
                "req-30",
                "cambia el nombre del chat a azul",
                "UPDATE_STYLE",
                null,
                null,
                "#2563eb",
                null,
                "Cambiar nombre del chat a azul",
                0.95d,
                null,
                null,
                null
        );

        assertTrue(response.isSuccess());
        assertEquals("CHAT_LIST_ITEM_NAME", response.getArea());
        assertEquals("TEXT_COLOR", response.getProperty());
        assertEquals("#2563eb", response.getValue());
    }

    @Test
    void validate_statusDotMapsToChatListStatusDot() {
        AiUiCustomizationResponseDTO response = service.validate(
                "req-31",
                "pon el punto de estado del listado en verde",
                "UPDATE_STYLE",
                null,
                null,
                "#22c55e",
                null,
                "Cambiar punto de estado del listado a verde",
                0.95d,
                null,
                null,
                null
        );

        assertTrue(response.isSuccess());
        assertEquals("CHAT_LIST_STATUS_DOT", response.getArea());
        assertEquals("BACKGROUND_COLOR", response.getProperty());
        assertEquals("#22c55e", response.getValue());
    }

    @Test
    void validate_stickerPreviewMapsToChatListStickerPreview() {
        AiUiCustomizationResponseDTO response = service.validate(
                "req-32",
                "cambia el preview de sticker del listado a morado",
                "UPDATE_STYLE",
                null,
                null,
                "#7c3aed",
                null,
                "Cambiar preview de sticker del listado a morado",
                0.95d,
                null,
                null,
                null
        );

        assertTrue(response.isSuccess());
        assertEquals("CHAT_LIST_STICKER_PREVIEW", response.getArea());
        assertEquals("TEXT_COLOR", response.getProperty());
        assertEquals("#7c3aed", response.getValue());
    }

    @Test
    void validate_favoriteIndicatorMapsToChatListFavoriteIndicator() {
        AiUiCustomizationResponseDTO response = service.validate(
                "req-33",
                "pon la estrella de favorito en amarillo",
                "UPDATE_STYLE",
                null,
                null,
                "#eab308",
                null,
                "Cambiar estrella de favorito a amarillo",
                0.95d,
                null,
                null,
                null
        );

        assertTrue(response.isSuccess());
        assertEquals("CHAT_LIST_FAVORITE_INDICATOR", response.getArea());
        assertEquals("ICON_COLOR", response.getProperty());
        assertEquals("#eab308", response.getValue());
    }

    @Test
    void validate_mutedIndicatorMapsToChatListMutedIndicator() {
        AiUiCustomizationResponseDTO response = service.validate(
                "req-34",
                "pon el icono de silenciado en gris",
                "UPDATE_STYLE",
                null,
                null,
                "#6b7280",
                null,
                "Cambiar icono de silenciado a gris",
                0.95d,
                null,
                null,
                null
        );

        assertTrue(response.isSuccess());
        assertEquals("CHAT_LIST_MUTED_INDICATOR", response.getArea());
        assertEquals("ICON_COLOR", response.getProperty());
        assertEquals("#6b7280", response.getValue());
    }

    @Test
    void validate_closedIndicatorMapsToChatListClosedIndicator() {
        AiUiCustomizationResponseDTO response = service.validate(
                "req-35",
                "cambia el candado de chat cerrado a rojo",
                "UPDATE_STYLE",
                null,
                null,
                "#ef4444",
                null,
                "Cambiar candado de chat cerrado a rojo",
                0.95d,
                null,
                null,
                null
        );

        assertTrue(response.isSuccess());
        assertEquals("CHAT_LIST_CLOSED_INDICATOR", response.getArea());
        assertEquals("ICON_COLOR", response.getProperty());
        assertEquals("#ef4444", response.getValue());
    }

    @Test
    void validate_publicPanelMapsToChatListPublicPanel() {
        AiUiCustomizationResponseDTO response = service.validate(
                "req-36",
                "cambia la seccion de chats publicos a fondo oscuro",
                "UPDATE_STYLE",
                null,
                null,
                "#111827",
                null,
                "Cambiar seccion de chats publicos a fondo oscuro",
                0.95d,
                null,
                null,
                null
        );

        assertTrue(response.isSuccess());
        assertEquals("CHAT_LIST_PUBLIC_PANEL", response.getArea());
        assertEquals("BACKGROUND_COLOR", response.getProperty());
        assertEquals("#111827", response.getValue());
    }

    @Test
    void validate_publicCardMapsToChatListPublicCard() {
        AiUiCustomizationResponseDTO response = service.validate(
                "req-37",
                "cambia las tarjetas de chats publicos a azul",
                "UPDATE_STYLE",
                null,
                null,
                "#2563eb",
                null,
                "Cambiar tarjetas de chats publicos a azul",
                0.95d,
                null,
                null,
                null
        );

        assertTrue(response.isSuccess());
        assertEquals("CHAT_LIST_PUBLIC_CARD", response.getArea());
        assertEquals("BACKGROUND_COLOR", response.getProperty());
        assertEquals("#2563eb", response.getValue());
    }

    @Test
    void validate_groupItemBackgroundAutoExpandsByDefault() {
        AiUiCustomizationResponseDTO response = service.validate(
                "req-38",
                "cambia el background color de todos los chats grupales a rojo",
                "UPDATE_STYLE",
                "CHAT_LIST_ITEM_GROUP",
                "BACKGROUND_COLOR",
                "#ef4444",
                null,
                "Cambiar chats grupales a rojo",
                0.95d,
                null,
                null,
                null
        );

        assertTrue(response.isSuccess());
        assertEquals("UPDATE_STYLE_MULTI", response.getAction());
        assertTrue(response.getChanges().stream().anyMatch(c -> "CHAT_LIST_ITEM_GROUP".equals(c.getArea()) && "BACKGROUND_COLOR".equals(c.getProperty())));
        assertTrue(response.getChanges().stream().anyMatch(c -> "CHAT_LIST_GROUP_PILL".equals(c.getArea()) && "TEXT_COLOR".equals(c.getProperty())));
    }

    @Test
    void validate_groupItemBackgroundDoesNotExpandWhenOnlyRequested() {
        AiUiCustomizationResponseDTO response = service.validate(
                "req-39",
                "cambia solo el background color de todos los chats grupales a rojo",
                "UPDATE_STYLE",
                "CHAT_LIST_ITEM_GROUP",
                "BACKGROUND_COLOR",
                "#ef4444",
                null,
                "Cambiar solo fondo chats grupales a rojo",
                0.95d,
                null,
                null,
                null
        );

        assertTrue(response.isSuccess());
        assertEquals("UPDATE_STYLE", response.getAction());
        assertEquals("CHAT_LIST_ITEM_GROUP", response.getArea());
        assertEquals("BACKGROUND_COLOR", response.getProperty());
    }

    @Test
    void validate_searchBackgroundAutoExpandsByDefault() {
        AiUiCustomizationResponseDTO response = service.validate(
                "req-40",
                "pon el buscador de chats gris",
                "UPDATE_STYLE",
                "CHAT_LIST_SEARCH",
                "BACKGROUND_COLOR",
                "#6b7280",
                null,
                "Buscador de chats gris",
                0.95d,
                null,
                null,
                null
        );

        assertTrue(response.isSuccess());
        assertEquals("UPDATE_STYLE_MULTI", response.getAction());
        assertTrue(response.getChanges().stream().anyMatch(c -> "CHAT_LIST_SEARCH".equals(c.getArea()) && "PLACEHOLDER_COLOR".equals(c.getProperty())));
        assertTrue(response.getChanges().stream().anyMatch(c -> "CHAT_LIST_SEARCH".equals(c.getArea()) && "BORDER_COLOR".equals(c.getProperty())));
    }

    @Test
    void validate_pinMenuBackgroundAutoExpandsByDefault() {
        AiUiCustomizationResponseDTO response = service.validate(
                "req-41",
                "pon el desplegable de opciones del chat azul",
                "UPDATE_STYLE",
                "CHAT_LIST_PIN_MENU",
                "BACKGROUND_COLOR",
                "#2563eb",
                null,
                "Desplegable de opciones azul",
                0.95d,
                null,
                null,
                null
        );

        assertTrue(response.isSuccess());
        assertEquals("UPDATE_STYLE_MULTI", response.getAction());
        assertTrue(response.getChanges().stream().anyMatch(c -> "CHAT_LIST_PIN_MENU_ITEM".equals(c.getArea()) && "HOVER_BACKGROUND_COLOR".equals(c.getProperty())));
        assertTrue(response.getChanges().stream().anyMatch(c -> "CHAT_LIST_PIN_MENU_REPORT".equals(c.getArea()) && "ICON_COLOR".equals(c.getProperty())));
        assertTrue(response.getChanges().stream().anyMatch(c -> "CHAT_LIST_PIN_MENU_DANGER".equals(c.getArea()) && "ICON_COLOR".equals(c.getProperty())));
    }

    @Test
    void validate_sidebarItemsBackgroundAutoExpandsByDefault() {
        AiUiCustomizationResponseDTO response = service.validate(
                "req-42",
                "pon los botones de la sidebar morados",
                "UPDATE_STYLE",
                "SIDEBAR_NAV_ITEM",
                "BACKGROUND_COLOR",
                "#7c3aed",
                null,
                "Botones sidebar morados",
                0.95d,
                null,
                null,
                null
        );

        assertTrue(response.isSuccess());
        assertEquals("UPDATE_STYLE_MULTI", response.getAction());
        assertTrue(response.getChanges().stream().anyMatch(c -> "SIDEBAR_NAV_ITEM_ACTIVE".equals(c.getArea()) && "BACKGROUND_COLOR".equals(c.getProperty())));
        assertTrue(response.getChanges().stream().anyMatch(c -> "SIDEBAR_NAV_ITEM".equals(c.getArea()) && "ICON_COLOR".equals(c.getProperty())));
    }

    @Test
    void validate_sidebarItemsBackgroundDoesNotExpandWhenOnlyRequested() {
        AiUiCustomizationResponseDTO response = service.validate(
                "req-43",
                "pon solo los botones de la sidebar morados",
                "UPDATE_STYLE",
                "SIDEBAR_NAV_ITEM",
                "BACKGROUND_COLOR",
                "#7c3aed",
                null,
                "Solo botones sidebar morados",
                0.95d,
                null,
                null,
                null
        );

        assertTrue(response.isSuccess());
        assertEquals("UPDATE_STYLE", response.getAction());
        assertEquals("SIDEBAR_NAV_ITEM", response.getArea());
        assertEquals("BACKGROUND_COLOR", response.getProperty());
    }
}






