package com.chat.chat.Service.AiService;

import com.chat.chat.Configuracion.DeepSeekProperties;
import com.chat.chat.DTO.AiUsageInfoDTO;
import com.chat.chat.Exceptions.SemanticApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

@Service
public class DeepSeekApiClientImpl implements DeepSeekApiClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(DeepSeekApiClientImpl.class);
    private static final ThreadLocal<AiUsageInfoDTO> LAST_USAGE = new ThreadLocal<>();

    private final RestTemplate restTemplate;
    private final RestTemplate adminReportRestTemplate;
    private final DeepSeekProperties deepSeekProperties;

    public DeepSeekApiClientImpl(@Qualifier("deepSeekRestTemplate") RestTemplate restTemplate,
                                 @Qualifier("deepSeekAdminReportRestTemplate") RestTemplate adminReportRestTemplate,
                                 DeepSeekProperties deepSeekProperties) {
        this.restTemplate = restTemplate;
        this.adminReportRestTemplate = adminReportRestTemplate;
        this.deepSeekProperties = deepSeekProperties;
    }

    @Override
    public String completarTexto(String systemPrompt, String userContent) {
        return completarTexto(systemPrompt, userContent, null);
    }

    @Override
    public String completarTexto(String systemPrompt, String userContent, Integer maxOutputTokens) {
        return completarTextoInternal(restTemplate, systemPrompt, userContent, maxOutputTokens);
    }

    @Override
    public String completarTextoAdminReport(String systemPrompt, String userContent, Integer maxOutputTokens) {
        return completarTextoInternal(adminReportRestTemplate, systemPrompt, userContent, maxOutputTokens);
    }

    private String completarTextoInternal(RestTemplate template,
                                          String systemPrompt,
                                          String userContent,
                                          Integer maxOutputTokens) {
        String apiKey = deepSeekProperties.getApiKey();
        if (!StringUtils.hasText(apiKey)) {
            throw new SemanticApiException(HttpStatus.INTERNAL_SERVER_ERROR, "AI_API_KEY_MISSING",
                    "La API Key de DeepSeek no esta configurada.", null);
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey.trim());

        DeepSeekChatRequest request = new DeepSeekChatRequest();
        request.setModel(deepSeekProperties.getModel());
        request.setMessages(List.of(
                new DeepSeekMessage("system", systemPrompt),
                new DeepSeekMessage("user", userContent)
        ));
        request.setTemperature(deepSeekProperties.getTemperature());
        request.setMaxTokens(resolveMaxOutputTokens(maxOutputTokens));
        request.setStream(false);

        try {
            ResponseEntity<DeepSeekChatResponse> response = template.exchange(
                    buildUrl(),
                    HttpMethod.POST,
                    new HttpEntity<>(request, headers),
                    DeepSeekChatResponse.class
            );
            DeepSeekChatResponse responseBody = response.getBody();
            logUsage(responseBody, request.getModel(), request.getMaxTokens());
            String content = extractContent(responseBody);
            if (!StringUtils.hasText(content)) {
                throw new SemanticApiException(HttpStatus.BAD_GATEWAY, "AI_EMPTY_RESPONSE",
                        "DeepSeek devolvio una respuesta vacia.", null);
            }
            return content.trim();
        } catch (HttpStatusCodeException ex) {
            LAST_USAGE.remove();
            HttpStatus status = HttpStatus.resolve(ex.getStatusCode().value());
            LOGGER.warn("[AI][DEEPSEEK] request-failed status={} bodyLength={}",
                    ex.getStatusCode().value(),
                    ex.getResponseBodyAsString() == null ? 0 : ex.getResponseBodyAsString().length());
            throw mapHttpException(status);
        } catch (ResourceAccessException ex) {
            LAST_USAGE.remove();
            LOGGER.warn("[AI][DEEPSEEK] request-timeout type={}", ex.getClass().getSimpleName());
            throw new SemanticApiException(HttpStatus.GATEWAY_TIMEOUT, "AI_TIMEOUT",
                    "La solicitud a DeepSeek ha excedido el tiempo limite.", null);
        }
    }

    private String buildUrl() {
        String baseUrl = deepSeekProperties.getApiUrl();
        String path = deepSeekProperties.getChatCompletionsPath();
        String normalizedBase = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        String normalizedPath = path.startsWith("/") ? path : "/" + path;
        return normalizedBase + normalizedPath;
    }

    private int resolveMaxOutputTokens(Integer requestedMaxOutputTokens) {
        if (requestedMaxOutputTokens == null || requestedMaxOutputTokens < 1) {
            return deepSeekProperties.getMaxOutputTokens();
        }
        return requestedMaxOutputTokens;
    }

    private String extractContent(DeepSeekChatResponse response) {
        if (response == null || response.getChoices() == null || response.getChoices().isEmpty()) {
            return null;
        }
        DeepSeekChoice firstChoice = response.getChoices().get(0);
        if (firstChoice == null || firstChoice.getMessage() == null) {
            return null;
        }
        return firstChoice.getMessage().getContent();
    }

    private void logUsage(DeepSeekChatResponse response, String model, Integer maxOutputTokens) {
        if (response == null || response.getUsage() == null) {
            LAST_USAGE.remove();
            LOGGER.info("[AI][DEEPSEEK][USAGE] usageMissing=true");
            return;
        }
        DeepSeekUsage usage = response.getUsage();
        AiUsageInfoDTO usageInfo = new AiUsageInfoDTO();
        usageInfo.setModel(model);
        usageInfo.setPromptTokens(usage.getPrompt_tokens());
        usageInfo.setCompletionTokens(usage.getCompletion_tokens());
        usageInfo.setTotalTokens(usage.getTotal_tokens());
        usageInfo.setOutputTokens(usage.getCompletion_tokens());
        LAST_USAGE.set(usageInfo);
        LOGGER.info("[AI][DEEPSEEK][USAGE] model={} promptTokens={} completionTokens={} totalTokens={} maxOutputTokens={}",
                model,
                usage.getPrompt_tokens(),
                usage.getCompletion_tokens(),
                usage.getTotal_tokens(),
                maxOutputTokens);
    }

    @Override
    public AiUsageInfoDTO getLastUsage() {
        AiUsageInfoDTO usage = LAST_USAGE.get();
        if (usage == null) {
            return null;
        }
        AiUsageInfoDTO copy = new AiUsageInfoDTO();
        copy.setModel(usage.getModel());
        copy.setPromptTokens(usage.getPromptTokens());
        copy.setCompletionTokens(usage.getCompletionTokens());
        copy.setTotalTokens(usage.getTotalTokens());
        copy.setInputCacheHitTokens(usage.getInputCacheHitTokens());
        copy.setInputCacheMissTokens(usage.getInputCacheMissTokens());
        copy.setOutputTokens(usage.getOutputTokens());
        copy.setEstimatedCostUsd(usage.getEstimatedCostUsd());
        LAST_USAGE.remove();
        return copy;
    }

    private SemanticApiException mapHttpException(HttpStatus status) {
        if (status == null) {
            return new SemanticApiException(HttpStatus.BAD_GATEWAY, "AI_PROVIDER_ERROR",
                    "No se pudo procesar la respuesta de DeepSeek.", null);
        }
        return switch (status) {
            case UNAUTHORIZED, FORBIDDEN -> new SemanticApiException(HttpStatus.BAD_GATEWAY, "AI_AUTH_ERROR",
                    "DeepSeek rechazo la autenticacion de la solicitud.", null);
            case PAYMENT_REQUIRED -> new SemanticApiException(HttpStatus.BAD_GATEWAY, "AI_BILLING_ERROR",
                    "La cuenta de DeepSeek no tiene saldo suficiente.", null);
            case TOO_MANY_REQUESTS -> new SemanticApiException(HttpStatus.TOO_MANY_REQUESTS, "AI_PROVIDER_RATE_LIMIT",
                    "DeepSeek esta recibiendo demasiadas solicitudes. Intenta de nuevo en breve.", null);
            case BAD_REQUEST, UNPROCESSABLE_ENTITY -> new SemanticApiException(HttpStatus.BAD_GATEWAY, "AI_BAD_REQUEST",
                    "DeepSeek rechazo los parametros enviados.", null);
            case INTERNAL_SERVER_ERROR, BAD_GATEWAY, SERVICE_UNAVAILABLE, GATEWAY_TIMEOUT -> new SemanticApiException(
                    HttpStatus.BAD_GATEWAY,
                    "AI_PROVIDER_UNAVAILABLE",
                    "DeepSeek no esta disponible temporalmente.",
                    null);
            default -> new SemanticApiException(HttpStatus.BAD_GATEWAY, "AI_PROVIDER_ERROR",
                    "No se pudo completar la operacion con DeepSeek.", null);
        };
    }

    public static class DeepSeekChatRequest {
        private String model;
        private List<DeepSeekMessage> messages = new ArrayList<>();
        private Double temperature;
        private Integer max_tokens;
        private Boolean stream;

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }

        public List<DeepSeekMessage> getMessages() {
            return messages;
        }

        public void setMessages(List<DeepSeekMessage> messages) {
            this.messages = messages;
        }

        public Double getTemperature() {
            return temperature;
        }

        public void setTemperature(Double temperature) {
            this.temperature = temperature;
        }

        public Integer getMaxTokens() {
            return max_tokens;
        }

        public void setMaxTokens(Integer maxTokens) {
            this.max_tokens = maxTokens;
        }

        public Boolean getStream() {
            return stream;
        }

        public void setStream(Boolean stream) {
            this.stream = stream;
        }
    }

    public static class DeepSeekChatResponse {
        private List<DeepSeekChoice> choices;
        private DeepSeekUsage usage;

        public List<DeepSeekChoice> getChoices() {
            return choices;
        }

        public void setChoices(List<DeepSeekChoice> choices) {
            this.choices = choices;
        }

        public DeepSeekUsage getUsage() {
            return usage;
        }

        public void setUsage(DeepSeekUsage usage) {
            this.usage = usage;
        }
    }

    public static class DeepSeekUsage {
        private Integer prompt_tokens;
        private Integer completion_tokens;
        private Integer total_tokens;

        public Integer getPrompt_tokens() {
            return prompt_tokens;
        }

        public void setPrompt_tokens(Integer prompt_tokens) {
            this.prompt_tokens = prompt_tokens;
        }

        public Integer getCompletion_tokens() {
            return completion_tokens;
        }

        public void setCompletion_tokens(Integer completion_tokens) {
            this.completion_tokens = completion_tokens;
        }

        public Integer getTotal_tokens() {
            return total_tokens;
        }

        public void setTotal_tokens(Integer total_tokens) {
            this.total_tokens = total_tokens;
        }
    }

    public static class DeepSeekChoice {
        private DeepSeekMessage message;

        public DeepSeekMessage getMessage() {
            return message;
        }

        public void setMessage(DeepSeekMessage message) {
            this.message = message;
        }
    }

    public static class DeepSeekMessage {
        private String role;
        private String content;

        public DeepSeekMessage() {
        }

        public DeepSeekMessage(String role, String content) {
            this.role = role;
            this.content = content;
        }

        public String getRole() {
            return role;
        }

        public void setRole(String role) {
            this.role = role;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }
    }
}
