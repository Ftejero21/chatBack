package com.chat.chat.DTO;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class AiChatContextMessageDTO {

    @NotBlank(message = "El autor del contexto es obligatorio")
    @Size(max = 80, message = "El autor del contexto supera la longitud maxima permitida")
    private String autor;

    @NotBlank(message = "El contenido del contexto es obligatorio")
    @Size(max = 2000, message = "El contenido del contexto supera la longitud maxima permitida")
    private String contenido;

    private boolean esUsuarioActual;

    public String getAutor() {
        return autor;
    }

    public void setAutor(String autor) {
        this.autor = autor;
    }

    public String getContenido() {
        return contenido;
    }

    public void setContenido(String contenido) {
        this.contenido = contenido;
    }

    public boolean isEsUsuarioActual() {
        return esUsuarioActual;
    }

    public void setEsUsuarioActual(boolean esUsuarioActual) {
        this.esUsuarioActual = esUsuarioActual;
    }
}
