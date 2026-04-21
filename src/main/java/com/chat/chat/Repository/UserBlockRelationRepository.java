package com.chat.chat.Repository;

import com.chat.chat.Entity.UserBlockRelationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserBlockRelationRepository extends JpaRepository<UserBlockRelationEntity, Long> {
    Optional<UserBlockRelationEntity> findByBlocker_IdAndBlocked_Id(Long blockerId, Long blockedId);
    List<UserBlockRelationEntity> findByBlocker_Id(Long blockerId);
    void deleteByBlocker_IdAndBlocked_Id(Long blockerId, Long blockedId);
}
