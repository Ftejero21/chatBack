package com.chat.chat.DTO;

public class UserChatFavoriteResponseDTO {
    private Long chatId;

    public UserChatFavoriteResponseDTO() {
    }

    public UserChatFavoriteResponseDTO(Long chatId) {
        this.chatId = chatId;
    }

    public Long getChatId() {
        return chatId;
    }

    public void setChatId(Long chatId) {
        this.chatId = chatId;
    }
}
