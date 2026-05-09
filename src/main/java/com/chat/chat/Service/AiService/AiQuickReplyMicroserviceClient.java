package com.chat.chat.Service.AiService;

import com.chat.chat.DTO.AiQuickReplyInternalRequestDTO;
import com.chat.chat.DTO.AiQuickReplyInternalResponseDTO;

public interface AiQuickReplyMicroserviceClient {

    AiQuickReplyInternalResponseDTO generarSugerencias(String requestId, AiQuickReplyInternalRequestDTO request);
}
