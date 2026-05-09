package com.chat.chat.Service.AiService;

import com.chat.chat.Configuracion.TejechatAiServiceProperties;
import com.chat.chat.DTO.AiSearchIntentInternalRequestDTO;
import com.chat.chat.DTO.AiSearchIntentInternalResponseDTO;
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

    public AiSearchIntentMicroserviceClientImpl(@Qualifier("aiMessageSearchRestTemplate") RestTemplate restTemplate,
                                                TejechatAiServiceProperties tejechatAiServiceProperties) {
        this.restTemplate = restTemplate;
        this.tejechatAiServiceProperties = tejechatAiServiceProperties;
    }

    @Override
    public AiSearchIntentInternalResponseDTO classifyIntent(String requestId, AiSearchIntentInternalRequestDTO request) {
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
            ResponseEntity<AiSearchIntentInternalResponseDTO> response = restTemplate.exchange(
                    tejechatAiServiceProperties.buildSearchIntentUrl(),
                    HttpMethod.POST,
                    new HttpEntity<>(request, headers),
                    AiSearchIntentInternalResponseDTO.class
            );
            LOGGER.info("[AI][SEARCH_INTENT_CLIENT] requestId={} service-status={} hasBody={}",
                    requestId, response.getStatusCode().value(), response.getBody() != null);
            AiSearchIntentInternalResponseDTO body = response.getBody();
            if (body != null) {
                LOGGER.info("[AI][SEARCH_INTENT_CLIENT] requestId={} inbound success={} codigo={} target={} complaintDirection={} senderScope={} tipoScopeSolicitado={} tipoMensajeSolicitado={} readStatus={} personaMencionada=\"{}\" grupoMencionado=\"{}\" temporalExpression=\"{}\" orden={} confidence={}",
                        requestId,
                        body.isSuccess(),
                        body.getCodigo(),
                        body.getTarget(),
                        body.getComplaintDirection(),
                        body.getSenderScope(),
                        body.getTipoScopeSolicitado(),
                        body.getTipoMensajeSolicitado(),
                        body.getReadStatus(),
                        safe(body.getPersonaMencionada()),
                        safe(body.getGrupoMencionado()),
                        safe(body.getTemporalExpression()),
                        body.getOrden(),
                        body.getConfidence());
            }
            return body;
        } catch (ResourceAccessException ex) {
            LOGGER.warn("[AI][SEARCH_INTENT_CLIENT] requestId={} service-unavailable type={}", requestId, ex.getClass().getSimpleName());
            return null;
        } catch (HttpStatusCodeException ex) {
            LOGGER.warn("[AI][SEARCH_INTENT_CLIENT] requestId={} service-error status={}", requestId, ex.getStatusCode().value());
            return null;
        } catch (RestClientException ex) {
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
}
