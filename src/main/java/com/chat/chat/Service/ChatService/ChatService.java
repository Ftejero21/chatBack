package com.chat.chat.Service.ChatService;


import com.chat.chat.DTO.*;


import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.web.multipart.MultipartFile;

public interface ChatService {

    ChatIndividualDTO crearChatIndividual(Long usuario1Id, Long usuario2Id);
    AdminDirectMessageResponseDTO enviarMensajeDirectoAdmin(AdminDirectMessageRequestDTO request);
    AdminDirectMessageResponseDTO enviarMensajeDirectoAdminComoSistema(Long adminUserId, AdminDirectMessageRequestDTO request);
    BulkEmailResponseDTO enviarBulkEmailAdmin(BulkEmailRequestDTO request, List<MultipartFile> attachments);
    ChatGrupalDTO crearChatGrupal(ChatGrupalDTO dto);

    MessagueSalirGrupoDTO salirDeChatGrupal(Long groupId, Long userId);
    GroupMemberExpulsionResponseDTO expulsarMiembroDeGrupo(Long groupId, Long targetUserId);

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
    ChatClearResponseDTO clearChat(Long chatId);
    ChatMuteStateDTO muteChat(Long chatId, ChatMuteRequestDTO request);
    ChatMuteStateDTO unmuteChat(Long chatId);
    List<ChatMuteStateDTO> listarChatsMuteadosActivos();
    UserPinnedChatResponseDTO setPinnedChat(UserPinnedChatRequestDTO request);
    UserPinnedChatResponseDTO getPinnedChat();
    UserChatFavoriteResponseDTO getFavoriteChat();
    UserChatFavoriteResponseDTO setFavoriteChat(Long chatId);
    UserChatFavoriteResponseDTO removeFavoriteChat(Long chatId);
    ChatPinnedMessageDTO getPinnedMessage(Long chatId);
    ChatPinnedMessageDTO pinMessage(Long chatId, ChatPinMessageRequestDTO request);
    void unpinMessage(Long chatId);

    GroupMediaPageDTO listarMediaPorChatGrupal(Long chatId, String cursor, Integer size, String types);

    List<MensajeDTO> listarMensajesPorChatIdAdmin(Long chatId, Boolean includeExpired);
    default List<MensajeDTO> listarMensajesPorChatIdAdmin(Long chatId) {
        return listarMensajesPorChatIdAdmin(chatId, false);
    }

    AddUsuariosGrupoWSResponse anadirUsuariosAGrupo(AddUsuariosGrupoDTO dto);

    List<ChatResumenDTO> listarConversacionesDeUsuario(Long usuarioId, Boolean includeExpired);
    default List<ChatResumenDTO> listarConversacionesDeUsuario(Long usuarioId) {
        return listarConversacionesDeUsuario(usuarioId, false);
    }

    GroupDetailDTO obtenerDetalleGrupo(Long groupId);
    GroupDetailDTO actualizarMetadataGrupo(Long groupId, GroupMetadataUpdateDTO dto);
    Page<AdminGroupListDTO> listarGruposAdmin(Integer page, Integer size);
    ChatCloseStateDTO cerrarChatGrupalComoAdmin(Long chatId, String motivo, String ip, String userAgent);
    ChatCloseStateDTO reabrirChatGrupalComoAdmin(Long chatId, String ip, String userAgent);

    void setAdminGrupo(Long groupId, Long targetUserId, boolean makeAdmin);
}
