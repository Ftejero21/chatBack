package com.chat.chat.DTO;

public class AiAppReportStatusSummaryInternalResponseDTO {

    private boolean success;
    private String codigo;
    private String mensaje;
    private String resumenBusqueda;
    private AiUsageInfoDTO usage;

    public AiAppReportStatusSummaryInternalResponseDTO() {}

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }

    public String getMensaje() { return mensaje; }
    public void setMensaje(String mensaje) { this.mensaje = mensaje; }

    public String getResumenBusqueda() { return resumenBusqueda; }
    public void setResumenBusqueda(String resumenBusqueda) { this.resumenBusqueda = resumenBusqueda; }
    public AiUsageInfoDTO getUsage() { return usage; }
    public void setUsage(AiUsageInfoDTO usage) { this.usage = usage; }
}
