package com.chat.chat.DTO;

import java.util.List;

public class AiAppReportStatusSummaryInternalRequestDTO {

    private String requestId;
    private String consultaOriginal;
    private String usuarioActualNombre;
    private String tipoReporteSolicitado;
    private String reportStatusSolicitado;
    private String motivoReporteDetectado;
    private String temporalExpression;
    private Integer totalReportesEncontrados;
    private List<AiAppReportCandidateDTO> reportes;

    public AiAppReportStatusSummaryInternalRequestDTO() {}

    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }

    public String getConsultaOriginal() { return consultaOriginal; }
    public void setConsultaOriginal(String consultaOriginal) { this.consultaOriginal = consultaOriginal; }

    public String getUsuarioActualNombre() { return usuarioActualNombre; }
    public void setUsuarioActualNombre(String usuarioActualNombre) { this.usuarioActualNombre = usuarioActualNombre; }

    public String getTipoReporteSolicitado() { return tipoReporteSolicitado; }
    public void setTipoReporteSolicitado(String tipoReporteSolicitado) { this.tipoReporteSolicitado = tipoReporteSolicitado; }

    public String getReportStatusSolicitado() { return reportStatusSolicitado; }
    public void setReportStatusSolicitado(String reportStatusSolicitado) { this.reportStatusSolicitado = reportStatusSolicitado; }

    public String getMotivoReporteDetectado() { return motivoReporteDetectado; }
    public void setMotivoReporteDetectado(String motivoReporteDetectado) { this.motivoReporteDetectado = motivoReporteDetectado; }

    public String getTemporalExpression() { return temporalExpression; }
    public void setTemporalExpression(String temporalExpression) { this.temporalExpression = temporalExpression; }

    public Integer getTotalReportesEncontrados() { return totalReportesEncontrados; }
    public void setTotalReportesEncontrados(Integer totalReportesEncontrados) { this.totalReportesEncontrados = totalReportesEncontrados; }

    public List<AiAppReportCandidateDTO> getReportes() { return reportes; }
    public void setReportes(List<AiAppReportCandidateDTO> reportes) { this.reportes = reportes; }
}
