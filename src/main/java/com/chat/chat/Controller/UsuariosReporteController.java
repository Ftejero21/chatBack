package com.chat.chat.Controller;

import com.chat.chat.Batch.UsuariosReporteScheduler;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.chat.chat.Utils.Constantes;

@RestController
@RequestMapping(Constantes.API_REPORTES)
@CrossOrigin("*")
public class UsuariosReporteController {

    private final UsuariosReporteScheduler scheduler;

    public UsuariosReporteController(UsuariosReporteScheduler scheduler) {
        this.scheduler = scheduler;
    }

    @PostMapping(Constantes.USUARIOS_SUB)
    public ResponseEntity<String> exportar(@RequestParam(defaultValue = "false") boolean soloActivos) {
        try {
            scheduler.lanzar(soloActivos);
            return ResponseEntity.ok(Constantes.MSG_REPORTE_LANZADO + soloActivos + Constantes.MSG_REPORTE_LANZADO_FIN);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Constantes.MSG_ERROR_LANZAR_REPORTE + e.getMessage());
        }
    }

}
