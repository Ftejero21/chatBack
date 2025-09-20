package com.chat.chat.Controller;


import com.chat.chat.DTO.*;

import com.chat.chat.Service.ChatService.ChatService;
import com.chat.chat.Utils.Constantes;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(Constantes.API_CHAT)
@CrossOrigin("*")
public class ChatController {

    @Autowired
    private ChatService chatService;

    @PostMapping(Constantes.INDIVIDUAL)
    public ChatIndividualDTO crearChatIndividual(@RequestBody ChatIndividualCreateDTO dto) {
        return chatService.crearChatIndividual(dto.getUsuario1Id(), dto.getUsuario2Id());
    }

    @GetMapping(Constantes.GRUPAL_ES_MIEMBRO)
    public EsMiembroDTO esMiembroDeGrupo(@PathVariable Long groupId, @PathVariable Long userId) {
        return chatService.esMiembroDeChatGrupal(groupId, userId);
    }

    @PostMapping(Constantes.GRUPAL)
    public ChatGrupalDTO crearChatGrupal(@RequestBody ChatGrupalDTO dto) {
        return chatService.crearChatGrupal(dto);
    }

    @PostMapping("/{groupId}/usuarios")
    public AddUsuariosGrupoWSResponse anadirUsuariosAGrupo(
            @PathVariable("groupId") Long groupId,
            @RequestBody AddUsuariosGrupoDTO dto) {

        dto.setGroupId(groupId);

        return chatService.anadirUsuariosAGrupo(dto);
    }

    @PostMapping(Constantes.GRUPAL_SALIR)
    public MessagueSalirGrupoDTO salirDeChatGrupal(@RequestBody LeaveGroupRequestDTO dto) {
        // userId viene SIEMPRE en el body (no usamos JWT)
        return chatService.salirDeChatGrupal(dto.getGroupId(), dto.getUserId());
    }

    @GetMapping(Constantes.GRUPALES_USUARIO)
    public List<ChatGrupalDTO> listarGrupalesPorUsuario(@PathVariable Long usuarioId) {
        return chatService.listarChatsGrupalesPorUsuario(usuarioId);
    }

    @GetMapping(Constantes.CHATS_USUARIO)
    public List<Object> listarTodosLosChats(@PathVariable Long usuarioId) {
        return chatService.listarTodosLosChatsDeUsuario(usuarioId);
    }

    @GetMapping(Constantes.LISTAR_MENSAJES_CHAT + "/{chatId}")
    public ResponseEntity<List<MensajeDTO>> listarMensajesPorChatId(@PathVariable Long chatId) {
        List<MensajeDTO> mensajes = chatService.listarMensajesPorChatId(chatId);
        return ResponseEntity.ok(mensajes);
    }

    @GetMapping("/mensajes/grupo/{chatId}")
    public ResponseEntity<List<MensajeDTO>> listarMensajesPorChatGrupal(@PathVariable Long chatId) {
        return ResponseEntity.ok(chatService.listarMensajesPorChatGrupal(chatId));
    }
}
