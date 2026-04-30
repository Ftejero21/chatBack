package com.chat.chat.Service.AiService;

import com.chat.chat.DTO.AiEncryptedContextMessageDTO;
import com.chat.chat.DTO.AiEncryptedResponseDTO;

import java.util.List;

public interface AiEncryptedContextService {

    List<AiPlainContextMessage> decryptContextMessages(List<AiEncryptedContextMessageDTO> mensajes);

    String decryptMessagePayload(String encryptedPayload);

    AiEncryptedResponseDTO encryptAiResponseForUser(String textoPlano, Long userId);
}
