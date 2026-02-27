package com.chat.chat.Controller;

import com.chat.chat.Batch.UsuariosReporteScheduler;
import com.chat.chat.Utils.Constantes;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(Constantes.API_REPORTES)
@CrossOrigin(Constantes.CORS_ANY_ORIGIN)
@Tag(name = "Reportes", description = "Ejecucion de exportaciones administrativas por lotes.")
public class UsuariosReporteController {

    private final UsuariosReporteScheduler scheduler;

    public UsuariosReporteController(UsuariosReporteScheduler scheduler) {
        this.scheduler = scheduler;
    }

    @PostMapping(Constantes.USUARIOS_SUB)
    @Operation(summary = "Lanzar reporte de usuarios", description = "Dispara un proceso batch para generar reporte CSV de usuarios.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Reporte lanzado"),
            @ApiResponse(responseCode = "500", description = "Error al lanzar reporte", content = @Content(schema = @Schema(implementation = String.class)))
    })
    public ResponseEntity<String> exportar(
            @Parameter(description = "Si es true, exporta solo usuarios activos")
            @RequestParam(defaultValue = Constantes.DEFAULT_FALSE) boolean soloActivos) {
        try {
            scheduler.lanzar(soloActivos);
            return ResponseEntity.ok(Constantes.MSG_REPORTE_LANZADO + soloActivos + Constantes.MSG_REPORTE_LANZADO_FIN);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Constantes.MSG_ERROR_LANZAR_REPORTE + e.getMessage());
        }
    }
}
