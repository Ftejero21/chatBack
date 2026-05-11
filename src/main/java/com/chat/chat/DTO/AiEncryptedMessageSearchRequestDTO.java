package com.chat.chat.DTO;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public class AiEncryptedMessageSearchRequestDTO {

    @NotBlank(message = "consulta es obligatoria")
    @Size(max = 1000, message = "consulta no puede superar 1000 caracteres")
    private String consulta;

    @Max(value = 20, message = "maxResultados no puede superar 20")
    private Integer maxResultados;

    @Max(value = 1000, message = "maxMensajesAnalizar no puede superar 1000")
    private Integer maxMensajesAnalizar;

    private LocalDateTime fechaInicio;

    private LocalDateTime fechaFin;

    private Boolean incluirGrupales;

    private Boolean incluirIndividuales;

    private String imagenReporteBase64;

    private String imagenReporteMimeType;

    private String imagenReporteNombre;

    public String getConsulta() {
        return consulta;
    }

    public void setConsulta(String consulta) {
        this.consulta = consulta;
    }

    public Integer getMaxResultados() {
        return maxResultados;
    }

    public void setMaxResultados(Integer maxResultados) {
        this.maxResultados = maxResultados;
    }

    public Integer getMaxMensajesAnalizar() {
        return maxMensajesAnalizar;
    }

    public void setMaxMensajesAnalizar(Integer maxMensajesAnalizar) {
        this.maxMensajesAnalizar = maxMensajesAnalizar;
    }

    public LocalDateTime getFechaInicio() {
        return fechaInicio;
    }

    public void setFechaInicio(LocalDateTime fechaInicio) {
        this.fechaInicio = fechaInicio;
    }

    public LocalDateTime getFechaFin() {
        return fechaFin;
    }

    public void setFechaFin(LocalDateTime fechaFin) {
        this.fechaFin = fechaFin;
    }

    public Boolean getIncluirGrupales() {
        return incluirGrupales;
    }

    public void setIncluirGrupales(Boolean incluirGrupales) {
        this.incluirGrupales = incluirGrupales;
    }

    public Boolean getIncluirIndividuales() {
        return incluirIndividuales;
    }

    public void setIncluirIndividuales(Boolean incluirIndividuales) {
        this.incluirIndividuales = incluirIndividuales;
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
}
