package com.chat.chat.Service.ChatService;

import com.chat.chat.DTO.ChatCloseStateDTO;
import com.chat.chat.DTO.ChatResumenDTO;
import com.chat.chat.Entity.ChatGrupalEntity;
import com.chat.chat.Entity.UsuarioEntity;
import com.chat.chat.Exceptions.RecursoNoEncontradoException;
import com.chat.chat.Repository.ChatGrupalRepository;
import com.chat.chat.Repository.ChatIndividualRepository;
import com.chat.chat.Repository.MensajeRepository;
import com.chat.chat.Repository.UsuarioRepository;
import com.chat.chat.Utils.Constantes;
import com.chat.chat.Utils.SecurityUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.AccessDeniedException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatServiceImplGroupCloseTest {

    @Mock
    private UsuarioRepository usuarioRepo;
    @Mock
    private ChatGrupalRepository chatGrupalRepo;
    @Mock
    private ChatIndividualRepository chatIndRepo;
    @Mock
    private MensajeRepository mensajeRepository;
    @Mock
    private SecurityUtils securityUtils;
    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private ChatServiceImpl service;

    @Test
    void adminPuedeCerrarYEmiteEventoWs() {
        Long adminId = 9L;
        Long chatId = 120L;
        ChatGrupalEntity chat = activeGroup(chatId);

        when(securityUtils.hasRole(Constantes.ADMIN)).thenReturn(true);
        when(securityUtils.getAuthenticatedUserId()).thenReturn(adminId);
        when(chatGrupalRepo.findByIdWithUsuariosForUpdate(chatId)).thenReturn(Optional.of(chat));
        when(chatGrupalRepo.save(any(ChatGrupalEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        ChatCloseStateDTO out = service.cerrarChatGrupalComoAdmin(chatId, "  Mantenimiento  ", "127.0.0.1", "JUnit");

        assertTrue(out.isOk());
        assertTrue(out.isClosed());
        assertTrue(out.isCerrado());
        assertEquals(chatId, out.getChatId());
        assertEquals("Mantenimiento", out.getReason());
        assertNotNull(out.getClosedAt());
        assertEquals(adminId, out.getClosedByAdminId());
        verify(chatGrupalRepo, times(1)).save(any(ChatGrupalEntity.class));
        verify(messagingTemplate, times(1))
                .convertAndSendToUser(eq("u1@test.com"), eq(Constantes.WS_QUEUE_CHAT_CIERRES), any());
        verify(messagingTemplate, times(1))
                .convertAndSendToUser(eq("u2@test.com"), eq(Constantes.WS_QUEUE_CHAT_CIERRES), any());
    }

    @Test
    void noAdminNoPuedeCerrar() {
        Long requesterId = 4L;
        UsuarioEntity user = new UsuarioEntity();
        user.setId(requesterId);
        user.setRoles(Set.of(Constantes.ROLE_USER));

        when(securityUtils.hasRole(Constantes.ADMIN)).thenReturn(false);
        when(securityUtils.hasRole(Constantes.ROLE_ADMIN)).thenReturn(false);
        when(securityUtils.getAuthenticatedUserId()).thenReturn(requesterId);
        when(usuarioRepo.findById(requesterId)).thenReturn(Optional.of(user));

        assertThrows(AccessDeniedException.class,
                () -> service.cerrarChatGrupalComoAdmin(10L, "x", "127.0.0.1", "JUnit"));
        verify(chatGrupalRepo, never()).findByIdWithUsuariosForUpdate(any());
    }

    @Test
    void validaMotivoConNullByte() {
        Long adminId = 8L;
        when(securityUtils.hasRole(Constantes.ADMIN)).thenReturn(true);
        when(securityUtils.getAuthenticatedUserId()).thenReturn(adminId);

        assertThrows(IllegalArgumentException.class,
                () -> service.cerrarChatGrupalComoAdmin(10L, "bad\u0000reason", "127.0.0.1", "JUnit"));
        verify(chatGrupalRepo, never()).findByIdWithUsuariosForUpdate(any());
    }

    @Test
    void closeEsIdempotenteSiYaEstabaCerrado() {
        Long adminId = 9L;
        Long chatId = 120L;
        ChatGrupalEntity chat = activeGroup(chatId);
        chat.setClosed(true);
        chat.setClosedReason("ya cerrado");
        chat.setClosedAt(Instant.parse("2026-04-15T10:15:30Z"));
        chat.setClosedByAdminId(55L);

        when(securityUtils.hasRole(Constantes.ADMIN)).thenReturn(true);
        when(securityUtils.getAuthenticatedUserId()).thenReturn(adminId);
        when(chatGrupalRepo.findByIdWithUsuariosForUpdate(chatId)).thenReturn(Optional.of(chat));

        ChatCloseStateDTO out = service.cerrarChatGrupalComoAdmin(chatId, "nuevo", "127.0.0.1", "JUnit");

        assertTrue(out.isClosed());
        assertEquals("ya cerrado", out.getReason());
        assertEquals(55L, out.getClosedByAdminId());
        verify(chatGrupalRepo, never()).save(any(ChatGrupalEntity.class));
    }

    @Test
    void reopenEsIdempotenteYLimpiaEstado() {
        Long adminId = 9L;
        Long chatId = 120L;
        ChatGrupalEntity chat = activeGroup(chatId);
        chat.setClosed(true);
        chat.setClosedReason("mantenimiento");
        chat.setClosedAt(Instant.now());
        chat.setClosedByAdminId(adminId);

        when(securityUtils.hasRole(Constantes.ADMIN)).thenReturn(true);
        when(securityUtils.getAuthenticatedUserId()).thenReturn(adminId);
        when(chatGrupalRepo.findByIdWithUsuariosForUpdate(chatId)).thenReturn(Optional.of(chat));
        when(chatGrupalRepo.save(any(ChatGrupalEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        ChatCloseStateDTO out = service.reabrirChatGrupalComoAdmin(chatId, "127.0.0.1", "JUnit");

        assertFalse(out.isClosed());
        assertEquals(null, out.getReason());
        assertEquals(null, out.getClosedAt());
        assertEquals(null, out.getClosedByAdminId());
        verify(chatGrupalRepo, times(1)).save(any(ChatGrupalEntity.class));
    }

    @Test
    void closeRetorna404SiNoExiste() {
        when(securityUtils.hasRole(Constantes.ADMIN)).thenReturn(true);
        when(securityUtils.getAuthenticatedUserId()).thenReturn(7L);
        when(chatGrupalRepo.findByIdWithUsuariosForUpdate(33L)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class,
                () -> service.cerrarChatGrupalComoAdmin(33L, null, "127.0.0.1", "JUnit"));
    }

    @Test
    void listadoConversacionesIncluyeCamposCompatDeCierre() {
        Long requesterId = 1L;
        Long targetUserId = 2L;
        UsuarioEntity admin = new UsuarioEntity();
        admin.setId(requesterId);
        admin.setRoles(Set.of(Constantes.ROLE_ADMIN));

        ChatGrupalEntity group = activeGroup(555L);
        group.setNombreGrupo("Soporte");
        group.setClosed(true);
        group.setClosedReason("Moderacion");

        when(securityUtils.getAuthenticatedUserId()).thenReturn(requesterId);
        when(usuarioRepo.findById(requesterId)).thenReturn(Optional.of(admin));
        when(chatIndRepo.findAllByUsuario1IdOrUsuario2Id(targetUserId, targetUserId)).thenReturn(List.of());
        when(chatGrupalRepo.findAllByUsuariosId(targetUserId)).thenReturn(List.of(group));
        when(mensajeRepository.countActivosByChatIds(List.of(group.getId()))).thenReturn(List.of());
        when(mensajeRepository.findLatestByChatIds(List.of(group.getId()))).thenReturn(List.of());

        List<ChatResumenDTO> result = service.listarConversacionesDeUsuario(targetUserId, false);

        assertEquals(1, result.size());
        ChatResumenDTO dto = result.get(0);
        assertTrue(Boolean.TRUE.equals(dto.getChatCerrado()));
        assertTrue(Boolean.TRUE.equals(dto.getClosed()));
        assertEquals("Moderacion", dto.getChatCerradoMotivo());
        assertEquals("Moderacion", dto.getReason());
    }

    private ChatGrupalEntity activeGroup(Long chatId) {
        ChatGrupalEntity chat = new ChatGrupalEntity();
        chat.setId(chatId);
        chat.setActivo(true);
        chat.setUsuarios(List.of(user(1L, "u1@test.com"), user(2L, "u2@test.com")));
        return chat;
    }

    private UsuarioEntity user(Long id, String email) {
        UsuarioEntity u = new UsuarioEntity();
        u.setId(id);
        u.setEmail(email);
        u.setActivo(true);
        return u;
    }
}
