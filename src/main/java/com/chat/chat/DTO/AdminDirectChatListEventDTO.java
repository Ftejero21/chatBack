package com.chat.chat.DTO;

import java.time.LocalDateTime;

public class AdminDirectChatListEventDTO {
    private String systemEvent;
    private Long chatId;
    private Long userId;
    private boolean removed;
    private Long lastVisibleMessageId;
    private String ultimoMensaje;
    private String ultimoMensajeTipo;
    private Long ultimoMensajeEmisorId;
    private LocalDateTime ultimaFecha;

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

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public boolean isRemoved() {
        return removed;
    }

    public void setRemoved(boolean removed) {
        this.removed = removed;
    }

    public Long getLastVisibleMessageId() {
        return lastVisibleMessageId;
    }

    public void setLastVisibleMessageId(Long lastVisibleMessageId) {
        this.lastVisibleMessageId = lastVisibleMessageId;
    }

    public String getUltimoMensaje() {
        return ultimoMensaje;
    }

    public void setUltimoMensaje(String ultimoMensaje) {
        this.ultimoMensaje = ultimoMensaje;
    }

    public String getUltimoMensajeTipo() {
        return ultimoMensajeTipo;
    }

    public void setUltimoMensajeTipo(String ultimoMensajeTipo) {
        this.ultimoMensajeTipo = ultimoMensajeTipo;
    }

    public Long getUltimoMensajeEmisorId() {
        return ultimoMensajeEmisorId;
    }

    public void setUltimoMensajeEmisorId(Long ultimoMensajeEmisorId) {
        this.ultimoMensajeEmisorId = ultimoMensajeEmisorId;
    }

    public LocalDateTime getUltimaFecha() {
        return ultimaFecha;
    }

    public void setUltimaFecha(LocalDateTime ultimaFecha) {
        this.ultimaFecha = ultimaFecha;
    }
}
