package com.chat.chat.Service.AiService;

import com.chat.chat.Configuracion.TejechatAiServiceProperties;
import com.chat.chat.DTO.AiTextInternalRequestDTO;
import com.chat.chat.DTO.AiTextInternalResponseDTO;
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
public class AiTextMicroserviceClientImpl implements AiTextMicroserviceClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(AiTextMicroserviceClientImpl.class);
    private static final String INTERNAL_API_KEY_HEADER = "X-Internal-Api-Key";
    private static final String REQUEST_ID_HEADER = "X-Request-Id";

    private final RestTemplate restTemplate;
    private final TejechatAiServiceProperties tejechatAiServiceProperties;
    private final AiUsageMetricAuthenticatedCaptureService usageCaptureService;
    private final AiUsageLimitService aiUsageLimitService;

    public AiTextMicroserviceClientImpl(@Qualifier("aiTextRestTemplate") RestTemplate restTemplate,
                                        TejechatAiServiceProperties tejechatAiServiceProperties,
                                        AiUsageMetricAuthenticatedCaptureService usageCaptureService,
                                        AiUsageLimitService aiUsageLimitService) {
        this.restTemplate = restTemplate;
        this.tejechatAiServiceProperties = tejechatAiServiceProperties;
        this.usageCaptureService = usageCaptureService;
        this.aiUsageLimitService = aiUsageLimitService;
    }

    @Override
    public AiTextInternalResponseDTO procesarTexto(String requestId, AiTextInternalRequestDTO request) {
        aiUsageLimitService.assertCurrentUserCanUseAi();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set(INTERNAL_API_KEY_HEADER, tejechatAiServiceProperties.getInternalKey());
        if (requestId != null && !requestId.isBlank()) {
            headers.set(REQUEST_ID_HEADER, requestId);
        }

        try {
            ResponseEntity<AiTextInternalResponseDTO> response = restTemplate.exchange(
                    tejechatAiServiceProperties.buildTextUrl(),
                    HttpMethod.POST,
                    new HttpEntity<>(request, headers),
                    AiTextInternalResponseDTO.class
            );
            AiTextInternalResponseDTO body = response.getBody();
            usageCaptureService.capture(
                    requestId,
                    "TEXT",
                    null,
                    null,
                    body == null ? null : body.getUsage(),
                    body != null && body.isSuccess(),
                    body == null ? "AI_SERVICE_EMPTY_RESPONSE" : body.getCodigo()
            );
            LOGGER.info("[AI][TEXT_CLIENT] requestId={} service-status={} hasBody={}",
                    requestId, response.getStatusCode().value(), body != null);
            return body;
        } catch (ResourceAccessException ex) {
            usageCaptureService.capture(requestId, "TEXT", null, null, null, false, "AI_SERVICE_UNAVAILABLE");
            LOGGER.warn("[AI][TEXT_CLIENT] requestId={} service-unavailable type={}", requestId, ex.getClass().getSimpleName());
            throw new AiTextMicroserviceUnavailableException(ex);
        } catch (HttpStatusCodeException ex) {
            usageCaptureService.capture(requestId, "TEXT", null, null, null, false, "AI_SERVICE_ERROR");
            LOGGER.warn("[AI][TEXT_CLIENT] requestId={} service-error status={} bodyLength={}",
                    requestId,
                    ex.getStatusCode().value(),
                    ex.getResponseBodyAsString() == null ? 0 : ex.getResponseBodyAsString().length());
            throw new AiTextMicroserviceException(ex);
        } catch (RestClientException ex) {
            usageCaptureService.capture(requestId, "TEXT", null, null, null, false, "AI_SERVICE_ERROR");
            LOGGER.warn("[AI][TEXT_CLIENT] requestId={} service-error type={}", requestId, ex.getClass().getSimpleName());
            throw new AiTextMicroserviceException(ex);
        }
    }
}
