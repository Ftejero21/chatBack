package com.chat.chat.DTO;

public class AdminDirectChatRemovedEventDTO {
    private String systemEvent;
    private Long chatId;

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
}
