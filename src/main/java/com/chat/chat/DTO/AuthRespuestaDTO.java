package com.chat.chat.DTO;

public class AuthRespuestaDTO {
    private String token;
    private UsuarioDTO usuario;
    private String auditPublicKey;
    private Boolean profileCompletionRequired;

    public AuthRespuestaDTO() {
    }

    public AuthRespuestaDTO(String token, UsuarioDTO usuario) {
        this.token = token;
        this.usuario = usuario;
    }

    public AuthRespuestaDTO(String token, UsuarioDTO usuario, String auditPublicKey) {
        this.token = token;
        this.usuario = usuario;
        this.auditPublicKey = auditPublicKey;
    }

    public AuthRespuestaDTO(String token, UsuarioDTO usuario, String auditPublicKey, Boolean profileCompletionRequired) {
        this.token = token;
        this.usuario = usuario;
        this.auditPublicKey = auditPublicKey;
        this.profileCompletionRequired = profileCompletionRequired;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public UsuarioDTO getUsuario() {
        return usuario;
    }

    public void setUsuario(UsuarioDTO usuario) {
        this.usuario = usuario;
    }

    public String getAuditPublicKey() {
        return auditPublicKey;
    }

    public void setAuditPublicKey(String auditPublicKey) {
        this.auditPublicKey = auditPublicKey;
    }

    public Boolean getProfileCompletionRequired() {
        return profileCompletionRequired;
    }

    public void setProfileCompletionRequired(Boolean profileCompletionRequired) {
        this.profileCompletionRequired = profileCompletionRequired;
    }
}
