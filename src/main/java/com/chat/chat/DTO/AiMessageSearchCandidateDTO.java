package com.chat.chat.DTO;

public class AiMessageSearchCandidateDTO {

    private Long mensajeId;
    private Long chatId;
    private Long autorId;
    private String tipoChat;
    private String nombreChat;
    private String autor;
    private String fechaEnvio;
    private String tipoMensaje;
    private String contenidoVisible;
    private Boolean esMultimedia;
    private String contenido;

    public Long getMensajeId() {
        return mensajeId;
    }

    public void setMensajeId(Long mensajeId) {
        this.mensajeId = mensajeId;
    }

    public Long getChatId() {
        return chatId;
    }

    public void setChatId(Long chatId) {
        this.chatId = chatId;
    }

    public Long getAutorId() {
        return autorId;
    }

    public void setAutorId(Long autorId) {
        this.autorId = autorId;
    }

    public String getTipoChat() {
        return tipoChat;
    }

    public void setTipoChat(String tipoChat) {
        this.tipoChat = tipoChat;
    }

    public String getNombreChat() {
        return nombreChat;
    }

    public void setNombreChat(String nombreChat) {
        this.nombreChat = nombreChat;
    }

    public String getAutor() {
        return autor;
    }

    public void setAutor(String autor) {
        this.autor = autor;
    }

    public String getFechaEnvio() {
        return fechaEnvio;
    }

    public void setFechaEnvio(String fechaEnvio) {
        this.fechaEnvio = fechaEnvio;
    }

    public String getTipoMensaje() {
        return tipoMensaje;
    }

    public void setTipoMensaje(String tipoMensaje) {
        this.tipoMensaje = tipoMensaje;
    }

    public String getContenidoVisible() {
        return contenidoVisible;
    }

    public void setContenidoVisible(String contenidoVisible) {
        this.contenidoVisible = contenidoVisible;
    }

    public Boolean getEsMultimedia() {
        return esMultimedia;
    }

    public void setEsMultimedia(Boolean esMultimedia) {
        this.esMultimedia = esMultimedia;
    }

    public String getContenido() {
        return contenido;
    }

    public void setContenido(String contenido) {
        this.contenido = contenido;
    }
}
