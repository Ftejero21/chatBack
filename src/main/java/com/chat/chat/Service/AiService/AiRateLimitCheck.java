package com.chat.chat.Service.AiService;

public class AiRateLimitCheck {

    private final boolean allowed;
    private final String code;
    private final String message;

    public AiRateLimitCheck(boolean allowed, String code, String message) {
        this.allowed = allowed;
        this.code = code;
        this.message = message;
    }

    public boolean isAllowed() {
        return allowed;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
