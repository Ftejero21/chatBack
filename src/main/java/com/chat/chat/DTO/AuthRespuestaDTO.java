package com.chat.chat.DTO;

public class AuthRespuestaDTO {
    private String token;
    private UsuarioDTO usuario;
    private String auditPublicKey;
    private String auditPrivateKey;
    private String privateKey_admin_audit;
    private String forAdminPrivateKey;

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

    public String getAuditPrivateKey() {
        return auditPrivateKey;
    }

    public void setAuditPrivateKey(String auditPrivateKey) {
        this.auditPrivateKey = auditPrivateKey;
    }

    public String getPrivateKey_admin_audit() {
        return privateKey_admin_audit;
    }

    public void setPrivateKey_admin_audit(String privateKey_admin_audit) {
        this.privateKey_admin_audit = privateKey_admin_audit;
    }

    public String getForAdminPrivateKey() {
        return forAdminPrivateKey;
    }

    public void setForAdminPrivateKey(String forAdminPrivateKey) {
        this.forAdminPrivateKey = forAdminPrivateKey;
    }
}
