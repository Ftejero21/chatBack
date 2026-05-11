package com.chat.chat.Entity;

import com.chat.chat.Utils.ReporteTipo;
import com.chat.chat.Utils.SolicitudDesbaneoEstado;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "solicitud_reporte_historial",
        indexes = {
                @Index(name = "idx_solicitud_reporte_historial_solicitud_created", columnList = "solicitud_id,created_at"),
                @Index(name = "idx_solicitud_reporte_historial_estado", columnList = "estado_nuevo,created_at")
        }
)
public class SolicitudReporteHistorialEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "solicitud_id", nullable = false)
    private Long solicitudId;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_reporte", nullable = false, length = 20)
    private ReporteTipo tipoReporte;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_anterior", length = 20)
    private SolicitudDesbaneoEstado estadoAnterior;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_nuevo", nullable = false, length = 20)
    private SolicitudDesbaneoEstado estadoNuevo;

    @Column(name = "motivo_snapshot", length = 1000)
    private String motivoSnapshot;

    @Column(name = "resolucion_motivo", length = 1000)
    private String resolucionMotivo;

    @Column(name = "usuario_id")
    private Long usuarioId;

    @Column(name = "admin_id")
    private Long adminId;

    @Column(name = "accion", nullable = false, length = 40)
    private String accion;

    @Column(name = "comentario_interno", length = 1000)
    private String comentarioInterno;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getSolicitudId() {
        return solicitudId;
    }

    public void setSolicitudId(Long solicitudId) {
        this.solicitudId = solicitudId;
    }

    public ReporteTipo getTipoReporte() {
        return tipoReporte;
    }

    public void setTipoReporte(ReporteTipo tipoReporte) {
        this.tipoReporte = tipoReporte;
    }

    public SolicitudDesbaneoEstado getEstadoAnterior() {
        return estadoAnterior;
    }

    public void setEstadoAnterior(SolicitudDesbaneoEstado estadoAnterior) {
        this.estadoAnterior = estadoAnterior;
    }

    public SolicitudDesbaneoEstado getEstadoNuevo() {
        return estadoNuevo;
    }

    public void setEstadoNuevo(SolicitudDesbaneoEstado estadoNuevo) {
        this.estadoNuevo = estadoNuevo;
    }

    public String getMotivoSnapshot() {
        return motivoSnapshot;
    }

    public void setMotivoSnapshot(String motivoSnapshot) {
        this.motivoSnapshot = motivoSnapshot;
    }

    public String getResolucionMotivo() {
        return resolucionMotivo;
    }

    public void setResolucionMotivo(String resolucionMotivo) {
        this.resolucionMotivo = resolucionMotivo;
    }

    public Long getUsuarioId() {
        return usuarioId;
    }

    public void setUsuarioId(Long usuarioId) {
        this.usuarioId = usuarioId;
    }

    public Long getAdminId() {
        return adminId;
    }

    public void setAdminId(Long adminId) {
        this.adminId = adminId;
    }

    public String getAccion() {
        return accion;
    }

    public void setAccion(String accion) {
        this.accion = accion;
    }

    public String getComentarioInterno() {
        return comentarioInterno;
    }

    public void setComentarioInterno(String comentarioInterno) {
        this.comentarioInterno = comentarioInterno;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
