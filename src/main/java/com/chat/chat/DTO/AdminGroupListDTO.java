package com.chat.chat.DTO;

import com.chat.chat.Utils.GroupVisibility;

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

    public AdminGroupListDTO(Long id,
                             String nombreGrupo,
                             String descripcion,
                             GroupVisibility visibilidad,
                             boolean activo,
                             LocalDateTime fechaCreacion,
                             Long creadorId,
                             long totalMiembros) {
        this.id = id;
        this.nombreGrupo = nombreGrupo;
        this.descripcion = descripcion;
        this.visibilidad = visibilidad == null ? null : visibilidad.name();
        this.activo = activo;
        this.fechaCreacion = fechaCreacion;
        this.creadorId = creadorId;
        this.totalMiembros = totalMiembros;
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
}
