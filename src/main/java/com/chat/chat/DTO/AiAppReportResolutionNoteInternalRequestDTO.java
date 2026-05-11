package com.chat.chat.DTO;

public class AiAppReportResolutionNoteInternalRequestDTO {

    private String requestId;
    private String tipoReporte;
    private String estadoDestino;
    private String motivo;
    private String resolucionAdmin;
    private String usuarioNombre;
    private String createdAt;

    public AiAppReportResolutionNoteInternalRequestDTO() {}

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getTipoReporte() {
        return tipoReporte;
    }

    public void setTipoReporte(String tipoReporte) {
        this.tipoReporte = tipoReporte;
    }

    public String getEstadoDestino() {
        return estadoDestino;
    }

    public void setEstadoDestino(String estadoDestino) {
        this.estadoDestino = estadoDestino;
    }

    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo;
    }

    public String getResolucionAdmin() {
        return resolucionAdmin;
    }

    public void setResolucionAdmin(String resolucionAdmin) {
        this.resolucionAdmin = resolucionAdmin;
    }

    public String getUsuarioNombre() {
        return usuarioNombre;
    }

    public void setUsuarioNombre(String usuarioNombre) {
        this.usuarioNombre = usuarioNombre;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
}
