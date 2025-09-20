package com.chat.chat.Repository;

import com.chat.chat.Entity.NotificationEntity;
import com.chat.chat.Utils.NotificationType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NotificationRepo extends JpaRepository<NotificationEntity, Long> {
    long countByUserIdAndSeenFalse(Long userId);
    List<NotificationEntity> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<NotificationEntity> findByUserIdAndResolvedFalseOrderByCreatedAtDesc(Long userId);

    // Para encontrar la notificación de invitación concreta (buscamos por inviteId en el JSON)
    Optional<NotificationEntity> findFirstByUserIdAndTypeAndPayloadJsonContaining(
            Long userId, NotificationType type, String token);
}