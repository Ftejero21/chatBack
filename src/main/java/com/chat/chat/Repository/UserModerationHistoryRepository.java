package com.chat.chat.Repository;

import com.chat.chat.Entity.UserModerationHistoryEntity;
import com.chat.chat.Utils.ModerationActionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserModerationHistoryRepository extends JpaRepository<UserModerationHistoryEntity, Long> {
    interface ModerationActionCountView {
        ModerationActionType getActionType();

        Long getTotal();
    }

    interface ModerationReasonCountView {
        String getReason();

        Long getTotal();
    }

    List<UserModerationHistoryEntity> findByUser_IdOrderByCreatedAtDescIdDesc(Long userId);

    Optional<UserModerationHistoryEntity> findFirstByUser_IdAndActionTypeAndOriginOrderByCreatedAtDescIdDesc(
            Long userId,
            ModerationActionType actionType,
            String origin);

    long countByActionTypeAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
            ModerationActionType actionType,
            java.time.LocalDateTime from,
            java.time.LocalDateTime to
    );

    @Query("""
            select h.actionType as actionType, count(h.id) as total
            from UserModerationHistoryEntity h
            where h.createdAt >= :from
              and h.createdAt < :to
            group by h.actionType
            order by count(h.id) desc, h.actionType asc
            """)
    List<ModerationActionCountView> countByActionTypeGroupedByCreatedAtBetween(
            @Param("from") java.time.LocalDateTime from,
            @Param("to") java.time.LocalDateTime to
    );

    @Query("""
            select h.reason as reason, count(h.id) as total
            from UserModerationHistoryEntity h
            where h.createdAt >= :from
              and h.createdAt < :to
              and h.reason is not null
              and h.reason <> ''
            group by h.reason
            order by count(h.id) desc, h.reason asc
            """)
    List<ModerationReasonCountView> countByReasonGroupedByCreatedAtBetween(
            @Param("from") java.time.LocalDateTime from,
            @Param("to") java.time.LocalDateTime to,
            org.springframework.data.domain.Pageable pageable
    );

    Optional<UserModerationHistoryEntity> findFirstByUser_IdOrderByCreatedAtDescIdDesc(Long userId);
}
