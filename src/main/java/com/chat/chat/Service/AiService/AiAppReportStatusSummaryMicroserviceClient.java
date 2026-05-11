package com.chat.chat.Service.AiService;

import com.chat.chat.DTO.AiAppReportStatusSummaryInternalRequestDTO;
import com.chat.chat.DTO.AiAppReportStatusSummaryInternalResponseDTO;

public interface AiAppReportStatusSummaryMicroserviceClient {

    /**
     * Calls tejechat-ai-service POST /internal/ai/app-report-status-summary.
     * Returns null if microservice unavailable or returns error — caller falls back to deterministic summary.
     */
    AiAppReportStatusSummaryInternalResponseDTO generarResumen(String requestId,
                                                                AiAppReportStatusSummaryInternalRequestDTO request);
}
