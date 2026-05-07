package com.chat.chat.DTO;

import jakarta.validation.constraints.Size;

public class AiEncryptedContextMessageDTO {

    private Long id;
    private Long messageId;

    private Long autorId;
    private Long emisorId;
    private Long receptorId;
    private Long chatId;
    private Long chatGrupalId;

    @Size(max = 80, message = "El autor del mensaje supera la longitud maxima permitida")
    private String autor;

    @Size(max = 80, message = "La fecha del mensaje supera la longitud maxima permitida")
    private String fecha;

    private boolean esUsuarioActual;

    @Size(max = 50000, message = "El encryptedPayload supera la longitud maxima permitida")
    private String encryptedPayload;

    @Size(max = 20, message = "El tipo de mensaje no es valido")
    private String tipoMensaje;

    @Size(max = 50000, message = "El contenido supera la longitud maxima permitida")
    private String contenido;

    @Size(max = 50000, message = "El audioEncryptedPayload supera la longitud maxima permitida")
    private String audioEncryptedPayload;

    @Size(max = 1024, message = "El audioUrl supera la longitud maxima permitida")
    private String audioUrl;

    @Size(max = 120, message = "El mimeType supera la longitud maxima permitida")
    private String mimeType;

    @Size(max = 255, message = "El nombre de archivo supera la longitud maxima permitida")
    private String nombreArchivo;

    @Size(max = 80, message = "La fechaEnvio supera la longitud maxima permitida")
    private String fechaEnvio;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getMessageId() {
        return messageId;
    }

    public void setMessageId(Long messageId) {
        this.messageId = messageId;
    }

    public Long getAutorId() {
        return autorId;
    }

    public void setAutorId(Long autorId) {
        this.autorId = autorId;
    }

    public Long getEmisorId() {
        return emisorId;
    }

    public void setEmisorId(Long emisorId) {
        this.emisorId = emisorId;
    }

    public Long getReceptorId() {
        return receptorId;
    }

    public void setReceptorId(Long receptorId) {
        this.receptorId = receptorId;
    }

    public Long getChatId() {
        return chatId;
    }

    public void setChatId(Long chatId) {
        this.chatId = chatId;
    }

    public Long getChatGrupalId() {
        return chatGrupalId;
    }

    public void setChatGrupalId(Long chatGrupalId) {
        this.chatGrupalId = chatGrupalId;
    }

    public String getAutor() {
        return autor;
    }

    public void setAutor(String autor) {
        this.autor = autor;
    }

    public String getFecha() {
        return fecha;
    }

    public void setFecha(String fecha) {
        this.fecha = fecha;
    }

    public String getFechaEnvio() {
        return fechaEnvio;
    }

    public void setFechaEnvio(String fechaEnvio) {
        this.fechaEnvio = fechaEnvio;
    }

    public boolean isEsUsuarioActual() {
        return esUsuarioActual;
    }

    public void setEsUsuarioActual(boolean esUsuarioActual) {
        this.esUsuarioActual = esUsuarioActual;
    }

    public String getEncryptedPayload() {
        return encryptedPayload;
    }

    public void setEncryptedPayload(String encryptedPayload) {
        this.encryptedPayload = encryptedPayload;
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

    public String getAudioEncryptedPayload() {
        return audioEncryptedPayload;
    }

    public void setAudioEncryptedPayload(String audioEncryptedPayload) {
        this.audioEncryptedPayload = audioEncryptedPayload;
    }

    public String getAudioUrl() {
        return audioUrl;
    }

    public void setAudioUrl(String audioUrl) {
        this.audioUrl = audioUrl;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public String getNombreArchivo() {
        return nombreArchivo;
    }

    public void setNombreArchivo(String nombreArchivo) {
        this.nombreArchivo = nombreArchivo;
    }
}
