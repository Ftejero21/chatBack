package com.chat.chat.Controller;

import com.chat.chat.DTO.AiUiCustomizationRequestDTO;
import com.chat.chat.DTO.AiUiCustomizationResponseDTO;
import com.chat.chat.Service.AiService.AiUiCustomizationService;
import com.chat.chat.Utils.Constantes;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(Constantes.API_AI)
@CrossOrigin(Constantes.CORS_ANY_ORIGIN)
@Tag(name = "IA", description = "Personalización visual asistida por IA.")
public class AiUiCustomizationController {

    private final AiUiCustomizationService service;

    public AiUiCustomizationController(AiUiCustomizationService service) {
        this.service = service;
    }

    @Operation(summary = "Clasifica una consulta de personalización visual en una intención segura.")
    @PostMapping("/ui-customization/intent")
    public ResponseEntity<AiUiCustomizationResponseDTO> classifyIntent(@Valid @RequestBody AiUiCustomizationRequestDTO request) {
        return ResponseEntity.ok(service.classifyIntent(request));
    }
}
