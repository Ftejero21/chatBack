package com.chat.chat.DTO;

import com.chat.chat.Utils.GroupVisibility;

import java.time.Instant;
import java.time.LocalDateTime;

public class AdminGroupListDTO {
    private Long id;
    private String nombreGrupo;
    private String descripcion;
    private String visibilidad;
    private boolean activo;
    private LocalDateTime fechaCreacion;
    private Long creadorId;
    private long totalMiembros;
    private boolean chatCerrado;
    private String chatCerradoMotivo;
    private boolean closed;
    private String reason;
    private Instant chatCerradoAt;
    private Long chatCerradoByAdminId;

    public AdminGroupListDTO(Long id,
                             String nombreGrupo,
                             String descripcion,
                             GroupVisibility visibilidad,
                             boolean activo,
                             LocalDateTime fechaCreacion,
                             Long creadorId,
                             long totalMiembros,
                             boolean closedFlag,
                             String closedReason,
                             Instant closedAt,
                             Long closedByAdminId) {
        this.id = id;
        this.nombreGrupo = nombreGrupo;
        this.descripcion = descripcion;
        this.visibilidad = visibilidad == null ? null : visibilidad.name();
        this.activo = activo;
        this.fechaCreacion = fechaCreacion;
        this.creadorId = creadorId;
        this.totalMiembros = totalMiembros;
        boolean computedClosed = closedFlag || closedAt != null;
        this.chatCerrado = computedClosed;
        this.chatCerradoMotivo = computedClosed ? closedReason : null;
        this.closed = computedClosed;
        this.reason = computedClosed ? closedReason : null;
        this.chatCerradoAt = computedClosed ? closedAt : null;
        this.chatCerradoByAdminId = computedClosed ? closedByAdminId : null;
    }

    public Long getId() {
        return id;
    }

    public String getNombreGrupo() {
        return nombreGrupo;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public String getVisibilidad() {
        return visibilidad;
    }

    public boolean isActivo() {
        return activo;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public Long getCreadorId() {
        return creadorId;
    }

    public long getTotalMiembros() {
        return totalMiembros;
    }

    public boolean isChatCerrado() {
        return chatCerrado;
    }

    public String getChatCerradoMotivo() {
        return chatCerradoMotivo;
    }

    public boolean isClosed() {
        return closed;
    }

    public String getReason() {
        return reason;
    }

    public Instant getChatCerradoAt() {
        return chatCerradoAt;
    }

    public Long getChatCerradoByAdminId() {
        return chatCerradoByAdminId;
    }
}
