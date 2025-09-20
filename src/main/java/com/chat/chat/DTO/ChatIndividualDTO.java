package com.chat.chat.DTO;

import java.time.LocalDateTime;

public class ChatIndividualDTO {
    private Long id;
    private UsuarioDTO receptor;

    private String ultimaMensaje;
    private LocalDateTime ultimaFecha;

    private Long unreadCount;

    public Long getId() {
        return id;
    }

    public String getUltimaMensaje() {
        return ultimaMensaje;
    }

    public void setUltimaMensaje(String ultimaMensaje) {
        this.ultimaMensaje = ultimaMensaje;
    }

    public LocalDateTime getUltimaFecha() {
        return ultimaFecha;
    }

    public Long getUnreadCount() {
        return unreadCount;
    }

    public void setUnreadCount(Long unreadCount) {
        this.unreadCount = unreadCount;
    }

    public void setUltimaFecha(LocalDateTime ultimaFecha) {
        this.ultimaFecha = ultimaFecha;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public UsuarioDTO getReceptor() {
        return receptor;
    }

    public void setReceptor(UsuarioDTO receptor) {
        this.receptor = receptor;
    }
}
