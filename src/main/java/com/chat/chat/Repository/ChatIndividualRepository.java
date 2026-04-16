package com.chat.chat.Repository;

import com.chat.chat.Entity.ChatIndividualEntity;
import com.chat.chat.Entity.UsuarioEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatIndividualRepository extends JpaRepository<ChatIndividualEntity, Long> {
    List<ChatIndividualEntity> findAllByUsuario1IdOrUsuario2Id(Long usuario1Id, Long usuario2Id);
    Optional<ChatIndividualEntity> findByUsuario1AndUsuario2AndAdminDirectFalse(UsuarioEntity u1, UsuarioEntity u2);

    @Query("""
            select ci from ChatIndividualEntity ci
            where ci.adminDirect = false
              and ((ci.usuario1 = :u1 and ci.usuario2 = :u2) or (ci.usuario1 = :u2 and ci.usuario2 = :u1))
            """)
    Optional<ChatIndividualEntity> findRegularChatBetween(@Param("u1") UsuarioEntity u1,
                                                          @Param("u2") UsuarioEntity u2);

    @Query("""
            select ci from ChatIndividualEntity ci
            where ci.adminDirect = true
              and ((ci.usuario1.id = :userAId and ci.usuario2.id = :userBId)
                or (ci.usuario1.id = :userBId and ci.usuario2.id = :userAId))
            """)
    Optional<ChatIndividualEntity> findAdminDirectChatBetween(@Param("userAId") Long userAId,
                                                              @Param("userBId") Long userBId);

    @Query("""
            select distinct
            case when ci.usuario1.id = :userId then ci.usuario2.id else ci.usuario1.id end
            from ChatIndividualEntity ci
            where (ci.usuario1.id = :userId or ci.usuario2.id = :userId)
              and (
                    (ci.usuario1.id = :userId and ci.usuario2.activo = true)
                 or (ci.usuario2.id = :userId and ci.usuario1.activo = true)
              )
            """)
    List<Long> findVisibleContactIdsByUserId(@Param("userId") Long userId);
}
