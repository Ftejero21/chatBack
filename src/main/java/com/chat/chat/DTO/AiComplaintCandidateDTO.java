package com.chat.chat.DTO;

public class AiComplaintCandidateDTO {

    private Long denunciaId;
    private String motivo;
    private String estado;
    private String fechaCreacion;
    private String nombreDenunciado;
    private String complaintDirection;

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

    public String getNombreDenunciado() { return nombreDenunciado; }
    public void setNombreDenunciado(String nombreDenunciado) { this.nombreDenunciado = nombreDenunciado; }

    public String getComplaintDirection() { return complaintDirection; }
    public void setComplaintDirection(String complaintDirection) { this.complaintDirection = complaintDirection; }
}
