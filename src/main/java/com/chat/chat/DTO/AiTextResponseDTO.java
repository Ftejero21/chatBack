package com.chat.chat.DTO;

public class AiTextResponseDTO {

    private String textoOriginal;
    private String textoGenerado;
    private String modo;
    private boolean success;
    private String codigo;
    private String mensaje;

    public String getTextoOriginal() {
        return textoOriginal;
    }

    public void setTextoOriginal(String textoOriginal) {
        this.textoOriginal = textoOriginal;
    }

    public String getTextoGenerado() {
        return textoGenerado;
    }

    public void setTextoGenerado(String textoGenerado) {
        this.textoGenerado = textoGenerado;
    }

    public String getModo() {
        return modo;
    }

    public void setModo(String modo) {
        this.modo = modo;
    }

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
}
