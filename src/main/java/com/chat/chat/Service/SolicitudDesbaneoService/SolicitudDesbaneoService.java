package com.chat.chat.Service.SolicitudDesbaneoService;

import com.chat.chat.DTO.SolicitudDesbaneoCreateDTO;
import com.chat.chat.DTO.SolicitudDesbaneoCreateResponseDTO;
import com.chat.chat.DTO.SolicitudDesbaneoDTO;
import com.chat.chat.DTO.SolicitudDesbaneoEstadoUpdateDTO;
import com.chat.chat.DTO.SolicitudDesbaneoStatsDTO;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;

public interface SolicitudDesbaneoService {
    SolicitudDesbaneoCreateResponseDTO crearSolicitud(SolicitudDesbaneoCreateDTO request);
    SolicitudDesbaneoDTO crearReporteChatCerrado(Long chatId, String motivo, String ip, String userAgent);

    Page<SolicitudDesbaneoDTO> listarSolicitudes(String estado, String estados, String tipoReporte, Integer page, Integer size, String sort);

    SolicitudDesbaneoDTO obtenerSolicitud(Long id);

    SolicitudDesbaneoDTO actualizarEstado(Long id, SolicitudDesbaneoEstadoUpdateDTO request, HttpServletRequest httpRequest);

    SolicitudDesbaneoStatsDTO obtenerStats();
    SolicitudDesbaneoStatsDTO obtenerStats(String tz);
}
