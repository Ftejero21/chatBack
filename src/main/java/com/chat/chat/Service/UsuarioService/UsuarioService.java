package com.chat.chat.Service.UsuarioService;

import com.chat.chat.DTO.AuthRespuestaDTO;
import com.chat.chat.DTO.UsuarioDTO;

import com.chat.chat.DTO.DashboardStatsDTO;
import java.util.List;

public interface UsuarioService {

    UsuarioDTO crearUsuario(UsuarioDTO usuarioDTO);

    AuthRespuestaDTO crearUsuarioConToken(UsuarioDTO dto);

    List<UsuarioDTO> listarUsuariosActivos();

    UsuarioDTO login(String email, String password);

    AuthRespuestaDTO loginConToken(String email, String password);

    UsuarioDTO getById(Long id);

    List<UsuarioDTO> buscarPorNombre(String q);

    void updatePublicKey(Long id, String publicKey);

    void bloquearUsuario(Long bloqueadoId);

    void desbloquearUsuario(Long bloqueadoId);

    boolean existePorEmail(String email);

    void actualizarPasswordPorEmail(String email, String newPassword);

    DashboardStatsDTO getDashboardStats();

    List<UsuarioDTO> listarRecientes();

    void banearUsuario(Long id, String motivo);

    void desbanearAdministrativamente(Long id);
}
