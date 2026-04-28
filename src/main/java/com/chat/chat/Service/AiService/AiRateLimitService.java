package com.chat.chat.Service.AiService;

public interface AiRateLimitService {

    boolean puedeUsar(Long userId);

    void registrarUso(Long userId);

    AiRateLimitCheck checkUsage(Long userId);
}
