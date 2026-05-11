package com.chat.chat.DTO;

public class AiAppReportHistoryDTO {

    private String estadoAnterior;
    private String estadoNuevo;
    private String estadoLabel;
    private String motivo;
    private String resolucionMotivo;
    private String fecha;
    private Long adminId;
    private String accion;
    private Boolean tieneImagenReporte;
    private String imagenReporteMimeType;
    private String imagenReporteNombre;
    private Long imagenReporteSize;
    private String imagenReporteUrl;

    public String getEstadoAnterior() {
        return estadoAnterior;
    }

    public void setEstadoAnterior(String estadoAnterior) {
        this.estadoAnterior = estadoAnterior;
    }

    public String getEstadoNuevo() {
        return estadoNuevo;
    }

    public void setEstadoNuevo(String estadoNuevo) {
        this.estadoNuevo = estadoNuevo;
    }

    public String getEstadoLabel() {
        return estadoLabel;
    }

    public void setEstadoLabel(String estadoLabel) {
        this.estadoLabel = estadoLabel;
    }

    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo;
    }

    public String getResolucionMotivo() {
        return resolucionMotivo;
    }

    public void setResolucionMotivo(String resolucionMotivo) {
        this.resolucionMotivo = resolucionMotivo;
    }

    public String getFecha() {
        return fecha;
    }

    public void setFecha(String fecha) {
        this.fecha = fecha;
    }

    public Long getAdminId() {
        return adminId;
    }

    public void setAdminId(Long adminId) {
        this.adminId = adminId;
    }

    public String getAccion() {
        return accion;
    }

    public void setAccion(String accion) {
        this.accion = accion;
    }

    public Boolean getTieneImagenReporte() {
        return tieneImagenReporte;
    }

    public void setTieneImagenReporte(Boolean tieneImagenReporte) {
        this.tieneImagenReporte = tieneImagenReporte;
    }

    public String getImagenReporteMimeType() {
        return imagenReporteMimeType;
    }

    public void setImagenReporteMimeType(String imagenReporteMimeType) {
        this.imagenReporteMimeType = imagenReporteMimeType;
    }

    public String getImagenReporteNombre() {
        return imagenReporteNombre;
    }

    public void setImagenReporteNombre(String imagenReporteNombre) {
        this.imagenReporteNombre = imagenReporteNombre;
    }

    public Long getImagenReporteSize() {
        return imagenReporteSize;
    }

    public void setImagenReporteSize(Long imagenReporteSize) {
        this.imagenReporteSize = imagenReporteSize;
    }

    public String getImagenReporteUrl() {
        return imagenReporteUrl;
    }

    public void setImagenReporteUrl(String imagenReporteUrl) {
        this.imagenReporteUrl = imagenReporteUrl;
    }
}
