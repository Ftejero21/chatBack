package com.chat.chat.Service.AiService;

import com.chat.chat.DTO.AiEncryptedMessageSearchResponseDTO;
import com.chat.chat.DTO.AiSearchIntentInternalResponseDTO;
import com.chat.chat.DTO.AiSmartActionRequestDTO;
import com.chat.chat.DTO.AiUiCustomizationResponseDTO;
import com.chat.chat.DTO.AiSearchProgressWS;
import com.chat.chat.DTO.UiCustomizationChangeDTO;
import com.chat.chat.DTO.UiCustomizationScopeDTO;
import com.chat.chat.Entity.UsuarioEntity;
import com.chat.chat.Repository.UsuarioRepository;
import com.chat.chat.Utils.SecurityUtils;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AiSmartActionServiceImplTest {

    @Test
    void process_routesUiCustomizationWithoutCallingSearchService() {
        SecurityUtils securityUtils = mock(SecurityUtils.class);
        UsuarioRepository usuarioRepository = mock(UsuarioRepository.class);
        AiSearchIntentMicroserviceClient intentClient = mock(AiSearchIntentMicroserviceClient.class);
        AiEncryptedMessageSearchService searchService = mock(AiEncryptedMessageSearchService.class);
        AiUiCustomizationValidationService validationService = new AiUiCustomizationValidationService();
        SimpMessagingTemplate messagingTemplate = mock(SimpMessagingTemplate.class);
        AiSmartActionHistoryService historyService = mock(AiSmartActionHistoryService.class);
        AiUiCustomizationMicroserviceClient uiCustomizationMicroserviceClient = mock(AiUiCustomizationMicroserviceClient.class);

        when(securityUtils.getAuthenticatedUserId()).thenReturn(7L);
        when(securityUtils.getAuthenticatedUserEmail()).thenReturn("user@test.com");
        UsuarioEntity user = new UsuarioEntity();
        user.setNombre("Fernando");
        user.setApellido("Tejero");
        when(usuarioRepository.findById(7L)).thenReturn(Optional.of(user));

        AiSearchIntentInternalResponseDTO intent = new AiSearchIntentInternalResponseDTO();
        intent.setSuccess(true);
        intent.setTarget("UI_CUSTOMIZATION");
        intent.setAction("UPDATE_STYLE");
        intent.setArea("CHAT_MESSAGES_AREA");
        intent.setProperty("BACKGROUND_COLOR");
        intent.setValue("#ef4444");
        intent.setLabel("Cambiar fondo del chat a rojo");
        intent.setConfidence(0.94d);
        when(intentClient.classifyIntent(anyString(), any())).thenReturn(intent);

        AiSmartActionServiceImpl service = new AiSmartActionServiceImpl(
                securityUtils, usuarioRepository, intentClient, searchService, validationService, uiCustomizationMicroserviceClient, messagingTemplate, historyService
        );
        AiSmartActionRequestDTO request = new AiSmartActionRequestDTO();
        request.setConsulta("Nexo cambia el fondo del chat a rojo");

        Object response = service.process(request);

        assertInstanceOf(AiUiCustomizationResponseDTO.class, response);
        assertEquals("UI_CUSTOMIZATION_OK", ((AiUiCustomizationResponseDTO) response).getCodigo());
        verify(searchService, never()).buscarMensajes(any());
        ArgumentCaptor<AiSearchProgressWS> payloadCaptor = ArgumentCaptor.forClass(AiSearchProgressWS.class);
        verify(messagingTemplate, atLeast(3)).convertAndSendToUser(anyString(), anyString(), payloadCaptor.capture());
        boolean hasAnalyzing = payloadCaptor.getAllValues().stream().anyMatch(p -> "UI_CUSTOMIZATION_ANALYZING".equals(p.getStep()));
        boolean hasValidating = payloadCaptor.getAllValues().stream().anyMatch(p -> "UI_CUSTOMIZATION_VALIDATING".equals(p.getStep()));
        boolean hasReady = payloadCaptor.getAllValues().stream().anyMatch(p -> "UI_CUSTOMIZATION_READY".equals(p.getStep()));
        assertEquals(true, hasAnalyzing);
        assertEquals(true, hasValidating);
        assertEquals(true, hasReady);
    }

    @Test
    void process_routesNonUiTargetToSearchServiceWithAuthoritativeIntent() {
        SecurityUtils securityUtils = mock(SecurityUtils.class);
        UsuarioRepository usuarioRepository = mock(UsuarioRepository.class);
        AiSearchIntentMicroserviceClient intentClient = mock(AiSearchIntentMicroserviceClient.class);
        AiEncryptedMessageSearchService searchService = mock(AiEncryptedMessageSearchService.class);
        AiUiCustomizationValidationService validationService = new AiUiCustomizationValidationService();
        SimpMessagingTemplate messagingTemplate = mock(SimpMessagingTemplate.class);
        AiSmartActionHistoryService historyService = mock(AiSmartActionHistoryService.class);
        AiUiCustomizationMicroserviceClient uiCustomizationMicroserviceClient = mock(AiUiCustomizationMicroserviceClient.class);

        when(securityUtils.getAuthenticatedUserId()).thenReturn(7L);
        when(securityUtils.getAuthenticatedUserEmail()).thenReturn("user@test.com");
        when(usuarioRepository.findById(7L)).thenReturn(Optional.empty());

        AiSearchIntentInternalResponseDTO intent = new AiSearchIntentInternalResponseDTO();
        intent.setSuccess(true);
        intent.setTarget("APP_REPORT_STATUS");
        intent.setTipoReporte("ANY");
        intent.setListMode(true);
        intent.setLimitSolicitado(2);
        intent.setOrden("LATEST");
        intent.setConfidence(0.95d);
        when(intentClient.classifyIntent(anyString(), any())).thenReturn(intent);

        AiEncryptedMessageSearchResponseDTO searchResponse = new AiEncryptedMessageSearchResponseDTO();
        searchResponse.setSuccess(true);
        searchResponse.setCodigo("APP_REPORT_STATUS_OK");
        when(searchService.buscarMensajes(any(), anyString(), any(), anyBoolean())).thenReturn(searchResponse);

        AiSmartActionServiceImpl service = new AiSmartActionServiceImpl(
                securityUtils, usuarioRepository, intentClient, searchService, validationService, uiCustomizationMicroserviceClient, messagingTemplate, historyService
        );
        AiSmartActionRequestDTO request = new AiSmartActionRequestDTO();
        request.setConsulta("sacame los ultimos 2 reportes");

        Object response = service.process(request);

        assertInstanceOf(AiEncryptedMessageSearchResponseDTO.class, response);
        assertEquals("APP_REPORT_STATUS_OK", ((AiEncryptedMessageSearchResponseDTO) response).getCodigo());
        verify(searchService).buscarMensajes(any(), anyString(), any(), anyBoolean());
    }

    @Test
    void process_uiCustomizationValidationFailure_emitsFailedProgress() {
        SecurityUtils securityUtils = mock(SecurityUtils.class);
        UsuarioRepository usuarioRepository = mock(UsuarioRepository.class);
        AiSearchIntentMicroserviceClient intentClient = mock(AiSearchIntentMicroserviceClient.class);
        AiEncryptedMessageSearchService searchService = mock(AiEncryptedMessageSearchService.class);
        AiUiCustomizationValidationService validationService = new AiUiCustomizationValidationService();
        SimpMessagingTemplate messagingTemplate = mock(SimpMessagingTemplate.class);
        AiSmartActionHistoryService historyService = mock(AiSmartActionHistoryService.class);
        AiUiCustomizationMicroserviceClient uiCustomizationMicroserviceClient = mock(AiUiCustomizationMicroserviceClient.class);

        when(securityUtils.getAuthenticatedUserId()).thenReturn(7L);
        when(securityUtils.getAuthenticatedUserEmail()).thenReturn("user@test.com");
        when(usuarioRepository.findById(7L)).thenReturn(Optional.empty());

        AiSearchIntentInternalResponseDTO intent = new AiSearchIntentInternalResponseDTO();
        intent.setSuccess(true);
        intent.setTarget("UI_CUSTOMIZATION");
        intent.setAction("UPDATE_STYLE");
        intent.setArea("CHAT_MESSAGES_AREA");
        intent.setProperty("BACKGROUND_COLOR");
        intent.setValue("invalid");
        intent.setConfidence(0.99d);
        when(intentClient.classifyIntent(anyString(), any())).thenReturn(intent);

        AiSmartActionServiceImpl service = new AiSmartActionServiceImpl(
                securityUtils, usuarioRepository, intentClient, searchService, validationService, uiCustomizationMicroserviceClient, messagingTemplate, historyService
        );
        AiSmartActionRequestDTO request = new AiSmartActionRequestDTO();
        request.setConsulta("cambia fondo del chat a valor raro");

        service.process(request);

        ArgumentCaptor<AiSearchProgressWS> payloadCaptor = ArgumentCaptor.forClass(AiSearchProgressWS.class);
        verify(messagingTemplate, atLeast(1)).convertAndSendToUser(anyString(), anyString(), payloadCaptor.capture());
        boolean hasFailed = payloadCaptor.getAllValues().stream().anyMatch(p -> "UI_CUSTOMIZATION_FAILED".equals(p.getStep()));
        assertEquals(true, hasFailed);
    }

    @Test
    void process_overridesMessageSearchToUiCustomizationForClearVisualChatListTheme() {
        SecurityUtils securityUtils = mock(SecurityUtils.class);
        UsuarioRepository usuarioRepository = mock(UsuarioRepository.class);
        AiSearchIntentMicroserviceClient intentClient = mock(AiSearchIntentMicroserviceClient.class);
        AiEncryptedMessageSearchService searchService = mock(AiEncryptedMessageSearchService.class);
        AiUiCustomizationValidationService validationService = new AiUiCustomizationValidationService();
        SimpMessagingTemplate messagingTemplate = mock(SimpMessagingTemplate.class);
        AiSmartActionHistoryService historyService = mock(AiSmartActionHistoryService.class);
        AiUiCustomizationMicroserviceClient uiCustomizationMicroserviceClient = mock(AiUiCustomizationMicroserviceClient.class);

        when(securityUtils.getAuthenticatedUserId()).thenReturn(7L);
        when(securityUtils.getAuthenticatedUserEmail()).thenReturn("user@test.com");
        when(usuarioRepository.findById(7L)).thenReturn(Optional.empty());

        AiSearchIntentInternalResponseDTO intent = new AiSearchIntentInternalResponseDTO();
        intent.setSuccess(true);
        intent.setTarget("MESSAGES");
        intent.setConfidence(0.93d);
        when(intentClient.classifyIntent(anyString(), any())).thenReturn(intent);

        AiSmartActionServiceImpl service = new AiSmartActionServiceImpl(
                securityUtils, usuarioRepository, intentClient, searchService, validationService, uiCustomizationMicroserviceClient, messagingTemplate, historyService
        );
        AiSmartActionRequestDTO request = new AiSmartActionRequestDTO();
        request.setConsulta("cambiame todo el listado de chat a un chat oscuro en plan darkMode");

        Object response = service.process(request);

        assertInstanceOf(AiUiCustomizationResponseDTO.class, response);
        AiUiCustomizationResponseDTO uiResponse = (AiUiCustomizationResponseDTO) response;
        assertTrue(uiResponse.isSuccess());
        assertEquals("UI_CUSTOMIZATION_OK", uiResponse.getCodigo());
        assertEquals("UPDATE_STYLE_MULTI", uiResponse.getAction());
        verify(searchService, never()).buscarMensajes(any());
    }

    @Test
    void process_keepsExplicitMessageSearchInSearchBranch() {
        SecurityUtils securityUtils = mock(SecurityUtils.class);
        UsuarioRepository usuarioRepository = mock(UsuarioRepository.class);
        AiSearchIntentMicroserviceClient intentClient = mock(AiSearchIntentMicroserviceClient.class);
        AiEncryptedMessageSearchService searchService = mock(AiEncryptedMessageSearchService.class);
        AiUiCustomizationValidationService validationService = new AiUiCustomizationValidationService();
        SimpMessagingTemplate messagingTemplate = mock(SimpMessagingTemplate.class);
        AiSmartActionHistoryService historyService = mock(AiSmartActionHistoryService.class);
        AiUiCustomizationMicroserviceClient uiCustomizationMicroserviceClient = mock(AiUiCustomizationMicroserviceClient.class);

        when(securityUtils.getAuthenticatedUserId()).thenReturn(7L);
        when(securityUtils.getAuthenticatedUserEmail()).thenReturn("user@test.com");
        when(usuarioRepository.findById(7L)).thenReturn(Optional.empty());

        AiSearchIntentInternalResponseDTO intent = new AiSearchIntentInternalResponseDTO();
        intent.setSuccess(true);
        intent.setTarget("MESSAGES");
        intent.setConfidence(0.93d);
        when(intentClient.classifyIntent(anyString(), any())).thenReturn(intent);

        AiEncryptedMessageSearchResponseDTO searchResponse = new AiEncryptedMessageSearchResponseDTO();
        searchResponse.setSuccess(true);
        searchResponse.setCodigo("AI_MESSAGE_SEARCH_OK");
        when(searchService.buscarMensajes(any(), anyString(), any(), anyBoolean())).thenReturn(searchResponse);

        AiSmartActionServiceImpl service = new AiSmartActionServiceImpl(
                securityUtils, usuarioRepository, intentClient, searchService, validationService, uiCustomizationMicroserviceClient, messagingTemplate, historyService
        );
        AiSmartActionRequestDTO request = new AiSmartActionRequestDTO();
        request.setConsulta("busca mensajes sobre dark mode");

        Object response = service.process(request);

        assertInstanceOf(AiEncryptedMessageSearchResponseDTO.class, response);
        assertEquals("AI_MESSAGE_SEARCH_OK", ((AiEncryptedMessageSearchResponseDTO) response).getCodigo());
        verify(searchService).buscarMensajes(any(), anyString(), any(), anyBoolean());
    }

    @Test
    void process_repairsGroupActiveWhenIntentComesAsNeedsClarification() {
        SecurityUtils securityUtils = mock(SecurityUtils.class);
        UsuarioRepository usuarioRepository = mock(UsuarioRepository.class);
        AiSearchIntentMicroserviceClient intentClient = mock(AiSearchIntentMicroserviceClient.class);
        AiEncryptedMessageSearchService searchService = mock(AiEncryptedMessageSearchService.class);
        AiUiCustomizationValidationService validationService = new AiUiCustomizationValidationService();
        SimpMessagingTemplate messagingTemplate = mock(SimpMessagingTemplate.class);
        AiSmartActionHistoryService historyService = mock(AiSmartActionHistoryService.class);
        AiUiCustomizationMicroserviceClient uiCustomizationMicroserviceClient = mock(AiUiCustomizationMicroserviceClient.class);

        when(securityUtils.getAuthenticatedUserId()).thenReturn(7L);
        when(securityUtils.getAuthenticatedUserEmail()).thenReturn("user@test.com");
        when(usuarioRepository.findById(7L)).thenReturn(Optional.empty());

        AiSearchIntentInternalResponseDTO intent = new AiSearchIntentInternalResponseDTO();
        intent.setSuccess(true);
        intent.setTarget("UI_CUSTOMIZATION");
        intent.setAction("NEEDS_CLARIFICATION");
        intent.setArea(null);
        intent.setProperty(null);
        intent.setValue(null);
        intent.setLabel("Cambiar chat grupal seleccionado");
        intent.setConfidence(0.95d);
        when(intentClient.classifyIntent(anyString(), any())).thenReturn(intent);

        AiSmartActionServiceImpl service = new AiSmartActionServiceImpl(
                securityUtils, usuarioRepository, intentClient, searchService, validationService, uiCustomizationMicroserviceClient, messagingTemplate, historyService
        );
        AiSmartActionRequestDTO request = new AiSmartActionRequestDTO();
        request.setConsulta("Pon el chat grupal seleccionado con fondo morado, texto blanco y borde amarillo");

        Object response = service.process(request);

        assertInstanceOf(AiUiCustomizationResponseDTO.class, response);
        AiUiCustomizationResponseDTO ui = (AiUiCustomizationResponseDTO) response;
        assertTrue(ui.isSuccess());
        assertEquals("UI_CUSTOMIZATION_OK", ui.getCodigo());
        assertEquals("UPDATE_STYLE_MULTI", ui.getAction());
        assertTrue(ui.getChanges().stream().anyMatch(c ->
                "CHAT_LIST_ITEM_GROUP_ACTIVE".equals(c.getArea()) && "BACKGROUND_COLOR".equals(c.getProperty())));
    }

    @Test
    void process_repairsSingleGroupActivePropertyBeforeValidation() {
        SecurityUtils securityUtils = mock(SecurityUtils.class);
        UsuarioRepository usuarioRepository = mock(UsuarioRepository.class);
        AiSearchIntentMicroserviceClient intentClient = mock(AiSearchIntentMicroserviceClient.class);
        AiEncryptedMessageSearchService searchService = mock(AiEncryptedMessageSearchService.class);
        AiUiCustomizationValidationService validationService = new AiUiCustomizationValidationService();
        SimpMessagingTemplate messagingTemplate = mock(SimpMessagingTemplate.class);
        AiSmartActionHistoryService historyService = mock(AiSmartActionHistoryService.class);
        AiUiCustomizationMicroserviceClient uiCustomizationMicroserviceClient = mock(AiUiCustomizationMicroserviceClient.class);

        when(securityUtils.getAuthenticatedUserId()).thenReturn(7L);
        when(securityUtils.getAuthenticatedUserEmail()).thenReturn("user@test.com");
        when(usuarioRepository.findById(7L)).thenReturn(Optional.empty());

        AiSearchIntentInternalResponseDTO intent = new AiSearchIntentInternalResponseDTO();
        intent.setSuccess(true);
        intent.setTarget("UI_CUSTOMIZATION");
        intent.setAction("NEEDS_CLARIFICATION");
        intent.setArea("CHAT_LIST_ITEM_ACTIVE");
        intent.setProperty(null);
        intent.setValue(null);
        intent.setLabel("Cambiar fondo del chat grupal activo a verde");
        intent.setConfidence(0.60d);
        when(intentClient.classifyIntent(anyString(), any())).thenReturn(intent);

        AiSmartActionServiceImpl service = new AiSmartActionServiceImpl(
                securityUtils, usuarioRepository, intentClient, searchService, validationService, uiCustomizationMicroserviceClient, messagingTemplate, historyService
        );
        AiSmartActionRequestDTO request = new AiSmartActionRequestDTO();
        request.setConsulta("Cambiar fondo del chat grupal activo a verde");

        Object response = service.process(request);

        assertInstanceOf(AiUiCustomizationResponseDTO.class, response);
        AiUiCustomizationResponseDTO ui = (AiUiCustomizationResponseDTO) response;
        assertTrue(ui.isSuccess());
        assertEquals("UPDATE_STYLE", ui.getAction());
        assertEquals("CHAT_LIST_ITEM_GROUP_ACTIVE", ui.getArea());
        assertEquals("BACKGROUND_COLOR", ui.getProperty());
    }

    @Test
    void process_resolvesUiAreaFromStructuredScope() {
        SecurityUtils securityUtils = mock(SecurityUtils.class);
        UsuarioRepository usuarioRepository = mock(UsuarioRepository.class);
        AiSearchIntentMicroserviceClient intentClient = mock(AiSearchIntentMicroserviceClient.class);
        AiEncryptedMessageSearchService searchService = mock(AiEncryptedMessageSearchService.class);
        AiUiCustomizationValidationService validationService = new AiUiCustomizationValidationService();
        SimpMessagingTemplate messagingTemplate = mock(SimpMessagingTemplate.class);
        AiSmartActionHistoryService historyService = mock(AiSmartActionHistoryService.class);
        AiUiCustomizationMicroserviceClient uiCustomizationMicroserviceClient = mock(AiUiCustomizationMicroserviceClient.class);

        when(securityUtils.getAuthenticatedUserId()).thenReturn(7L);
        when(securityUtils.getAuthenticatedUserEmail()).thenReturn("user@test.com");
        when(usuarioRepository.findById(7L)).thenReturn(Optional.empty());

        UiCustomizationScopeDTO scope = new UiCustomizationScopeDTO();
        scope.setModule("CHAT_LIST");
        scope.setElement("CHAT_ITEM");
        scope.setChatType("GROUP");
        scope.setState("ACTIVE");

        UiCustomizationChangeDTO bg = new UiCustomizationChangeDTO();
        bg.setProperty("BACKGROUND_COLOR");
        bg.setValue("#d8b4fe");
        UiCustomizationChangeDTO text = new UiCustomizationChangeDTO();
        text.setProperty("TEXT_COLOR");
        text.setValue("#ffffff");

        AiSearchIntentInternalResponseDTO intent = new AiSearchIntentInternalResponseDTO();
        intent.setSuccess(true);
        intent.setTarget("UI_CUSTOMIZATION");
        intent.setAction("NEEDS_CLARIFICATION");
        intent.setScope(scope);
        intent.setNeedsClarification(true);
        intent.setChanges(List.of(bg, text));
        intent.setConfidence(0.92d);
        when(intentClient.classifyIntent(anyString(), any())).thenReturn(intent);

        AiSmartActionServiceImpl service = new AiSmartActionServiceImpl(
                securityUtils, usuarioRepository, intentClient, searchService, validationService, uiCustomizationMicroserviceClient, messagingTemplate, historyService
        );
        AiSmartActionRequestDTO request = new AiSmartActionRequestDTO();
        request.setConsulta("Poner fondo azul muy suave en chats grupales activos y textos en rojo");

        Object response = service.process(request);

        assertInstanceOf(AiUiCustomizationResponseDTO.class, response);
        AiUiCustomizationResponseDTO ui = (AiUiCustomizationResponseDTO) response;
        assertTrue(ui.isSuccess());
        assertEquals("UPDATE_STYLE_MULTI", ui.getAction());
        assertTrue(ui.getChanges().stream().allMatch(c -> "CHAT_LIST_ITEM_GROUP_ACTIVE".equals(c.getArea())));
    }

    @Test
    void process_repairsPluralGroupActiveFromTextBeforeValidation() {
        SecurityUtils securityUtils = mock(SecurityUtils.class);
        UsuarioRepository usuarioRepository = mock(UsuarioRepository.class);
        AiSearchIntentMicroserviceClient intentClient = mock(AiSearchIntentMicroserviceClient.class);
        AiEncryptedMessageSearchService searchService = mock(AiEncryptedMessageSearchService.class);
        AiUiCustomizationValidationService validationService = new AiUiCustomizationValidationService();
        SimpMessagingTemplate messagingTemplate = mock(SimpMessagingTemplate.class);
        AiSmartActionHistoryService historyService = mock(AiSmartActionHistoryService.class);
        AiUiCustomizationMicroserviceClient uiCustomizationMicroserviceClient = mock(AiUiCustomizationMicroserviceClient.class);

        when(securityUtils.getAuthenticatedUserId()).thenReturn(7L);
        when(securityUtils.getAuthenticatedUserEmail()).thenReturn("user@test.com");
        when(usuarioRepository.findById(7L)).thenReturn(Optional.empty());

        AiSearchIntentInternalResponseDTO intent = new AiSearchIntentInternalResponseDTO();
        intent.setSuccess(true);
        intent.setTarget("UI_CUSTOMIZATION");
        intent.setAction("NEEDS_CLARIFICATION");
        intent.setArea(null);
        intent.setProperty(null);
        intent.setValue(null);
        intent.setLabel("Fondo azul muy suave y texto rojo en chats grupales activos");
        intent.setConfidence(0.60d);
        when(intentClient.classifyIntent(anyString(), any())).thenReturn(intent);

        AiSmartActionServiceImpl service = new AiSmartActionServiceImpl(
                securityUtils, usuarioRepository, intentClient, searchService, validationService, uiCustomizationMicroserviceClient, messagingTemplate, historyService
        );
        AiSmartActionRequestDTO request = new AiSmartActionRequestDTO();
        request.setConsulta("Poner fondo azul muy suave en chats grupales activos y textos en rojo");

        Object response = service.process(request);

        assertInstanceOf(AiUiCustomizationResponseDTO.class, response);
        AiUiCustomizationResponseDTO ui = (AiUiCustomizationResponseDTO) response;
        assertTrue(ui.isSuccess());
        assertEquals("UPDATE_STYLE_MULTI", ui.getAction());
        assertTrue(ui.getChanges().stream().anyMatch(c ->
                "CHAT_LIST_ITEM_GROUP_ACTIVE".equals(c.getArea()) && "BACKGROUND_COLOR".equals(c.getProperty())));
        assertTrue(ui.getChanges().stream().anyMatch(c ->
                "CHAT_LIST_ITEM_GROUP_ACTIVE".equals(c.getArea()) && "TEXT_COLOR".equals(c.getProperty())));
    }
}
