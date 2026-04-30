package com.chat.chat.Service.AiService;

import com.chat.chat.DTO.AiEncryptedConversationSummaryRequestDTO;
import com.chat.chat.DTO.AiEncryptedResponseDTO;

public interface AiEncryptedConversationSummaryService {

    AiEncryptedResponseDTO resumirConversacionCifrada(AiEncryptedConversationSummaryRequestDTO request);
}
