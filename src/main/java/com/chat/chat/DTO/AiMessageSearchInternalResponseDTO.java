package com.chat.chat.DTO;

import java.util.List;

public class AiMessageSearchInternalResponseDTO {

    private boolean success;
    private String codigo;
    private String mensaje;
    private String resumenBusquedaNatural;
    private List<AiMessageSearchInternalResultDTO> resultados;
    private Boolean sinResultadosFuertes;
    private AiUsageInfoDTO usage;

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

    public String getResumenBusquedaNatural() {
        return resumenBusquedaNatural;
    }

    public void setResumenBusquedaNatural(String resumenBusquedaNatural) {
        this.resumenBusquedaNatural = resumenBusquedaNatural;
    }

    public List<AiMessageSearchInternalResultDTO> getResultados() {
        return resultados;
    }

    public void setResultados(List<AiMessageSearchInternalResultDTO> resultados) {
        this.resultados = resultados;
    }

    public Boolean getSinResultadosFuertes() {
        return sinResultadosFuertes;
    }

    public void setSinResultadosFuertes(Boolean sinResultadosFuertes) {
        this.sinResultadosFuertes = sinResultadosFuertes;
    }

    public AiUsageInfoDTO getUsage() {
        return usage;
    }

    public void setUsage(AiUsageInfoDTO usage) {
        this.usage = usage;
    }
}
