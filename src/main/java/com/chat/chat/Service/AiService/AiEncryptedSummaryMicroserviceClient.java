package com.chat.chat.Service.AiService;

import com.chat.chat.DTO.AiEncryptedConversationSummaryInternalRequestDTO;
import com.chat.chat.DTO.AiEncryptedConversationSummaryInternalResponseDTO;

public interface AiEncryptedSummaryMicroserviceClient {

    AiEncryptedConversationSummaryInternalResponseDTO resumirConversacion(String requestId,
                                                                         AiEncryptedConversationSummaryInternalRequestDTO request);
}
