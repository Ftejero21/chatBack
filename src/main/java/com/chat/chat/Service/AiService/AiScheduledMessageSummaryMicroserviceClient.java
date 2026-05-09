package com.chat.chat.Service.AiService;

import com.chat.chat.DTO.AiScheduledMessageSummaryInternalRequestDTO;
import com.chat.chat.DTO.AiScheduledMessageSummaryInternalResponseDTO;

public interface AiScheduledMessageSummaryMicroserviceClient {

    AiScheduledMessageSummaryInternalResponseDTO resumirMensajesProgramados(String requestId,
                                                                            AiScheduledMessageSummaryInternalRequestDTO request);
}
