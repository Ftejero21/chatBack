package com.chat.chat.Service.AiService;

import java.time.LocalDateTime;

public class AiMessageSearchNaturalQueryAnalysis {

    private String consultaNormalizada;
    private boolean intencionGrupo;
    private boolean intencionIndividual;
    private boolean intencionAudio;
    private boolean intencionImagen;
    private boolean intencionSticker;
    private boolean intencionArchivo;
    private boolean intencionUltimoMensaje;
    private boolean intencionPrimerMensaje;
    private boolean intencionLocalizacion;
    private boolean intencionDenuncia;
    private boolean intencionContenidoOfensivo;
    private boolean intencionMensajesNoLeidos;
    private boolean intencionDenunciaRecibida;
    private boolean intencionDenunciaCreada;
    private String personaDenunciadaSolicitada;
    private String motivoDenunciaDetectado;
    private LocalDateTime fechaInicioExpandida;
    private LocalDateTime fechaFinExpandida;
    private String descripcionRangoExpandido;
    private AiMessageSearchSenderScope senderScope;
    private String nombreGrupoDetectado;
    private String nombrePersonaDetectado;
    private String personaObjetivoDetectada;
    private String emisorObjetivoDetectado;
    private LocalDateTime fechaInicioDetectada;
    private LocalDateTime fechaFinDetectada;
    private boolean rangoTemporalDetectado;
    private String descripcionRangoTemporal;
    private Integer confidenceTemporal;

    public String getConsultaNormalizada() { return consultaNormalizada; }
    public void setConsultaNormalizada(String consultaNormalizada) { this.consultaNormalizada = consultaNormalizada; }
    public boolean isIntencionGrupo() { return intencionGrupo; }
    public void setIntencionGrupo(boolean intencionGrupo) { this.intencionGrupo = intencionGrupo; }
    public boolean isIntencionIndividual() { return intencionIndividual; }
    public void setIntencionIndividual(boolean intencionIndividual) { this.intencionIndividual = intencionIndividual; }
    public boolean isIntencionAudio() { return intencionAudio; }
    public void setIntencionAudio(boolean intencionAudio) { this.intencionAudio = intencionAudio; }
    public boolean isIntencionImagen() { return intencionImagen; }
    public void setIntencionImagen(boolean intencionImagen) { this.intencionImagen = intencionImagen; }
    public boolean isIntencionSticker() { return intencionSticker; }
    public void setIntencionSticker(boolean intencionSticker) { this.intencionSticker = intencionSticker; }
    public boolean isIntencionArchivo() { return intencionArchivo; }
    public void setIntencionArchivo(boolean intencionArchivo) { this.intencionArchivo = intencionArchivo; }
    public boolean isIntencionUltimoMensaje() { return intencionUltimoMensaje; }
    public void setIntencionUltimoMensaje(boolean intencionUltimoMensaje) { this.intencionUltimoMensaje = intencionUltimoMensaje; }
    public boolean isIntencionPrimerMensaje() { return intencionPrimerMensaje; }
    public void setIntencionPrimerMensaje(boolean intencionPrimerMensaje) { this.intencionPrimerMensaje = intencionPrimerMensaje; }
    public boolean isIntencionLocalizacion() { return intencionLocalizacion; }
    public void setIntencionLocalizacion(boolean intencionLocalizacion) { this.intencionLocalizacion = intencionLocalizacion; }
    public boolean isIntencionDenuncia() { return intencionDenunciaCreada || intencionDenunciaRecibida; }
    public void setIntencionDenuncia(boolean intencionDenuncia) { /* legacy — no-op */ }

    public boolean isIntencionContenidoOfensivo() { return intencionContenidoOfensivo; }
    public void setIntencionContenidoOfensivo(boolean intencionContenidoOfensivo) { this.intencionContenidoOfensivo = intencionContenidoOfensivo; }

    public boolean isIntencionMensajesNoLeidos() { return intencionMensajesNoLeidos; }
    public void setIntencionMensajesNoLeidos(boolean intencionMensajesNoLeidos) { this.intencionMensajesNoLeidos = intencionMensajesNoLeidos; }

    public boolean isIntencionDenunciaRecibida() { return intencionDenunciaRecibida; }
    public void setIntencionDenunciaRecibida(boolean intencionDenunciaRecibida) { this.intencionDenunciaRecibida = intencionDenunciaRecibida; }

    public boolean isIntencionDenunciaCreada() { return intencionDenunciaCreada; }
    public void setIntencionDenunciaCreada(boolean intencionDenunciaCreada) { this.intencionDenunciaCreada = intencionDenunciaCreada; }

    public String getPersonaDenunciadaSolicitada() { return personaDenunciadaSolicitada; }
    public void setPersonaDenunciadaSolicitada(String personaDenunciadaSolicitada) { this.personaDenunciadaSolicitada = personaDenunciadaSolicitada; }

    public String getMotivoDenunciaDetectado() { return motivoDenunciaDetectado; }
    public void setMotivoDenunciaDetectado(String motivoDenunciaDetectado) { this.motivoDenunciaDetectado = motivoDenunciaDetectado; }

    public LocalDateTime getFechaInicioExpandida() { return fechaInicioExpandida; }
    public void setFechaInicioExpandida(LocalDateTime fechaInicioExpandida) { this.fechaInicioExpandida = fechaInicioExpandida; }

    public LocalDateTime getFechaFinExpandida() { return fechaFinExpandida; }
    public void setFechaFinExpandida(LocalDateTime fechaFinExpandida) { this.fechaFinExpandida = fechaFinExpandida; }

    public String getDescripcionRangoExpandido() { return descripcionRangoExpandido; }
    public void setDescripcionRangoExpandido(String descripcionRangoExpandido) { this.descripcionRangoExpandido = descripcionRangoExpandido; }
    public AiMessageSearchSenderScope getSenderScope() { return senderScope; }
    public void setSenderScope(AiMessageSearchSenderScope senderScope) { this.senderScope = senderScope; }
    public String getNombreGrupoDetectado() { return nombreGrupoDetectado; }
    public void setNombreGrupoDetectado(String nombreGrupoDetectado) { this.nombreGrupoDetectado = nombreGrupoDetectado; }
    public String getNombrePersonaDetectado() { return nombrePersonaDetectado; }
    public void setNombrePersonaDetectado(String nombrePersonaDetectado) { this.nombrePersonaDetectado = nombrePersonaDetectado; }
    public String getPersonaObjetivoDetectada() { return personaObjetivoDetectada; }
    public void setPersonaObjetivoDetectada(String personaObjetivoDetectada) { this.personaObjetivoDetectada = personaObjetivoDetectada; }
    public String getEmisorObjetivoDetectado() { return emisorObjetivoDetectado; }
    public void setEmisorObjetivoDetectado(String emisorObjetivoDetectado) { this.emisorObjetivoDetectado = emisorObjetivoDetectado; }
    public LocalDateTime getFechaInicioDetectada() { return fechaInicioDetectada; }
    public void setFechaInicioDetectada(LocalDateTime fechaInicioDetectada) { this.fechaInicioDetectada = fechaInicioDetectada; }
    public LocalDateTime getFechaFinDetectada() { return fechaFinDetectada; }
    public void setFechaFinDetectada(LocalDateTime fechaFinDetectada) { this.fechaFinDetectada = fechaFinDetectada; }
    public boolean isRangoTemporalDetectado() { return rangoTemporalDetectado; }
    public void setRangoTemporalDetectado(boolean rangoTemporalDetectado) { this.rangoTemporalDetectado = rangoTemporalDetectado; }
    public String getDescripcionRangoTemporal() { return descripcionRangoTemporal; }
    public void setDescripcionRangoTemporal(String descripcionRangoTemporal) { this.descripcionRangoTemporal = descripcionRangoTemporal; }
    public Integer getConfidenceTemporal() { return confidenceTemporal; }
    public void setConfidenceTemporal(Integer confidenceTemporal) { this.confidenceTemporal = confidenceTemporal; }
}
