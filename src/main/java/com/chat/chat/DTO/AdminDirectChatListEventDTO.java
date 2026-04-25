package com.chat.chat.DTO;

import java.time.LocalDateTime;

public class AdminDirectChatListEventDTO {
    private String systemEvent;
    private Long chatId;
    private Long lastVisibleMessageId;
    private Long ultimoMensajeId;
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

    public Long getLastVisibleMessageId() {
        return lastVisibleMessageId;
    }

    public void setLastVisibleMessageId(Long lastVisibleMessageId) {
        this.lastVisibleMessageId = lastVisibleMessageId;
    }

    public Long getUltimoMensajeId() {
        return ultimoMensajeId;
    }

    public void setUltimoMensajeId(Long ultimoMensajeId) {
        this.ultimoMensajeId = ultimoMensajeId;
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
