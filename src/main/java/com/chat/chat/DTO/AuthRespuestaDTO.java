package com.chat.chat.DTO;

public class AuthRespuestaDTO {
    private String token;
    private UsuarioDTO usuario;

    public AuthRespuestaDTO() {
    }

    public AuthRespuestaDTO(String token, UsuarioDTO usuario) {
        this.token = token;
        this.usuario = usuario;
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
}
