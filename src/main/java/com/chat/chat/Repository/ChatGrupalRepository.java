package com.chat.chat.Repository;

import com.chat.chat.Entity.ChatGrupalEntity;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ChatGrupalRepository extends JpaRepository<ChatGrupalEntity, Long> {
    List<ChatGrupalEntity> findAllByUsuariosId(Long usuarioId);
}
