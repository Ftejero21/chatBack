package com.chat.chat.Controller;

import com.chat.chat.DTO.AddUsuariosGrupoDTO;
import com.chat.chat.DTO.AddUsuariosGrupoWSResponse;
import com.chat.chat.DTO.ChatGrupalDTO;
import com.chat.chat.DTO.ChatIndividualCreateDTO;
import com.chat.chat.DTO.ChatIndividualDTO;
import com.chat.chat.DTO.ChatMensajeBusquedaPageDTO;
import com.chat.chat.DTO.ChatResumenDTO;
import com.chat.chat.DTO.EsMiembroDTO;
import com.chat.chat.DTO.GroupDetailDTO;
import com.chat.chat.DTO.GroupMediaPageDTO;
import com.chat.chat.DTO.LeaveGroupRequestDTO;
import com.chat.chat.DTO.MensajeDTO;
import com.chat.chat.DTO.MessagueSalirGrupoDTO;
import com.chat.chat.Exceptions.ApiError;
import com.chat.chat.Service.ChatService.ChatService;
import com.chat.chat.Utils.Constantes;
import com.chat.chat.Utils.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(Constantes.API_CHAT)
@CrossOrigin(Constantes.CORS_ANY_ORIGIN)
@Tag(name = "Chats", description = "Endpoints para crear y gestionar conversaciones individuales y grupales.")
public class ChatController {

    @Autowired
    private ChatService chatService;

    @Autowired
    private SecurityUtils securityUtils;

    @PostMapping(Constantes.INDIVIDUAL)
    @Operation(summary = "Crear chat individual", description = "Crea o recupera un chat individual entre dos usuarios.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Chat individual listo", content = @Content(schema = @Schema(implementation = ChatIndividualDTO.class))),
            @ApiResponse(responseCode = "400", description = "No se puede crear chat", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public ChatIndividualDTO crearChatIndividual(@RequestBody ChatIndividualCreateDTO dto) {
        return chatService.crearChatIndividual(dto.getUsuario1Id(), dto.getUsuario2Id());
    }

    @GetMapping(Constantes.GRUPAL_ES_MIEMBRO)
    @Operation(summary = "Validar membresia de grupo", description = "Indica si un usuario pertenece al grupo solicitado.")
    @ApiResponse(responseCode = "200", description = "Resultado de membresia", content = @Content(schema = @Schema(implementation = EsMiembroDTO.class)))
    public EsMiembroDTO esMiembroDeGrupo(
            @Parameter(description = "ID del grupo") @PathVariable("groupId") Long groupId,
            @Parameter(description = "ID del usuario") @PathVariable("userId") Long userId) {
        return chatService.esMiembroDeChatGrupal(groupId, userId);
    }

    @GetMapping(Constantes.GRUPAL_DETALLE)
    @Operation(summary = "Detalle de grupo", description = "Devuelve metadatos del grupo y sus miembros.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Detalle obtenido", content = @Content(schema = @Schema(implementation = GroupDetailDTO.class))),
            @ApiResponse(responseCode = "404", description = "Grupo no encontrado", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public GroupDetailDTO detalleGrupo(@PathVariable("groupId") Long groupId) {
        return chatService.obtenerDetalleGrupo(groupId);
    }

    @PostMapping(Constantes.GRUPAL_ADMIN_ADD)
    @Operation(summary = "Asignar admin de grupo", description = "Promueve un miembro como administrador del grupo.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Rol actualizado"),
            @ApiResponse(responseCode = "403", description = "Sin permisos", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Grupo o usuario no encontrado", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public void addAdmin(@PathVariable("groupId") Long groupId, @PathVariable("userId") Long userId) {
        chatService.setAdminGrupo(groupId, userId, true);
    }

    @DeleteMapping(Constantes.GRUPAL_ADMIN_REMOVE)
    @Operation(summary = "Quitar admin de grupo", description = "Retira privilegios de administrador a un miembro del grupo.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Rol actualizado"),
            @ApiResponse(responseCode = "403", description = "Sin permisos", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Grupo o usuario no encontrado", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public void removeAdmin(@PathVariable("groupId") Long groupId, @PathVariable("userId") Long userId) {
        chatService.setAdminGrupo(groupId, userId, false);
    }

    @PostMapping(Constantes.GRUPAL)
    @Operation(summary = "Crear chat grupal", description = "Crea un grupo nuevo con nombre, foto y miembros iniciales.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Grupo creado", content = @Content(schema = @Schema(implementation = ChatGrupalDTO.class))),
            @ApiResponse(responseCode = "400", description = "Datos invalidos", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public ChatGrupalDTO crearChatGrupal(@RequestBody ChatGrupalDTO dto) {
        return chatService.crearChatGrupal(dto);
    }

    @PostMapping(Constantes.GRUPAL_ADD_USUARIOS)
    @Operation(summary = "Anadir usuarios al grupo", description = "Invita o incorpora una lista de usuarios a un grupo existente.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Usuarios anadidos", content = @Content(schema = @Schema(implementation = AddUsuariosGrupoWSResponse.class))),
            @ApiResponse(responseCode = "403", description = "Sin permisos para gestionar miembros", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Grupo no encontrado", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public AddUsuariosGrupoWSResponse anadirUsuariosAGrupo(
            @PathVariable("groupId") Long groupId,
            @RequestBody AddUsuariosGrupoDTO dto) {

        dto.setGroupId(groupId);

        return chatService.anadirUsuariosAGrupo(dto);
    }

    @GetMapping(Constantes.ADMIN_USUARIO_CHATS)
    @Operation(summary = "Listar chats de usuario (admin)", description = "Devuelve todas las conversaciones de un usuario para auditoria administrativa.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Conversaciones obtenidas"),
            @ApiResponse(responseCode = "403", description = "Solo administradores", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public List<ChatResumenDTO> getChatsUsuario(@PathVariable("id") Long id) {
        return chatService.listarConversacionesDeUsuario(id);
    }

    @PostMapping(Constantes.GRUPAL_SALIR)
    @Operation(summary = "Salir de grupo", description = "Permite al usuario autenticado abandonar un grupo.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Salida procesada", content = @Content(schema = @Schema(implementation = MessagueSalirGrupoDTO.class))),
            @ApiResponse(responseCode = "404", description = "Grupo no encontrado", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public MessagueSalirGrupoDTO salirDeChatGrupal(@RequestBody LeaveGroupRequestDTO dto) {
        Long authenticatedUserId = securityUtils.getAuthenticatedUserId();
        return chatService.salirDeChatGrupal(dto.getGroupId(), authenticatedUserId);
    }

    @GetMapping(Constantes.GRUPALES_USUARIO)
    @Operation(summary = "Listar grupos por usuario", description = "Obtiene chats grupales donde participa un usuario.")
    @ApiResponse(responseCode = "200", description = "Grupos obtenidos")
    public List<ChatGrupalDTO> listarGrupalesPorUsuario(@PathVariable("usuarioId") Long usuarioId) {
        return chatService.listarChatsGrupalesPorUsuario(usuarioId);
    }

    @GetMapping(Constantes.CHATS_USUARIO)
    @Operation(summary = "Listar todos los chats por usuario", description = "Devuelve juntos chats individuales y grupales de un usuario.")
    @ApiResponse(responseCode = "200", description = "Chats obtenidos")
    public List<Object> listarTodosLosChats(@PathVariable("usuarioId") Long usuarioId) {
        return chatService.listarTodosLosChatsDeUsuario(usuarioId);
    }

    @GetMapping(Constantes.LISTAR_MENSAJES_CHAT + "/{chatId}")
    @Operation(summary = "Historial de chat", description = "Lista mensajes de un chat (individual o grupal) con paginacion basica.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Mensajes obtenidos"),
            @ApiResponse(responseCode = "404", description = "Chat no encontrado", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<List<MensajeDTO>> listarMensajesPorChatId(
            @PathVariable("chatId") Long chatId,
            @Parameter(description = "Pagina, inicia en 0") @RequestParam(value = "page", defaultValue = "0") Integer page,
            @Parameter(description = "Cantidad por pagina") @RequestParam(value = "size", defaultValue = "50") Integer size) {
        List<MensajeDTO> mensajes = chatService.listarMensajesPorChatId(chatId, page, size);
        return ResponseEntity.ok(mensajes);
    }

    @GetMapping(Constantes.MENSAJES_GRUPO)
    @Operation(summary = "Historial de grupo", description = "Lista mensajes de un chat grupal con paginacion.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Mensajes grupales obtenidos"),
            @ApiResponse(responseCode = "404", description = "Grupo no encontrado", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<List<MensajeDTO>> listarMensajesPorChatGrupal(
            @PathVariable("chatId") Long chatId,
            @RequestParam(value = "page", defaultValue = "0") Integer page,
            @RequestParam(value = "size", defaultValue = "50") Integer size) {
        return ResponseEntity.ok(chatService.listarMensajesPorChatGrupal(chatId, page, size));
    }

    @GetMapping(Constantes.MENSAJES_BUSCAR_CHAT)
    @Operation(summary = "Buscar mensajes en chat", description = "Busca texto dentro del historial de un chat y devuelve resultados paginados.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Busqueda completada", content = @Content(schema = @Schema(implementation = ChatMensajeBusquedaPageDTO.class))),
            @ApiResponse(responseCode = "400", description = "Consulta invalida", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<ChatMensajeBusquedaPageDTO> buscarMensajesPorChat(
            @PathVariable("chatId") Long chatId,
            @Parameter(description = "Texto a buscar") @RequestParam(value = "q") String q,
            @RequestParam(value = "page", defaultValue = "0") Integer page,
            @RequestParam(value = "size", defaultValue = "20") Integer size) {
        return ResponseEntity.ok(chatService.buscarMensajesEnChat(chatId, q, page, size));
    }

    @GetMapping(Constantes.GRUPAL_MEDIA)
    @Operation(summary = "Feed multimedia de grupo", description = "Lista audio, imagen, video y archivos de un grupo usando cursor para navegar.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Feed multimedia obtenido", content = @Content(schema = @Schema(implementation = GroupMediaPageDTO.class))),
            @ApiResponse(responseCode = "404", description = "Grupo no encontrado", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<GroupMediaPageDTO> listarMediaPorChatGrupal(
            @PathVariable("chatId") Long chatId,
            @Parameter(description = "Cursor de paginacion para cargar mas") @RequestParam(value = "cursor", required = false) String cursor,
            @Parameter(description = "Tamano de pagina; si no se envia usa valor por defecto") @RequestParam(value = "size", required = false) Integer size,
            @Parameter(description = "Tipos separados por coma: AUDIO,IMAGE,VIDEO,FILE") @RequestParam(value = "types", required = false) String types) {
        return ResponseEntity.ok(chatService.listarMediaPorChatGrupal(chatId, cursor, size, types));
    }

    @GetMapping(Constantes.ADMIN_CHAT_MENSAJES)
    @Operation(summary = "Mensajes de chat (admin)", description = "Devuelve historial de un chat para revision administrativa.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Mensajes obtenidos"),
            @ApiResponse(responseCode = "403", description = "Solo administradores", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<List<MensajeDTO>> listarMensajesPorChatIdAdmin(@PathVariable("chatId") Long chatId) {
        return ResponseEntity.ok(chatService.listarMensajesPorChatIdAdmin(chatId));
    }
}
