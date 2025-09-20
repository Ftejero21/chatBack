package com.chat.chat.Controller;

import com.chat.chat.Service.MensajeriaService.MensajeriaService;
import com.chat.chat.Utils.Constantes;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping(Constantes.API_MENSAJERIA)
@CrossOrigin("*")
public class MensajeriaController {

    @Autowired
    private MensajeriaService mensajeriaService;


    @PostMapping("/mensajes/marcar-leidos")
    public ResponseEntity<Void> marcarMensajesComoLeidos(@RequestBody List<Long> ids) {
        mensajeriaService.marcarMensajesComoLeidos(ids);
        return ResponseEntity.ok().build();
    }
}
