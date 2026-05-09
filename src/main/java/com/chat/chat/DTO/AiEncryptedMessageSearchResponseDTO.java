package com.chat.chat.DTO;

import java.util.List;

public class AiEncryptedMessageSearchResponseDTO {

    private boolean success;
    private String codigo;
    private String mensaje;
    private String tipoScopeAplicado;
    private String nombreScopeAplicado;
    private Boolean scopeResuelto;
    private String motivoScope;
    private Integer confidenceScope;
    private String resumenBusqueda;
    private String encryptedPayload;
    private List<AiEncryptedMessageSearchResultDTO> resultados;

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

    public List<AiEncryptedMessageSearchResultDTO> getResultados() {
        return resultados;
    }

    public void setResultados(List<AiEncryptedMessageSearchResultDTO> resultados) {
        this.resultados = resultados;
    }

    public String getTipoScopeAplicado() {
        return tipoScopeAplicado;
    }

    public void setTipoScopeAplicado(String tipoScopeAplicado) {
        this.tipoScopeAplicado = tipoScopeAplicado;
    }

    public String getNombreScopeAplicado() {
        return nombreScopeAplicado;
    }

    public void setNombreScopeAplicado(String nombreScopeAplicado) {
        this.nombreScopeAplicado = nombreScopeAplicado;
    }

    public Boolean getScopeResuelto() {
        return scopeResuelto;
    }

    public void setScopeResuelto(Boolean scopeResuelto) {
        this.scopeResuelto = scopeResuelto;
    }

    public String getMotivoScope() {
        return motivoScope;
    }

    public void setMotivoScope(String motivoScope) {
        this.motivoScope = motivoScope;
    }

    public Integer getConfidenceScope() {
        return confidenceScope;
    }

    public void setConfidenceScope(Integer confidenceScope) {
        this.confidenceScope = confidenceScope;
    }

    public String getResumenBusqueda() {
        return resumenBusqueda;
    }

    public void setResumenBusqueda(String resumenBusqueda) {
        this.resumenBusqueda = resumenBusqueda;
    }

    public String getEncryptedPayload() {
        return encryptedPayload;
    }

    public void setEncryptedPayload(String encryptedPayload) {
        this.encryptedPayload = encryptedPayload;
    }
}
