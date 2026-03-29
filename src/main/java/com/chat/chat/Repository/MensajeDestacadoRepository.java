package com.chat.chat.Repository;

import com.chat.chat.Entity.MensajeDestacadoEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface MensajeDestacadoRepository extends JpaRepository<MensajeDestacadoEntity, Long> {

    boolean existsByUsuarioIdAndMensajeId(Long usuarioId, Long mensajeId);

    long deleteByUsuarioIdAndMensajeId(Long usuarioId, Long mensajeId);

    @Query(
            value = "select md from MensajeDestacadoEntity md " +
            "join fetch md.mensaje m " +
            "join fetch m.chat c " +
            "left join fetch m.emisor e " +
            "where md.usuario.id = :usuarioId " +
            "order by coalesce(m.fechaEnvio, md.createdAt) desc, md.createdAt desc, md.id desc",
            countQuery = "select count(md) from MensajeDestacadoEntity md where md.usuario.id = :usuarioId"
    )
    Page<MensajeDestacadoEntity> findDestacadosPageByUsuarioIdOrderByFechaMensajeDesc(
            @Param("usuarioId") Long usuarioId,
            Pageable pageable
    );
}
