package com.chat.chat.DTO;

import java.util.Map;

public class AiSemanticRerankCandidateDTO {

    private Long id;
    private String tipo;
    private String estado;
    private String titulo;
    private String descripcion;
    private String fecha;
    private String motivo;
    private String resolucionMotivo;
    private String fechaCreacion;
    private String fechaActualizacion;
    private Map<String, Object> metadata;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public String getFecha() { return fecha; }
    public void setFecha(String fecha) { this.fecha = fecha; }

    public String getMotivo() { return motivo; }
    public void setMotivo(String motivo) { this.motivo = motivo; }

    public String getResolucionMotivo() { return resolucionMotivo; }
    public void setResolucionMotivo(String resolucionMotivo) { this.resolucionMotivo = resolucionMotivo; }

    public String getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(String fechaCreacion) { this.fechaCreacion = fechaCreacion; }

    public String getFechaActualizacion() { return fechaActualizacion; }
    public void setFechaActualizacion(String fechaActualizacion) { this.fechaActualizacion = fechaActualizacion; }

    public Map<String, Object> getMetadata() { return metadata; }
    public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
}
