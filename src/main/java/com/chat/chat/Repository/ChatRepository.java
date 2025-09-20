package com.chat.chat.Repository;

import com.chat.chat.Entity.ChatEntity;
import com.chat.chat.Entity.GroupInviteEntity;
import com.chat.chat.Utils.InviteStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatRepository extends JpaRepository<ChatEntity, Long> {


}
