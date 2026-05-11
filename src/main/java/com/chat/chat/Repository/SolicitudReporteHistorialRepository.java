package com.chat.chat.Repository;

import com.chat.chat.Entity.SolicitudReporteHistorialEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface SolicitudReporteHistorialRepository extends JpaRepository<SolicitudReporteHistorialEntity, Long> {

    List<SolicitudReporteHistorialEntity> findBySolicitudIdOrderByCreatedAtAsc(Long solicitudId);

    List<SolicitudReporteHistorialEntity> findBySolicitudIdInOrderByCreatedAtAsc(Collection<Long> solicitudIds);

    SolicitudReporteHistorialEntity findTopBySolicitudIdOrderByCreatedAtDesc(Long solicitudId);
}
