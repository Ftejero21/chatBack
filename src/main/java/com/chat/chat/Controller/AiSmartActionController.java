package com.chat.chat.Controller;

import com.chat.chat.DTO.AiSmartActionRequestDTO;
import com.chat.chat.Service.AiService.AiSmartActionService;
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
@Tag(name = "IA", description = "Enrutador inteligente centralizado de acciones IA.")
public class AiSmartActionController {

    private final AiSmartActionService aiSmartActionService;

    public AiSmartActionController(AiSmartActionService aiSmartActionService) {
        this.aiSmartActionService = aiSmartActionService;
    }

    @PostMapping(Constantes.AI_SMART_ACTION_PATH)
    @Operation(summary = "Enruta una consulta IA segun su intencion", description = "Clasifica la consulta y ejecuta la rama de negocio adecuada sin heuristicas en frontend.")
    public Object process(@Valid @RequestBody AiSmartActionRequestDTO request) {
        return aiSmartActionService.process(request);
    }
}
