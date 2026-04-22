package com.chat.chat.Repository;

import com.chat.chat.Entity.ChatGrupalEntity;
import com.chat.chat.DTO.AdminGroupListDTO;
import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ChatGrupalRepository extends JpaRepository<ChatGrupalEntity, Long> {
    List<ChatGrupalEntity> findAllByUsuariosId(Long usuarioId);

    @Query("select distinct c from ChatGrupalEntity c left join fetch c.usuarios where c.id = :id")
    Optional<ChatGrupalEntity> findByIdWithUsuarios(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from ChatGrupalEntity c where c.id = :id")
    Optional<ChatGrupalEntity> findByIdWithUsuariosForUpdate(@Param("id") Long id);

    @Query("""
            select distinct member.id
            from ChatGrupalEntity c
            join c.usuarios me
            join c.usuarios member
            where me.id = :userId
              and c.activo = true
              and member.activo = true
              and member.id <> :userId
            """)
    List<Long> findVisibleMemberIdsByUserId(@Param("userId") Long userId);

    @Query(value = """
            select new com.chat.chat.DTO.AdminGroupListDTO(
                c.id,
                c.nombreGrupo,
                c.descripcion,
                c.fotoUrl,
                c.visibilidad,
                c.activo,
                c.fechaCreacion,
                c.creador.id,
                count(distinct member.id),
                c.closed,
                c.closedReason,
                c.closedAt,
                c.closedByAdminId
            )
            from ChatGrupalEntity c
            left join c.usuarios member
            group by c.id, c.nombreGrupo, c.descripcion, c.visibilidad, c.activo, c.fechaCreacion, c.creador.id,
                     c.closed, c.closedReason, c.closedAt, c.closedByAdminId
            """,
            countQuery = """
                    select count(c.id)
                    from ChatGrupalEntity c
                    """)
    Page<AdminGroupListDTO> findAdminGroupPage(Pageable pageable);

    @Query("""
            select case when count(c) > 0 then true else false end
            from ChatGrupalEntity c
            join c.usuarios u
            where c.id = :chatId
              and c.activo = true
              and u.id = :userId
              and u.activo = true
            """)
    boolean existsActiveMemberByChatIdAndUserId(@Param("chatId") Long chatId, @Param("userId") Long userId);
}
