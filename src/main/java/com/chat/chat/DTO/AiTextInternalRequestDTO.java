package com.chat.chat.DTO;

import java.util.List;
import java.util.Map;

public class AiTextInternalRequestDTO {

    private String requestId;
    private String texto;
    private String modo;
    private Map<String, Object> metadata;
    private String mensajeRecibido;
    private List<AiTextContextMessageDTO> contextoConversacion;
    private List<String> ejemplosEstiloUsuario;

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getTexto() {
        return texto;
    }

    public void setTexto(String texto) {
        this.texto = texto;
    }

    public String getModo() {
        return modo;
    }

    public void setModo(String modo) {
        this.modo = modo;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    public String getMensajeRecibido() {
        return mensajeRecibido;
    }

    public void setMensajeRecibido(String mensajeRecibido) {
        this.mensajeRecibido = mensajeRecibido;
    }

    public List<AiTextContextMessageDTO> getContextoConversacion() {
        return contextoConversacion;
    }

    public void setContextoConversacion(List<AiTextContextMessageDTO> contextoConversacion) {
        this.contextoConversacion = contextoConversacion;
    }

    public List<String> getEjemplosEstiloUsuario() {
        return ejemplosEstiloUsuario;
    }

    public void setEjemplosEstiloUsuario(List<String> ejemplosEstiloUsuario) {
        this.ejemplosEstiloUsuario = ejemplosEstiloUsuario;
    }
}
