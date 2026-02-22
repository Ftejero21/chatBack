package com.chat.chat.Service.ChatService;

import com.chat.chat.DTO.ChatResumenDTO;
import com.chat.chat.Entity.ChatIndividualEntity;
import com.chat.chat.Entity.MensajeEntity;
import com.chat.chat.Entity.UsuarioEntity;
import com.chat.chat.Repository.ChatGrupalRepository;
import com.chat.chat.Repository.ChatIndividualRepository;
import com.chat.chat.Repository.MensajeRepository;
import com.chat.chat.Repository.UsuarioRepository;
import com.chat.chat.Utils.SecurityUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatServiceImplAdminPreviewTest {

    @Mock
    private UsuarioRepository usuarioRepo;
    @Mock
    private ChatIndividualRepository chatIndRepo;
    @Mock
    private MensajeRepository mensajeRepository;
    @Mock
    private ChatGrupalRepository chatGrupalRepo;
    @Mock
    private SecurityUtils securityUtils;

    @InjectMocks
    private ChatServiceImpl chatService;

    @Test
    void adminRecibeUltimoMensajeDescifrado() {
        Long usuarioObjetivoId = 11L;
        Long adminId = 1L;

        UsuarioEntity admin = usuarioConRoles(adminId, Set.of("ROLE_ADMIN"));
        when(securityUtils.getAuthenticatedUserId()).thenReturn(adminId);
        when(usuarioRepo.findById(adminId)).thenReturn(Optional.of(admin));

        ChatIndividualEntity chat = chatIndividual(100L, "Ana", "Luis");
        when(chatIndRepo.findAllByUsuario1IdOrUsuario2Id(usuarioObjetivoId, usuarioObjetivoId)).thenReturn(List.of(chat));
        when(chatGrupalRepo.findAllByUsuariosId(usuarioObjetivoId)).thenReturn(List.of());
        when(mensajeRepository.countByChatIdAndActivoTrue(100L)).thenReturn(1L);

        MensajeEntity ultimo = new MensajeEntity();
        ultimo.setContenido("hola en claro");
        when(mensajeRepository.findTopByChatIdAndActivoTrueOrderByFechaEnvioDesc(100L)).thenReturn(Optional.of(ultimo));

        List<ChatResumenDTO> resultado = chatService.listarConversacionesDeUsuario(usuarioObjetivoId);

        assertEquals(1, resultado.size());
        assertEquals("hola en claro", resultado.get(0).getUltimoMensajeDescifrado());
    }

    @Test
    void noAdminNoRecibeUltimoMensajeDescifrado() {
        Long usuarioObjetivoId = 11L;
        Long userId = 2L;

        UsuarioEntity user = usuarioConRoles(userId, Set.of("ROLE_USER"));
        when(securityUtils.getAuthenticatedUserId()).thenReturn(userId);
        when(usuarioRepo.findById(userId)).thenReturn(Optional.of(user));

        ChatIndividualEntity chat = chatIndividual(100L, "Ana", "Luis");
        when(chatIndRepo.findAllByUsuario1IdOrUsuario2Id(usuarioObjetivoId, usuarioObjetivoId)).thenReturn(List.of(chat));
        when(chatGrupalRepo.findAllByUsuariosId(usuarioObjetivoId)).thenReturn(List.of());
        when(mensajeRepository.countByChatIdAndActivoTrue(100L)).thenReturn(1L);

        MensajeEntity ultimo = new MensajeEntity();
        ultimo.setContenido("hola en claro");
        when(mensajeRepository.findTopByChatIdAndActivoTrueOrderByFechaEnvioDesc(100L)).thenReturn(Optional.of(ultimo));

        List<ChatResumenDTO> resultado = chatService.listarConversacionesDeUsuario(usuarioObjetivoId);

        assertEquals(1, resultado.size());
        assertNull(resultado.get(0).getUltimoMensajeDescifrado());
        assertEquals("hola en claro", resultado.get(0).getUltimoMensaje());
    }

    private static UsuarioEntity usuarioConRoles(Long id, Set<String> roles) {
        UsuarioEntity usuario = new UsuarioEntity();
        usuario.setId(id);
        usuario.setRoles(roles);
        return usuario;
    }

    private static ChatIndividualEntity chatIndividual(Long id, String nombre1, String nombre2) {
        ChatIndividualEntity chat = new ChatIndividualEntity();
        chat.setId(id);

        UsuarioEntity u1 = new UsuarioEntity();
        u1.setNombre(nombre1);
        UsuarioEntity u2 = new UsuarioEntity();
        u2.setNombre(nombre2);

        chat.setUsuario1(u1);
        chat.setUsuario2(u2);
        return chat;
    }
}
