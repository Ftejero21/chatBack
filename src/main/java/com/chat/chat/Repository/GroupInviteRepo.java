package com.chat.chat.Repository;

import com.chat.chat.Entity.GroupInviteEntity;
import com.chat.chat.Utils.InviteStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GroupInviteRepo extends JpaRepository<GroupInviteEntity, Long> {
    long countByInviteeIdAndStatus(Long inviteeId, InviteStatus status);
    List<GroupInviteEntity> findAllByInviteeIdOrderByCreatedAtDesc(Long inviteeId);
    List<GroupInviteEntity> findAllByChatIdAndStatus(Long chatId, InviteStatus status);
}
