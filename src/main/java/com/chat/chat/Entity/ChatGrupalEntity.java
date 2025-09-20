package com.chat.chat.Entity;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "chats_grupales")
public class ChatGrupalEntity extends ChatEntity {

    private String nombreGrupo;

    @ManyToMany
    @JoinTable(
            name = "chat_grupal_usuarios",
            joinColumns = @JoinColumn(name = "chat_id"),
            inverseJoinColumns = @JoinColumn(name = "usuario_id")
    )
    private List<UsuarioEntity> usuarios = new ArrayList<>();

    @Column(name = "foto_url", length = 512)
    private String fotoUrl;

    public String getNombreGrupo() {
        return nombreGrupo;
    }

    public String getFotoUrl() {
        return fotoUrl;
    }

    public void setFotoUrl(String fotoUrl) {
        this.fotoUrl = fotoUrl;
    }

    public void setNombreGrupo(String nombreGrupo) {
        this.nombreGrupo = nombreGrupo;
    }

    public List<UsuarioEntity> getUsuarios() {
        return usuarios;
    }

    public void setUsuarios(List<UsuarioEntity> usuarios) {
        this.usuarios = usuarios;
    }
}
