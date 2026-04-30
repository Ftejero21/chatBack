package com.chat.chat.Service.AiService;

import com.chat.chat.DTO.AiReportAnalysisRequestDTO;
import com.chat.chat.DTO.AiReportAnalysisResponseDTO;

public interface AiReportAnalysisService {

    AiReportAnalysisResponseDTO analizarDenuncia(AiReportAnalysisRequestDTO request);
}
