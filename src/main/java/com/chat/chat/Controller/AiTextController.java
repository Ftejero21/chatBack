package com.chat.chat.Controller;

import com.chat.chat.DTO.AiTextRequestDTO;
import com.chat.chat.DTO.AiTextResponseDTO;
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

    public AiTextController(AiTextService aiTextService) {
        this.aiTextService = aiTextService;
    }

    @PostMapping(Constantes.AI_TEXT_PATH)
    @Operation(summary = "Procesar texto con IA", description = "Corrige, reformula, resume o completa texto del usuario con DeepSeek.")
    public AiTextResponseDTO procesarTexto(@Valid @RequestBody AiTextRequestDTO request) {
        return aiTextService.procesarTexto(request);
    }
}
