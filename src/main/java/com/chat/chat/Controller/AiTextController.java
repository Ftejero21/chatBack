package com.chat.chat.Controller;

import com.chat.chat.DTO.AiTextRequestDTO;
import com.chat.chat.DTO.AiTextResponseDTO;
import com.chat.chat.DTO.AiQuickReplyRequestDTO;
import com.chat.chat.DTO.AiQuickReplyResponseDTO;
import com.chat.chat.DTO.AiConversationSummaryRequestDTO;
import com.chat.chat.DTO.AiConversationSummaryResponseDTO;
import com.chat.chat.Service.AiService.AiConversationSummaryService;
import com.chat.chat.Service.AiService.AiQuickReplyService;
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

    public AiTextController(AiTextService aiTextService,
                            AiQuickReplyService aiQuickReplyService,
                            AiConversationSummaryService aiConversationSummaryService) {
        this.aiTextService = aiTextService;
        this.aiQuickReplyService = aiQuickReplyService;
        this.aiConversationSummaryService = aiConversationSummaryService;
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
}
