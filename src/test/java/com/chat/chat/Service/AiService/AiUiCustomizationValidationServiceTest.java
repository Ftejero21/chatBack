package com.chat.chat.Service.AiService;

import com.chat.chat.DTO.AiUiCustomizationResponseDTO;
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
}






