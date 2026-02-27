package com.chat.chat.DTO;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class MensajeDTO {
    private Long id;
    private Long emisorId;
    private Long receptorId;
    private String contenido;
    private LocalDateTime fechaEnvio;
    private boolean activo;

    private String tipo;             // opcional: usar String o MessageType en el DTO

    private boolean reenviado;
    private Long mensajeOriginalId;

    private Long replyToMessageId;
    private String replySnippet;
    private String replyAuthorName;

    // AUDIO (entrada desde el front)
    private String audioDataUrl;     // data:image/...;base64,...  (pero de audio)
    private String audioUrl;         // si ya viene subido (/uploads/voice/xxx.webm)
    private String audioMime;        // p.ej "audio/webm"
    private Integer audioDuracionMs;
    private String imageUrl;
    private String imageMime;
    private String imageNombre;
    private String reaccionEmoji;
    private Long reaccionUsuarioId;
    private LocalDateTime reaccionFecha;
    private List<MensajeReaccionResumenDTO> reacciones;

    private boolean leido;

    private Long chatId;

    private String emisorNombre;
    private String emisorApellido;
    private String emisorNombreCompleto;
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

    public String getEmisorNombreCompleto() {
        return emisorNombreCompleto;
    }

    public void setEmisorNombreCompleto(String emisorNombreCompleto) {
        this.emisorNombreCompleto = emisorNombreCompleto;
    }

    public String getEmisorFoto() {
        return emisorFoto;
    }

    public void setEmisorFoto(String emisorFoto) {
        this.emisorFoto = emisorFoto;
    }

    public boolean isReenviado() {
        return reenviado;
    }

    public void setReenviado(boolean reenviado) {
        this.reenviado = reenviado;
    }

    public Long getMensajeOriginalId() {
        return mensajeOriginalId;
    }

    public void setMensajeOriginalId(Long mensajeOriginalId) {
        this.mensajeOriginalId = mensajeOriginalId;
    }

    public Long getReplyToMessageId() {
        return replyToMessageId;
    }

    public void setReplyToMessageId(Long replyToMessageId) {
        this.replyToMessageId = replyToMessageId;
    }

    public String getReplySnippet() {
        return replySnippet;
    }

    public void setReplySnippet(String replySnippet) {
        this.replySnippet = replySnippet;
    }

    public String getReplyAuthorName() {
        return replyAuthorName;
    }

    public void setReplyAuthorName(String replyAuthorName) {
        this.replyAuthorName = replyAuthorName;
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

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getImageMime() {
        return imageMime;
    }

    public void setImageMime(String imageMime) {
        this.imageMime = imageMime;
    }

    public String getImageNombre() {
        return imageNombre;
    }

    public void setImageNombre(String imageNombre) {
        this.imageNombre = imageNombre;
    }


    public void setAudioDuracionMs(Integer audioDuracionMs) {
        this.audioDuracionMs = audioDuracionMs;
    }

    public String getReaccionEmoji() {
        return reaccionEmoji;
    }

    public void setReaccionEmoji(String reaccionEmoji) {
        this.reaccionEmoji = reaccionEmoji;
    }

    public Long getReaccionUsuarioId() {
        return reaccionUsuarioId;
    }

    public void setReaccionUsuarioId(Long reaccionUsuarioId) {
        this.reaccionUsuarioId = reaccionUsuarioId;
    }

    public LocalDateTime getReaccionFecha() {
        return reaccionFecha;
    }

    public void setReaccionFecha(LocalDateTime reaccionFecha) {
        this.reaccionFecha = reaccionFecha;
    }

    public List<MensajeReaccionResumenDTO> getReacciones() {
        return reacciones;
    }

    public void setReacciones(List<MensajeReaccionResumenDTO> reacciones) {
        this.reacciones = reacciones;
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
