package com.chat.chat.DTO;

public class DashboardStatsDTO {

    private long totalUsuarios;
    private double porcentajeUsuarios; // Positivo (aumento) o negativo (disminución)

    private long chatsActivos;
    private double porcentajeChats;

    private long reportes;
    private double porcentajeReportes;

    private long mensajesHoy;
    private double porcentajeMensajes;

    public DashboardStatsDTO() {
    }

    public DashboardStatsDTO(long totalUsuarios, double porcentajeUsuarios, long chatsActivos, double porcentajeChats,
            long reportes, double porcentajeReportes, long mensajesHoy, double porcentajeMensajes) {
        this.totalUsuarios = totalUsuarios;
        this.porcentajeUsuarios = porcentajeUsuarios;
        this.chatsActivos = chatsActivos;
        this.porcentajeChats = porcentajeChats;
        this.reportes = reportes;
        this.porcentajeReportes = porcentajeReportes;
        this.mensajesHoy = mensajesHoy;
        this.porcentajeMensajes = porcentajeMensajes;
    }

    public long getTotalUsuarios() {
        return totalUsuarios;
    }

    public void setTotalUsuarios(long totalUsuarios) {
        this.totalUsuarios = totalUsuarios;
    }

    public double getPorcentajeUsuarios() {
        return porcentajeUsuarios;
    }

    public void setPorcentajeUsuarios(double porcentajeUsuarios) {
        this.porcentajeUsuarios = porcentajeUsuarios;
    }

    public long getChatsActivos() {
        return chatsActivos;
    }

    public void setChatsActivos(long chatsActivos) {
        this.chatsActivos = chatsActivos;
    }

    public double getPorcentajeChats() {
        return porcentajeChats;
    }

    public void setPorcentajeChats(double porcentajeChats) {
        this.porcentajeChats = porcentajeChats;
    }

    public long getReportes() {
        return reportes;
    }

    public void setReportes(long reportes) {
        this.reportes = reportes;
    }

    public double getPorcentajeReportes() {
        return porcentajeReportes;
    }

    public void setPorcentajeReportes(double porcentajeReportes) {
        this.porcentajeReportes = porcentajeReportes;
    }

    public long getMensajesHoy() {
        return mensajesHoy;
    }

    public void setMensajesHoy(long mensajesHoy) {
        this.mensajesHoy = mensajesHoy;
    }

    public double getPorcentajeMensajes() {
        return porcentajeMensajes;
    }

    public void setPorcentajeMensajes(double porcentajeMensajes) {
        this.porcentajeMensajes = porcentajeMensajes;
    }
}
