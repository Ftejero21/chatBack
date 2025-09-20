package com.chat.chat.Service.UsuarioService;

import com.chat.chat.DTO.UsuarioDTO;

import java.util.List;

public interface UsuarioService {

    UsuarioDTO crearUsuario(UsuarioDTO usuarioDTO);
    List<UsuarioDTO> listarUsuariosActivos();

    UsuarioDTO login(String email, String password);

    UsuarioDTO getById(Long id);

    List<UsuarioDTO> buscarPorNombre(String q);
}
