package com.chat.chat.DTO;

public class ChatResumenDTO {
    private Long id;
    private String tipo;
    private String nombreChat;
    private Integer totalMensajes;
    private String ultimoMensaje;
    private String ultimoMensajeDescifrado;

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
}
