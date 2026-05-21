package com.chat.chat.Service.AiService;

import com.chat.chat.DTO.AiUsageInfoDTO;

public interface DeepSeekApiClient {

    String completarTexto(String systemPrompt, String userContent);

    String completarTexto(String systemPrompt, String userContent, Integer maxOutputTokens);

    String completarTextoAdminReport(String systemPrompt, String userContent, Integer maxOutputTokens);

    AiUsageInfoDTO getLastUsage();
}
