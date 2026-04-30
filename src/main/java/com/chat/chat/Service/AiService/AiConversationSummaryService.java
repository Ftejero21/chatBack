package com.chat.chat.Service.AiService;

import com.chat.chat.DTO.AiConversationSummaryRequestDTO;
import com.chat.chat.DTO.AiConversationSummaryResponseDTO;

public interface AiConversationSummaryService {

    AiConversationSummaryResponseDTO resumirConversacion(AiConversationSummaryRequestDTO request);
}
