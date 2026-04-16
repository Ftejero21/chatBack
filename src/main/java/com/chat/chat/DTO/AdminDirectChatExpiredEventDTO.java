package com.chat.chat.DTO;

import java.util.ArrayList;
import java.util.List;

public class AdminDirectChatExpiredEventDTO {
    private String systemEvent;
    private Long chatId;
    private Long userId;
    private List<Long> expiredMessageIds = new ArrayList<>();

    public String getSystemEvent() {
        return systemEvent;
    }

    public void setSystemEvent(String systemEvent) {
        this.systemEvent = systemEvent;
    }

    public Long getChatId() {
        return chatId;
    }

    public void setChatId(Long chatId) {
        this.chatId = chatId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public List<Long> getExpiredMessageIds() {
        return expiredMessageIds;
    }

    public void setExpiredMessageIds(List<Long> expiredMessageIds) {
        this.expiredMessageIds = expiredMessageIds;
    }
}
