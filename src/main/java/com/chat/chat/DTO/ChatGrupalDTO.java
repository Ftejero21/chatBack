package com.chat.chat.DTO;

import java.time.LocalDateTime;
import java.util.List;

public class ChatGrupalDTO {
    private Long id;
    private String nombreGrupo;
    private List<UsuarioDTO> usuarios;

    private Long idCreador;

    private String fotoGrupo;
    private String ultimaMensaje;           // 👈 NEW
    private LocalDateTime ultimaFecha;

    public Long getId() {
        return id;
    }

    public String getUltimaMensaje() {
        return ultimaMensaje;
    }

    public void setUltimaMensaje(String ultimaMensaje) {
        this.ultimaMensaje = ultimaMensaje;
    }

    public String getFotoGrupo() {
        return fotoGrupo;
    }

    public void setFotoGrupo(String fotoGrupo) {
        this.fotoGrupo = fotoGrupo;
    }

    public Long getIdCreador() {
        return idCreador;
    }

    public void setIdCreador(Long idCreador) {
        this.idCreador = idCreador;
    }

    public LocalDateTime getUltimaFecha() {
        return ultimaFecha;
    }

    public void setUltimaFecha(LocalDateTime ultimaFecha) {
        this.ultimaFecha = ultimaFecha;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNombreGrupo() {
        return nombreGrupo;
    }

    public void setNombreGrupo(String nombreGrupo) {
        this.nombreGrupo = nombreGrupo;
    }

    public List<UsuarioDTO> getUsuarios() {
        return usuarios;
    }

    public void setUsuarios(List<UsuarioDTO> usuarios) {
        this.usuarios = usuarios;
    }
}
