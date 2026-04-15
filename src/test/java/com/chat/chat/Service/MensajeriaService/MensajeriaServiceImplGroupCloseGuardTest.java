package com.chat.chat.Service.MensajeriaService;

import com.chat.chat.DTO.MensajeDTO;
import com.chat.chat.Entity.ChatGrupalEntity;
import com.chat.chat.Entity.MensajeEntity;
import com.chat.chat.Entity.UsuarioEntity;
import com.chat.chat.Exceptions.ChatCerradoException;
import com.chat.chat.Repository.ChatGrupalRepository;
import com.chat.chat.Repository.ChatIndividualRepository;
import com.chat.chat.Repository.MensajeRepository;
import com.chat.chat.Repository.UsuarioRepository;
import com.chat.chat.Service.EncuestaService.EncuestaService;
import com.chat.chat.Utils.Constantes;
import com.chat.chat.Utils.MessageType;
import com.chat.chat.Utils.SecurityUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MensajeriaServiceImplGroupCloseGuardTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private MensajeRepository mensajeRepository;
    @Mock
    private ChatIndividualRepository chatIndividualRepository;
    @Mock
    private ChatGrupalRepository chatGrupalRepository;
    @Mock
    private SecurityUtils securityUtils;
    @Mock
    private EncuestaService encuestaService;

    @InjectMocks
    private MensajeriaServiceImpl service;

    @Test
    void bloqueaEnvioCuandoGrupoEstaCerrado() {
        Long senderId = 21L;
        Long groupId = 60L;

        UsuarioEntity sender = usuario(senderId, "PUBLIC_KEY_21");
        ChatGrupalEntity chat = new ChatGrupalEntity();
        chat.setId(groupId);
        chat.setActivo(true);
        chat.setClosed(true);
        chat.setUsuarios(List.of(sender));

        when(securityUtils.getAuthenticatedUserId()).thenReturn(senderId);
        when(usuarioRepository.findById(senderId)).thenReturn(Optional.of(sender));
        when(chatGrupalRepository.findByIdWithUsuariosForUpdate(groupId)).thenReturn(Optional.of(chat));

        MensajeDTO dto = new MensajeDTO();
        dto.setTipo(Constantes.TIPO_TEXT);
        dto.setChatId(groupId);
        dto.setReceptorId(groupId);
        dto.setContenido("{\"type\":\"E2E_GROUP\",\"iv\":\"iv\",\"ciphertext\":\"c\",\"forEmisor\":\"e\",\"forAdmin\":\"a\",\"forReceptores\":{}}");

        assertThrows(ChatCerradoException.class, () -> service.guardarMensajeGrupal(dto));
        verify(mensajeRepository, never()).save(any());
    }

    @Test
    void bloqueaSpoofingDeEmisorEnGrupo() {
        when(securityUtils.getAuthenticatedUserId()).thenReturn(21L);

        MensajeDTO dto = new MensajeDTO();
        dto.setEmisorId(99L);
        dto.setTipo(Constantes.TIPO_TEXT);
        dto.setChatId(60L);
        dto.setReceptorId(60L);

        assertThrows(AccessDeniedException.class, () -> service.guardarMensajeGrupal(dto));
        verify(chatGrupalRepository, never()).findByIdWithUsuariosForUpdate(any());
    }

    @Test
    void bloqueaSpoofingDeChatIdEnGrupo() {
        when(securityUtils.getAuthenticatedUserId()).thenReturn(21L);

        MensajeDTO dto = new MensajeDTO();
        dto.setTipo(Constantes.TIPO_TEXT);
        dto.setChatId(61L);
        dto.setReceptorId(60L);

        assertThrows(IllegalArgumentException.class, () -> service.guardarMensajeGrupal(dto));
        verify(chatGrupalRepository, never()).findByIdWithUsuariosForUpdate(any());
    }

    @Test
    void grupoAbiertoPermitePersistirMensaje() {
        Long senderId = 21L;
        Long recipientId = 16L;
        Long groupId = 60L;

        UsuarioEntity sender = usuario(senderId, "PUBLIC_KEY_21");
        UsuarioEntity recipient = usuario(recipientId, "PUBLIC_KEY_16");
        ChatGrupalEntity chat = new ChatGrupalEntity();
        chat.setId(groupId);
        chat.setActivo(true);
        chat.setClosed(false);
        chat.setUsuarios(List.of(sender, recipient));

        when(securityUtils.getAuthenticatedUserId()).thenReturn(senderId);
        when(usuarioRepository.findById(senderId)).thenReturn(Optional.of(sender));
        when(chatGrupalRepository.findByIdWithUsuariosForUpdate(groupId)).thenReturn(Optional.of(chat));
        when(usuarioRepository.findFreshById(recipientId)).thenReturn(Optional.of(recipient));
        when(mensajeRepository.save(any(MensajeEntity.class))).thenAnswer(inv -> {
            MensajeEntity m = inv.getArgument(0);
            m.setId(999L);
            m.setTipo(m.getTipo() == null ? MessageType.TEXT : m.getTipo());
            m.setFechaEnvio(m.getFechaEnvio() == null ? LocalDateTime.now() : m.getFechaEnvio());
            m.setActivo(true);
            return m;
        });

        MensajeDTO dto = new MensajeDTO();
        dto.setTipo(Constantes.TIPO_TEXT);
        dto.setChatId(groupId);
        dto.setReceptorId(groupId);
        dto.setContenido("{\"type\":\"E2E_GROUP\",\"iv\":\"iv\",\"ciphertext\":\"cipher\",\"forEmisor\":\"envSender\",\"forAdmin\":\"envAdmin\",\"forReceptores\":{\"16\":\"env16\"}}");

        MensajeDTO out = service.guardarMensajeGrupal(dto);

        assertNotNull(out);
        assertNotNull(out.getId());
        verify(mensajeRepository).save(any(MensajeEntity.class));
    }

    private static UsuarioEntity usuario(Long id, String publicKey) {
        UsuarioEntity u = new UsuarioEntity();
        u.setId(id);
        u.setActivo(true);
        u.setPublicKey(publicKey);
        return u;
    }
}
