package com.chat.chat.DTO;

public class AiTextInternalResponseDTO {

    private boolean success;
    private String codigo;
    private String mensaje;
    private String textoGenerado;

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

    public String getTextoGenerado() {
        return textoGenerado;
    }

    public void setTextoGenerado(String textoGenerado) {
        this.textoGenerado = textoGenerado;
    }
}
