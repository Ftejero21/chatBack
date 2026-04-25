package com.chat.chat.Repository;

import com.chat.chat.Entity.UserModerationHistoryEntity;
import com.chat.chat.Utils.ModerationActionType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserModerationHistoryRepository extends JpaRepository<UserModerationHistoryEntity, Long> {
    List<UserModerationHistoryEntity> findByUser_IdOrderByCreatedAtDescIdDesc(Long userId);

    Optional<UserModerationHistoryEntity> findFirstByUser_IdAndActionTypeAndOriginOrderByCreatedAtDescIdDesc(
            Long userId,
            ModerationActionType actionType,
            String origin);
}
