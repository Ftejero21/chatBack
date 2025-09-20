package com.chat.chat.Controller;

import com.chat.chat.Batch.UsuariosReporteScheduler;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reportes")
@CrossOrigin("*")
public class UsuariosReporteController {

    private final UsuariosReporteScheduler scheduler;

    public UsuariosReporteController(UsuariosReporteScheduler scheduler) {
        this.scheduler = scheduler;
    }

    @PostMapping("/usuarios")
    public ResponseEntity<String> exportar(@RequestParam(defaultValue = "false") boolean soloActivos) {
        try {
            scheduler.lanzar(soloActivos);
            return ResponseEntity.ok("Reporte de usuarios lanzado (soloActivos=" + soloActivos + ").");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error al lanzar reporte: " + e.getMessage());
        }
    }


}
