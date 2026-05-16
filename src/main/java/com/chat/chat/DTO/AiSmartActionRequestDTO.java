package com.chat.chat.DTO;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class AiSmartActionRequestDTO {

    @NotBlank(message = "consulta es obligatoria")
    @Size(max = 1000, message = "consulta no puede superar 1000 caracteres")
    private String consulta;

    private String imagenReporteBase64;
    private String imagenReporteMimeType;
    private String imagenReporteNombre;
    private UiCustomizationContextDTO uiContext;

    public String getConsulta() {
        return consulta;
    }

    public void setConsulta(String consulta) {
        this.consulta = consulta;
    }

    public String getImagenReporteBase64() {
        return imagenReporteBase64;
    }

    public void setImagenReporteBase64(String imagenReporteBase64) {
        this.imagenReporteBase64 = imagenReporteBase64;
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

    public UiCustomizationContextDTO getUiContext() {
        return uiContext;
    }

    public void setUiContext(UiCustomizationContextDTO uiContext) {
        this.uiContext = uiContext;
    }
}
