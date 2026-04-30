package com.chat.chat.DTO;

import java.util.List;

public class AiPollDraftResponseDTO {

    private boolean success;
    private String codigo;
    private String mensaje;
    private String pregunta;
    private List<String> opciones;
    private boolean multipleRespuestas;

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

    public String getPregunta() {
        return pregunta;
    }

    public void setPregunta(String pregunta) {
        this.pregunta = pregunta;
    }

    public List<String> getOpciones() {
        return opciones;
    }

    public void setOpciones(List<String> opciones) {
        this.opciones = opciones;
    }

    public boolean isMultipleRespuestas() {
        return multipleRespuestas;
    }

    public void setMultipleRespuestas(boolean multipleRespuestas) {
        this.multipleRespuestas = multipleRespuestas;
    }
}
