package com.chat.chat.Service.AiService;

import com.chat.chat.DTO.AiAppReportResolutionNoteInternalRequestDTO;
import com.chat.chat.DTO.AiAppReportResolutionNoteInternalResponseDTO;

public interface AiAppReportResolutionNoteMicroserviceClient {

    AiAppReportResolutionNoteInternalResponseDTO generateResolutionNote(String requestId,
                                                                        AiAppReportResolutionNoteInternalRequestDTO request);
}
