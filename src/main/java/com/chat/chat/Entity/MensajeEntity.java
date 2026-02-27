package com.chat.chat.Entity;

import com.chat.chat.Utils.MessageType;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "mensajes")
public class MensajeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "emisor_id")
    private UsuarioEntity emisor;

    @ManyToOne
    @JoinColumn(name = "receptor_id")
    private UsuarioEntity receptor;

    @ManyToOne
    @JoinColumn(name = "chat_id")
    private ChatEntity chat;

    @Enumerated(EnumType.STRING)
    private MessageType tipo = MessageType.TEXT;

    // AUDIO
    @Column(name="media_url")
    private String mediaUrl;            // /uploads/voice/xxxxx.webm (o .ogg/.m4a)
    @Column(name="media_mime")
    private String mediaMime;           // p.ej "audio/webm"
    @Column(name="media_duracion_ms")
    private Integer mediaDuracionMs;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String contenido;
    private LocalDateTime fechaEnvio;
    @Column(nullable = false)
    private boolean activo = true;

    @Column(nullable = false)
    private boolean reenviado = false;

    @Column(name = "mensaje_original_id")
    private Long mensajeOriginalId;

    @Column(name = "reply_to_message_id")
    private Long replyToMessageId;

    @Column(name = "reply_snippet", length = 255)
    private String replySnippet;

    @Column(name = "reply_author_name", length = 120)
    private String replyAuthorName;

    public MessageType getTipo() {
        return tipo;
    }

    public void setTipo(MessageType tipo) {
        this.tipo = tipo;
    }

    public String getMediaUrl() {
        return mediaUrl;
    }

    public void setMediaUrl(String mediaUrl) {
        this.mediaUrl = mediaUrl;
    }

    public String getMediaMime() {
        return mediaMime;
    }

    public void setMediaMime(String mediaMime) {
        this.mediaMime = mediaMime;
    }

    public Integer getMediaDuracionMs() {
        return mediaDuracionMs;
    }

    public void setMediaDuracionMs(Integer mediaDuracionMs) {
        this.mediaDuracionMs = mediaDuracionMs;
    }

    @Column(nullable = false)
    private boolean leido = false; // Inicialmente no leído

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public boolean isLeido() {
        return leido;
    }

    public void setLeido(boolean leido) {
        this.leido = leido;
    }

    public UsuarioEntity getEmisor() {
        return emisor;
    }

    public void setEmisor(UsuarioEntity emisor) {
        this.emisor = emisor;
    }

    public UsuarioEntity getReceptor() {
        return receptor;
    }

    public void setReceptor(UsuarioEntity receptor) {
        this.receptor = receptor;
    }

    public ChatEntity getChat() {
        return chat;
    }

    public void setChat(ChatEntity chat) {
        this.chat = chat;
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


}
