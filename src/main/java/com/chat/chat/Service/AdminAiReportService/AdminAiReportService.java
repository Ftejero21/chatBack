package com.chat.chat.Service.AdminAiReportService;

import com.chat.chat.DTO.AdminAiReportDataDTO;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;

public interface AdminAiReportService {
    AdminAiReportDataDTO recopilarDatosReporte(LocalDate fechaInicio, LocalDate fechaFin);

    String generarReporteConIa(AdminAiReportDataDTO data);

    byte[] generarArchivoReporte(String reporte, AdminAiReportDataDTO data);

    ResponseEntity<byte[]> descargarReporteIa(LocalDate fechaInicio, LocalDate fechaFin);
}
