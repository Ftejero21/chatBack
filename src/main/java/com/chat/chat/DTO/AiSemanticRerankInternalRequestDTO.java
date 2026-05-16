package com.chat.chat.DTO;

import java.util.List;

public class AiSemanticRerankInternalRequestDTO {

    private String requestId;
    private String tipoBusqueda;
    private String consultaOriginal;
    private String target;
    private String motivoBuscado;
    private Integer limitSolicitado;
    private String temporalExpression;
    private String tipoReporte;
    private String orden;
    private List<AiSemanticRerankCandidateDTO> candidatos;

    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }

    public String getTipoBusqueda() { return tipoBusqueda; }
    public void setTipoBusqueda(String tipoBusqueda) { this.tipoBusqueda = tipoBusqueda; }

    public String getConsultaOriginal() { return consultaOriginal; }
    public void setConsultaOriginal(String consultaOriginal) { this.consultaOriginal = consultaOriginal; }

    public String getTarget() { return target; }
    public void setTarget(String target) { this.target = target; }

    public String getMotivoBuscado() { return motivoBuscado; }
    public void setMotivoBuscado(String motivoBuscado) { this.motivoBuscado = motivoBuscado; }

    public Integer getLimitSolicitado() { return limitSolicitado; }
    public void setLimitSolicitado(Integer limitSolicitado) { this.limitSolicitado = limitSolicitado; }

    public String getTemporalExpression() { return temporalExpression; }
    public void setTemporalExpression(String temporalExpression) { this.temporalExpression = temporalExpression; }

    public String getTipoReporte() { return tipoReporte; }
    public void setTipoReporte(String tipoReporte) { this.tipoReporte = tipoReporte; }

    public String getOrden() { return orden; }
    public void setOrden(String orden) { this.orden = orden; }

    public List<AiSemanticRerankCandidateDTO> getCandidatos() { return candidatos; }
    public void setCandidatos(List<AiSemanticRerankCandidateDTO> candidatos) { this.candidatos = candidatos; }
}
