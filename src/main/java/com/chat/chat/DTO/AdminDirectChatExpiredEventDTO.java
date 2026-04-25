package com.chat.chat.DTO;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class AdminDirectChatExpiredEventDTO {
    private String systemEvent;
    private Long chatId;
    private Long id;
    private boolean activo;
    private boolean adminMessage;
    private boolean mensajeTemporal;
    private String estadoTemporal;
    private String motivoEliminacion;
    private boolean expiredByPolicy;
    private LocalDateTime expiraEn;
    private List<Long> expiredMessageIds = new ArrayList<>();

    public String getSystemEvent() {
        return systemEvent;
    }

    public void setSystemEvent(String systemEvent) {
        this.systemEvent = systemEvent;
    }

    public Long getChatId() {
        return chatId;
    }

    public void setChatId(Long chatId) {
        this.chatId = chatId;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }

    public boolean isAdminMessage() {
        return adminMessage;
    }

    public void setAdminMessage(boolean adminMessage) {
        this.adminMessage = adminMessage;
    }

    public boolean isMensajeTemporal() {
        return mensajeTemporal;
    }

    public void setMensajeTemporal(boolean mensajeTemporal) {
        this.mensajeTemporal = mensajeTemporal;
    }

    public String getEstadoTemporal() {
        return estadoTemporal;
    }

    public void setEstadoTemporal(String estadoTemporal) {
        this.estadoTemporal = estadoTemporal;
    }

    public String getMotivoEliminacion() {
        return motivoEliminacion;
    }

    public void setMotivoEliminacion(String motivoEliminacion) {
        this.motivoEliminacion = motivoEliminacion;
    }

    public boolean isExpiredByPolicy() {
        return expiredByPolicy;
    }

    public void setExpiredByPolicy(boolean expiredByPolicy) {
        this.expiredByPolicy = expiredByPolicy;
    }

    public LocalDateTime getExpiraEn() {
        return expiraEn;
    }

    public void setExpiraEn(LocalDateTime expiraEn) {
        this.expiraEn = expiraEn;
    }

    public List<Long> getExpiredMessageIds() {
        return expiredMessageIds;
    }

    public void setExpiredMessageIds(List<Long> expiredMessageIds) {
        this.expiredMessageIds = expiredMessageIds;
    }
}
