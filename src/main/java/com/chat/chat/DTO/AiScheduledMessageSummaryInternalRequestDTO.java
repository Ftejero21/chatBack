package com.chat.chat.DTO;

import java.util.List;

public class AiScheduledMessageSummaryInternalRequestDTO {

    private String consultaOriginal;
    private String usuarioActualNombre;
    private String temporalExpression;
    private String personaMencionada;
    private String grupoMencionado;
    private String scheduledStatus;
    private String orden;
    private Boolean huboFallbackTemporal;
    private List<AiScheduledMessageSummaryCandidateDTO> resultados;

    public String getConsultaOriginal() {
        return consultaOriginal;
    }

    public void setConsultaOriginal(String consultaOriginal) {
        this.consultaOriginal = consultaOriginal;
    }

    public String getUsuarioActualNombre() {
        return usuarioActualNombre;
    }

    public void setUsuarioActualNombre(String usuarioActualNombre) {
        this.usuarioActualNombre = usuarioActualNombre;
    }

    public String getTemporalExpression() {
        return temporalExpression;
    }

    public void setTemporalExpression(String temporalExpression) {
        this.temporalExpression = temporalExpression;
    }

    public String getPersonaMencionada() {
        return personaMencionada;
    }

    public void setPersonaMencionada(String personaMencionada) {
        this.personaMencionada = personaMencionada;
    }

    public String getGrupoMencionado() {
        return grupoMencionado;
    }

    public void setGrupoMencionado(String grupoMencionado) {
        this.grupoMencionado = grupoMencionado;
    }

    public String getScheduledStatus() {
        return scheduledStatus;
    }

    public void setScheduledStatus(String scheduledStatus) {
        this.scheduledStatus = scheduledStatus;
    }

    public String getOrden() {
        return orden;
    }

    public void setOrden(String orden) {
        this.orden = orden;
    }

    public Boolean getHuboFallbackTemporal() {
        return huboFallbackTemporal;
    }

    public void setHuboFallbackTemporal(Boolean huboFallbackTemporal) {
        this.huboFallbackTemporal = huboFallbackTemporal;
    }

    public List<AiScheduledMessageSummaryCandidateDTO> getResultados() {
        return resultados;
    }

    public void setResultados(List<AiScheduledMessageSummaryCandidateDTO> resultados) {
        this.resultados = resultados;
    }
}
