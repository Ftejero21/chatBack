package com.chat.chat.DTO;

import com.fasterxml.jackson.annotation.JsonAlias;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class AdminDirectMessageScheduledRequestDTO {
    private String audienceMode;
    private List<Long> userIds = new ArrayList<>();
    private String message;
    private String contenido;
    private List<AdminDirectMessagePayloadDTO> encryptedPayloads = new ArrayList<>();
    @JsonAlias({"fechaProgramada", "scheduled_at"})
    private Instant scheduledAt;
    private String scheduledAtLocal;
    private Long expiresAfterReadSeconds;

    public String getAudienceMode() {
        return audienceMode;
    }

    public void setAudienceMode(String audienceMode) {
        this.audienceMode = audienceMode;
    }

    public List<Long> getUserIds() {
        return userIds;
    }

    public void setUserIds(List<Long> userIds) {
        this.userIds = userIds;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getContenido() {
        return contenido;
    }

    public void setContenido(String contenido) {
        this.contenido = contenido;
    }

    public List<AdminDirectMessagePayloadDTO> getEncryptedPayloads() {
        return encryptedPayloads;
    }

    public void setEncryptedPayloads(List<AdminDirectMessagePayloadDTO> encryptedPayloads) {
        this.encryptedPayloads = encryptedPayloads;
    }

    public Instant getScheduledAt() {
        return scheduledAt;
    }

    public void setScheduledAt(Instant scheduledAt) {
        this.scheduledAt = scheduledAt;
    }

    public String getScheduledAtLocal() {
        return scheduledAtLocal;
    }

    public void setScheduledAtLocal(String scheduledAtLocal) {
        this.scheduledAtLocal = scheduledAtLocal;
    }

    public Long getExpiresAfterReadSeconds() {
        return expiresAfterReadSeconds;
    }

    public void setExpiresAfterReadSeconds(Long expiresAfterReadSeconds) {
        this.expiresAfterReadSeconds = expiresAfterReadSeconds;
    }
}
