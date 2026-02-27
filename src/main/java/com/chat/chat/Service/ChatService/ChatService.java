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

    List<MensajeDTO> listarMensajesPorChatId(Long chatId, Integer page, Integer size);
    default List<MensajeDTO> listarMensajesPorChatId(Long chatId) {
        return listarMensajesPorChatId(chatId, 0, 50);
    }

    List<MensajeDTO> listarMensajesPorChatGrupal(Long chatId, Integer page, Integer size);
    default List<MensajeDTO> listarMensajesPorChatGrupal(Long chatId) {
        return listarMensajesPorChatGrupal(chatId, 0, 50);
    }

    ChatMensajeBusquedaPageDTO buscarMensajesEnChat(Long chatId, String q, Integer page, Integer size);

    GroupMediaPageDTO listarMediaPorChatGrupal(Long chatId, String cursor, Integer size, String types);

    List<MensajeDTO> listarMensajesPorChatIdAdmin(Long chatId);

    AddUsuariosGrupoWSResponse anadirUsuariosAGrupo(AddUsuariosGrupoDTO dto);

    List<ChatResumenDTO> listarConversacionesDeUsuario(Long usuarioId);

    GroupDetailDTO obtenerDetalleGrupo(Long groupId);

    void setAdminGrupo(Long groupId, Long targetUserId, boolean makeAdmin);
}
