package com.chat.chat.Entity;

import jakarta.persistence.*;

@Entity
@Table(name = "chats_individuales")
public class ChatIndividualEntity extends ChatEntity {

    @ManyToOne
    @JoinColumn(name = "usuario1_id")
    private UsuarioEntity usuario1;

    @ManyToOne
    @JoinColumn(name = "usuario2_id")
    private UsuarioEntity usuario2;

    @Column(name = "admin_direct", nullable = false)
    private boolean adminDirect = false;

    public UsuarioEntity getUsuario1() {
        return usuario1;
    }

    public void setUsuario1(UsuarioEntity usuario1) {
        this.usuario1 = usuario1;
    }

    public UsuarioEntity getUsuario2() {
        return usuario2;
    }

    public void setUsuario2(UsuarioEntity usuario2) {
        this.usuario2 = usuario2;
    }

    public boolean isAdminDirect() {
        return adminDirect;
    }

    public void setAdminDirect(boolean adminDirect) {
        this.adminDirect = adminDirect;
    }
}
