package com.chat.chat.Configuracion;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Component
@Validated
@ConfigurationProperties(prefix = "deepseek")
public class DeepSeekProperties {

    private final Api api = new Api();

    @NotBlank
    private String chatCompletionsPath = "/chat/completions";

    @NotBlank
    private String model = "deepseek-v4-flash";

    @Min(1)
    @Max(4000)
    private int maxOutputTokens = 300;

    @Min(1)
    @Max(4000)
    private int summaryMaxOutputTokens = 500;

    @DecimalMin("0.0")
    @DecimalMax("2.0")
    private double temperature = 0.4d;

    @Min(1)
    @Max(120)
    private int timeoutSeconds = 20;

    public String getApiKey() {
        return api.getKey();
    }

    public String getApiUrl() {
        return api.getUrl();
    }

    public String getChatCompletionsPath() {
        return chatCompletionsPath;
    }

    public void setChatCompletionsPath(String chatCompletionsPath) {
        this.chatCompletionsPath = chatCompletionsPath;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public int getMaxOutputTokens() {
        return maxOutputTokens;
    }

    public void setMaxOutputTokens(int maxOutputTokens) {
        this.maxOutputTokens = maxOutputTokens;
    }

    public int getSummaryMaxOutputTokens() {
        return summaryMaxOutputTokens;
    }

    public void setSummaryMaxOutputTokens(int summaryMaxOutputTokens) {
        this.summaryMaxOutputTokens = summaryMaxOutputTokens;
    }

    public double getTemperature() {
        return temperature;
    }

    public void setTemperature(double temperature) {
        this.temperature = temperature;
    }

    public int getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public void setTimeoutSeconds(int timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }

    public Api getApi() {
        return api;
    }

    public static class Api {

        private String key;

        @NotBlank
        private String url = "https://api.deepseek.com";

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }
    }
}
