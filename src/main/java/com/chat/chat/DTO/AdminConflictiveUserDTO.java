package com.chat.chat.DTO;

public class AdminConflictiveUserDTO {

    private Long usuarioId;
    private String nombre;
    private String email;
    private long denunciasRecibidas;
    private long reportesRecibidos;
    private String motivoPrincipal;

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

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public long getDenunciasRecibidas() {
        return denunciasRecibidas;
    }

    public void setDenunciasRecibidas(long denunciasRecibidas) {
        this.denunciasRecibidas = denunciasRecibidas;
    }

    public long getReportesRecibidos() {
        return reportesRecibidos;
    }

    public void setReportesRecibidos(long reportesRecibidos) {
        this.reportesRecibidos = reportesRecibidos;
    }

    public String getMotivoPrincipal() {
        return motivoPrincipal;
    }

    public void setMotivoPrincipal(String motivoPrincipal) {
        this.motivoPrincipal = motivoPrincipal;
    }
}
