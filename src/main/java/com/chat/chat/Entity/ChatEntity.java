package com.chat.chat.Entity;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Inheritance(strategy = InheritanceType.JOINED)
@Table(name = "chats")
public abstract class ChatEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToMany(mappedBy = "chat", cascade = CascadeType.ALL)
    private List<MensajeEntity> mensajes = new ArrayList<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public List<MensajeEntity> getMensajes() {
        return mensajes;
    }

    public void setMensajes(List<MensajeEntity> mensajes) {
        this.mensajes = mensajes;
    }
}
