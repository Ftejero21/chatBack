package com.chat.chat.Service.UsuarioService;

import com.chat.chat.DTO.AuthRespuestaDTO;
import com.chat.chat.DTO.UsuarioDTO;
import com.chat.chat.DTO.E2EStateDTO;
import com.chat.chat.DTO.E2ERekeyRequestDTO;

import com.chat.chat.DTO.DashboardStatsDTO;
import org.springframework.data.domain.Page;
import java.util.List;
import com.chat.chat.DTO.ActualizarPerfilDTO;

public interface UsuarioService {

    UsuarioDTO crearUsuario(UsuarioDTO usuarioDTO);

    AuthRespuestaDTO crearUsuarioConToken(UsuarioDTO dto);

    List<UsuarioDTO> listarUsuariosActivos();

    UsuarioDTO login(String email, String password);

    AuthRespuestaDTO loginConToken(String email, String password);

    UsuarioDTO getById(Long id);

    List<UsuarioDTO> buscarPorNombre(String q);

    void updatePublicKey(Long id, String publicKey);
    E2EStateDTO getE2EState(Long id);
    E2EStateDTO rekeyE2E(Long id, E2ERekeyRequestDTO request);

    void bloquearUsuario(Long bloqueadoId);

    void desbloquearUsuario(Long bloqueadoId);

    boolean existePorEmail(String email);

    void actualizarPasswordPorEmail(String email, String newPassword);

    DashboardStatsDTO getDashboardStats();
    DashboardStatsDTO getDashboardStats(String tz);

    Page<UsuarioDTO> listarRecientes(int page, int size);
    UsuarioDTO actualizarPerfil(ActualizarPerfilDTO dto);
    void solicitarCodigoCambioPassword();
    void cambiarPasswordConCodigo(String code, String newPassword);

    void banearUsuario(Long id, String motivo);

    void desbanearAdministrativamente(Long id);
    void desbanearAdministrativamente(Long id, String motivo);
}
