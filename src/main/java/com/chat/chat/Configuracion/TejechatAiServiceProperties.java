package com.chat.chat.Configuracion;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Component
@Validated
@ConfigurationProperties(prefix = "tejechat.ai-service")
public class TejechatAiServiceProperties {

    @NotBlank
    private String baseUrl = "http://localhost:8081";

    @NotBlank
    private String internalKey = "clave-interna-dev";

    @NotBlank
    private String messageSearchPath = "/internal/ai/buscar-mensajes";

    @NotBlank
    private String encryptedSummaryPath = "/internal/ai/resumir-conversacion/encrypted";

    @NotBlank
    private String textPath = "/internal/ai/texto";

    @NotBlank
    private String reportAnalysisPath = "/internal/ai/analizar-denuncia";

    @NotBlank
    private String quickReplyPath = "/internal/ai/respuestas-rapidas";

    @NotBlank
    private String searchIntentPath = "/internal/ai/search-intent";

    @NotBlank
    private String appReportStatusSummaryPath = "/internal/ai/app-report-status-summary";

    @NotBlank
    private String uiCustomizationIntentPath = "/internal/ai/ui-customization/intent";

    @NotBlank
    private String appReportResolutionNotePath = "/internal/ai/app-report-resolution-note";

    @NotBlank
    private String scheduledMessageSummaryPath = "/internal/ai/resumir-mensajes-programados";

    @NotBlank
    private String semanticRerankPath = "/internal/ai/semantic-rerank";

    @Min(1)
    @Max(120)
    private int timeoutSeconds = 10;

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getInternalKey() {
        return internalKey;
    }

    public void setInternalKey(String internalKey) {
        this.internalKey = internalKey;
    }

    public String getMessageSearchPath() {
        return messageSearchPath;
    }

    public void setMessageSearchPath(String messageSearchPath) {
        this.messageSearchPath = messageSearchPath;
    }

    public String getEncryptedSummaryPath() {
        return encryptedSummaryPath;
    }

    public void setEncryptedSummaryPath(String encryptedSummaryPath) {
        this.encryptedSummaryPath = encryptedSummaryPath;
    }

    public String getTextPath() {
        return textPath;
    }

    public void setTextPath(String textPath) {
        this.textPath = textPath;
    }

    public String getReportAnalysisPath() {
        return reportAnalysisPath;
    }

    public void setReportAnalysisPath(String reportAnalysisPath) {
        this.reportAnalysisPath = reportAnalysisPath;
    }

    public String getQuickReplyPath() {
        return quickReplyPath;
    }

    public void setQuickReplyPath(String quickReplyPath) {
        this.quickReplyPath = quickReplyPath;
    }

    public String getSearchIntentPath() {
        return searchIntentPath;
    }

    public void setSearchIntentPath(String searchIntentPath) {
        this.searchIntentPath = searchIntentPath;
    }

    public String getAppReportStatusSummaryPath() {
        return appReportStatusSummaryPath;
    }

    public void setAppReportStatusSummaryPath(String appReportStatusSummaryPath) {
        this.appReportStatusSummaryPath = appReportStatusSummaryPath;
    }

    public String getUiCustomizationIntentPath() {
        return uiCustomizationIntentPath;
    }

    public void setUiCustomizationIntentPath(String uiCustomizationIntentPath) {
        this.uiCustomizationIntentPath = uiCustomizationIntentPath;
    }

    public String getScheduledMessageSummaryPath() {
        return scheduledMessageSummaryPath;
    }

    public void setScheduledMessageSummaryPath(String scheduledMessageSummaryPath) {
        this.scheduledMessageSummaryPath = scheduledMessageSummaryPath;
    }

    public String getAppReportResolutionNotePath() {
        return appReportResolutionNotePath;
    }

    public void setAppReportResolutionNotePath(String appReportResolutionNotePath) {
        this.appReportResolutionNotePath = appReportResolutionNotePath;
    }

    public int getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public void setTimeoutSeconds(int timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }

    public String getSemanticRerankPath() {
        return semanticRerankPath;
    }

    public void setSemanticRerankPath(String semanticRerankPath) {
        this.semanticRerankPath = semanticRerankPath;
    }

    public String buildMessageSearchUrl() {
        String normalizedBaseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        String normalizedPath = messageSearchPath.startsWith("/") ? messageSearchPath : "/" + messageSearchPath;
        return normalizedBaseUrl + normalizedPath;
    }

    public String buildEncryptedSummaryUrl() {
        String normalizedBaseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        String normalizedPath = encryptedSummaryPath.startsWith("/") ? encryptedSummaryPath : "/" + encryptedSummaryPath;
        return normalizedBaseUrl + normalizedPath;
    }

    public String buildTextUrl() {
        String normalizedBaseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        String normalizedPath = textPath.startsWith("/") ? textPath : "/" + textPath;
        return normalizedBaseUrl + normalizedPath;
    }

    public String buildReportAnalysisUrl() {
        String normalizedBaseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        String normalizedPath = reportAnalysisPath.startsWith("/") ? reportAnalysisPath : "/" + reportAnalysisPath;
        return normalizedBaseUrl + normalizedPath;
    }

    public String buildQuickReplyUrl() {
        String normalizedBaseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        String normalizedPath = quickReplyPath.startsWith("/") ? quickReplyPath : "/" + quickReplyPath;
        return normalizedBaseUrl + normalizedPath;
    }

    public String buildSearchIntentUrl() {
        String normalizedBaseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        String normalizedPath = searchIntentPath.startsWith("/") ? searchIntentPath : "/" + searchIntentPath;
        return normalizedBaseUrl + normalizedPath;
    }

    public String buildAppReportStatusSummaryUrl() {
        String normalizedBaseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        String normalizedPath = appReportStatusSummaryPath.startsWith("/") ? appReportStatusSummaryPath : "/" + appReportStatusSummaryPath;
        return normalizedBaseUrl + normalizedPath;
    }

    public String buildUiCustomizationIntentUrl() {
        String normalizedBaseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        String normalizedPath = uiCustomizationIntentPath.startsWith("/") ? uiCustomizationIntentPath : "/" + uiCustomizationIntentPath;
        return normalizedBaseUrl + normalizedPath;
    }

    public String buildAppReportResolutionNoteUrl() {
        String normalizedBaseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        String normalizedPath = appReportResolutionNotePath.startsWith("/") ? appReportResolutionNotePath : "/" + appReportResolutionNotePath;
        return normalizedBaseUrl + normalizedPath;
    }

    public String buildScheduledMessageSummaryUrl() {
        String normalizedBaseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        String normalizedPath = scheduledMessageSummaryPath.startsWith("/") ? scheduledMessageSummaryPath : "/" + scheduledMessageSummaryPath;
        return normalizedBaseUrl + normalizedPath;
    }

    public String buildSemanticRerankUrl() {
        String normalizedBaseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        String normalizedPath = semanticRerankPath.startsWith("/") ? semanticRerankPath : "/" + semanticRerankPath;
        return normalizedBaseUrl + normalizedPath;
    }
}
