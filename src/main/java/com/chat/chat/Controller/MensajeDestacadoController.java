package com.chat.chat.Controller;

import com.chat.chat.DTO.MensajesDestacadosPageDTO;
import com.chat.chat.Exceptions.ApiError;
import com.chat.chat.Service.MensajeriaService.MensajeriaService;
import com.chat.chat.Utils.Constantes;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(Constantes.API_MENSAJES)
@CrossOrigin(Constantes.CORS_ANY_ORIGIN)
@Tag(name = "Mensajes Destacados", description = "Gestion de destacados por usuario autenticado.")
public class MensajeDestacadoController {

    @Autowired
    private MensajeriaService mensajeriaService;

    @PostMapping(Constantes.MENSAJE_DESTACAR)
    @Operation(summary = "Destacar mensaje", description = "Marca un mensaje como destacado para el usuario autenticado.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Operación idempotente aplicada"),
            @ApiResponse(responseCode = "403", description = "Sin permisos sobre el mensaje/chat", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Mensaje no encontrado", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<Void> destacarMensaje(@PathVariable("mensajeId") Long mensajeId) {
        mensajeriaService.destacarMensaje(mensajeId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping(Constantes.MENSAJE_DESTACAR)
    @Operation(summary = "Quitar destacado", description = "Elimina el destacado de un mensaje para el usuario autenticado.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Operación idempotente aplicada"),
            @ApiResponse(responseCode = "403", description = "Sin permisos sobre el mensaje/chat", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Mensaje no encontrado", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<Void> quitarDestacado(@PathVariable("mensajeId") Long mensajeId) {
        mensajeriaService.quitarDestacado(mensajeId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping(Constantes.MENSAJES_DESTACADOS)
    @Operation(summary = "Listar destacados", description = "Devuelve la lista paginada de mensajes destacados del usuario autenticado.")
    @ApiResponse(responseCode = "200", description = "Destacados obtenidos")
    public ResponseEntity<MensajesDestacadosPageDTO> listarDestacados(
            @RequestParam(value = "page", defaultValue = "0") Integer page,
            @RequestParam(value = "size", defaultValue = "10") Integer size,
            @RequestParam(value = "sort", required = false) String sort) {
        return ResponseEntity.ok(mensajeriaService.listarMensajesDestacadosUsuario(page, size, sort));
    }
}
