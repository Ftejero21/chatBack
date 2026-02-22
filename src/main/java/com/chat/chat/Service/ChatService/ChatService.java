package com.chat.chat.Service.ChatService;


import com.chat.chat.DTO.*;
import com.chat.chat.DTO.*;


import java.util.List;

public interface ChatService {

    ChatIndividualDTO crearChatIndividual(Long usuario1Id, Long usuario2Id);
    ChatGrupalDTO crearChatGrupal(ChatGrupalDTO dto);

    MessagueSalirGrupoDTO salirDeChatGrupal(Long groupId, Long userId);

    EsMiembroDTO esMiembroDeChatGrupal(Long groupId, Long userId);
    List<ChatGrupalDTO> listarChatsGrupalesPorUsuario(Long usuarioId);

    List<Object> listarTodosLosChatsDeUsuario(Long usuarioId);

    List<MensajeDTO> listarMensajesPorChatId(Long chatId);

    List<MensajeDTO> listarMensajesPorChatGrupal(Long chatId);

    AddUsuariosGrupoWSResponse anadirUsuariosAGrupo(AddUsuariosGrupoDTO dto);

    List<ChatResumenDTO> listarConversacionesDeUsuario(Long usuarioId);
}
