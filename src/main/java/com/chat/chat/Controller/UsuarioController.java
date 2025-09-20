package com.chat.chat.Controller;


import com.chat.chat.DTO.LoginRequestDTO;
import com.chat.chat.DTO.UsuarioDTO;
import com.chat.chat.Service.UsuarioService.UsuarioService;
import com.chat.chat.Utils.Constantes;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(Constantes.USUARIO_API)
@CrossOrigin("*")
public class UsuarioController {


    @Autowired
    private UsuarioService usuarioService;

    @PostMapping(Constantes.LOGIN)
    public UsuarioDTO login(@RequestBody LoginRequestDTO dto) {
        return usuarioService.login(dto.getEmail(), dto.getPassword());
    }

    @PostMapping(Constantes.REGISTRO)
    public UsuarioDTO crearUsuario(@RequestBody UsuarioDTO dto) {
        return usuarioService.crearUsuario(dto);
    }

    @GetMapping("/activos")
    public List<UsuarioDTO> listarActivos() {
        return usuarioService.listarUsuariosActivos();
    }

    @GetMapping("/{id}")
    public UsuarioDTO getById(@PathVariable Long id) {
        return usuarioService.getById(id);
    }

    @GetMapping("/buscar")
    public List<UsuarioDTO> buscar(@RequestParam("q") String q) {
        return usuarioService.buscarPorNombre(q);
    }


}
