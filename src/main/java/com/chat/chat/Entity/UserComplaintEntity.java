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
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "user_complaint",
        indexes = {
                @Index(name = "idx_user_complaint_created_at", columnList = "created_at"),
                @Index(name = "idx_user_complaint_leida_created_at", columnList = "leida,created_at"),
                @Index(name = "idx_user_complaint_estado_created_at", columnList = "estado,created_at"),
                @Index(name = "idx_user_complaint_denunciado_created_at", columnList = "denunciado_id,created_at"),
                @Index(name = "idx_user_complaint_denunciante_created_at", columnList = "denunciante_id,created_at")
        }
)
public class UserComplaintEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "denunciante_id", nullable = false)
    private Long denuncianteId;

    @Column(name = "denunciado_id", nullable = false)
    private Long denunciadoId;

    @Column(name = "chat_id")
    private Long chatId;

    @Column(nullable = false, length = 120)
    private String motivo;

    @Lob
    @Column(nullable = false, columnDefinition = "TEXT")
    private String detalle;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserComplaintEstado estado = UserComplaintEstado.PENDIENTE;

    @Column(nullable = false)
    private boolean leida = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "leida_at")
    private LocalDateTime leidaAt;

    @Column(name = "denunciante_nombre", length = 190)
    private String denuncianteNombre;

    @Column(name = "denunciado_nombre", length = 190)
    private String denunciadoNombre;

    @Column(name = "chat_nombre_snapshot", length = 190)
    private String chatNombreSnapshot;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (estado == null) {
            estado = UserComplaintEstado.PENDIENTE;
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

    public Long getDenuncianteId() {
        return denuncianteId;
    }

    public void setDenuncianteId(Long denuncianteId) {
        this.denuncianteId = denuncianteId;
    }

    public Long getDenunciadoId() {
        return denunciadoId;
    }

    public void setDenunciadoId(Long denunciadoId) {
        this.denunciadoId = denunciadoId;
    }

    public Long getChatId() {
        return chatId;
    }

    public void setChatId(Long chatId) {
        this.chatId = chatId;
    }

    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo;
    }

    public String getDetalle() {
        return detalle;
    }

    public void setDetalle(String detalle) {
        this.detalle = detalle;
    }

    public UserComplaintEstado getEstado() {
        return estado;
    }

    public void setEstado(UserComplaintEstado estado) {
        this.estado = estado;
    }

    public boolean isLeida() {
        return leida;
    }

    public void setLeida(boolean leida) {
        this.leida = leida;
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

    public LocalDateTime getLeidaAt() {
        return leidaAt;
    }

    public void setLeidaAt(LocalDateTime leidaAt) {
        this.leidaAt = leidaAt;
    }

    public String getDenuncianteNombre() {
        return denuncianteNombre;
    }

    public void setDenuncianteNombre(String denuncianteNombre) {
        this.denuncianteNombre = denuncianteNombre;
    }

    public String getDenunciadoNombre() {
        return denunciadoNombre;
    }

    public void setDenunciadoNombre(String denunciadoNombre) {
        this.denunciadoNombre = denunciadoNombre;
    }

    public String getChatNombreSnapshot() {
        return chatNombreSnapshot;
    }

    public void setChatNombreSnapshot(String chatNombreSnapshot) {
        this.chatNombreSnapshot = chatNombreSnapshot;
    }
}
