package com.chat.chat.DTO;

import java.util.List;

public class AiSearchIntentInternalResponseDTO {

    private boolean success;
    private String codigo;
    private String mensaje;
    private String target;
    private String senderScope;
    private String personaMencionada;
    private String grupoMencionado;
    private String tipoMensajeSolicitado;
    private String tipoScopeSolicitado;
    private String complaintDirection;
    private String motivoDenuncia;
    private String scheduledStatus;
    private String readStatus;
    private String tipoReporte;
    private String motivoReporte;
    private String reportStatus;
    private Integer limitSolicitado;
    private String complaintStatus;
    private String temporalExpression;
    private String orden;
    private String rangoTemporalSugerido;
    private Boolean listMode;
    private String action;
    private String area;
    private String property;
    private String value;
    private String valuePreset;
    private String label;
    private Double confidence;
    private List<UiCustomizationChangeDTO> changes;

    public AiSearchIntentInternalResponseDTO() {}

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }

    public String getMensaje() { return mensaje; }
    public void setMensaje(String mensaje) { this.mensaje = mensaje; }

    public String getTarget() { return target; }
    public void setTarget(String target) { this.target = target; }

    public String getSenderScope() { return senderScope; }
    public void setSenderScope(String senderScope) { this.senderScope = senderScope; }

    public String getPersonaMencionada() { return personaMencionada; }
    public void setPersonaMencionada(String personaMencionada) { this.personaMencionada = personaMencionada; }

    public String getGrupoMencionado() { return grupoMencionado; }
    public void setGrupoMencionado(String grupoMencionado) { this.grupoMencionado = grupoMencionado; }

    public String getTipoMensajeSolicitado() { return tipoMensajeSolicitado; }
    public void setTipoMensajeSolicitado(String tipoMensajeSolicitado) { this.tipoMensajeSolicitado = tipoMensajeSolicitado; }

    public String getTipoScopeSolicitado() { return tipoScopeSolicitado; }
    public void setTipoScopeSolicitado(String tipoScopeSolicitado) { this.tipoScopeSolicitado = tipoScopeSolicitado; }

    public String getComplaintDirection() { return complaintDirection; }
    public void setComplaintDirection(String complaintDirection) { this.complaintDirection = complaintDirection; }

    public String getMotivoDenuncia() { return motivoDenuncia; }
    public void setMotivoDenuncia(String motivoDenuncia) { this.motivoDenuncia = motivoDenuncia; }

    public String getScheduledStatus() { return scheduledStatus; }
    public void setScheduledStatus(String scheduledStatus) { this.scheduledStatus = scheduledStatus; }

    public String getReadStatus() { return readStatus; }
    public void setReadStatus(String readStatus) { this.readStatus = readStatus; }

    public String getTipoReporte() { return tipoReporte; }
    public void setTipoReporte(String tipoReporte) { this.tipoReporte = tipoReporte; }

    public String getMotivoReporte() { return motivoReporte; }
    public void setMotivoReporte(String motivoReporte) { this.motivoReporte = motivoReporte; }

    public String getReportStatus() { return reportStatus; }
    public void setReportStatus(String reportStatus) { this.reportStatus = reportStatus; }

    public Integer getLimitSolicitado() { return limitSolicitado; }
    public void setLimitSolicitado(Integer limitSolicitado) { this.limitSolicitado = limitSolicitado; }

    public String getComplaintStatus() { return complaintStatus; }
    public void setComplaintStatus(String complaintStatus) { this.complaintStatus = complaintStatus; }

    public String getTemporalExpression() { return temporalExpression; }
    public void setTemporalExpression(String temporalExpression) { this.temporalExpression = temporalExpression; }

    public String getOrden() { return orden; }
    public void setOrden(String orden) { this.orden = orden; }

    public String getRangoTemporalSugerido() { return rangoTemporalSugerido; }
    public void setRangoTemporalSugerido(String rangoTemporalSugerido) { this.rangoTemporalSugerido = rangoTemporalSugerido; }

    public Boolean getListMode() { return listMode; }
    public void setListMode(Boolean listMode) { this.listMode = listMode; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public String getArea() { return area; }
    public void setArea(String area) { this.area = area; }

    public String getProperty() { return property; }
    public void setProperty(String property) { this.property = property; }

    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }

    public String getValuePreset() { return valuePreset; }
    public void setValuePreset(String valuePreset) { this.valuePreset = valuePreset; }

    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }

    public Double getConfidence() { return confidence; }
    public void setConfidence(Double confidence) { this.confidence = confidence; }

    public List<UiCustomizationChangeDTO> getChanges() { return changes; }
    public void setChanges(List<UiCustomizationChangeDTO> changes) { this.changes = changes; }
}
