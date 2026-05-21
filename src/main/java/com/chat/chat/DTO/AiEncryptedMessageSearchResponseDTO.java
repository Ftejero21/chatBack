package com.chat.chat.DTO;

import java.util.List;

public class AiEncryptedMessageSearchResponseDTO {

    private boolean success;
    private String codigo;
    private String mensaje;
    private String target;
    private String action;
    private Boolean needsClarification;
    private String clarificationReason;
    private String clarificationQuestion;
    private String tipoScopeAplicado;
    private String nombreScopeAplicado;
    private Boolean scopeResuelto;
    private String motivoScope;
    private Integer confidenceScope;
    private String resumenBusqueda;
    private String encryptedPayload;
    private Long chatId;
    private Long recipientId;
    private String recipientName;
    private String message;
    private String contenidoBusqueda;
    private String scheduledAt;
    private String scheduledBatchId;
    private Boolean requiresClientEncryption;
    private Boolean sensitivePayloadEncrypted;
    private String encryptedScheduledConfirmation;
    private List<String> scheduledMissingFields;
    private List<AiEncryptedMessageSearchResultDTO> resultados;

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public String getMensaje() {
        return mensaje;
    }

    public void setMensaje(String mensaje) {
        this.mensaje = mensaje;
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public Boolean getNeedsClarification() {
        return needsClarification;
    }

    public void setNeedsClarification(Boolean needsClarification) {
        this.needsClarification = needsClarification;
    }

    public String getClarificationReason() {
        return clarificationReason;
    }

    public void setClarificationReason(String clarificationReason) {
        this.clarificationReason = clarificationReason;
    }

    public String getClarificationQuestion() {
        return clarificationQuestion;
    }

    public void setClarificationQuestion(String clarificationQuestion) {
        this.clarificationQuestion = clarificationQuestion;
    }

    public List<AiEncryptedMessageSearchResultDTO> getResultados() {
        return resultados;
    }

    public void setResultados(List<AiEncryptedMessageSearchResultDTO> resultados) {
        this.resultados = resultados;
    }

    public String getTipoScopeAplicado() {
        return tipoScopeAplicado;
    }

    public void setTipoScopeAplicado(String tipoScopeAplicado) {
        this.tipoScopeAplicado = tipoScopeAplicado;
    }

    public String getNombreScopeAplicado() {
        return nombreScopeAplicado;
    }

    public void setNombreScopeAplicado(String nombreScopeAplicado) {
        this.nombreScopeAplicado = nombreScopeAplicado;
    }

    public Boolean getScopeResuelto() {
        return scopeResuelto;
    }

    public void setScopeResuelto(Boolean scopeResuelto) {
        this.scopeResuelto = scopeResuelto;
    }

    public String getMotivoScope() {
        return motivoScope;
    }

    public void setMotivoScope(String motivoScope) {
        this.motivoScope = motivoScope;
    }

    public Integer getConfidenceScope() {
        return confidenceScope;
    }

    public void setConfidenceScope(Integer confidenceScope) {
        this.confidenceScope = confidenceScope;
    }

    public String getResumenBusqueda() {
        return resumenBusqueda;
    }

    public void setResumenBusqueda(String resumenBusqueda) {
        this.resumenBusqueda = resumenBusqueda;
    }

    public String getEncryptedPayload() {
        return encryptedPayload;
    }

    public void setEncryptedPayload(String encryptedPayload) {
        this.encryptedPayload = encryptedPayload;
    }

    public Long getChatId() {
        return chatId;
    }

    public void setChatId(Long chatId) {
        this.chatId = chatId;
    }

    public String getRecipientName() {
        return recipientName;
    }

    public void setRecipientName(String recipientName) {
        this.recipientName = recipientName;
    }

    public Long getRecipientId() {
        return recipientId;
    }

    public void setRecipientId(Long recipientId) {
        this.recipientId = recipientId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getContenidoBusqueda() {
        return contenidoBusqueda;
    }

    public void setContenidoBusqueda(String contenidoBusqueda) {
        this.contenidoBusqueda = contenidoBusqueda;
    }

    public String getScheduledAt() {
        return scheduledAt;
    }

    public void setScheduledAt(String scheduledAt) {
        this.scheduledAt = scheduledAt;
    }

    public String getScheduledBatchId() {
        return scheduledBatchId;
    }

    public void setScheduledBatchId(String scheduledBatchId) {
        this.scheduledBatchId = scheduledBatchId;
    }

    public Boolean getRequiresClientEncryption() {
        return requiresClientEncryption;
    }

    public void setRequiresClientEncryption(Boolean requiresClientEncryption) {
        this.requiresClientEncryption = requiresClientEncryption;
    }

    public Boolean getSensitivePayloadEncrypted() {
        return sensitivePayloadEncrypted;
    }

    public void setSensitivePayloadEncrypted(Boolean sensitivePayloadEncrypted) {
        this.sensitivePayloadEncrypted = sensitivePayloadEncrypted;
    }

    public String getEncryptedScheduledConfirmation() {
        return encryptedScheduledConfirmation;
    }

    public void setEncryptedScheduledConfirmation(String encryptedScheduledConfirmation) {
        this.encryptedScheduledConfirmation = encryptedScheduledConfirmation;
    }

    public List<String> getScheduledMissingFields() {
        return scheduledMissingFields;
    }

    public void setScheduledMissingFields(List<String> scheduledMissingFields) {
        this.scheduledMissingFields = scheduledMissingFields;
    }
}
