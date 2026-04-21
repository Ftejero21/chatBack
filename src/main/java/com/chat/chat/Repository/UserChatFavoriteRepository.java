package com.chat.chat.Repository;

import com.chat.chat.Entity.UserChatFavoriteEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserChatFavoriteRepository extends JpaRepository<UserChatFavoriteEntity, Long> {
}
