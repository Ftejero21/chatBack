package com.chat.chat.DTO;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class MensajeDTO {
    private Long id;
    private Long emisorId;
    private Long receptorId;
    private String contenido;
    private LocalDateTime fechaEnvio;
    private boolean activo;

    private String tipo;             // opcional: usar String o MessageType en el DTO

    // AUDIO (entrada desde el front)
    private String audioDataUrl;     // data:image/...;base64,...  (pero de audio)
    private String audioUrl;         // si ya viene subido (/uploads/voice/xxx.webm)
    private String audioMime;        // p.ej "audio/webm"
    private Integer audioDuracionMs;

    private boolean leido;

    private Long chatId;

    private String emisorNombre;
    private String emisorApellido;
    private String emisorFoto;

    public String getEmisorNombre() {
        return emisorNombre;
    }

    public void setEmisorNombre(String emisorNombre) {
        this.emisorNombre = emisorNombre;
    }

    public String getEmisorApellido() {
        return emisorApellido;
    }

    public void setEmisorApellido(String emisorApellido) {
        this.emisorApellido = emisorApellido;
    }

    public String getEmisorFoto() {
        return emisorFoto;
    }

    public void setEmisorFoto(String emisorFoto) {
        this.emisorFoto = emisorFoto;
    }

    public boolean isLeido() {
        return leido;
    }

    public Long getChatId() {
        return chatId;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public String getAudioDataUrl() {
        return audioDataUrl;
    }

    public void setAudioDataUrl(String audioDataUrl) {
        this.audioDataUrl = audioDataUrl;
    }

    public String getAudioUrl() {
        return audioUrl;
    }

    public void setAudioUrl(String audioUrl) {
        this.audioUrl = audioUrl;
    }

    public String getAudioMime() {
        return audioMime;
    }

    public void setAudioMime(String audioMime) {
        this.audioMime = audioMime;
    }

    public Integer getAudioDuracionMs() {
        return audioDuracionMs;
    }


    public void setAudioDuracionMs(Integer audioDuracionMs) {
        this.audioDuracionMs = audioDuracionMs;
    }

    public void setChatId(Long chatId) {
        this.chatId = chatId;
    }

    public void setLeido(boolean leido) {
        this.leido = leido;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public String getContenido() {
        return contenido;
    }

    public void setContenido(String contenido) {
        this.contenido = contenido;
    }

    public LocalDateTime getFechaEnvio() {
        return fechaEnvio;
    }

    public void setFechaEnvio(LocalDateTime fechaEnvio) {
        this.fechaEnvio = fechaEnvio;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }
}
