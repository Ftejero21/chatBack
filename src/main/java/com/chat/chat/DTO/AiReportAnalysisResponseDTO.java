package com.chat.chat.DTO;

public class AiReportAnalysisResponseDTO {

    private boolean success;
    private String codigo;
    private String mensaje;
    private String motivoSeleccionado;
    private String descripcionDenuncia;
    private String gravedad;
    private String resumen;
    private String accionSugerida;

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public String getMensaje() {
        return mensaje;
    }

    public void setMensaje(String mensaje) {
        this.mensaje = mensaje;
    }

    public String getMotivoSeleccionado() {
        return motivoSeleccionado;
    }

    public void setMotivoSeleccionado(String motivoSeleccionado) {
        this.motivoSeleccionado = motivoSeleccionado;
    }

    public String getDescripcionDenuncia() {
        return descripcionDenuncia;
    }

    public void setDescripcionDenuncia(String descripcionDenuncia) {
        this.descripcionDenuncia = descripcionDenuncia;
    }

    public String getGravedad() {
        return gravedad;
    }

    public void setGravedad(String gravedad) {
        this.gravedad = gravedad;
    }

    public String getResumen() {
        return resumen;
    }

    public void setResumen(String resumen) {
        this.resumen = resumen;
    }

    public String getAccionSugerida() {
        return accionSugerida;
    }

    public void setAccionSugerida(String accionSugerida) {
        this.accionSugerida = accionSugerida;
    }
}
