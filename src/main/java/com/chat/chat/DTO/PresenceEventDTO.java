package com.chat.chat.DTO;

public class PresenceEventDTO {
    private Long userId;
    private String estado;

    public PresenceEventDTO() {
    }

    public PresenceEventDTO(Long userId, String estado) {
        this.userId = userId;
        this.estado = estado;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }
}
