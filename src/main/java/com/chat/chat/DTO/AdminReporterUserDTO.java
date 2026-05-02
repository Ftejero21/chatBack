package com.chat.chat.DTO;

public class AdminReporterUserDTO {

    private Long usuarioId;
    private String nombre;
    private long denunciasRealizadas;

    public Long getUsuarioId() {
        return usuarioId;
    }

    public void setUsuarioId(Long usuarioId) {
        this.usuarioId = usuarioId;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public long getDenunciasRealizadas() {
        return denunciasRealizadas;
    }

    public void setDenunciasRealizadas(long denunciasRealizadas) {
        this.denunciasRealizadas = denunciasRealizadas;
    }
}
