package com.chat.chat.Exceptions;

public class ChatCerradoException extends RuntimeException {

    private final Long chatId;

    public ChatCerradoException(String message, Long chatId) {
        super(message);
        this.chatId = chatId;
    }

    public Long getChatId() {
        return chatId;
    }
}
