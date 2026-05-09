package com.chat.chat.Service.AiService;

import com.chat.chat.DTO.AiSearchIntentInternalRequestDTO;
import com.chat.chat.DTO.AiSearchIntentInternalResponseDTO;

public interface AiSearchIntentMicroserviceClient {

    /**
     * Calls tejechat-ai-service POST /internal/ai/search-intent.
     * Returns null if microservice unavailable or returns error — caller falls back to deterministic analysis.
     */
    AiSearchIntentInternalResponseDTO classifyIntent(String requestId, AiSearchIntentInternalRequestDTO request);
}
