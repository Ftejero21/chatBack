package com.chat.chat.Repository;

import com.chat.chat.Entity.MensajeEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
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

    @Query("SELECT COUNT(m) FROM MensajeEntity m WHERE m.fechaEnvio >= :inicio AND m.fechaEnvio < :fin")
    long countMensajesEntreFechas(@Param("inicio") java.time.LocalDateTime inicio,
                                  @Param("fin") java.time.LocalDateTime fin);
}
