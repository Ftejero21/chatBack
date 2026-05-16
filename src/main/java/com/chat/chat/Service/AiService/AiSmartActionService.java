package com.chat.chat.Service.AiService;

import com.chat.chat.DTO.AiSmartActionRequestDTO;

public interface AiSmartActionService {

    Object process(AiSmartActionRequestDTO request);
}
