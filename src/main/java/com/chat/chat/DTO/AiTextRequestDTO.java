package com.chat.chat.DTO;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class AiTextRequestDTO {

    @NotBlank(message = "El texto es obligatorio")
    private String texto;

    @Size(max = 40, message = "El modo no es valido")
    private String modo;

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
}
