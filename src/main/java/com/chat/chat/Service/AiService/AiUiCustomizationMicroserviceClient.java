package com.chat.chat.Service.AiService;

import com.chat.chat.DTO.AiUiCustomizationIntentInternalRequestDTO;
import com.chat.chat.DTO.AiUiCustomizationIntentInternalResponseDTO;

public interface AiUiCustomizationMicroserviceClient {

    /**
     * Calls tejechat-ai-service POST /internal/ai/ui-customization/intent.
     * Returns null on transport failure → caller responds with failure code.
     */
    AiUiCustomizationIntentInternalResponseDTO classifyIntent(String requestId,
                                                              AiUiCustomizationIntentInternalRequestDTO request);
}
