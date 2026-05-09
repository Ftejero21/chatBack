package com.chat.chat.DTO;

public class AiEncryptedConversationSummaryInternalMessageDTO {

    private String autor;
    private boolean esUsuarioActual;
    private String contenido;
    private String tipoMensaje;
    private String fechaEnvio;

    public String getAutor() {
        return autor;
    }

    public void setAutor(String autor) {
        this.autor = autor;
    }

    public boolean isEsUsuarioActual() {
        return esUsuarioActual;
    }

    public void setEsUsuarioActual(boolean esUsuarioActual) {
        this.esUsuarioActual = esUsuarioActual;
    }

    public String getContenido() {
        return contenido;
    }

    public void setContenido(String contenido) {
        this.contenido = contenido;
    }

    public String getTipoMensaje() {
        return tipoMensaje;
    }

    public void setTipoMensaje(String tipoMensaje) {
        this.tipoMensaje = tipoMensaje;
    }

    public String getFechaEnvio() {
        return fechaEnvio;
    }

    public void setFechaEnvio(String fechaEnvio) {
        this.fechaEnvio = fechaEnvio;
    }
}
