package com.chat.chat.Service.AiService;

import com.chat.chat.DTO.AiQuickReplyRequestDTO;
import com.chat.chat.DTO.AiQuickReplyResponseDTO;

public interface AiQuickReplyService {

    AiQuickReplyResponseDTO generarSugerencias(AiQuickReplyRequestDTO request);
}
