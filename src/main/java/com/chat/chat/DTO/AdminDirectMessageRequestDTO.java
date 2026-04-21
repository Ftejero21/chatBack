package com.chat.chat.DTO;

import java.util.List;

public class AdminDirectMessageRequestDTO {
    private List<Long> userIds;
    private String contenido;
    private List<AdminDirectMessagePayloadDTO> encryptedPayloads;
    private Long expiresAfterReadSeconds;
    private String origen;
    private String motivo;
    private String descripcion;

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

    public String getOrigen() {
        return origen;
    }

    public void setOrigen(String origen) {
        this.origen = origen;
    }

    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }
}
