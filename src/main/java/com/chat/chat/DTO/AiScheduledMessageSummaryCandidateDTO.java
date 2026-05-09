package com.chat.chat.DTO;

public class AiScheduledMessageSummaryCandidateDTO {

    private Long scheduledMessageId;
    private String tipoChat;
    private String nombreChat;
    private String scheduledAt;
    private String scheduledStatus;
    private String contenidoVisible;
    private String motivoCoincidencia;

    public Long getScheduledMessageId() {
        return scheduledMessageId;
    }

    public void setScheduledMessageId(Long scheduledMessageId) {
        this.scheduledMessageId = scheduledMessageId;
    }

    public String getTipoChat() {
        return tipoChat;
    }

    public void setTipoChat(String tipoChat) {
        this.tipoChat = tipoChat;
    }

    public String getNombreChat() {
        return nombreChat;
    }

    public void setNombreChat(String nombreChat) {
        this.nombreChat = nombreChat;
    }

    public String getScheduledAt() {
        return scheduledAt;
    }

    public void setScheduledAt(String scheduledAt) {
        this.scheduledAt = scheduledAt;
    }

    public String getScheduledStatus() {
        return scheduledStatus;
    }

    public void setScheduledStatus(String scheduledStatus) {
        this.scheduledStatus = scheduledStatus;
    }

    public String getContenidoVisible() {
        return contenidoVisible;
    }

    public void setContenidoVisible(String contenidoVisible) {
        this.contenidoVisible = contenidoVisible;
    }

    public String getMotivoCoincidencia() {
        return motivoCoincidencia;
    }

    public void setMotivoCoincidencia(String motivoCoincidencia) {
        this.motivoCoincidencia = motivoCoincidencia;
    }
}
