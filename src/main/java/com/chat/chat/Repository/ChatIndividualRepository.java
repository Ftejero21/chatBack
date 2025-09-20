package com.chat.chat.Repository;

import com.chat.chat.Entity.ChatIndividualEntity;
import com.chat.chat.Entity.UsuarioEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ChatIndividualRepository extends JpaRepository<ChatIndividualEntity, Long> {

    Optional<ChatIndividualEntity> findByUsuario1AndUsuario2(UsuarioEntity u1, UsuarioEntity u2);

}
