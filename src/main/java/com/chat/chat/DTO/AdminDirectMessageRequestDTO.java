package com.chat.chat.DTO;

import java.util.List;

public class AdminDirectMessageRequestDTO {
    private List<Long> userIds;
    private String contenido;
    private List<AdminDirectMessagePayloadDTO> encryptedPayloads;
    private Long expiresAfterReadSeconds;

    public List<Long> getUserIds() {
        return userIds;
    }

    public void setUserIds(List<Long> userIds) {
        this.userIds = userIds;
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

    public Long getExpiresAfterReadSeconds() {
        return expiresAfterReadSeconds;
    }

    public void setExpiresAfterReadSeconds(Long expiresAfterReadSeconds) {
        this.expiresAfterReadSeconds = expiresAfterReadSeconds;
    }
}
