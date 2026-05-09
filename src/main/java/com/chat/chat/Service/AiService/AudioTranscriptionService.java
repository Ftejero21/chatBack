package com.chat.chat.Service.AiService;

import com.chat.chat.DTO.AudioTranscriptionResultDTO;
import com.chat.chat.Entity.MensajeEntity;

public interface AudioTranscriptionService {

    AudioTranscriptionResultDTO transcribirAudio(MensajeEntity mensaje, byte[] audioBytes, String mimeType);
}
