package com.chat.chat.Controller;

import com.chat.chat.DTO.AiTextRequestDTO;
import com.chat.chat.DTO.AiTextResponseDTO;
import com.chat.chat.DTO.AiQuickReplyRequestDTO;
import com.chat.chat.DTO.AiQuickReplyResponseDTO;
import com.chat.chat.DTO.AiConversationSummaryRequestDTO;
import com.chat.chat.DTO.AiConversationSummaryResponseDTO;
import com.chat.chat.DTO.AiEncryptedConversationSummaryRequestDTO;
import com.chat.chat.DTO.AiEncryptedResponseDTO;
import com.chat.chat.DTO.AiPollDraftRequestDTO;
import com.chat.chat.DTO.AiPollDraftResponseDTO;
import com.chat.chat.DTO.AiReportAnalysisRequestDTO;
import com.chat.chat.DTO.AiReportAnalysisResponseDTO;
import com.chat.chat.Service.AiService.AiConversationSummaryService;
import com.chat.chat.Service.AiService.AiEncryptedConversationSummaryService;
import com.chat.chat.Service.AiService.AiPollDraftService;
import com.chat.chat.Service.AiService.AiQuickReplyService;
import com.chat.chat.Service.AiService.AiReportAnalysisService;
import com.chat.chat.Service.AiService.AiTextService;
import com.chat.chat.Utils.Constantes;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(Constantes.API_AI)
@CrossOrigin(Constantes.CORS_ANY_ORIGIN)
@Tag(name = "IA", description = "Funciones auxiliares de texto con DeepSeek.")
public class AiTextController {

    private final AiTextService aiTextService;
    private final AiQuickReplyService aiQuickReplyService;
    private final AiConversationSummaryService aiConversationSummaryService;
    private final AiEncryptedConversationSummaryService aiEncryptedConversationSummaryService;
    private final AiPollDraftService aiPollDraftService;
    private final AiReportAnalysisService aiReportAnalysisService;

    public AiTextController(AiTextService aiTextService,
                            AiQuickReplyService aiQuickReplyService,
                            AiConversationSummaryService aiConversationSummaryService,
                            AiEncryptedConversationSummaryService aiEncryptedConversationSummaryService,
                            AiPollDraftService aiPollDraftService,
                            AiReportAnalysisService aiReportAnalysisService) {
        this.aiTextService = aiTextService;
        this.aiQuickReplyService = aiQuickReplyService;
        this.aiConversationSummaryService = aiConversationSummaryService;
        this.aiEncryptedConversationSummaryService = aiEncryptedConversationSummaryService;
        this.aiPollDraftService = aiPollDraftService;
        this.aiReportAnalysisService = aiReportAnalysisService;
    }

    @PostMapping(Constantes.AI_TEXT_PATH)
    @Operation(summary = "Procesar texto con IA", description = "Corrige, reformula, resume o completa texto del usuario con DeepSeek.")
    public AiTextResponseDTO procesarTexto(@Valid @RequestBody AiTextRequestDTO request) {
        return aiTextService.procesarTexto(request);
    }

    @PostMapping("/respuestas-rapidas")
    @Operation(summary = "Generar respuestas rapidas", description = "Genera exactamente 3 sugerencias cortas y seguras para responder al ultimo mensaje recibido.")
    public AiQuickReplyResponseDTO generarRespuestasRapidas(@Valid @RequestBody AiQuickReplyRequestDTO request) {
        return aiQuickReplyService.generarSugerencias(request);
    }

    @PostMapping("/resumir-conversacion")
    @Operation(summary = "Resumir conversacion", description = "Resume los ultimos mensajes relevantes de un chat individual o grupal.")
    public AiConversationSummaryResponseDTO resumirConversacion(@Valid @RequestBody AiConversationSummaryRequestDTO request) {
        return aiConversationSummaryService.resumirConversacion(request);
    }

    @PostMapping(Constantes.AI_SUMMARY_ENCRYPTED_PATH)
    @Operation(summary = "Resumir conversacion cifrada", description = "Descifra contexto solo en memoria, genera resumen con IA y devuelve el resultado cifrado para el usuario autenticado.")
    public AiEncryptedResponseDTO resumirConversacionCifrada(@Valid @RequestBody AiEncryptedConversationSummaryRequestDTO request) {
        return aiEncryptedConversationSummaryService.resumirConversacionCifrada(request);
    }

    @PostMapping(Constantes.AI_POLL_DRAFT_PATH)
    @Operation(summary = "Generar borrador de encuesta", description = "Genera un borrador de encuesta para un chat grupal sin crearla ni publicarla.")
    public AiPollDraftResponseDTO generarBorradorEncuesta(@Valid @RequestBody AiPollDraftRequestDTO request) {
        return aiPollDraftService.generarBorrador(request);
    }

    @PostMapping(Constantes.AI_REPORT_ANALYSIS_PATH)
    @Operation(summary = "Analizar denuncia", description = "Analiza mensajes recientes de un chat individual para sugerir un borrador de denuncia sin crearla ni enviarla.")
    public AiReportAnalysisResponseDTO analizarDenuncia(@Valid @RequestBody AiReportAnalysisRequestDTO request) {
        return aiReportAnalysisService.analizarDenuncia(request);
    }
}
