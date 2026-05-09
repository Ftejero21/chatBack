package com.chat.chat.Service.AiService;

import com.chat.chat.Configuracion.TejechatAiServiceProperties;
import com.chat.chat.DTO.AiScheduledMessageSummaryInternalRequestDTO;
import com.chat.chat.DTO.AiScheduledMessageSummaryInternalResponseDTO;
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
public class AiScheduledMessageSummaryMicroserviceClientImpl implements AiScheduledMessageSummaryMicroserviceClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(AiScheduledMessageSummaryMicroserviceClientImpl.class);
    private static final String INTERNAL_API_KEY_HEADER = "X-Internal-Api-Key";
    private static final String REQUEST_ID_HEADER = "X-Request-Id";

    private final RestTemplate restTemplate;
    private final TejechatAiServiceProperties tejechatAiServiceProperties;

    public AiScheduledMessageSummaryMicroserviceClientImpl(@Qualifier("aiMessageSearchRestTemplate") RestTemplate restTemplate,
                                                           TejechatAiServiceProperties tejechatAiServiceProperties) {
        this.restTemplate = restTemplate;
        this.tejechatAiServiceProperties = tejechatAiServiceProperties;
    }

    @Override
    public AiScheduledMessageSummaryInternalResponseDTO resumirMensajesProgramados(String requestId,
                                                                                   AiScheduledMessageSummaryInternalRequestDTO request) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set(INTERNAL_API_KEY_HEADER, tejechatAiServiceProperties.getInternalKey());
        if (requestId != null && !requestId.isBlank()) {
            headers.set(REQUEST_ID_HEADER, requestId);
        }

        try {
            ResponseEntity<AiScheduledMessageSummaryInternalResponseDTO> response = restTemplate.exchange(
                    tejechatAiServiceProperties.buildScheduledMessageSummaryUrl(),
                    HttpMethod.POST,
                    new HttpEntity<>(request, headers),
                    AiScheduledMessageSummaryInternalResponseDTO.class
            );
            LOGGER.info("[AI][SCHEDULED_SUMMARY_CLIENT] requestId={} service-status={} hasBody={}",
                    requestId, response.getStatusCode().value(), response.getBody() != null);
            return response.getBody();
        } catch (ResourceAccessException ex) {
            LOGGER.warn("[AI][SCHEDULED_SUMMARY_CLIENT] requestId={} service-unavailable type={}",
                    requestId, ex.getClass().getSimpleName());
            return null;
        } catch (HttpStatusCodeException ex) {
            LOGGER.warn("[AI][SCHEDULED_SUMMARY_CLIENT] requestId={} service-error status={}",
                    requestId, ex.getStatusCode().value());
            return null;
        } catch (RestClientException ex) {
            LOGGER.warn("[AI][SCHEDULED_SUMMARY_CLIENT] requestId={} service-error type={}",
                    requestId, ex.getClass().getSimpleName());
            return null;
        }
    }
}
