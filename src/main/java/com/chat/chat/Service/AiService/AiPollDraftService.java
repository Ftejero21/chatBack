package com.chat.chat.Service.AiService;

import com.chat.chat.DTO.AiPollDraftRequestDTO;
import com.chat.chat.DTO.AiPollDraftResponseDTO;

public interface AiPollDraftService {

    AiPollDraftResponseDTO generarBorrador(AiPollDraftRequestDTO request);
}
