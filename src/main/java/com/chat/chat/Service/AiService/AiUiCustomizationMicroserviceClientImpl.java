package com.chat.chat.Service.AiService;

import com.chat.chat.Configuracion.TejechatAiServiceProperties;
import com.chat.chat.DTO.AiUiCustomizationIntentInternalRequestDTO;
import com.chat.chat.DTO.AiUiCustomizationIntentInternalResponseDTO;
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
public class AiUiCustomizationMicroserviceClientImpl implements AiUiCustomizationMicroserviceClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(AiUiCustomizationMicroserviceClientImpl.class);
    private static final String INTERNAL_API_KEY_HEADER = "X-Internal-Api-Key";
    private static final String REQUEST_ID_HEADER = "X-Request-Id";

    private final RestTemplate restTemplate;
    private final TejechatAiServiceProperties properties;
    private final AiUsageMetricAuthenticatedCaptureService usageCaptureService;
    private final AiUsageLimitService aiUsageLimitService;

    public AiUiCustomizationMicroserviceClientImpl(@Qualifier("aiMessageSearchRestTemplate") RestTemplate restTemplate,
                                                   TejechatAiServiceProperties properties,
                                                   AiUsageMetricAuthenticatedCaptureService usageCaptureService,
                                                   AiUsageLimitService aiUsageLimitService) {
        this.restTemplate = restTemplate;
        this.properties = properties;
        this.usageCaptureService = usageCaptureService;
        this.aiUsageLimitService = aiUsageLimitService;
    }

    @Override
    public AiUiCustomizationIntentInternalResponseDTO classifyIntent(String requestId,
                                                                      AiUiCustomizationIntentInternalRequestDTO request) {
        aiUsageLimitService.assertCurrentUserCanUseAi();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set(INTERNAL_API_KEY_HEADER, properties.getInternalKey());
        if (requestId != null && !requestId.isBlank()) {
            headers.set(REQUEST_ID_HEADER, requestId);
        }

        LOGGER.info("[AI][UI_CUSTOMIZATION_CLIENT] requestId={} outbound consulta=\"{}\"",
                requestId,
                request == null || request.getConsulta() == null ? "" : request.getConsulta().replaceAll("\\s+", " ").trim());

        try {
            ResponseEntity<AiUiCustomizationIntentInternalResponseDTO> response = restTemplate.exchange(
                    properties.buildUiCustomizationIntentUrl(),
                    HttpMethod.POST,
                    new HttpEntity<>(request, headers),
                    AiUiCustomizationIntentInternalResponseDTO.class
            );
            AiUiCustomizationIntentInternalResponseDTO body = response.getBody();
            usageCaptureService.capture(
                    requestId,
                    "UI_CUSTOMIZATION_INTENT",
                    body == null ? null : body.getAction(),
                    body == null ? null : body.getTarget(),
                    body == null ? null : body.getUsage(),
                    body != null && body.isSuccess(),
                    body == null ? "AI_SERVICE_EMPTY_RESPONSE" : body.getCodigo()
            );
            LOGGER.info("[AI][UI_CUSTOMIZATION_CLIENT] requestId={} inbound success={} area={} property={} confidence={}",
                    requestId,
                    body != null && body.isSuccess(),
                    body == null ? null : body.getArea(),
                    body == null ? null : body.getProperty(),
                    body == null ? null : body.getConfidence());
            return body;
        } catch (ResourceAccessException ex) {
            usageCaptureService.capture(requestId, "UI_CUSTOMIZATION_INTENT", null, "UI_CUSTOMIZATION", null, false, "AI_SERVICE_UNAVAILABLE");
            LOGGER.warn("[AI][UI_CUSTOMIZATION_CLIENT] requestId={} service-unavailable type={}", requestId, ex.getClass().getSimpleName());
            return null;
        } catch (HttpStatusCodeException ex) {
            usageCaptureService.capture(requestId, "UI_CUSTOMIZATION_INTENT", null, "UI_CUSTOMIZATION", null, false, "AI_SERVICE_ERROR");
            LOGGER.warn("[AI][UI_CUSTOMIZATION_CLIENT] requestId={} service-error status={}", requestId, ex.getStatusCode().value());
            return null;
        } catch (RestClientException ex) {
            usageCaptureService.capture(requestId, "UI_CUSTOMIZATION_INTENT", null, "UI_CUSTOMIZATION", null, false, "AI_SERVICE_ERROR");
            LOGGER.warn("[AI][UI_CUSTOMIZATION_CLIENT] requestId={} service-error type={}", requestId, ex.getClass().getSimpleName());
            return null;
        }
    }
}
