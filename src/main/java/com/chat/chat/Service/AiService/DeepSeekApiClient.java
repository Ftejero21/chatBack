package com.chat.chat.Service.AiService;

public interface DeepSeekApiClient {

    String completarTexto(String systemPrompt, String userContent);

    String completarTexto(String systemPrompt, String userContent, Integer maxOutputTokens);

    String completarTextoAdminReport(String systemPrompt, String userContent, Integer maxOutputTokens);
}
