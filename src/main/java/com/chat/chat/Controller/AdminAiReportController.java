package com.chat.chat.Controller;

import com.chat.chat.Service.AdminAiReportService.AdminAiReportService;
import com.chat.chat.Utils.Constantes;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping(Constantes.USUARIO_API)
@CrossOrigin(Constantes.CORS_ANY_ORIGIN)
@Tag(name = "Admin AI Report", description = "Reporte administrativo descargable generado con metricas reales y apoyo de IA.")
public class AdminAiReportController {

    private final AdminAiReportService adminAiReportService;

    public AdminAiReportController(AdminAiReportService adminAiReportService) {
        this.adminAiReportService = adminAiReportService;
    }

    @GetMapping(Constantes.ADMIN_AI_REPORT)
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Descargar reporte administrativo con IA")
    public ResponseEntity<byte[]> descargarReporteIa(
            @RequestParam(value = "fechaInicio", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam(value = "fechaFin", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin) {
        return adminAiReportService.descargarReporteIa(fechaInicio, fechaFin);
    }
}
