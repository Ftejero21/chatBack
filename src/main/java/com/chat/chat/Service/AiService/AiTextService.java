package com.chat.chat.Service.AiService;

import com.chat.chat.DTO.AiTextRequestDTO;
import com.chat.chat.DTO.AiTextResponseDTO;

public interface AiTextService {

    AiTextResponseDTO procesarTexto(AiTextRequestDTO request);
}
