package com.chat.chat.Service.AiService;

import com.chat.chat.Configuracion.TejechatAiServiceProperties;
import com.chat.chat.DTO.AiSemanticRerankInternalRequestDTO;
import com.chat.chat.DTO.AiSemanticRerankInternalResponseDTO;
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
public class AiSemanticRerankMicroserviceClientImpl implements AiSemanticRerankMicroserviceClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(AiSemanticRerankMicroserviceClientImpl.class);
    private static final String INTERNAL_API_KEY_HEADER = "X-Internal-Api-Key";
    private static final String REQUEST_ID_HEADER = "X-Request-Id";

    private final RestTemplate restTemplate;
    private final TejechatAiServiceProperties tejechatAiServiceProperties;
    private final AiUsageMetricAuthenticatedCaptureService usageCaptureService;
    private final AiUsageLimitService aiUsageLimitService;

    public AiSemanticRerankMicroserviceClientImpl(@Qualifier("aiMessageSearchRestTemplate") RestTemplate restTemplate,
                                                  TejechatAiServiceProperties tejechatAiServiceProperties,
                                                  AiUsageMetricAuthenticatedCaptureService usageCaptureService,
                                                  AiUsageLimitService aiUsageLimitService) {
        this.restTemplate = restTemplate;
        this.tejechatAiServiceProperties = tejechatAiServiceProperties;
        this.usageCaptureService = usageCaptureService;
        this.aiUsageLimitService = aiUsageLimitService;
    }

    @Override
    public AiSemanticRerankInternalResponseDTO rerank(String requestId, AiSemanticRerankInternalRequestDTO request) {
        aiUsageLimitService.assertCurrentUserCanUseAi();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set(INTERNAL_API_KEY_HEADER, tejechatAiServiceProperties.getInternalKey());
        if (requestId != null && !requestId.isBlank()) {
            headers.set(REQUEST_ID_HEADER, requestId);
        }

        LOGGER.info("[AI][SEMANTIC_RERANK_CLIENT] requestId={} tipoBusqueda={} totalCandidatos={}",
                requestId,
                request == null ? null : request.getTipoBusqueda(),
                request == null || request.getCandidatos() == null ? 0 : request.getCandidatos().size());
        try {
            ResponseEntity<AiSemanticRerankInternalResponseDTO> response = restTemplate.exchange(
                    tejechatAiServiceProperties.buildSemanticRerankUrl(),
                    HttpMethod.POST,
                    new HttpEntity<>(request, headers),
                    AiSemanticRerankInternalResponseDTO.class
            );
            AiSemanticRerankInternalResponseDTO body = response.getBody();
            usageCaptureService.capture(
                    requestId,
                    "SEMANTIC_RERANK",
                    null,
                    request == null ? null : request.getTipoBusqueda(),
                    body == null ? null : body.getUsage(),
                    body != null && body.isSuccess(),
                    body == null ? "AI_SERVICE_EMPTY_RESPONSE" : body.getCodigo()
            );
            LOGGER.info("[AI][SEMANTIC_RERANK_CLIENT] requestId={} selectedIds={} confidence={}",
                    requestId,
                    body == null ? null : body.getSelectedIds(),
                    body == null ? null : body.getConfidence());
            return body;
        } catch (ResourceAccessException ex) {
            usageCaptureService.capture(requestId, "SEMANTIC_RERANK", null, request == null ? null : request.getTipoBusqueda(), null, false, "AI_SERVICE_UNAVAILABLE");
            LOGGER.warn("[AI][SEMANTIC_RERANK_CLIENT] requestId={} service-unavailable type={}", requestId, ex.getClass().getSimpleName());
            return null;
        } catch (HttpStatusCodeException ex) {
            usageCaptureService.capture(requestId, "SEMANTIC_RERANK", null, request == null ? null : request.getTipoBusqueda(), null, false, "AI_SERVICE_ERROR");
            LOGGER.warn("[AI][SEMANTIC_RERANK_CLIENT] requestId={} service-error status={}", requestId, ex.getStatusCode().value());
            return null;
        } catch (RestClientException ex) {
            usageCaptureService.capture(requestId, "SEMANTIC_RERANK", null, request == null ? null : request.getTipoBusqueda(), null, false, "AI_SERVICE_ERROR");
            LOGGER.warn("[AI][SEMANTIC_RERANK_CLIENT] requestId={} service-error type={}", requestId, ex.getClass().getSimpleName());
            return null;
        }
    }
}
