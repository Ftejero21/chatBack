package com.chat.chat.DTO;

public class AiMessageSearchInternalResultDTO {

    private Long mensajeId;
    private String motivoCoincidencia;
    private Integer relevancia;
    private Boolean mejorResultadoAproximado;

    public Long getMensajeId() {
        return mensajeId;
    }

    public void setMensajeId(Long mensajeId) {
        this.mensajeId = mensajeId;
    }

    public String getMotivoCoincidencia() {
        return motivoCoincidencia;
    }

    public void setMotivoCoincidencia(String motivoCoincidencia) {
        this.motivoCoincidencia = motivoCoincidencia;
    }

    public Integer getRelevancia() {
        return relevancia;
    }

    public void setRelevancia(Integer relevancia) {
        this.relevancia = relevancia;
    }

    public Boolean getMejorResultadoAproximado() {
        return mejorResultadoAproximado;
    }

    public void setMejorResultadoAproximado(Boolean mejorResultadoAproximado) {
        this.mejorResultadoAproximado = mejorResultadoAproximado;
    }
}
