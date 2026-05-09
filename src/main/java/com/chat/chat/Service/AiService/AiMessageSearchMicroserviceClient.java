package com.chat.chat.Service.AiService;

import com.chat.chat.DTO.AiMessageSearchInternalRequestDTO;
import com.chat.chat.DTO.AiMessageSearchInternalResponseDTO;

public interface AiMessageSearchMicroserviceClient {

    AiMessageSearchInternalResponseDTO buscarMensajesConIA(String requestId, AiMessageSearchInternalRequestDTO request);
}
