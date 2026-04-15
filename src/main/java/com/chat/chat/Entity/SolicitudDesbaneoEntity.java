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
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "solicitud_desbaneo",
        indexes = {
                @Index(name = "idx_solicitud_desbaneo_estado_created_at", columnList = "estado,created_at"),
                @Index(name = "idx_solicitud_desbaneo_created_at", columnList = "created_at"),
                @Index(name = "idx_solicitud_desbaneo_tipo_chat_usuario_estado", columnList = "tipo_reporte,chat_id,usuario_id,estado")
        }
)
public class SolicitudDesbaneoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "usuario_id")
    private Long usuarioId;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_reporte", nullable = false, length = 20)
    private ReporteTipo tipoReporte = ReporteTipo.DESBANEO;

    @Column(name = "chat_id")
    private Long chatId;

    @Column(name = "chat_nombre_snapshot", length = 190)
    private String chatNombreSnapshot;

    @Column(name = "chat_cerrado_motivo_snapshot", length = 500)
    private String chatCerradoMotivoSnapshot;

    @Column(nullable = false, length = 190)
    private String email;

    @Column(length = 1000)
    private String motivo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SolicitudDesbaneoEstado estado = SolicitudDesbaneoEstado.PENDIENTE;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "reviewed_by_admin_id")
    private Long reviewedByAdminId;

    @Column(name = "resolucion_motivo", length = 1000)
    private String resolucionMotivo;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (estado == null) {
            estado = SolicitudDesbaneoEstado.PENDIENTE;
        }
        if (tipoReporte == null) {
            tipoReporte = ReporteTipo.DESBANEO;
        }
        updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUsuarioId() {
        return usuarioId;
    }

    public void setUsuarioId(Long usuarioId) {
        this.usuarioId = usuarioId;
    }

    public ReporteTipo getTipoReporte() {
        return tipoReporte;
    }

    public void setTipoReporte(ReporteTipo tipoReporte) {
        this.tipoReporte = tipoReporte;
    }

    public Long getChatId() {
        return chatId;
    }

    public void setChatId(Long chatId) {
        this.chatId = chatId;
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

    public Long getReviewedByAdminId() {
        return reviewedByAdminId;
    }

    public void setReviewedByAdminId(Long reviewedByAdminId) {
        this.reviewedByAdminId = reviewedByAdminId;
    }

    public String getResolucionMotivo() {
        return resolucionMotivo;
    }

    public void setResolucionMotivo(String resolucionMotivo) {
        this.resolucionMotivo = resolucionMotivo;
    }
}
