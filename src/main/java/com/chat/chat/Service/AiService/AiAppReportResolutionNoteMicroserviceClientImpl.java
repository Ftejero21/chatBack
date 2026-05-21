package com.chat.chat.Service.AiService;

import com.chat.chat.Configuracion.TejechatAiServiceProperties;
import com.chat.chat.DTO.AiAppReportResolutionNoteInternalRequestDTO;
import com.chat.chat.DTO.AiAppReportResolutionNoteInternalResponseDTO;
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
public class AiAppReportResolutionNoteMicroserviceClientImpl implements AiAppReportResolutionNoteMicroserviceClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(AiAppReportResolutionNoteMicroserviceClientImpl.class);
    private static final String INTERNAL_API_KEY_HEADER = "X-Internal-Api-Key";
    private static final String REQUEST_ID_HEADER = "X-Request-Id";

    private final RestTemplate restTemplate;
    private final TejechatAiServiceProperties tejechatAiServiceProperties;
    private final AiUsageMetricAuthenticatedCaptureService usageCaptureService;
    private final AiUsageLimitService aiUsageLimitService;

    public AiAppReportResolutionNoteMicroserviceClientImpl(@Qualifier("aiMessageSearchRestTemplate") RestTemplate restTemplate,
                                                           TejechatAiServiceProperties tejechatAiServiceProperties,
                                                           AiUsageMetricAuthenticatedCaptureService usageCaptureService,
                                                           AiUsageLimitService aiUsageLimitService) {
        this.restTemplate = restTemplate;
        this.tejechatAiServiceProperties = tejechatAiServiceProperties;
        this.usageCaptureService = usageCaptureService;
        this.aiUsageLimitService = aiUsageLimitService;
    }

    @Override
    public AiAppReportResolutionNoteInternalResponseDTO generateResolutionNote(String requestId,
                                                                               AiAppReportResolutionNoteInternalRequestDTO request) {
        aiUsageLimitService.assertCurrentUserCanUseAi();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set(INTERNAL_API_KEY_HEADER, tejechatAiServiceProperties.getInternalKey());
        if (requestId != null && !requestId.isBlank()) {
            headers.set(REQUEST_ID_HEADER, requestId);
        }

        LOGGER.info("[AI][APP_REPORT_RESOLUTION_NOTE_CLIENT] requestId={} outbound tipoReporte={} estadoDestino={} motivoLength={}",
                requestId,
                request == null ? null : request.getTipoReporte(),
                request == null ? null : request.getEstadoDestino(),
                request == null || request.getMotivo() == null ? 0 : request.getMotivo().length());

        try {
            ResponseEntity<AiAppReportResolutionNoteInternalResponseDTO> response = restTemplate.exchange(
                    tejechatAiServiceProperties.buildAppReportResolutionNoteUrl(),
                    HttpMethod.POST,
                    new HttpEntity<>(request, headers),
                    AiAppReportResolutionNoteInternalResponseDTO.class
            );
            AiAppReportResolutionNoteInternalResponseDTO body = response.getBody();
            usageCaptureService.capture(
                    requestId,
                    "APP_REPORT_RESOLUTION_NOTE",
                    null,
                    "APP_REPORT",
                    body == null ? null : body.getUsage(),
                    body != null && body.isSuccess(),
                    body == null ? "AI_SERVICE_EMPTY_RESPONSE" : body.getCodigo()
            );
            LOGGER.info("[AI][APP_REPORT_RESOLUTION_NOTE_CLIENT] requestId={} inbound success={} codigo={} resolucionLength={}",
                    requestId,
                    body != null && body.isSuccess(),
                    body == null ? null : body.getCodigo(),
                    body == null || body.getResolucionMotivo() == null ? 0 : body.getResolucionMotivo().length());
            return body;
        } catch (ResourceAccessException ex) {
            usageCaptureService.capture(requestId, "APP_REPORT_RESOLUTION_NOTE", null, "APP_REPORT", null, false, "AI_SERVICE_UNAVAILABLE");
            LOGGER.warn("[AI][APP_REPORT_RESOLUTION_NOTE_CLIENT] requestId={} service-unavailable type={}",
                    requestId, ex.getClass().getSimpleName());
            return null;
        } catch (HttpStatusCodeException ex) {
            usageCaptureService.capture(requestId, "APP_REPORT_RESOLUTION_NOTE", null, "APP_REPORT", null, false, "AI_SERVICE_ERROR");
            LOGGER.warn("[AI][APP_REPORT_RESOLUTION_NOTE_CLIENT] requestId={} service-error status={}",
                    requestId, ex.getStatusCode().value());
            return null;
        } catch (RestClientException ex) {
            usageCaptureService.capture(requestId, "APP_REPORT_RESOLUTION_NOTE", null, "APP_REPORT", null, false, "AI_SERVICE_ERROR");
            LOGGER.warn("[AI][APP_REPORT_RESOLUTION_NOTE_CLIENT] requestId={} service-error type={}",
                    requestId, ex.getClass().getSimpleName());
            return null;
        }
    }
}
