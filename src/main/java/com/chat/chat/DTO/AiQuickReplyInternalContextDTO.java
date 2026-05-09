package com.chat.chat.DTO;

public class AiQuickReplyInternalContextDTO {

    private String autor;
    private Long autorId;
    private boolean esUsuarioActual;
    private String tipoMensaje;
    private String contenido;
    private String fechaEnvio;

    public String getAutor() {
        return autor;
    }

    public void setAutor(String autor) {
        this.autor = autor;
    }

    public Long getAutorId() {
        return autorId;
    }

    public void setAutorId(Long autorId) {
        this.autorId = autorId;
    }

    public boolean isEsUsuarioActual() {
        return esUsuarioActual;
    }

    public void setEsUsuarioActual(boolean esUsuarioActual) {
        this.esUsuarioActual = esUsuarioActual;
    }

    public String getTipoMensaje() {
        return tipoMensaje;
    }

    public void setTipoMensaje(String tipoMensaje) {
        this.tipoMensaje = tipoMensaje;
    }

    public String getContenido() {
        return contenido;
    }

    public void setContenido(String contenido) {
        this.contenido = contenido;
    }

    public String getFechaEnvio() {
        return fechaEnvio;
    }

    public void setFechaEnvio(String fechaEnvio) {
        this.fechaEnvio = fechaEnvio;
    }
}
