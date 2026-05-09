package com.chat.chat.Service.AiService;

public class AiMessageSearchScopeDTO {

    private AiMessageSearchScopeType tipoScope;
    private Long chatId;
    private Long chatGrupalId;
    private Long usuarioRelacionadoId;
    private String nombreDetectado;
    private String nombreNormalizadoDetectado;
    private String nombreScopeAplicado;
    private String motivo;
    private boolean scopeResuelto;
    private Integer confidence;
    private boolean intencionGrupoDetectada;
    private boolean intencionIndividualDetectada;

    public AiMessageSearchScopeType getTipoScope() {
        return tipoScope;
    }

    public void setTipoScope(AiMessageSearchScopeType tipoScope) {
        this.tipoScope = tipoScope;
    }

    public Long getChatId() {
        return chatId;
    }

    public void setChatId(Long chatId) {
        this.chatId = chatId;
    }

    public Long getChatGrupalId() {
        return chatGrupalId;
    }

    public void setChatGrupalId(Long chatGrupalId) {
        this.chatGrupalId = chatGrupalId;
    }

    public Long getUsuarioRelacionadoId() {
        return usuarioRelacionadoId;
    }

    public void setUsuarioRelacionadoId(Long usuarioRelacionadoId) {
        this.usuarioRelacionadoId = usuarioRelacionadoId;
    }

    public String getNombreDetectado() {
        return nombreDetectado;
    }

    public void setNombreDetectado(String nombreDetectado) {
        this.nombreDetectado = nombreDetectado;
    }

    public String getNombreNormalizadoDetectado() {
        return nombreNormalizadoDetectado;
    }

    public void setNombreNormalizadoDetectado(String nombreNormalizadoDetectado) {
        this.nombreNormalizadoDetectado = nombreNormalizadoDetectado;
    }

    public String getNombreScopeAplicado() {
        return nombreScopeAplicado;
    }

    public void setNombreScopeAplicado(String nombreScopeAplicado) {
        this.nombreScopeAplicado = nombreScopeAplicado;
    }

    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo;
    }

    public boolean isScopeResuelto() {
        return scopeResuelto;
    }

    public void setScopeResuelto(boolean scopeResuelto) {
        this.scopeResuelto = scopeResuelto;
    }

    public Integer getConfidence() {
        return confidence;
    }

    public void setConfidence(Integer confidence) {
        this.confidence = confidence;
    }

    public boolean isIntencionGrupoDetectada() {
        return intencionGrupoDetectada;
    }

    public void setIntencionGrupoDetectada(boolean intencionGrupoDetectada) {
        this.intencionGrupoDetectada = intencionGrupoDetectada;
    }

    public boolean isIntencionIndividualDetectada() {
        return intencionIndividualDetectada;
    }

    public void setIntencionIndividualDetectada(boolean intencionIndividualDetectada) {
        this.intencionIndividualDetectada = intencionIndividualDetectada;
    }
}
