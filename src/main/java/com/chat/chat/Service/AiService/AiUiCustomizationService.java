package com.chat.chat.Service.AiService;

import com.chat.chat.DTO.AiUiCustomizationRequestDTO;
import com.chat.chat.DTO.AiUiCustomizationResponseDTO;

public interface AiUiCustomizationService {

    AiUiCustomizationResponseDTO classifyIntent(AiUiCustomizationRequestDTO request);
}
