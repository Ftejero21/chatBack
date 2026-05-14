package com.chat.chat.Service.AiService;

import com.chat.chat.DTO.AiEncryptedMessageSearchRequestDTO;
import com.chat.chat.DTO.AiEncryptedMessageSearchResponseDTO;
import com.chat.chat.DTO.AiSearchIntentInternalResponseDTO;

public interface AiEncryptedMessageSearchService {

    AiEncryptedMessageSearchResponseDTO buscarMensajes(AiEncryptedMessageSearchRequestDTO request);

    AiEncryptedMessageSearchResponseDTO buscarMensajes(AiEncryptedMessageSearchRequestDTO request,
                                                       String requestId,
                                                       AiSearchIntentInternalResponseDTO intent,
                                                       boolean authoritativeIntentRouting);
}
