package com.chat.chat.Controller;

import com.chat.chat.DTO.AiSmartActionHistoryDTO;
import com.chat.chat.DTO.AiSmartActionRequestDTO;
import com.chat.chat.Service.AiService.AiSmartActionHistoryService;
import com.chat.chat.Service.AiService.AiSmartActionService;
import com.chat.chat.Service.AiService.AiSmartActionSuggestionService;
import com.chat.chat.Utils.Constantes;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(Constantes.API_AI)
@CrossOrigin(Constantes.CORS_ANY_ORIGIN)
@Tag(name = "IA", description = "Enrutador inteligente centralizado de acciones IA.")
public class AiSmartActionController {

    private final AiSmartActionService aiSmartActionService;
    private final AiSmartActionHistoryService aiSmartActionHistoryService;
    private final AiSmartActionSuggestionService aiSmartActionSuggestionService;

    public AiSmartActionController(AiSmartActionService aiSmartActionService,
                                   AiSmartActionHistoryService aiSmartActionHistoryService,
                                   AiSmartActionSuggestionService aiSmartActionSuggestionService) {
        this.aiSmartActionService = aiSmartActionService;
        this.aiSmartActionHistoryService = aiSmartActionHistoryService;
        this.aiSmartActionSuggestionService = aiSmartActionSuggestionService;
    }

    @PostMapping(Constantes.AI_SMART_ACTION_PATH)
    @Operation(summary = "Enruta una consulta IA segun su intencion", description = "Clasifica la consulta y ejecuta la rama de negocio adecuada sin heuristicas en frontend.")
    public Object process(@Valid @RequestBody AiSmartActionRequestDTO request) {
        return aiSmartActionService.process(request);
    }

    @GetMapping(Constantes.AI_SMART_ACTION_HISTORY_PATH)
    @Operation(summary = "Historial Smart Action", description = "Devuelve el historial seguro del usuario autenticado ordenado por fecha descendente.")
    public List<AiSmartActionHistoryDTO> listHistory(@RequestParam(value = "limit", required = false) Integer limit) {
        return aiSmartActionHistoryService.listAuthenticatedUserHistory(limit);
    }

    @GetMapping(Constantes.AI_SMART_ACTION_SUGGESTIONS_PATH)
    @Operation(summary = "Sugerencias Smart Action", description = "Devuelve sugerencias basadas en historial del usuario autenticado.")
    public List<String> listSuggestions() {
        return aiSmartActionSuggestionService.listSuggestionsForAuthenticatedUser();
    }

    @DeleteMapping(Constantes.AI_SMART_ACTION_HISTORY_ITEM_PATH)
    @Operation(summary = "Borrar entrada de historial Smart Action", description = "Elimina una entrada del historial si pertenece al usuario autenticado.")
    public ResponseEntity<Void> deleteHistoryEntry(@PathVariable("id") Long id) {
        aiSmartActionHistoryService.deleteAuthenticatedUserHistoryEntry(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping(Constantes.AI_SMART_ACTION_HISTORY_PATH)
    @Operation(summary = "Borrar historial Smart Action", description = "Elimina todo el historial del usuario autenticado.")
    public ResponseEntity<Void> deleteAllHistory() {
        aiSmartActionHistoryService.deleteAllAuthenticatedUserHistory();
        return ResponseEntity.noContent().build();
    }
}
