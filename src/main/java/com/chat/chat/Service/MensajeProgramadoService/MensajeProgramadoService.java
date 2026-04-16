package com.chat.chat.Service.MensajeProgramadoService;

import com.chat.chat.DTO.AdminDirectMessageScheduledRequestDTO;
import com.chat.chat.DTO.BulkEmailRequestDTO;
import com.chat.chat.DTO.MensajeProgramadoDTO;
import com.chat.chat.DTO.ProgramarMensajeRequestDTO;
import com.chat.chat.DTO.ProgramarMensajeResponseDTO;
import com.chat.chat.DTO.ScheduledBatchResponseDTO;
import com.chat.chat.Utils.EstadoMensajeProgramado;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.List;

public interface MensajeProgramadoService {
    ProgramarMensajeResponseDTO crearMensajesProgramados(ProgramarMensajeRequestDTO request);

    ScheduledBatchResponseDTO crearMensajesDirectosAdminProgramados(AdminDirectMessageScheduledRequestDTO request);

    ScheduledBatchResponseDTO crearBulkEmailsProgramados(BulkEmailRequestDTO request, List<MultipartFile> attachments);

    List<MensajeProgramadoDTO> listarMensajesProgramados(EstadoMensajeProgramado status);

    MensajeProgramadoDTO cancelarMensajeProgramado(Long id);

    List<Long> reclamarMensajesVencidos(Instant ahora, String lockToken, int limite, int lockSeconds);

    void procesarMensajeProgramado(Long id, String lockToken);
}
