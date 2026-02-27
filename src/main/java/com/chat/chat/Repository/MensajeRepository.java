package com.chat.chat.Repository;

import com.chat.chat.Entity.MensajeEntity;
import com.chat.chat.Utils.MessageType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface MensajeRepository extends JpaRepository<MensajeEntity, Long> {

    @Query("select m.chat.id, count(m) " +
            "from MensajeEntity m " +
            "where m.receptor.id = :uid and m.leido = false and m.activo = true " +
            "group by m.chat.id")
    List<Object[]> countUnreadByUser(@Param("uid") Long uid);

    MensajeEntity findTopByChatIdOrderByFechaEnvioDesc(Long chatId);

    long countByChatIdAndActivoTrue(Long chatId);

    Optional<MensajeEntity> findTopByChatIdAndActivoTrueOrderByFechaEnvioDesc(Long chatId);

    List<MensajeEntity> findByChatIdOrderByFechaEnvioAsc(Long chatId);

    @Query("select m.chat.id, count(m) from MensajeEntity m where m.chat.id in :chatIds and m.activo = true group by m.chat.id")
    List<Object[]> countActivosByChatIds(@Param("chatIds") List<Long> chatIds);

    @Query("select m from MensajeEntity m " +
            "where m.chat.id in :chatIds " +
            "and m.fechaEnvio = (select max(m2.fechaEnvio) from MensajeEntity m2 where m2.chat.id = m.chat.id) " +
            "and m.id = (select max(m3.id) from MensajeEntity m3 where m3.chat.id = m.chat.id and m3.fechaEnvio = m.fechaEnvio)")
    List<MensajeEntity> findLatestByChatIds(@Param("chatIds") List<Long> chatIds);

    Page<MensajeEntity> findByChatId(Long chatId, Pageable pageable);

    @Modifying
    @Query("update MensajeEntity m set m.leido = true where m.id in :ids")
    int markLeidoByIds(@Param("ids") List<Long> ids);

    @Query("select m.emisor.id from MensajeEntity m where m.id = :id")
    Optional<Long> findEmisorIdById(@Param("id") Long id);

    @Modifying
    @Query("update MensajeEntity m set m.activo = false where m.id = :id")
    int markInactivoById(@Param("id") Long id);

    @Query("SELECT COUNT(m) FROM MensajeEntity m WHERE m.fechaEnvio >= :inicio AND m.fechaEnvio < :fin")
    long countMensajesEntreFechas(@Param("inicio") java.time.LocalDateTime inicio,
                                  @Param("fin") java.time.LocalDateTime fin);

    Optional<MensajeEntity> findByIdAndChatId(Long id, Long chatId);

    @Query("select m from MensajeEntity m " +
            "left join fetch m.emisor e " +
            "where m.chat.id = :chatId " +
            "and m.activo = true " +
            "and m.tipo in :types " +
            "and (:cursorFecha is null or m.fechaEnvio < :cursorFecha or (m.fechaEnvio = :cursorFecha and m.id < :cursorId)) " +
            "order by m.fechaEnvio desc, m.id desc")
    List<MensajeEntity> findGroupMediaPage(
            @Param("chatId") Long chatId,
            @Param("types") List<MessageType> types,
            @Param("cursorFecha") LocalDateTime cursorFecha,
            @Param("cursorId") Long cursorId,
            Pageable pageable);

    @Query("select m from MensajeEntity m " +
            "left join fetch m.emisor " +
            "where m.chat.id = :chatId " +
            "and m.activo = true " +
            "and m.tipo = :tipo " +
            "order by m.fechaEnvio desc, m.id desc")
    List<MensajeEntity> findTextActivosByChatIdOrderByFechaEnvioDescIdDesc(
            @Param("chatId") Long chatId,
            @Param("tipo") MessageType tipo);
}
