package com.chat.chat.DTO;

import java.util.List;
import java.util.Map;

public class UserExpedienteDTO {
    private Long userId;
    private String nombre;
    private long totalDenunciasRecibidas;
    private long totalDenunciasRealizadas;
    private Map<String, Long> conteoPorMotivo;
    private List<UserComplaintDTO> ultimasCincoDenuncias;
    private String fechaRegistro;
    private String estadoCuenta;
    private boolean cuentaActiva;
    private List<UserModerationHistoryItemDTO> historialModeracion;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public long getTotalDenunciasRecibidas() {
        return totalDenunciasRecibidas;
    }

    public void setTotalDenunciasRecibidas(long totalDenunciasRecibidas) {
        this.totalDenunciasRecibidas = totalDenunciasRecibidas;
    }

    public long getTotalDenunciasRealizadas() {
        return totalDenunciasRealizadas;
    }

    public void setTotalDenunciasRealizadas(long totalDenunciasRealizadas) {
        this.totalDenunciasRealizadas = totalDenunciasRealizadas;
    }

    public Map<String, Long> getConteoPorMotivo() {
        return conteoPorMotivo;
    }

    public void setConteoPorMotivo(Map<String, Long> conteoPorMotivo) {
        this.conteoPorMotivo = conteoPorMotivo;
    }

    public List<UserComplaintDTO> getUltimasCincoDenuncias() {
        return ultimasCincoDenuncias;
    }

    public void setUltimasCincoDenuncias(List<UserComplaintDTO> ultimasCincoDenuncias) {
        this.ultimasCincoDenuncias = ultimasCincoDenuncias;
    }

    public String getFechaRegistro() {
        return fechaRegistro;
    }

    public void setFechaRegistro(String fechaRegistro) {
        this.fechaRegistro = fechaRegistro;
    }

    public String getEstadoCuenta() {
        return estadoCuenta;
    }

    public void setEstadoCuenta(String estadoCuenta) {
        this.estadoCuenta = estadoCuenta;
    }

    public boolean isCuentaActiva() {
        return cuentaActiva;
    }

    public void setCuentaActiva(boolean cuentaActiva) {
        this.cuentaActiva = cuentaActiva;
    }

    public List<UserModerationHistoryItemDTO> getHistorialModeracion() {
        return historialModeracion;
    }

    public void setHistorialModeracion(List<UserModerationHistoryItemDTO> historialModeracion) {
        this.historialModeracion = historialModeracion;
    }
}
