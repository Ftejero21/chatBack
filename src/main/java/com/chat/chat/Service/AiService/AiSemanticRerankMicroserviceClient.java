package com.chat.chat.Service.AiService;

import com.chat.chat.DTO.AiSemanticRerankInternalRequestDTO;
import com.chat.chat.DTO.AiSemanticRerankInternalResponseDTO;

public interface AiSemanticRerankMicroserviceClient {

    AiSemanticRerankInternalResponseDTO rerank(String requestId, AiSemanticRerankInternalRequestDTO request);
}
