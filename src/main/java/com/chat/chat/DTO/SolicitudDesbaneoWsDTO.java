package com.chat.chat.DTO;

import com.chat.chat.Utils.ReporteTipo;
import com.chat.chat.Utils.SolicitudDesbaneoEstado;

import java.time.LocalDateTime;

public class SolicitudDesbaneoWsDTO {
    private String event;
    private Long id;
    private ReporteTipo tipoReporte;
    private Long usuarioId;
    private Long chatId;
    private String email;
    private String motivo;
    private SolicitudDesbaneoEstado estado;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String usuarioNombre;
    private String usuarioApellido;
    private String chatNombreSnapshot;
    private String chatCerradoMotivoSnapshot;

    public String getEvent() {
        return event;
    }

    public void setEvent(String event) {
        this.event = event;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ReporteTipo getTipoReporte() {
        return tipoReporte;
    }

    public void setTipoReporte(ReporteTipo tipoReporte) {
        this.tipoReporte = tipoReporte;
    }

    public Long getUsuarioId() {
        return usuarioId;
    }

    public void setUsuarioId(Long usuarioId) {
        this.usuarioId = usuarioId;
    }

    public Long getChatId() {
        return chatId;
    }

    public void setChatId(Long chatId) {
        this.chatId = chatId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo;
    }

    public SolicitudDesbaneoEstado getEstado() {
        return estado;
    }

    public void setEstado(SolicitudDesbaneoEstado estado) {
        this.estado = estado;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getUsuarioNombre() {
        return usuarioNombre;
    }

    public void setUsuarioNombre(String usuarioNombre) {
        this.usuarioNombre = usuarioNombre;
    }

    public String getUsuarioApellido() {
        return usuarioApellido;
    }

    public void setUsuarioApellido(String usuarioApellido) {
        this.usuarioApellido = usuarioApellido;
    }

    public String getChatNombreSnapshot() {
        return chatNombreSnapshot;
    }

    public void setChatNombreSnapshot(String chatNombreSnapshot) {
        this.chatNombreSnapshot = chatNombreSnapshot;
    }

    public String getChatCerradoMotivoSnapshot() {
        return chatCerradoMotivoSnapshot;
    }

    public void setChatCerradoMotivoSnapshot(String chatCerradoMotivoSnapshot) {
        this.chatCerradoMotivoSnapshot = chatCerradoMotivoSnapshot;
    }
}
