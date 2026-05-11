package com.chat.chat.Service.SolicitudDesbaneoService;

import com.chat.chat.DTO.SolicitudDesbaneoCreateDTO;
import com.chat.chat.DTO.SolicitudDesbaneoCreateResponseDTO;
import com.chat.chat.DTO.SolicitudDesbaneoDTO;
import com.chat.chat.DTO.SolicitudDesbaneoEstadoUpdateDTO;
import com.chat.chat.DTO.SolicitudDesbaneoStatsDTO;
import com.chat.chat.Utils.ReporteTipo;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;

public interface SolicitudDesbaneoService {
    SolicitudDesbaneoCreateResponseDTO crearSolicitud(SolicitudDesbaneoCreateDTO request);
    SolicitudDesbaneoDTO crearReporteChatCerrado(Long chatId, String motivo, String ip, String userAgent);

    /**
     * Creates an app-related report (incident, complaint, improvement, bug, suggestion, unban request, other)
     * triggered from AI search-intent classification. usuarioId comes from authenticated user — never trust client.
     */
    SolicitudDesbaneoDTO crearReporteDesdeAi(Long usuarioId, ReporteTipo tipo, String motivo);
    SolicitudDesbaneoDTO crearReporteDesdeAi(Long usuarioId, ReporteTipo tipo, String motivo,
                                             String imagenMimeType, String imagenNombreArchivo, byte[] imagenContenido);

    Page<SolicitudDesbaneoDTO> listarSolicitudes(String estado, String estados, String tipoReporte, Integer page, Integer size, String sort);

    SolicitudDesbaneoDTO obtenerSolicitud(Long id);

    SolicitudDesbaneoDTO actualizarEstado(Long id, SolicitudDesbaneoEstadoUpdateDTO request, HttpServletRequest httpRequest);

    ResponseEntity<Resource> obtenerImagenReporteAdmin(Long id);

    ResponseEntity<Resource> obtenerImagenReporteUsuario(Long id);

    SolicitudDesbaneoStatsDTO obtenerStats();
    SolicitudDesbaneoStatsDTO obtenerStats(String tz);
}
