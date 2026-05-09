package com.chat.chat.Service.AiService;

import com.chat.chat.DTO.AiEncryptedMessageSearchRequestDTO;
import com.chat.chat.DTO.AiEncryptedMessageSearchResponseDTO;

public interface AiEncryptedMessageSearchService {

    AiEncryptedMessageSearchResponseDTO buscarMensajes(AiEncryptedMessageSearchRequestDTO request);
}