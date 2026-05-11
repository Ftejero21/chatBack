package com.chat.chat.Repository;

import com.chat.chat.Entity.SolicitudReporteAdjuntoEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface SolicitudReporteAdjuntoRepository extends JpaRepository<SolicitudReporteAdjuntoEntity, Long> {

    Optional<SolicitudReporteAdjuntoEntity> findBySolicitudId(Long solicitudId);

    List<SolicitudReporteAdjuntoEntity> findBySolicitudIdIn(Collection<Long> solicitudIds);
}
