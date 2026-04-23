package com.chat.chat.DTO;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class SolicitudDesbaneoEstadoUpdateDTO {
    @NotBlank(message = "estado es obligatorio")
    @Size(max = 32, message = "estado invalido")
    private String estado;

    @Size(max = 1000, message = "resolucionMotivo supera el maximo de 1000 caracteres")
    private String resolucionMotivo;

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public String getResolucionMotivo() {
        return resolucionMotivo;
    }

    public void setResolucionMotivo(String resolucionMotivo) {
        this.resolucionMotivo = resolucionMotivo;
    }
}
