package com.chat.chat.Service.AiService;

import com.chat.chat.Configuracion.TejechatAiServiceProperties;
import com.chat.chat.DTO.AiSearchIntentInternalRequestDTO;
import com.chat.chat.DTO.AiSearchIntentInternalResponseDTO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Service
public class AiSearchIntentMicroserviceClientImpl implements AiSearchIntentMicroserviceClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(AiSearchIntentMicroserviceClientImpl.class);
    private static final String INTERNAL_API_KEY_HEADER = "X-Internal-Api-Key";
    private static final String REQUEST_ID_HEADER = "X-Request-Id";

    private final RestTemplate restTemplate;
    private final TejechatAiServiceProperties tejechatAiServiceProperties;
    private final ObjectMapper objectMapper;
    private final AiUsageMetricAuthenticatedCaptureService usageCaptureService;
    private final AiUsageLimitService aiUsageLimitService;

    public AiSearchIntentMicroserviceClientImpl(@Qualifier("aiMessageSearchRestTemplate") RestTemplate restTemplate,
                                                TejechatAiServiceProperties tejechatAiServiceProperties,
                                                ObjectMapper objectMapper,
                                                AiUsageMetricAuthenticatedCaptureService usageCaptureService,
                                                AiUsageLimitService aiUsageLimitService) {
        this.restTemplate = restTemplate;
        this.tejechatAiServiceProperties = tejechatAiServiceProperties;
        this.objectMapper = objectMapper;
        this.usageCaptureService = usageCaptureService;
        this.aiUsageLimitService = aiUsageLimitService;
    }

    @Override
    public AiSearchIntentInternalResponseDTO classifyIntent(String requestId, AiSearchIntentInternalRequestDTO request) {
        aiUsageLimitService.assertCurrentUserCanUseAi();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set(INTERNAL_API_KEY_HEADER, tejechatAiServiceProperties.getInternalKey());
        if (requestId != null && !requestId.isBlank()) {
            headers.set(REQUEST_ID_HEADER, requestId);
        }

        LOGGER.info("[AI][SEARCH_INTENT_CLIENT] requestId={} outbound consulta=\"{}\" usuarioActualNombre=\"{}\"",
                requestId,
                safe(request == null ? null : request.getConsulta()),
                safe(request == null ? null : request.getUsuarioActualNombre()));

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    tejechatAiServiceProperties.buildSearchIntentUrl(),
                    HttpMethod.POST,
                    new HttpEntity<>(request, headers),
                    String.class
            );
            LOGGER.info("[AI][SEARCH_INTENT_CLIENT] requestId={} service-status={} hasBody={}",
                    requestId, response.getStatusCode().value(), response.getBody() != null);
            String rawBody = response.getBody();
            LOGGER.info("[AI][SEARCH_INTENT_CLIENT][RAW_BODY] requestId={} body=<<<{}>>>",
                    requestId, safeRawBody(rawBody));
            AiSearchIntentInternalResponseDTO body = mapResponseBody(requestId, rawBody);
            if (body != null) {
                body.setTarget(normalizeTarget(body.getTarget()));
                usageCaptureService.capture(requestId, "SMART_INTENT", body.getAction(), body.getTarget(), body.getUsage(), body.isSuccess(), body.getCodigo());
                LOGGER.info("[AI][SEARCH_INTENT_CLIENT][MAPPED_DTO] requestId={} target={} tipoReporte={} reportStatus={} complaintStatus={} temporalExpression={} confidence={} listMode={} action={} area={} property={}",
                        requestId, body.getTarget(), body.getTipoReporte(), body.getReportStatus(), body.getComplaintStatus(), safe(body.getTemporalExpression()), body.getConfidence(), body.getListMode(), body.getAction(), body.getArea(), body.getProperty());
                LOGGER.info("[AI][SEARCH_INTENT_CLIENT] requestId={} inbound success={} codigo={} target={} tipoReporte={} motivoReporte=\"{}\" reportStatus={} complaintStatus={} complaintDirection={} senderScope={} tipoScopeSolicitado={} tipoMensajeSolicitado={} readStatus={} personaMencionada=\"{}\" grupoMencionado=\"{}\" temporalExpression=\"{}\" orden={} confidence={} listMode={} action={} area={} property={} value=\"{}\" valuePreset={} label=\"{}\"",
                        requestId,
                        body.isSuccess(),
                        body.getCodigo(),
                        body.getTarget(),
                        body.getTipoReporte(),
                        safe(body.getMotivoReporte()),
                        body.getReportStatus(),
                        body.getComplaintStatus(),
                        body.getComplaintDirection(),
                        body.getSenderScope(),
                        body.getTipoScopeSolicitado(),
                        body.getTipoMensajeSolicitado(),
                        body.getReadStatus(),
                        safe(body.getPersonaMencionada()),
                        safe(body.getGrupoMencionado()),
                        safe(body.getTemporalExpression()),
                        body.getOrden(),
                        body.getConfidence(),
                        body.getListMode(),
                        body.getAction(),
                        body.getArea(),
                        body.getProperty(),
                        safe(body.getValue()),
                        body.getValuePreset(),
                        safe(body.getLabel()));
            }
            return body;
        } catch (ResourceAccessException ex) {
            usageCaptureService.capture(requestId, "SMART_INTENT", null, null, null, false, "AI_SERVICE_UNAVAILABLE");
            LOGGER.warn("[AI][SEARCH_INTENT_CLIENT] requestId={} service-unavailable type={}", requestId, ex.getClass().getSimpleName());
            return null;
        } catch (HttpStatusCodeException ex) {
            usageCaptureService.capture(requestId, "SMART_INTENT", null, null, null, false, "AI_SERVICE_ERROR");
            LOGGER.warn("[AI][SEARCH_INTENT_CLIENT] requestId={} service-error status={}", requestId, ex.getStatusCode().value());
            return null;
        } catch (RestClientException ex) {
            usageCaptureService.capture(requestId, "SMART_INTENT", null, null, null, false, "AI_SERVICE_ERROR");
            LOGGER.warn("[AI][SEARCH_INTENT_CLIENT] requestId={} service-error type={}", requestId, ex.getClass().getSimpleName());
            return null;
        }
    }

    private String safe(String value) {
        if (value == null) {
            return "";
        }
        return value.replaceAll("\\s+", " ").trim();
    }

    private String safeRawBody(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\r", "\\r").replace("\n", "\\n");
    }

    private AiSearchIntentInternalResponseDTO mapResponseBody(String requestId, String rawBody) {
        if (rawBody == null || rawBody.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(rawBody, AiSearchIntentInternalResponseDTO.class);
        } catch (JsonProcessingException ex) {
            LOGGER.warn("[AI][SEARCH_INTENT_CLIENT] requestId={} mapping-error type={}", requestId, ex.getClass().getSimpleName());
            return null;
        }
    }

    private String normalizeTarget(String target) {
        if (target == null) {
            return null;
        }
        String normalized = target.trim().toUpperCase();
        return normalized.isBlank() ? null : normalized;
    }
}
