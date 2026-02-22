package com.chat.chat.Controller;

import com.chat.chat.Configuracion.EstadoUsuarioManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.chat.chat.Utils.Constantes;

@RestController
@RequestMapping(Constantes.API_ESTADO)
@CrossOrigin("*")
public class EstadoUsuarioRestController {

    @Autowired
    private EstadoUsuarioManager estadoUsuarioManager;

    @PostMapping(Constantes.USUARIOS_SUB)
    public Map<Long, Boolean> obtenerEstadosUsuarios(@RequestBody List<Long> usuarioIds) {
        Map<Long, Boolean> resultado = new HashMap<>();
        for (Long id : usuarioIds) {
            resultado.put(id, estadoUsuarioManager.estaConectado(id));
        }
        return resultado;
    }
}
