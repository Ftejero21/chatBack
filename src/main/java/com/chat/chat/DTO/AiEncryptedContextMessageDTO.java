package com.chat.chat.DTO;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class AiEncryptedContextMessageDTO {

    private Long id;

    private Long autorId;

    @Size(max = 80, message = "El autor del mensaje supera la longitud maxima permitida")
    private String autor;

    @Size(max = 80, message = "La fecha del mensaje supera la longitud maxima permitida")
    private String fecha;

    private boolean esUsuarioActual;

    @NotBlank(message = "encryptedPayload es obligatorio")
    @Size(max = 50000, message = "El encryptedPayload supera la longitud maxima permitida")
    private String encryptedPayload;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getAutorId() {
        return autorId;
    }

    public void setAutorId(Long autorId) {
        this.autorId = autorId;
    }

    public String getAutor() {
        return autor;
    }

    public void setAutor(String autor) {
        this.autor = autor;
    }

    public String getFecha() {
        return fecha;
    }

    public void setFecha(String fecha) {
        this.fecha = fecha;
    }

    public boolean isEsUsuarioActual() {
        return esUsuarioActual;
    }

    public void setEsUsuarioActual(boolean esUsuarioActual) {
        this.esUsuarioActual = esUsuarioActual;
    }

    public String getEncryptedPayload() {
        return encryptedPayload;
    }

    public void setEncryptedPayload(String encryptedPayload) {
        this.encryptedPayload = encryptedPayload;
    }
}
