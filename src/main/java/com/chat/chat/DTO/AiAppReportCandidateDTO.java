package com.chat.chat.DTO;

import java.util.List;

public class AiAppReportCandidateDTO {

    private Long reporteId;
    private String tipoReporte;
    private String estado;
    private String estadoSemantico;
    private String motivo;
    private String resolucionMotivo;
    private String createdAt;
    private String updatedAt;
    private Long reviewedByAdminId;
    private String chatNombreSnapshot;
    private String chatCerradoMotivoSnapshot;
    private List<AiAppReportHistoryDTO> historialReporte;
    private Boolean aproximado;
    private String motivoCoincidencia;

    public AiAppReportCandidateDTO() {}

    public Long getReporteId() { return reporteId; }
    public void setReporteId(Long reporteId) { this.reporteId = reporteId; }

    public String getTipoReporte() { return tipoReporte; }
    public void setTipoReporte(String tipoReporte) { this.tipoReporte = tipoReporte; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public String getEstadoSemantico() { return estadoSemantico; }
    public void setEstadoSemantico(String estadoSemantico) { this.estadoSemantico = estadoSemantico; }

    public String getMotivo() { return motivo; }
    public void setMotivo(String motivo) { this.motivo = motivo; }

    public String getResolucionMotivo() { return resolucionMotivo; }
    public void setResolucionMotivo(String resolucionMotivo) { this.resolucionMotivo = resolucionMotivo; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }

    public Long getReviewedByAdminId() { return reviewedByAdminId; }
    public void setReviewedByAdminId(Long reviewedByAdminId) { this.reviewedByAdminId = reviewedByAdminId; }

    public String getChatNombreSnapshot() { return chatNombreSnapshot; }
    public void setChatNombreSnapshot(String chatNombreSnapshot) { this.chatNombreSnapshot = chatNombreSnapshot; }

    public String getChatCerradoMotivoSnapshot() { return chatCerradoMotivoSnapshot; }
    public void setChatCerradoMotivoSnapshot(String chatCerradoMotivoSnapshot) { this.chatCerradoMotivoSnapshot = chatCerradoMotivoSnapshot; }

    public List<AiAppReportHistoryDTO> getHistorialReporte() { return historialReporte; }
    public void setHistorialReporte(List<AiAppReportHistoryDTO> historialReporte) { this.historialReporte = historialReporte; }

    public Boolean getAproximado() { return aproximado; }
    public void setAproximado(Boolean aproximado) { this.aproximado = aproximado; }

    public String getMotivoCoincidencia() { return motivoCoincidencia; }
    public void setMotivoCoincidencia(String motivoCoincidencia) { this.motivoCoincidencia = motivoCoincidencia; }
}
