package com.chat.chat.DTO;

import java.util.List;

public class AiComplaintCandidateDTO {

    private Long denunciaId;
    private String motivo;
    private String estado;
    private String fechaCreacion;
    private String fechaActualizacion;
    private String nombreDenunciado;
    private String nombreDenunciante;
    private String complaintDirection;
    private String detalle;
    private Long denuncianteId;
    private Long denunciadoId;
    private Long chatId;
    private String chatNombreSnapshot;
    private String complaintStatus;
    private List<AiComplaintHistoryDTO> historialDenuncia;

    public AiComplaintCandidateDTO() {}

    public AiComplaintCandidateDTO(Long denunciaId, String motivo, String estado, String fechaCreacion) {
        this.denunciaId = denunciaId;
        this.motivo = motivo;
        this.estado = estado;
        this.fechaCreacion = fechaCreacion;
    }

    public AiComplaintCandidateDTO(Long denunciaId, String motivo, String estado, String fechaCreacion, String nombreDenunciado, String complaintDirection) {
        this.denunciaId = denunciaId;
        this.motivo = motivo;
        this.estado = estado;
        this.fechaCreacion = fechaCreacion;
        this.nombreDenunciado = nombreDenunciado;
        this.complaintDirection = complaintDirection;
    }

    public Long getDenunciaId() { return denunciaId; }
    public void setDenunciaId(Long denunciaId) { this.denunciaId = denunciaId; }

    public String getMotivo() { return motivo; }
    public void setMotivo(String motivo) { this.motivo = motivo; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public String getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(String fechaCreacion) { this.fechaCreacion = fechaCreacion; }

    public String getFechaActualizacion() { return fechaActualizacion; }
    public void setFechaActualizacion(String fechaActualizacion) { this.fechaActualizacion = fechaActualizacion; }

    public String getNombreDenunciado() { return nombreDenunciado; }
    public void setNombreDenunciado(String nombreDenunciado) { this.nombreDenunciado = nombreDenunciado; }

    public String getNombreDenunciante() { return nombreDenunciante; }
    public void setNombreDenunciante(String nombreDenunciante) { this.nombreDenunciante = nombreDenunciante; }

    public String getComplaintDirection() { return complaintDirection; }
    public void setComplaintDirection(String complaintDirection) { this.complaintDirection = complaintDirection; }

    public String getDetalle() { return detalle; }
    public void setDetalle(String detalle) { this.detalle = detalle; }

    public Long getDenuncianteId() { return denuncianteId; }
    public void setDenuncianteId(Long denuncianteId) { this.denuncianteId = denuncianteId; }

    public Long getDenunciadoId() { return denunciadoId; }
    public void setDenunciadoId(Long denunciadoId) { this.denunciadoId = denunciadoId; }

    public Long getChatId() { return chatId; }
    public void setChatId(Long chatId) { this.chatId = chatId; }

    public String getChatNombreSnapshot() { return chatNombreSnapshot; }
    public void setChatNombreSnapshot(String chatNombreSnapshot) { this.chatNombreSnapshot = chatNombreSnapshot; }

    public String getComplaintStatus() { return complaintStatus; }
    public void setComplaintStatus(String complaintStatus) { this.complaintStatus = complaintStatus; }

    public List<AiComplaintHistoryDTO> getHistorialDenuncia() { return historialDenuncia; }
    public void setHistorialDenuncia(List<AiComplaintHistoryDTO> historialDenuncia) { this.historialDenuncia = historialDenuncia; }
}
