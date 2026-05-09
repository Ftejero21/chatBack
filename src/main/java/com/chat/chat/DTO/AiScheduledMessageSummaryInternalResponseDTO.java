package com.chat.chat.DTO;

public class AiScheduledMessageSummaryInternalResponseDTO {

    private boolean success;
    private String codigo;
    private String mensaje;
    private String resumenBusquedaNatural;

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
}
