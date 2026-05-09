package com.chat.chat.Service.AiService;

import com.chat.chat.DTO.AiReportAnalysisInternalRequestDTO;
import com.chat.chat.DTO.AiReportAnalysisInternalResponseDTO;

public interface AiReportAnalysisMicroserviceClient {

    AiReportAnalysisInternalResponseDTO analizarDenuncia(String requestId, AiReportAnalysisInternalRequestDTO request);
}
