package com.chat.chat.DTO;

import jakarta.validation.constraints.Size;

public class CrearStickerRequestDTO {

    @Size(max = 100, message = "nombre excede longitud maxima")
    private String nombre;

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }
}

