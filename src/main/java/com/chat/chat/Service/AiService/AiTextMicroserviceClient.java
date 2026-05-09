package com.chat.chat.Service.AiService;

import com.chat.chat.DTO.AiTextInternalRequestDTO;
import com.chat.chat.DTO.AiTextInternalResponseDTO;

public interface AiTextMicroserviceClient {

    AiTextInternalResponseDTO procesarTexto(String requestId, AiTextInternalRequestDTO request);
}
