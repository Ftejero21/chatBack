package com.chat.chat.Entity;

import com.chat.chat.Utils.UserComplaintEstado;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "user_complaint_history",
        indexes = {
                @Index(name = "idx_user_complaint_history_complaint_created", columnList = "complaint_id,created_at"),
                @Index(name = "idx_user_complaint_history_estado", columnList = "estado_nuevo,created_at")
        }
)
public class UserComplaintHistoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "complaint_id", nullable = false)
    private Long complaintId;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_anterior", length = 20)
    private UserComplaintEstado estadoAnterior;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_nuevo", nullable = false, length = 20)
    private UserComplaintEstado estadoNuevo;

    @Column(name = "estado_label", nullable = false, length = 50)
    private String estadoLabel;

    @Column(name = "motivo_snapshot", length = 120)
    private String motivoSnapshot;

    @Lob
    @Column(name = "detalle_snapshot", columnDefinition = "TEXT")
    private String detalleSnapshot;

    @Column(name = "resolucion_motivo", length = 1000)
    private String resolucionMotivo;

    @Column(name = "admin_id")
    private Long adminId;

    @Column(name = "accion", nullable = false, length = 40)
    private String accion;

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

    public Long getComplaintId() {
        return complaintId;
    }

    public void setComplaintId(Long complaintId) {
        this.complaintId = complaintId;
    }

    public UserComplaintEstado getEstadoAnterior() {
        return estadoAnterior;
    }

    public void setEstadoAnterior(UserComplaintEstado estadoAnterior) {
        this.estadoAnterior = estadoAnterior;
    }

    public UserComplaintEstado getEstadoNuevo() {
        return estadoNuevo;
    }

    public void setEstadoNuevo(UserComplaintEstado estadoNuevo) {
        this.estadoNuevo = estadoNuevo;
    }

    public String getEstadoLabel() {
        return estadoLabel;
    }

    public void setEstadoLabel(String estadoLabel) {
        this.estadoLabel = estadoLabel;
    }

    public String getMotivoSnapshot() {
        return motivoSnapshot;
    }

    public void setMotivoSnapshot(String motivoSnapshot) {
        this.motivoSnapshot = motivoSnapshot;
    }

    public String getDetalleSnapshot() {
        return detalleSnapshot;
    }

    public void setDetalleSnapshot(String detalleSnapshot) {
        this.detalleSnapshot = detalleSnapshot;
    }

    public String getResolucionMotivo() {
        return resolucionMotivo;
    }

    public void setResolucionMotivo(String resolucionMotivo) {
        this.resolucionMotivo = resolucionMotivo;
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
