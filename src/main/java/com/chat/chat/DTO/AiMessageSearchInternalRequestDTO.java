package com.chat.chat.DTO;

import java.util.List;
import com.chat.chat.DTO.AiComplaintCandidateDTO;

public class AiMessageSearchInternalRequestDTO {

    private String consulta;
    private String consultaOriginal;
    private String usuarioActualNombre;
    private Integer maxResultados;
    private String senderScope;
    private String tipoScopeInicial;
    private String tipoScopeAplicado;
    private String nombreScopeSolicitado;
    private String nombreScopeAplicado;
    private String personaSolicitada;
    private String grupoSolicitado;
    private String personaObjetivoNombre;
    private String emisorObjetivoNombre;
    private Boolean scopeResuelto;
    private Boolean huboFallbackScope;
    private Boolean busquedaEnMensajesDeOtroUsuario;
    private Boolean intencionAudio;
    private Boolean intencionLocalizacion;
    private String tipoMensajeSolicitado;
    private Boolean rangoTemporalAplicado;
    private Boolean huboFallbackTemporal;
    private String descripcionRangoTemporal;
    private Boolean fallbackSinRangoTemporal;
    private List<AiMessageSearchPersonCandidateDTO> personasCandidatas;
    private List<AiMessageSearchCandidateDTO> candidatos;
    private String searchTarget;
    private List<AiComplaintCandidateDTO> denunciasCandidatas;
    private Integer totalDenunciasEncontradas;
    private String complaintDirection;
    private String personaDenunciadaSolicitada;
    private String motivoDetectado;
    private Boolean huboFallbackTemporalExpandido;

    public String getConsulta() {
        return consulta;
    }

    public void setConsulta(String consulta) {
        this.consulta = consulta;
    }

    public String getConsultaOriginal() {
        return consultaOriginal;
    }

    public void setConsultaOriginal(String consultaOriginal) {
        this.consultaOriginal = consultaOriginal;
    }

    public String getUsuarioActualNombre() {
        return usuarioActualNombre;
    }

    public void setUsuarioActualNombre(String usuarioActualNombre) {
        this.usuarioActualNombre = usuarioActualNombre;
    }

    public Integer getMaxResultados() {
        return maxResultados;
    }

    public void setMaxResultados(Integer maxResultados) {
        this.maxResultados = maxResultados;
    }

    public String getSenderScope() {
        return senderScope;
    }

    public void setSenderScope(String senderScope) {
        this.senderScope = senderScope;
    }

    public String getTipoScopeInicial() {
        return tipoScopeInicial;
    }

    public void setTipoScopeInicial(String tipoScopeInicial) {
        this.tipoScopeInicial = tipoScopeInicial;
    }

    public String getTipoScopeAplicado() {
        return tipoScopeAplicado;
    }

    public void setTipoScopeAplicado(String tipoScopeAplicado) {
        this.tipoScopeAplicado = tipoScopeAplicado;
    }

    public String getNombreScopeSolicitado() {
        return nombreScopeSolicitado;
    }

    public void setNombreScopeSolicitado(String nombreScopeSolicitado) {
        this.nombreScopeSolicitado = nombreScopeSolicitado;
    }

    public String getNombreScopeAplicado() {
        return nombreScopeAplicado;
    }

    public void setNombreScopeAplicado(String nombreScopeAplicado) {
        this.nombreScopeAplicado = nombreScopeAplicado;
    }

    public String getPersonaSolicitada() {
        return personaSolicitada;
    }

    public void setPersonaSolicitada(String personaSolicitada) {
        this.personaSolicitada = personaSolicitada;
    }

    public String getGrupoSolicitado() {
        return grupoSolicitado;
    }

    public void setGrupoSolicitado(String grupoSolicitado) {
        this.grupoSolicitado = grupoSolicitado;
    }

    public String getPersonaObjetivoNombre() {
        return personaObjetivoNombre;
    }

    public void setPersonaObjetivoNombre(String personaObjetivoNombre) {
        this.personaObjetivoNombre = personaObjetivoNombre;
    }

    public String getEmisorObjetivoNombre() {
        return emisorObjetivoNombre;
    }

    public void setEmisorObjetivoNombre(String emisorObjetivoNombre) {
        this.emisorObjetivoNombre = emisorObjetivoNombre;
    }

    public Boolean getScopeResuelto() {
        return scopeResuelto;
    }

    public void setScopeResuelto(Boolean scopeResuelto) {
        this.scopeResuelto = scopeResuelto;
    }

    public Boolean getHuboFallbackScope() {
        return huboFallbackScope;
    }

    public void setHuboFallbackScope(Boolean huboFallbackScope) {
        this.huboFallbackScope = huboFallbackScope;
    }

    public Boolean getBusquedaEnMensajesDeOtroUsuario() {
        return busquedaEnMensajesDeOtroUsuario;
    }

    public void setBusquedaEnMensajesDeOtroUsuario(Boolean busquedaEnMensajesDeOtroUsuario) {
        this.busquedaEnMensajesDeOtroUsuario = busquedaEnMensajesDeOtroUsuario;
    }

    public Boolean getIntencionAudio() {
        return intencionAudio;
    }

    public void setIntencionAudio(Boolean intencionAudio) {
        this.intencionAudio = intencionAudio;
    }

    public Boolean getIntencionLocalizacion() {
        return intencionLocalizacion;
    }

    public void setIntencionLocalizacion(Boolean intencionLocalizacion) {
        this.intencionLocalizacion = intencionLocalizacion;
    }

    public String getTipoMensajeSolicitado() {
        return tipoMensajeSolicitado;
    }

    public void setTipoMensajeSolicitado(String tipoMensajeSolicitado) {
        this.tipoMensajeSolicitado = tipoMensajeSolicitado;
    }

    public Boolean getRangoTemporalAplicado() {
        return rangoTemporalAplicado;
    }

    public void setRangoTemporalAplicado(Boolean rangoTemporalAplicado) {
        this.rangoTemporalAplicado = rangoTemporalAplicado;
    }

    public Boolean getHuboFallbackTemporal() {
        return huboFallbackTemporal;
    }

    public void setHuboFallbackTemporal(Boolean huboFallbackTemporal) {
        this.huboFallbackTemporal = huboFallbackTemporal;
    }

    public String getDescripcionRangoTemporal() {
        return descripcionRangoTemporal;
    }

    public void setDescripcionRangoTemporal(String descripcionRangoTemporal) {
        this.descripcionRangoTemporal = descripcionRangoTemporal;
    }

    public Boolean getFallbackSinRangoTemporal() {
        return fallbackSinRangoTemporal;
    }

    public void setFallbackSinRangoTemporal(Boolean fallbackSinRangoTemporal) {
        this.fallbackSinRangoTemporal = fallbackSinRangoTemporal;
    }

    public List<AiMessageSearchPersonCandidateDTO> getPersonasCandidatas() {
        return personasCandidatas;
    }

    public void setPersonasCandidatas(List<AiMessageSearchPersonCandidateDTO> personasCandidatas) {
        this.personasCandidatas = personasCandidatas;
    }

    public List<AiMessageSearchCandidateDTO> getCandidatos() {
        return candidatos;
    }

    public void setCandidatos(List<AiMessageSearchCandidateDTO> candidatos) {
        this.candidatos = candidatos;
    }

    public String getSearchTarget() {
        return searchTarget;
    }

    public void setSearchTarget(String searchTarget) {
        this.searchTarget = searchTarget;
    }

    public List<AiComplaintCandidateDTO> getDenunciasCandidatas() {
        return denunciasCandidatas;
    }

    public void setDenunciasCandidatas(List<AiComplaintCandidateDTO> denunciasCandidatas) {
        this.denunciasCandidatas = denunciasCandidatas;
    }

    public Integer getTotalDenunciasEncontradas() {
        return totalDenunciasEncontradas;
    }

    public void setTotalDenunciasEncontradas(Integer totalDenunciasEncontradas) {
        this.totalDenunciasEncontradas = totalDenunciasEncontradas;
    }

    public String getComplaintDirection() {
        return complaintDirection;
    }

    public void setComplaintDirection(String complaintDirection) {
        this.complaintDirection = complaintDirection;
    }

    public String getPersonaDenunciadaSolicitada() {
        return personaDenunciadaSolicitada;
    }

    public void setPersonaDenunciadaSolicitada(String personaDenunciadaSolicitada) {
        this.personaDenunciadaSolicitada = personaDenunciadaSolicitada;
    }

    public String getMotivoDetectado() {
        return motivoDetectado;
    }

    public void setMotivoDetectado(String motivoDetectado) {
        this.motivoDetectado = motivoDetectado;
    }

    public Boolean getHuboFallbackTemporalExpandido() {
        return huboFallbackTemporalExpandido;
    }

    public void setHuboFallbackTemporalExpandido(Boolean huboFallbackTemporalExpandido) {
        this.huboFallbackTemporalExpandido = huboFallbackTemporalExpandido;
    }
}
