package com.chat.chat.Repository;

import com.chat.chat.Entity.StickerEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

@Repository
public interface StickerRepository extends JpaRepository<StickerEntity, Long> {
    List<StickerEntity> findByUsuarioIdAndActivoTrueOrderByFechaCreacionDesc(Long usuarioId);
    Optional<StickerEntity> findByIdAndUsuarioIdAndActivoTrue(Long id, Long usuarioId);
    Optional<StickerEntity> findByIdAndActivoTrue(Long id);
    boolean existsByIdAndUsuarioIdAndActivoTrue(Long id, Long usuarioId);
    boolean existsByUsuarioIdAndSourceStickerIdAndActivoTrue(Long usuarioId, Long sourceStickerId);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<StickerEntity> findById(Long id);
    long countByUsuarioIdAndActivoTrue(Long usuarioId);
}
