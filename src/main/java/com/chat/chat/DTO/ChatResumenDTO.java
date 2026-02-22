package com.chat.chat.DTO;

import java.time.LocalDateTime;

public class ChatResumenDTO {

    private Long id;
    private String tipo; // "INDIVIDUAL" o "GRUPAL"
    private String nombreChat; // Nombre del grupo o nombre del "otro" usuario
    private Integer totalMensajes;
    private String ultimoMensaje;
    private String ultimoMensajeDescifrado;
    private LocalDateTime fechaUltimoMensaje;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public String getNombreChat() {
        return nombreChat;
    }

    public void setNombreChat(String nombreChat) {
        this.nombreChat = nombreChat;
    }

    public Integer getTotalMensajes() {
        return totalMensajes;
    }

    public void setTotalMensajes(Integer totalMensajes) {
        this.totalMensajes = totalMensajes;
    }

    public String getUltimoMensaje() {
        return ultimoMensaje;
    }

    public void setUltimoMensaje(String ultimoMensaje) {
        this.ultimoMensaje = ultimoMensaje;
    }

    public String getUltimoMensajeDescifrado() {
        return ultimoMensajeDescifrado;
    }

    public void setUltimoMensajeDescifrado(String ultimoMensajeDescifrado) {
        this.ultimoMensajeDescifrado = ultimoMensajeDescifrado;
    }

    public LocalDateTime getFechaUltimoMensaje() {
        return fechaUltimoMensaje;
    }

    public void setFechaUltimoMensaje(LocalDateTime fechaUltimoMensaje) {
        this.fechaUltimoMensaje = fechaUltimoMensaje;
    }
}