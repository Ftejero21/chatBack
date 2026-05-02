package com.chat.chat.DTO;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class AdminAiReportDataDTO {

    private LocalDate fechaInicio;
    private LocalDate fechaFin;
    private long totalUsuarios;
    private long usuariosNuevos;
    private long usuariosActivos;
    private long usuariosInactivos;
    private long usuariosBaneados;
    private long baneosRealizados;
    private long desbaneosRealizados;
    private long totalDenuncias;
    private long totalReportes;
    private long gruposCreados;
    private long gruposActivos;
    private long chatsIndividualesActivos;
    private long mensajesEnviados;
    private long encuestasCreadas;
    private List<AdminReportTypeMetricDTO> reportesPorTipo = new ArrayList<>();
    private List<AdminReportStatusMetricDTO> reportesPorEstado = new ArrayList<>();
    private List<AdminModerationActionMetricDTO> moderacionPorAccion = new ArrayList<>();
    private List<AdminModerationReasonMetricDTO> motivosModeracion = new ArrayList<>();
    private List<AdminBannedUserDTO> usuariosActualmenteBaneados = new ArrayList<>();
    private List<AdminReasonMetricDTO> motivosDenuncia = new ArrayList<>();
    private List<AdminConflictiveUserDTO> usuariosConflictivos = new ArrayList<>();
    private List<AdminReporterUserDTO> usuariosQueMasDenuncian = new ArrayList<>();
    private List<AdminGroupMetricDTO> gruposMasActivos = new ArrayList<>();
    private List<String> alertas = new ArrayList<>();

    public LocalDate getFechaInicio() {
        return fechaInicio;
    }

    public void setFechaInicio(LocalDate fechaInicio) {
        this.fechaInicio = fechaInicio;
    }

    public LocalDate getFechaFin() {
        return fechaFin;
    }

    public void setFechaFin(LocalDate fechaFin) {
        this.fechaFin = fechaFin;
    }

    public long getTotalUsuarios() {
        return totalUsuarios;
    }

    public void setTotalUsuarios(long totalUsuarios) {
        this.totalUsuarios = totalUsuarios;
    }

    public long getUsuariosNuevos() {
        return usuariosNuevos;
    }

    public void setUsuariosNuevos(long usuariosNuevos) {
        this.usuariosNuevos = usuariosNuevos;
    }

    public long getUsuariosActivos() {
        return usuariosActivos;
    }

    public void setUsuariosActivos(long usuariosActivos) {
        this.usuariosActivos = usuariosActivos;
    }

    public long getUsuariosInactivos() {
        return usuariosInactivos;
    }

    public void setUsuariosInactivos(long usuariosInactivos) {
        this.usuariosInactivos = usuariosInactivos;
    }

    public long getUsuariosBaneados() {
        return usuariosBaneados;
    }

    public void setUsuariosBaneados(long usuariosBaneados) {
        this.usuariosBaneados = usuariosBaneados;
    }

    public long getBaneosRealizados() {
        return baneosRealizados;
    }

    public void setBaneosRealizados(long baneosRealizados) {
        this.baneosRealizados = baneosRealizados;
    }

    public long getDesbaneosRealizados() {
        return desbaneosRealizados;
    }

    public void setDesbaneosRealizados(long desbaneosRealizados) {
        this.desbaneosRealizados = desbaneosRealizados;
    }

    public long getTotalDenuncias() {
        return totalDenuncias;
    }

    public void setTotalDenuncias(long totalDenuncias) {
        this.totalDenuncias = totalDenuncias;
    }

    public long getTotalReportes() {
        return totalReportes;
    }

    public void setTotalReportes(long totalReportes) {
        this.totalReportes = totalReportes;
    }

    public long getGruposCreados() {
        return gruposCreados;
    }

    public void setGruposCreados(long gruposCreados) {
        this.gruposCreados = gruposCreados;
    }

    public long getGruposActivos() {
        return gruposActivos;
    }

    public void setGruposActivos(long gruposActivos) {
        this.gruposActivos = gruposActivos;
    }

    public long getChatsIndividualesActivos() {
        return chatsIndividualesActivos;
    }

    public void setChatsIndividualesActivos(long chatsIndividualesActivos) {
        this.chatsIndividualesActivos = chatsIndividualesActivos;
    }

    public long getMensajesEnviados() {
        return mensajesEnviados;
    }

    public void setMensajesEnviados(long mensajesEnviados) {
        this.mensajesEnviados = mensajesEnviados;
    }

    public long getEncuestasCreadas() {
        return encuestasCreadas;
    }

    public void setEncuestasCreadas(long encuestasCreadas) {
        this.encuestasCreadas = encuestasCreadas;
    }

    public List<AdminReportTypeMetricDTO> getReportesPorTipo() {
        return reportesPorTipo;
    }

    public void setReportesPorTipo(List<AdminReportTypeMetricDTO> reportesPorTipo) {
        this.reportesPorTipo = reportesPorTipo;
    }

    public List<AdminReportStatusMetricDTO> getReportesPorEstado() {
        return reportesPorEstado;
    }

    public void setReportesPorEstado(List<AdminReportStatusMetricDTO> reportesPorEstado) {
        this.reportesPorEstado = reportesPorEstado;
    }

    public List<AdminModerationActionMetricDTO> getModeracionPorAccion() {
        return moderacionPorAccion;
    }

    public void setModeracionPorAccion(List<AdminModerationActionMetricDTO> moderacionPorAccion) {
        this.moderacionPorAccion = moderacionPorAccion;
    }

    public List<AdminModerationReasonMetricDTO> getMotivosModeracion() {
        return motivosModeracion;
    }

    public void setMotivosModeracion(List<AdminModerationReasonMetricDTO> motivosModeracion) {
        this.motivosModeracion = motivosModeracion;
    }

    public List<AdminBannedUserDTO> getUsuariosActualmenteBaneados() {
        return usuariosActualmenteBaneados;
    }

    public void setUsuariosActualmenteBaneados(List<AdminBannedUserDTO> usuariosActualmenteBaneados) {
        this.usuariosActualmenteBaneados = usuariosActualmenteBaneados;
    }

    public List<AdminReasonMetricDTO> getMotivosDenuncia() {
        return motivosDenuncia;
    }

    public void setMotivosDenuncia(List<AdminReasonMetricDTO> motivosDenuncia) {
        this.motivosDenuncia = motivosDenuncia;
    }

    public List<AdminConflictiveUserDTO> getUsuariosConflictivos() {
        return usuariosConflictivos;
    }

    public void setUsuariosConflictivos(List<AdminConflictiveUserDTO> usuariosConflictivos) {
        this.usuariosConflictivos = usuariosConflictivos;
    }

    public List<AdminReporterUserDTO> getUsuariosQueMasDenuncian() {
        return usuariosQueMasDenuncian;
    }

    public void setUsuariosQueMasDenuncian(List<AdminReporterUserDTO> usuariosQueMasDenuncian) {
        this.usuariosQueMasDenuncian = usuariosQueMasDenuncian;
    }

    public List<AdminGroupMetricDTO> getGruposMasActivos() {
        return gruposMasActivos;
    }

    public void setGruposMasActivos(List<AdminGroupMetricDTO> gruposMasActivos) {
        this.gruposMasActivos = gruposMasActivos;
    }

    public List<String> getAlertas() {
        return alertas;
    }

    public void setAlertas(List<String> alertas) {
        this.alertas = alertas;
    }
}
