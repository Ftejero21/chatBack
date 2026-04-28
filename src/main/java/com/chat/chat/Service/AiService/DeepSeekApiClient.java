package com.chat.chat.Service.AiService;

public interface DeepSeekApiClient {

    String completarTexto(String systemPrompt, String userContent);
}
