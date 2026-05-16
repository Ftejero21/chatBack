package com.chat.chat.DTO;

public class AiComplaintHistoryDTO {

    private String estadoAnterior;
    private String estadoNuevo;
    private String estadoLabel;
    private String motivo;
    private String detalle;
    private String resolucionMotivo;
    private String fecha;
    private Long adminId;
    private String accion;

    public String getEstadoAnterior() { return estadoAnterior; }
    public void setEstadoAnterior(String estadoAnterior) { this.estadoAnterior = estadoAnterior; }

    public String getEstadoNuevo() { return estadoNuevo; }
    public void setEstadoNuevo(String estadoNuevo) { this.estadoNuevo = estadoNuevo; }

    public String getEstadoLabel() { return estadoLabel; }
    public void setEstadoLabel(String estadoLabel) { this.estadoLabel = estadoLabel; }

    public String getMotivo() { return motivo; }
    public void setMotivo(String motivo) { this.motivo = motivo; }

    public String getDetalle() { return detalle; }
    public void setDetalle(String detalle) { this.detalle = detalle; }

    public String getResolucionMotivo() { return resolucionMotivo; }
    public void setResolucionMotivo(String resolucionMotivo) { this.resolucionMotivo = resolucionMotivo; }

    public String getFecha() { return fecha; }
    public void setFecha(String fecha) { this.fecha = fecha; }

    public Long getAdminId() { return adminId; }
    public void setAdminId(Long adminId) { this.adminId = adminId; }

    public String getAccion() { return accion; }
    public void setAccion(String accion) { this.accion = accion; }
}
