package com.chat.chat.DTO;

import com.chat.chat.Utils.UserComplaintEstado;

import java.time.LocalDateTime;

public class UserComplaintWsDTO {
    private String event;
    private Long id;
    private Long denuncianteId;
    private Long denunciadoId;
    private Long chatId;
    private String motivo;
    private String detalle;
    private UserComplaintEstado estado;
    private boolean leida;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime leidaAt;
    private String denuncianteNombre;
    private String denunciadoNombre;
    private String chatNombreSnapshot;

    public String getEvent() {
        return event;
    }

    public void setEvent(String event) {
        this.event = event;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getDenuncianteId() {
        return denuncianteId;
    }

    public void setDenuncianteId(Long denuncianteId) {
        this.denuncianteId = denuncianteId;
    }

    public Long getDenunciadoId() {
        return denunciadoId;
    }

    public void setDenunciadoId(Long denunciadoId) {
        this.denunciadoId = denunciadoId;
    }

    public Long getChatId() {
        return chatId;
    }

    public void setChatId(Long chatId) {
        this.chatId = chatId;
    }

    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo;
    }

    public String getDetalle() {
        return detalle;
    }

    public void setDetalle(String detalle) {
        this.detalle = detalle;
    }

    public UserComplaintEstado getEstado() {
        return estado;
    }

    public void setEstado(UserComplaintEstado estado) {
        this.estado = estado;
    }

    public boolean isLeida() {
        return leida;
    }

    public void setLeida(boolean leida) {
        this.leida = leida;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public LocalDateTime getLeidaAt() {
        return leidaAt;
    }

    public void setLeidaAt(LocalDateTime leidaAt) {
        this.leidaAt = leidaAt;
    }

    public String getDenuncianteNombre() {
        return denuncianteNombre;
    }

    public void setDenuncianteNombre(String denuncianteNombre) {
        this.denuncianteNombre = denuncianteNombre;
    }

    public String getDenunciadoNombre() {
        return denunciadoNombre;
    }

    public void setDenunciadoNombre(String denunciadoNombre) {
        this.denunciadoNombre = denunciadoNombre;
    }

    public String getChatNombreSnapshot() {
        return chatNombreSnapshot;
    }

    public void setChatNombreSnapshot(String chatNombreSnapshot) {
        this.chatNombreSnapshot = chatNombreSnapshot;
    }
}
