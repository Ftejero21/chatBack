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
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.time.LocalDateTime;
import java.util.Optional;
import com.chat.chat.Entity.ChatGrupalEntity;
import com.chat.chat.Entity.ChatIndividualEntity;

@Repository
public interface MensajeRepository extends JpaRepository<MensajeEntity, Long> {

    @Query("select m.chat.id, count(m) " +
            "from MensajeEntity m " +
            "left join ChatUserStateEntity s on s.chat.id = m.chat.id and s.user.id = :uid " +
            "where m.receptor.id = :uid and m.leido = false and m.activo = true " +
            "and (m.expiraEn is null or m.expiraEn > CURRENT_TIMESTAMP) " +
            "and (s.clearedBeforeMessageId is null or m.id > s.clearedBeforeMessageId) " +
            "group by m.chat.id")
    List<Object[]> countUnreadByUser(@Param("uid") Long uid);

    @Query(value = "select * from mensajes m " +
            "where m.chat_id = :chatId " +
            "order by m.fecha_envio desc, m.id desc " +
            "limit 1", nativeQuery = true)
    MensajeEntity findTopByChatIdOrderByFechaEnvioDesc(@Param("chatId") Long chatId);

    @Query(value = "select * from mensajes m " +
            "where m.chat_id = :chatId " +
            "and m.activo = true " +
            "and (m.expira_en is null or m.expira_en > CURRENT_TIMESTAMP) " +
            "and (:cutoff is null or m.id > :cutoff) " +
            "order by m.fecha_envio desc, m.id desc " +
            "limit 1", nativeQuery = true)
    Optional<MensajeEntity> findTopVisibleByChatIdOrderByFechaEnvioDesc(@Param("chatId") Long chatId,
                                                                         @Param("cutoff") Long cutoff);

    @Query("select count(m) from MensajeEntity m " +
            "where m.chat.id = :chatId and m.activo = true " +
            "and (m.expiraEn is null or m.expiraEn > CURRENT_TIMESTAMP)")
    long countByChatIdAndActivoTrue(@Param("chatId") Long chatId);

    @Query(value = "select * from mensajes m " +
            "where m.chat_id = :chatId and m.activo = true " +
            "order by m.fecha_envio desc, m.id desc " +
            "limit 1", nativeQuery = true)
    Optional<MensajeEntity> findTopByChatIdAndActivoTrueOrderByFechaEnvioDesc(@Param("chatId") Long chatId);

    @Query("select m from MensajeEntity m " +
            "where m.chat.id = :chatId " +
            "and (m.expiraEn is null or m.expiraEn > CURRENT_TIMESTAMP) " +
            "order by m.fechaEnvio asc, m.id asc")
    List<MensajeEntity> findByChatIdOrderByFechaEnvioAsc(@Param("chatId") Long chatId);

    @Query("select m from MensajeEntity m " +
            "where m.chat.id = :chatId " +
            "order by m.fechaEnvio asc, m.id asc")
    List<MensajeEntity> findByChatIdOrderByFechaEnvioAscIncluyendoExpirados(@Param("chatId") Long chatId);

    @Query("select m.chat.id, count(m) from MensajeEntity m " +
            "where m.chat.id in :chatIds and m.activo = true " +
            "and (m.expiraEn is null or m.expiraEn > CURRENT_TIMESTAMP) " +
            "group by m.chat.id")
    List<Object[]> countActivosByChatIds(@Param("chatIds") List<Long> chatIds);

    @Query("select m.chat.id, count(m) from MensajeEntity m " +
            "where m.chat.id in :chatIds and m.activo = true " +
            "group by m.chat.id")
    List<Object[]> countActivosByChatIdsIncluyendoExpirados(@Param("chatIds") List<Long> chatIds);

    @Query("select m from MensajeEntity m " +
            "where m.chat.id in :chatIds " +
            "and (m.expiraEn is null or m.expiraEn > CURRENT_TIMESTAMP) " +
            "and m.fechaEnvio = (select max(m2.fechaEnvio) from MensajeEntity m2 where m2.chat.id = m.chat.id and (m2.expiraEn is null or m2.expiraEn > CURRENT_TIMESTAMP)) " +
            "and m.id = (select max(m3.id) from MensajeEntity m3 where m3.chat.id = m.chat.id and m3.fechaEnvio = m.fechaEnvio and (m3.expiraEn is null or m3.expiraEn > CURRENT_TIMESTAMP))")
    List<MensajeEntity> findLatestByChatIds(@Param("chatIds") List<Long> chatIds);

    @Query("select m from MensajeEntity m " +
            "where m.chat.id in :chatIds " +
            "and m.fechaEnvio = (select max(m2.fechaEnvio) from MensajeEntity m2 where m2.chat.id = m.chat.id) " +
            "and m.id = (select max(m3.id) from MensajeEntity m3 where m3.chat.id = m.chat.id and m3.fechaEnvio = m.fechaEnvio)")
    List<MensajeEntity> findLatestByChatIdsIncluyendoExpirados(@Param("chatIds") List<Long> chatIds);

    @Query("select m from MensajeEntity m " +
            "where m.chat.id = :chatId")
    Page<MensajeEntity> findByChatId(@Param("chatId") Long chatId, Pageable pageable);

    @Query("select m from MensajeEntity m " +
            "where m.chat.id = :chatId " +
            "and m.activo = true " +
            "and (m.expiraEn is null or m.expiraEn > CURRENT_TIMESTAMP) " +
            "and (:cutoff is null or m.id > :cutoff)")
    Page<MensajeEntity> findByChatIdVisibleAfter(@Param("chatId") Long chatId,
                                                  @Param("cutoff") Long cutoff,
                                                  Pageable pageable);

    @Query("select max(m.id) from MensajeEntity m where m.chat.id = :chatId")
    Optional<Long> findMaxIdByChatId(@Param("chatId") Long chatId);

    @Modifying
    @Transactional
    @Query("update MensajeEntity m set m.leido = true " +
            "where m.id in :ids " +
            "and (m.expiraEn is null or m.expiraEn > CURRENT_TIMESTAMP)")
    int markLeidoByIds(@Param("ids") List<Long> ids);

    @Query("select m.emisor.id from MensajeEntity m where m.id = :id")
    Optional<Long> findEmisorIdById(@Param("id") Long id);

    @Query("select m.chat.id from MensajeEntity m where m.id = :id")
    Optional<Long> findChatIdByMessageId(@Param("id") Long id);

    @Modifying
    @Transactional
    @Query("update MensajeEntity m set m.activo = false, m.fechaEliminacion = CURRENT_TIMESTAMP " +
            "where m.id = :id " +
            "and m.activo = true " +
            "and (m.expiraEn is null or m.expiraEn > CURRENT_TIMESTAMP)")
    int markInactivoById(@Param("id") Long id);

    @Query("SELECT COUNT(m) FROM MensajeEntity m " +
            "WHERE m.fechaEnvio >= :inicio AND m.fechaEnvio < :fin " +
            "AND (m.expiraEn is null or m.expiraEn > CURRENT_TIMESTAMP)")
    long countMensajesEntreFechas(@Param("inicio") java.time.LocalDateTime inicio,
                                  @Param("fin") java.time.LocalDateTime fin);

    @Query("SELECT COUNT(m) FROM MensajeEntity m " +
            "WHERE m.fechaEnvio >= :inicio AND m.fechaEnvio < :fin " +
            "AND m.activo = true " +
            "AND m.adminMessage = false " +
            "AND (m.expiraEn is null or m.expiraEn > CURRENT_TIMESTAMP)")
    long countUserMensajesEntreFechas(@Param("inicio") java.time.LocalDateTime inicio,
                                      @Param("fin") java.time.LocalDateTime fin);

    @Query("select m from MensajeEntity m " +
            "where m.id = :id and m.chat.id = :chatId")
    Optional<MensajeEntity> findByIdAndChatId(@Param("id") Long id, @Param("chatId") Long chatId);

    @Query("select m from MensajeEntity m " +
            "left join fetch m.emisor emisor " +
            "left join fetch m.receptor receptor " +
            "left join fetch m.chat chat " +
            "where m.id = :id")
    Optional<MensajeEntity> findByIdWithChatAndUsers(@Param("id") Long id);

    @Query("select m from MensajeEntity m " +
            "where m.id = :messageId and m.mediaUrl = :mediaUrl")
    Optional<MensajeEntity> findByIdAndMediaUrl(@Param("messageId") Long messageId,
                                                @Param("mediaUrl") String mediaUrl);

    @Query("select m from MensajeEntity m " +
            "where m.chat.id = :chatId and m.mediaUrl = :mediaUrl " +
            "order by m.id desc")
    List<MensajeEntity> findByChatIdAndMediaUrlOrderByIdDesc(@Param("chatId") Long chatId,
                                                              @Param("mediaUrl") String mediaUrl,
                                                              Pageable pageable);

    @Query("select m from MensajeEntity m " +
            "left join fetch m.emisor e " +
            "where m.chat.id = :chatId " +
            "and m.activo = true " +
            "and (m.expiraEn is null or m.expiraEn > CURRENT_TIMESTAMP) " +
            "and m.tipo in :types " +
            "and (:cutoff is null or m.id > :cutoff) " +
            "and (:cursorFecha is null or m.fechaEnvio < :cursorFecha or (m.fechaEnvio = :cursorFecha and m.id < :cursorId)) " +
            "order by m.fechaEnvio desc, m.id desc")
    List<MensajeEntity> findGroupMediaPage(
            @Param("chatId") Long chatId,
            @Param("types") List<MessageType> types,
            @Param("cutoff") Long cutoff,
            @Param("cursorFecha") LocalDateTime cursorFecha,
            @Param("cursorId") Long cursorId,
            Pageable pageable);

    @Query("select m from MensajeEntity m " +
            "left join fetch m.emisor " +
            "where m.chat.id = :chatId " +
            "and m.activo = true " +
            "and (m.expiraEn is null or m.expiraEn > CURRENT_TIMESTAMP) " +
            "and m.tipo = :tipo " +
            "and (:cutoff is null or m.id > :cutoff) " +
            "order by m.fechaEnvio desc, m.id desc")
    List<MensajeEntity> findTextActivosByChatIdOrderByFechaEnvioDescIdDesc(
            @Param("chatId") Long chatId,
            @Param("tipo") MessageType tipo,
            @Param("cutoff") Long cutoff);

    @Query(value = "select m from MensajeEntity m " +
            "where m.chat.id = :chatId " +
            "and m.activo = true " +
            "and (m.expiraEn is null or m.expiraEn > CURRENT_TIMESTAMP) " +
            "and m.contenidoBusqueda is not null " +
            "and m.contenidoBusqueda like concat('%', :query, '%') " +
            "and (:cutoff is null or m.id > :cutoff)",
            countQuery = "select count(m) from MensajeEntity m " +
                    "where m.chat.id = :chatId " +
                    "and m.activo = true " +
                    "and (m.expiraEn is null or m.expiraEn > CURRENT_TIMESTAMP) " +
                    "and m.contenidoBusqueda is not null " +
                    "and m.contenidoBusqueda like concat('%', :query, '%') " +
                    "and (:cutoff is null or m.id > :cutoff)")
    Page<MensajeEntity> searchByContenidoBusqueda(
            @Param("chatId") Long chatId,
            @Param("query") String query,
            @Param("cutoff") Long cutoff,
            Pageable pageable);

    @Query("select m from MensajeEntity m " +
            "where m.mensajeTemporal = true " +
            "and m.expiraEn is not null " +
            "and m.expiraEn <= :ahora " +
            "order by m.expiraEn asc, m.id asc")
    List<MensajeEntity> findMensajesTemporalesExpirados(@Param("ahora") LocalDateTime ahora, Pageable pageable);

    @Query("select m from MensajeEntity m " +
            "where m.mensajeTemporal = true " +
            "and m.expiraEn is not null " +
            "and m.expiraEn <= :ahora " +
            "and (m.motivoEliminacion is null or m.motivoEliminacion <> :motivo) " +
            "order by m.expiraEn asc, m.id asc")
    List<MensajeEntity> findMensajesTemporalesPendientesExpirar(
            @Param("ahora") LocalDateTime ahora,
            @Param("motivo") String motivo,
            Pageable pageable);

    @Query("select m from MensajeEntity m " +
            "where m.mensajeTemporal = true " +
            "and m.adminMessage = true " +
            "and m.chat.id = :chatId " +
            "and m.receptor.id = :userId " +
            "and m.expiraEn is not null " +
            "and m.expiraEn <= :ahora " +
            "and (m.motivoEliminacion is null or m.motivoEliminacion <> :motivo) " +
            "order by m.expiraEn asc, m.id asc")
    List<MensajeEntity> findAdminDirectMensajesTemporalesPendientesExpirarByChatIdAndUserId(
            @Param("ahora") LocalDateTime ahora,
            @Param("motivo") String motivo,
            @Param("chatId") Long chatId,
            @Param("userId") Long userId,
            Pageable pageable);

    @Query("select m from MensajeEntity m " +
            "where m.chat.id = :chatId " +
            "and m.adminMessage = true " +
            "and m.activo = true " +
            "and (m.expiraEn is null or m.expiraEn > CURRENT_TIMESTAMP)")
    List<MensajeEntity> findVisibleAdminMessagesByChatId(@Param("chatId") Long chatId);

    @Query("select count(m) from MensajeEntity m " +
            "where m.chat.id = :chatId " +
            "and m.adminMessage = true " +
            "and m.activo = true " +
            "and m.receptor.id = :userId " +
            "and (m.expiraEn is null or m.expiraEn > CURRENT_TIMESTAMP)")
    long countVisibleAdminMessagesByChatIdAndUserId(@Param("chatId") Long chatId,
                                                    @Param("userId") Long userId);

    @Query("select count(m) from MensajeEntity m " +
            "where m.mensajeTemporal = true " +
            "and m.expiraEn is not null " +
            "and m.expiraEn <= :ahora")
    long countMensajesTemporalesExpirados(@Param("ahora") LocalDateTime ahora);

    @Modifying
    @Transactional
    @Query("delete from MensajeEntity m where m.id in :ids")
    int eliminarPorIds(@Param("ids") List<Long> ids);

    @Query("select m from MensajeEntity m " +
            "where m.motivoEliminacion = :motivo " +
            "and m.expiraEn is not null " +
            "and m.expiraEn <= :cutoff " +
            "order by m.expiraEn asc, m.id asc")
    List<MensajeEntity> findPlaceholdersParaLimpiezaTecnica(
            @Param("motivo") String motivo,
            @Param("cutoff") LocalDateTime cutoff,
            Pageable pageable);

    @Query("select m from MensajeEntity m " +
            "left join fetch m.emisor emisor " +
            "left join fetch m.receptor receptor " +
            "left join fetch m.chat chat " +
            "where m.chat.id = :chatId " +
            "and type(m.chat) = ChatIndividualEntity " +
            "and m.activo = true " +
            "and m.adminMessage = false " +
            "and (m.expiraEn is null or m.expiraEn > CURRENT_TIMESTAMP) " +
            "order by m.fechaEnvio desc, m.id desc")
    List<MensajeEntity> findLatestVisibleMessagesByChatIndividualId(
            @Param("chatId") Long chatId,
            Pageable pageable);

    @Query("select m from MensajeEntity m " +
            "left join fetch m.emisor emisor " +
            "left join fetch m.receptor receptor " +
            "left join fetch m.chat chat " +
            "where m.chat.id = :chatGrupalId " +
            "and type(m.chat) = ChatGrupalEntity " +
            "and m.activo = true " +
            "and m.adminMessage = false " +
            "and (m.expiraEn is null or m.expiraEn > CURRENT_TIMESTAMP) " +
            "order by m.fechaEnvio desc, m.id desc")
    List<MensajeEntity> findLatestVisibleMessagesByChatGrupalId(
            @Param("chatGrupalId") Long chatGrupalId,
            Pageable pageable);

    @Query(value = "select m from MensajeEntity m " +
            "where m.emisor.id = :userId " +
            "and m.activo = true " +
            "and m.adminMessage = false " +
            "and m.contenido is not null " +
            "and m.tipo = :tipo " +
            "and (m.expiraEn is null or m.expiraEn > CURRENT_TIMESTAMP) " +
            "and (:fechaInicio is null or m.fechaEnvio >= :fechaInicio) " +
            "and (:fechaFin is null or m.fechaEnvio <= :fechaFin) " +
            "order by m.fechaEnvio desc, m.id desc",
            countQuery = "select count(m) from MensajeEntity m " +
                    "where m.emisor.id = :userId " +
                    "and m.activo = true " +
                    "and m.adminMessage = false " +
                    "and m.contenido is not null " +
                    "and m.tipo = :tipo " +
                    "and (m.expiraEn is null or m.expiraEn > CURRENT_TIMESTAMP) " +
                    "and (:fechaInicio is null or m.fechaEnvio >= :fechaInicio) " +
                    "and (:fechaFin is null or m.fechaEnvio <= :fechaFin)")
    Page<MensajeEntity> findMensajesEnviadosPorUsuarioParaAi(
            @Param("userId") Long userId,
            @Param("tipo") MessageType tipo,
            @Param("fechaInicio") LocalDateTime fechaInicio,
            @Param("fechaFin") LocalDateTime fechaFin,
            Pageable pageable);

    @Query("select m from MensajeEntity m " +
            "left join fetch m.emisor emisor " +
            "left join fetch m.receptor receptor " +
            "left join fetch m.chat chat " +
            "where m.emisor.id = :userId " +
            "and m.activo = true " +
            "and m.adminMessage = false " +
            "and m.contenido is not null " +
            "and m.tipo = :tipo " +
            "and (m.expiraEn is null or m.expiraEn > CURRENT_TIMESTAMP) " +
            "and (:fechaInicio is null or m.fechaEnvio >= :fechaInicio) " +
            "and (:fechaFin is null or m.fechaEnvio <= :fechaFin) " +
            "and type(m.chat) = ChatGrupalEntity " +
            "and m.chat.id = :chatGrupalId " +
            "order by m.fechaEnvio desc, m.id desc")
    Page<MensajeEntity> buscarMensajesEnviadosPorUsuarioEnChatGrupal(
            @Param("userId") Long userId,
            @Param("tipo") MessageType tipo,
            @Param("chatGrupalId") Long chatGrupalId,
            @Param("fechaInicio") LocalDateTime fechaInicio,
            @Param("fechaFin") LocalDateTime fechaFin,
            Pageable pageable);

    @Query("select m from MensajeEntity m " +
            "left join fetch m.emisor emisor " +
            "left join fetch m.receptor receptor " +
            "left join fetch m.chat chat " +
            "where m.emisor.id = :userId " +
            "and m.activo = true " +
            "and m.adminMessage = false " +
            "and m.contenido is not null " +
            "and m.tipo = :tipo " +
            "and (m.expiraEn is null or m.expiraEn > CURRENT_TIMESTAMP) " +
            "and (:fechaInicio is null or m.fechaEnvio >= :fechaInicio) " +
            "and (:fechaFin is null or m.fechaEnvio <= :fechaFin) " +
            "and type(m.chat) = ChatIndividualEntity " +
            "and m.chat.id = :chatId " +
            "order by m.fechaEnvio desc, m.id desc")
    Page<MensajeEntity> buscarMensajesEnviadosPorUsuarioEnChatIndividual(
            @Param("userId") Long userId,
            @Param("tipo") MessageType tipo,
            @Param("chatId") Long chatId,
            @Param("fechaInicio") LocalDateTime fechaInicio,
            @Param("fechaFin") LocalDateTime fechaFin,
            Pageable pageable);

    @Query("select m from MensajeEntity m " +
            "left join fetch m.emisor emisor " +
            "left join fetch m.receptor receptor " +
            "left join fetch m.chat chat " +
            "where m.emisor.id = :userId " +
            "and m.activo = true " +
            "and m.adminMessage = false " +
            "and m.contenido is not null " +
            "and m.tipo = :tipo " +
            "and (m.expiraEn is null or m.expiraEn > CURRENT_TIMESTAMP) " +
            "and (:fechaInicio is null or m.fechaEnvio >= :fechaInicio) " +
            "and (:fechaFin is null or m.fechaEnvio <= :fechaFin) " +
            "order by m.fechaEnvio desc, m.id desc")
    Page<MensajeEntity> buscarMensajesEnviadosPorUsuarioGlobal(
            @Param("userId") Long userId,
            @Param("tipo") MessageType tipo,
            @Param("fechaInicio") LocalDateTime fechaInicio,
            @Param("fechaFin") LocalDateTime fechaFin,
            Pageable pageable);

    @Query("select m from MensajeEntity m " +
            "left join fetch m.emisor emisor " +
            "left join fetch m.receptor receptor " +
            "left join fetch m.chat chat " +
            "where m.emisor.id = :userId " +
            "and m.activo = true " +
            "and m.adminMessage = false " +
            "and m.contenido is not null " +
            "and m.tipo = :tipo " +
            "and (m.expiraEn is null or m.expiraEn > CURRENT_TIMESTAMP) " +
            "and (:fechaInicio is null or m.fechaEnvio >= :fechaInicio) " +
            "and (:fechaFin is null or m.fechaEnvio <= :fechaFin) " +
            "and type(m.chat) = ChatGrupalEntity " +
            "order by m.fechaEnvio desc, m.id desc")
    Page<MensajeEntity> buscarMensajesEnviadosPorUsuarioGlobalSoloGrupales(
            @Param("userId") Long userId,
            @Param("tipo") MessageType tipo,
            @Param("fechaInicio") LocalDateTime fechaInicio,
            @Param("fechaFin") LocalDateTime fechaFin,
            Pageable pageable);

    @Query("select m from MensajeEntity m " +
            "left join fetch m.emisor emisor " +
            "left join fetch m.receptor receptor " +
            "left join fetch m.chat chat " +
            "where m.emisor.id = :userId " +
            "and m.activo = true " +
            "and m.adminMessage = false " +
            "and (m.expiraEn is null or m.expiraEn > CURRENT_TIMESTAMP) " +
            "and (:fechaInicio is null or m.fechaEnvio >= :fechaInicio) " +
            "and (:fechaFin is null or m.fechaEnvio <= :fechaFin) " +
            "and type(m.chat) = ChatGrupalEntity " +
            "and m.chat.id = :chatGrupalId " +
            "order by m.fechaEnvio desc, m.id desc")
    Page<MensajeEntity> buscarMensajesEnviadosPorUsuarioEnChatGrupalSinFiltroTipo(
            @Param("userId") Long userId,
            @Param("chatGrupalId") Long chatGrupalId,
            @Param("fechaInicio") LocalDateTime fechaInicio,
            @Param("fechaFin") LocalDateTime fechaFin,
            Pageable pageable);

    @Query("select m from MensajeEntity m " +
            "left join fetch m.emisor emisor " +
            "left join fetch m.receptor receptor " +
            "left join fetch m.chat chat " +
            "where m.emisor.id = :userId " +
            "and m.activo = true " +
            "and m.adminMessage = false " +
            "and (m.expiraEn is null or m.expiraEn > CURRENT_TIMESTAMP) " +
            "and (:fechaInicio is null or m.fechaEnvio >= :fechaInicio) " +
            "and (:fechaFin is null or m.fechaEnvio <= :fechaFin) " +
            "and type(m.chat) = ChatIndividualEntity " +
            "and m.chat.id = :chatId " +
            "order by m.fechaEnvio desc, m.id desc")
    Page<MensajeEntity> buscarMensajesEnviadosPorUsuarioEnChatIndividualSinFiltroTipo(
            @Param("userId") Long userId,
            @Param("chatId") Long chatId,
            @Param("fechaInicio") LocalDateTime fechaInicio,
            @Param("fechaFin") LocalDateTime fechaFin,
            Pageable pageable);

    @Query("select m from MensajeEntity m " +
            "left join fetch m.emisor emisor " +
            "left join fetch m.receptor receptor " +
            "left join fetch m.chat chat " +
            "where m.emisor.id = :userId " +
            "and m.activo = true " +
            "and m.adminMessage = false " +
            "and (m.expiraEn is null or m.expiraEn > CURRENT_TIMESTAMP) " +
            "and (:fechaInicio is null or m.fechaEnvio >= :fechaInicio) " +
            "and (:fechaFin is null or m.fechaEnvio <= :fechaFin) " +
            "order by m.fechaEnvio desc, m.id desc")
    Page<MensajeEntity> buscarMensajesEnviadosPorUsuarioGlobalSinFiltroTipo(
            @Param("userId") Long userId,
            @Param("fechaInicio") LocalDateTime fechaInicio,
            @Param("fechaFin") LocalDateTime fechaFin,
            Pageable pageable);

    @Query("select m from MensajeEntity m " +
            "left join fetch m.emisor emisor " +
            "left join fetch m.receptor receptor " +
            "left join fetch m.chat chat " +
            "where m.emisor.id = :userId " +
            "and m.activo = true " +
            "and m.adminMessage = false " +
            "and (m.expiraEn is null or m.expiraEn > CURRENT_TIMESTAMP) " +
            "and (:fechaInicio is null or m.fechaEnvio >= :fechaInicio) " +
            "and (:fechaFin is null or m.fechaEnvio <= :fechaFin) " +
            "and type(m.chat) = ChatGrupalEntity " +
            "order by m.fechaEnvio desc, m.id desc")
    Page<MensajeEntity> buscarMensajesEnviadosPorUsuarioGlobalSoloGrupalesSinFiltroTipo(
            @Param("userId") Long userId,
            @Param("fechaInicio") LocalDateTime fechaInicio,
            @Param("fechaFin") LocalDateTime fechaFin,
            Pageable pageable);

    @Query("select m from MensajeEntity m " +
            "left join fetch m.emisor emisor " +
            "left join fetch m.receptor receptor " +
            "left join fetch m.chat chat " +
            "where m.emisor.id = :userId " +
            "and m.activo = true " +
            "and m.adminMessage = false " +
            "and (m.expiraEn is null or m.expiraEn > CURRENT_TIMESTAMP) " +
            "and (:fechaInicio is null or m.fechaEnvio >= :fechaInicio) " +
            "and (:fechaFin is null or m.fechaEnvio <= :fechaFin) " +
            "and type(m.chat) = ChatGrupalEntity " +
            "and m.chat.id = :chatGrupalId " +
            "order by m.fechaEnvio asc, m.id asc")
    Page<MensajeEntity> buscarPrimerosMensajesEnviadosPorUsuarioEnChatGrupalSinFiltroTipo(
            @Param("userId") Long userId,
            @Param("chatGrupalId") Long chatGrupalId,
            @Param("fechaInicio") LocalDateTime fechaInicio,
            @Param("fechaFin") LocalDateTime fechaFin,
            Pageable pageable);

    @Query("select m from MensajeEntity m " +
            "left join fetch m.emisor emisor " +
            "left join fetch m.receptor receptor " +
            "left join fetch m.chat chat " +
            "where m.emisor.id = :userId " +
            "and m.activo = true " +
            "and m.adminMessage = false " +
            "and (m.expiraEn is null or m.expiraEn > CURRENT_TIMESTAMP) " +
            "and (:fechaInicio is null or m.fechaEnvio >= :fechaInicio) " +
            "and (:fechaFin is null or m.fechaEnvio <= :fechaFin) " +
            "and type(m.chat) = ChatIndividualEntity " +
            "and m.chat.id = :chatId " +
            "order by m.fechaEnvio asc, m.id asc")
    Page<MensajeEntity> buscarPrimerosMensajesEnviadosPorUsuarioEnChatIndividualSinFiltroTipo(
            @Param("userId") Long userId,
            @Param("chatId") Long chatId,
            @Param("fechaInicio") LocalDateTime fechaInicio,
            @Param("fechaFin") LocalDateTime fechaFin,
            Pageable pageable);

    @Query("select m from MensajeEntity m " +
            "left join fetch m.emisor emisor " +
            "left join fetch m.receptor receptor " +
            "left join fetch m.chat chat " +
            "where m.emisor.id = :userId " +
            "and m.activo = true " +
            "and m.adminMessage = false " +
            "and (m.expiraEn is null or m.expiraEn > CURRENT_TIMESTAMP) " +
            "and (:fechaInicio is null or m.fechaEnvio >= :fechaInicio) " +
            "and (:fechaFin is null or m.fechaEnvio <= :fechaFin) " +
            "order by m.fechaEnvio asc, m.id asc")
    Page<MensajeEntity> buscarPrimerosMensajesEnviadosPorUsuarioGlobalSinFiltroTipo(
            @Param("userId") Long userId,
            @Param("fechaInicio") LocalDateTime fechaInicio,
            @Param("fechaFin") LocalDateTime fechaFin,
            Pageable pageable);

    @Query("select m from MensajeEntity m " +
            "left join fetch m.emisor emisor " +
            "left join fetch m.receptor receptor " +
            "left join fetch m.chat chat " +
            "where m.emisor.id = :userId " +
            "and m.activo = true " +
            "and m.adminMessage = false " +
            "and (m.expiraEn is null or m.expiraEn > CURRENT_TIMESTAMP) " +
            "and (:fechaInicio is null or m.fechaEnvio >= :fechaInicio) " +
            "and (:fechaFin is null or m.fechaEnvio <= :fechaFin) " +
            "and type(m.chat) = ChatGrupalEntity " +
            "order by m.fechaEnvio asc, m.id asc")
    Page<MensajeEntity> buscarPrimerosMensajesEnviadosPorUsuarioGlobalSoloGrupalesSinFiltroTipo(
            @Param("userId") Long userId,
            @Param("fechaInicio") LocalDateTime fechaInicio,
            @Param("fechaFin") LocalDateTime fechaFin,
            Pageable pageable);
}
