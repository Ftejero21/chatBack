package com.chat.chat.Repository;

import com.chat.chat.Entity.SolicitudDesbaneoEntity;
import com.chat.chat.Utils.ReporteTipo;
import com.chat.chat.Utils.SolicitudDesbaneoEstado;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SolicitudDesbaneoRepository extends JpaRepository<SolicitudDesbaneoEntity, Long> {
    interface ReportTypeCountView {
        ReporteTipo getTipoReporte();

        Long getTotal();
    }

    interface ReportStatusCountView {
        SolicitudDesbaneoEstado getEstado();

        Long getTotal();
    }

    Page<SolicitudDesbaneoEntity> findAllByEstado(SolicitudDesbaneoEstado estado, Pageable pageable);
    Page<SolicitudDesbaneoEntity> findAllByEstadoIn(Collection<SolicitudDesbaneoEstado> estados, Pageable pageable);
    Page<SolicitudDesbaneoEntity> findAllByTipoReporte(ReporteTipo tipoReporte, Pageable pageable);
    Page<SolicitudDesbaneoEntity> findAllByTipoReporteAndEstadoIn(ReporteTipo tipoReporte, Collection<SolicitudDesbaneoEstado> estados, Pageable pageable);

    boolean existsByEmailAndEstadoIn(String email, Collection<SolicitudDesbaneoEstado> estados);
    boolean existsByTipoReporteAndUsuarioIdAndChatIdAndEstadoIn(
            ReporteTipo tipoReporte,
            Long usuarioId,
            Long chatId,
            Collection<SolicitudDesbaneoEstado> estados);

    long countByEstado(SolicitudDesbaneoEstado estado);

    List<SolicitudDesbaneoEntity> findByCreatedAtGreaterThanEqualAndCreatedAtLessThan(LocalDateTime from, LocalDateTime to);

    long countByCreatedAtGreaterThanEqualAndCreatedAtLessThan(LocalDateTime from, LocalDateTime to);

    @Query("""
            select s.tipoReporte as tipoReporte, count(s.id) as total
            from SolicitudDesbaneoEntity s
            where s.createdAt >= :from
              and s.createdAt < :to
            group by s.tipoReporte
            order by count(s.id) desc, s.tipoReporte asc
            """)
    List<ReportTypeCountView> countByTipoReporteGroupedByCreatedAtBetween(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );

    @Query("""
            select s.estado as estado, count(s.id) as total
            from SolicitudDesbaneoEntity s
            where s.createdAt >= :from
              and s.createdAt < :to
            group by s.estado
            order by count(s.id) desc, s.estado asc
            """)
    List<ReportStatusCountView> countByEstadoGroupedByCreatedAtBetween(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );

    long countByTipoReporteAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
            ReporteTipo tipoReporte,
            LocalDateTime from,
            LocalDateTime to
    );

    long countByEstadoInAndUpdatedAtGreaterThanEqualAndUpdatedAtLessThan(
            Collection<SolicitudDesbaneoEstado> estados,
            LocalDateTime from,
            LocalDateTime to
    );

    @Query("select s.id from SolicitudDesbaneoEntity s " +
            "where s.estado in :estados and s.updatedAt >= :from and s.updatedAt < :to")
    List<Long> findIdsForWeeklyCleanup(
            @Param("estados") Collection<SolicitudDesbaneoEstado> estados,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            Pageable pageable
    );
}
