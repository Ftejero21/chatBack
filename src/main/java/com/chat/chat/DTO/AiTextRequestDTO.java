package com.chat.chat.DTO;

import jakarta.validation.constraints.Size;

public class AiTextRequestDTO {

    @Size(max = 10000, message = "El texto supera la longitud maxima permitida")
    private String texto;

    @Size(max = 50000, message = "El encryptedPayload supera la longitud maxima permitida")
    private String encryptedPayload;

    @Size(max = 40, message = "El modo no es valido")
    private String modo;

    @Size(max = 80, message = "El idioma de destino no es valido")
    private String idiomaDestino;

    private Long messageId;

    @Size(max = 20, message = "El tipo de mensaje no es valido")
    private String tipoMensaje;

    @Size(max = 1024, message = "El audioUrl supera la longitud maxima permitida")
    private String audioUrl;

    @Size(max = 50000, message = "El audioEncryptedPayload supera la longitud maxima permitida")
    private String audioEncryptedPayload;

    @Size(max = 120, message = "El mimeType supera la longitud maxima permitida")
    private String mimeType;

    @Size(max = 255, message = "El nombre de archivo supera la longitud maxima permitida")
    private String nombreArchivo;

    private Long chatId;

    private Long chatGrupalId;

    public String getTexto() {
        return texto;
    }

    public void setTexto(String texto) {
        this.texto = texto;
    }

    public String getEncryptedPayload() {
        return encryptedPayload;
    }

    public void setEncryptedPayload(String encryptedPayload) {
        this.encryptedPayload = encryptedPayload;
    }

    public String getModo() {
        return modo;
    }

    public void setModo(String modo) {
        this.modo = modo;
    }

    public String getIdiomaDestino() {
        return idiomaDestino;
    }

    public void setIdiomaDestino(String idiomaDestino) {
        this.idiomaDestino = idiomaDestino;
    }

    public Long getMessageId() {
        return messageId;
    }

    public void setMessageId(Long messageId) {
        this.messageId = messageId;
    }

    public String getTipoMensaje() {
        return tipoMensaje;
    }

    public void setTipoMensaje(String tipoMensaje) {
        this.tipoMensaje = tipoMensaje;
    }

    public String getAudioUrl() {
        return audioUrl;
    }

    public void setAudioUrl(String audioUrl) {
        this.audioUrl = audioUrl;
    }

    public String getAudioEncryptedPayload() {
        return audioEncryptedPayload;
    }

    public void setAudioEncryptedPayload(String audioEncryptedPayload) {
        this.audioEncryptedPayload = audioEncryptedPayload;
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
}
