package com.chat.chat.Service.AiService;

import com.chat.chat.Configuracion.TejechatAiServiceProperties;
import com.chat.chat.DTO.AiMessageSearchInternalRequestDTO;
import com.chat.chat.DTO.AiMessageSearchInternalResponseDTO;
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
public class AiMessageSearchMicroserviceClientImpl implements AiMessageSearchMicroserviceClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(AiMessageSearchMicroserviceClientImpl.class);
    private static final String INTERNAL_API_KEY_HEADER = "X-Internal-Api-Key";
    private static final String REQUEST_ID_HEADER = "X-Request-Id";

    private final RestTemplate restTemplate;
    private final TejechatAiServiceProperties tejechatAiServiceProperties;
    private final AiUsageMetricAuthenticatedCaptureService usageCaptureService;
    private final AiUsageLimitService aiUsageLimitService;

    public AiMessageSearchMicroserviceClientImpl(@Qualifier("aiMessageSearchRestTemplate") RestTemplate restTemplate,
                                                 TejechatAiServiceProperties tejechatAiServiceProperties,
                                                 AiUsageMetricAuthenticatedCaptureService usageCaptureService,
                                                 AiUsageLimitService aiUsageLimitService) {
        this.restTemplate = restTemplate;
        this.tejechatAiServiceProperties = tejechatAiServiceProperties;
        this.usageCaptureService = usageCaptureService;
        this.aiUsageLimitService = aiUsageLimitService;
    }

    @Override
    public AiMessageSearchInternalResponseDTO buscarMensajesConIA(String requestId, AiMessageSearchInternalRequestDTO request) {
        aiUsageLimitService.assertCurrentUserCanUseAi();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set(INTERNAL_API_KEY_HEADER, tejechatAiServiceProperties.getInternalKey());
        if (requestId != null && !requestId.isBlank()) {
            headers.set(REQUEST_ID_HEADER, requestId);
        }

        try {
            ResponseEntity<AiMessageSearchInternalResponseDTO> response = restTemplate.exchange(
                    tejechatAiServiceProperties.buildMessageSearchUrl(),
                    HttpMethod.POST,
                    new HttpEntity<>(request, headers),
                    AiMessageSearchInternalResponseDTO.class
            );
            AiMessageSearchInternalResponseDTO body = response.getBody();
            usageCaptureService.capture(
                    requestId,
                    "MESSAGE_SEARCH",
                    null,
                    "MESSAGES",
                    body == null ? null : body.getUsage(),
                    body != null && body.isSuccess(),
                    body == null ? "AI_SERVICE_EMPTY_RESPONSE" : body.getCodigo()
            );
            LOGGER.info("[AI][MESSAGE_SEARCH_CLIENT] requestId={} service-status={} hasBody={}",
                    requestId, response.getStatusCode().value(), body != null);
            return body;
        } catch (ResourceAccessException ex) {
            usageCaptureService.capture(requestId, "MESSAGE_SEARCH", null, "MESSAGES", null, false, "AI_SERVICE_UNAVAILABLE");
            LOGGER.warn("[AI][MESSAGE_SEARCH_CLIENT] requestId={} service-unavailable type={}", requestId, ex.getClass().getSimpleName());
            throw new AiMessageSearchMicroserviceUnavailableException(ex);
        } catch (HttpStatusCodeException ex) {
            usageCaptureService.capture(requestId, "MESSAGE_SEARCH", null, "MESSAGES", null, false, "AI_SERVICE_ERROR");
            LOGGER.warn("[AI][MESSAGE_SEARCH_CLIENT] requestId={} service-error status={} bodyLength={}",
                    requestId,
                    ex.getStatusCode().value(),
                    ex.getResponseBodyAsString() == null ? 0 : ex.getResponseBodyAsString().length());
            throw new AiMessageSearchMicroserviceException(ex);
        } catch (RestClientException ex) {
            usageCaptureService.capture(requestId, "MESSAGE_SEARCH", null, "MESSAGES", null, false, "AI_SERVICE_ERROR");
            LOGGER.warn("[AI][MESSAGE_SEARCH_CLIENT] requestId={} service-error type={}", requestId, ex.getClass().getSimpleName());
            throw new AiMessageSearchMicroserviceException(ex);
        }
    }
}
