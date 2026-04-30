package com.chat.chat.DTO;

import jakarta.validation.constraints.Size;

public class AiConversationMessageDTO {

    private Long id;

    @Size(max = 80, message = "El autor del mensaje supera la longitud maxima permitida")
    private String autor;

    @Size(max = 10000, message = "El contenido del mensaje supera la longitud maxima permitida")
    private String contenido;

    private boolean esUsuarioActual;

    @Size(max = 80, message = "La fecha del mensaje supera la longitud maxima permitida")
    private String fecha;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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

    public String getFecha() {
        return fecha;
    }

    public void setFecha(String fecha) {
        this.fecha = fecha;
    }
}
