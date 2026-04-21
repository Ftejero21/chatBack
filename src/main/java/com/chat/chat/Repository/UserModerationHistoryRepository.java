package com.chat.chat.Repository;

import com.chat.chat.Entity.UserModerationHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserModerationHistoryRepository extends JpaRepository<UserModerationHistoryEntity, Long> {
    List<UserModerationHistoryEntity> findByUser_IdOrderByCreatedAtDescIdDesc(Long userId);
}
