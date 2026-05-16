package com.chat.chat.Service.AiService;

import com.chat.chat.Configuracion.AiProperties;
import com.chat.chat.DTO.AiEncryptedMessageSearchRequestDTO;
import com.chat.chat.DTO.AiSearchIntentInternalResponseDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class DeepSeekAiEncryptedMessageSearchServiceImplIntentRoutingTest {

    private DeepSeekAiEncryptedMessageSearchServiceImpl buildService() {
        return new DeepSeekAiEncryptedMessageSearchServiceImpl(
                new AiProperties(),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                new DeterministicAiMessageSearchNaturalQueryAnalyzer(),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                new ObjectMapper(),
                null,
                null,
                null,
                null,
                null
        );
    }

    private Object invokePrivate(Object target, String methodName, Class<?>[] parameterTypes, Object... args) throws Exception {
        Method method = target.getClass().getDeclaredMethod(methodName, parameterTypes);
        method.setAccessible(true);
        return method.invoke(target, args);
    }

    @Test
    void resolveComplaintBranch_ignoresComplaintFallbackWhenLlmLockedTargetIsMessages() throws Exception {
        DeepSeekAiEncryptedMessageSearchServiceImpl service = buildService();
        AiSearchIntentInternalResponseDTO intent = new AiSearchIntentInternalResponseDTO();
        intent.setSuccess(true);
        intent.setTarget("MESSAGES");
        intent.setComplaintDirection("CREATED");
        intent.setConfidence(0.95d);

        AiMessageSearchNaturalQueryAnalysis analysis = new AiMessageSearchNaturalQueryAnalysis();
        analysis.setIntencionDenunciaCreada(true);

        Object result = invokePrivate(
                service,
                "resolveComplaintBranch",
                new Class[]{AiSearchIntentInternalResponseDTO.class, AiMessageSearchNaturalQueryAnalysis.class},
                intent,
                analysis
        );

        assertEquals("NONE", result.toString());
    }

    @Test
    void resolveComplaintBranch_usesComplaintTargetWhenLlmLocksComplaintDomain() throws Exception {
        DeepSeekAiEncryptedMessageSearchServiceImpl service = buildService();
        AiSearchIntentInternalResponseDTO intent = new AiSearchIntentInternalResponseDTO();
        intent.setSuccess(true);
        intent.setTarget("COMPLAINTS_CREATED");
        intent.setComplaintDirection("CREATED");
        intent.setConfidence(0.95d);

        Object result = invokePrivate(
                service,
                "resolveComplaintBranch",
                new Class[]{AiSearchIntentInternalResponseDTO.class, AiMessageSearchNaturalQueryAnalysis.class},
                intent,
                new AiMessageSearchNaturalQueryAnalysis()
        );

        assertEquals("CREATED", result.toString());
    }

    @Test
    void shouldSearchScheduledMessages_doesNotOverrideLockedMessagesTarget() throws Exception {
        DeepSeekAiEncryptedMessageSearchServiceImpl service = buildService();
        AiSearchIntentInternalResponseDTO intent = new AiSearchIntentInternalResponseDTO();
        intent.setSuccess(true);
        intent.setTarget("MESSAGES");
        intent.setConfidence(0.91d);

        boolean result = (boolean) invokePrivate(
                service,
                "shouldSearchScheduledMessages",
                new Class[]{AiSearchIntentInternalResponseDTO.class, String.class, AiMessageSearchNaturalQueryAnalysis.class},
                intent,
                "que mensaje programado tengo para mañana",
                new AiMessageSearchNaturalQueryAnalysis()
        );

        assertFalse(result);
    }

    @Test
    void resolveSearchIntent_listModeLatestDoesNotBecomeSingularDirectSearch() throws Exception {
        DeepSeekAiEncryptedMessageSearchServiceImpl service = buildService();
        AiSearchIntentInternalResponseDTO intent = new AiSearchIntentInternalResponseDTO();
        intent.setSuccess(true);
        intent.setTarget("MESSAGES");
        intent.setOrden("LATEST");
        intent.setListMode(true);
        intent.setConfidence(0.95d);

        Object searchIntent = invokePrivate(
                service,
                "resolveSearchIntent",
                new Class[]{String.class, AiMessageSearchNaturalQueryAnalysis.class, AiSearchIntentInternalResponseDTO.class},
                "dame todos los mensajes que he puesto los ultimos 2 dias",
                new AiMessageSearchNaturalQueryAnalysis(),
                intent
        );

        Method directResolution = searchIntent.getClass().getDeclaredMethod("directResolution");
        directResolution.setAccessible(true);
        assertFalse((boolean) directResolution.invoke(searchIntent));
    }

    @Test
    void applyMessageIntentOverrides_setsListLimitAndTemporalRangeForLockedMessages() throws Exception {
        DeepSeekAiEncryptedMessageSearchServiceImpl service = buildService();
        AiEncryptedMessageSearchRequestDTO request = new AiEncryptedMessageSearchRequestDTO();
        request.setConsulta("dame todos los mensajes que he puesto");

        Object validationValues = invokePrivate(
                service,
                "validateAndResolve",
                new Class[]{AiEncryptedMessageSearchRequestDTO.class},
                request
        );

        AiSearchIntentInternalResponseDTO intent = new AiSearchIntentInternalResponseDTO();
        intent.setSuccess(true);
        intent.setTarget("MESSAGES");
        intent.setListMode(true);
        intent.setOrden("LATEST");
        intent.setTemporalExpression("los ultimos 2 dias");
        intent.setConfidence(0.95d);

        Object updatedValues = invokePrivate(
                service,
                "applyMessageIntentOverrides",
                new Class[]{String.class, validationValues.getClass(), AiSearchIntentInternalResponseDTO.class},
                "req-1",
                validationValues,
                intent
        );

        Method maxResultados = updatedValues.getClass().getDeclaredMethod("maxResultados");
        Method rangoTemporalDetectado = updatedValues.getClass().getDeclaredMethod("rangoTemporalDetectado");
        Method fechaInicio = updatedValues.getClass().getDeclaredMethod("fechaInicio");
        Method fechaFin = updatedValues.getClass().getDeclaredMethod("fechaFin");
        maxResultados.setAccessible(true);
        rangoTemporalDetectado.setAccessible(true);
        fechaInicio.setAccessible(true);
        fechaFin.setAccessible(true);

        assertEquals(20, maxResultados.invoke(updatedValues));
        assertEquals(true, rangoTemporalDetectado.invoke(updatedValues));
        assertNotNull(fechaInicio.invoke(updatedValues));
        assertNotNull(fechaFin.invoke(updatedValues));
    }
}
