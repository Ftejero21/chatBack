package com.chat.chat.Service.GroupInviteService;

public interface GroupInviteService {
    void accept(Long inviteId, Long userId);
    void decline(Long inviteId, Long userId);
}
