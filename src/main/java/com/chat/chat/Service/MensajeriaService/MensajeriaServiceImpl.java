package com.chat.chat.Service.MensajeriaService;

import com.chat.chat.DTO.MensajeDTO;
import com.chat.chat.Entity.ChatGrupalEntity;
import com.chat.chat.Entity.ChatIndividualEntity;
import com.chat.chat.Entity.MensajeEntity;
import com.chat.chat.Entity.UsuarioEntity;
import com.chat.chat.Repository.ChatGrupalRepository;
import com.chat.chat.Repository.ChatIndividualRepository;
import com.chat.chat.Repository.MensajeRepository;
import com.chat.chat.Repository.UsuarioRepository;
import com.chat.chat.Utils.MappingUtils;
import com.chat.chat.Utils.SecurityUtils;
import com.chat.chat.Utils.Utils;
import com.chat.chat.Utils.Constantes;
import com.chat.chat.Utils.E2EPayloadUtils;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class MensajeriaServiceImpl implements MensajeriaService {

    @Value("${app.uploads.root:uploads}")
    private String uploadsRoot;

    @Value("${app.uploads.base-url:/uploads}")
    private String uploadsBaseUrl;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private MensajeRepository mensajeRepository;

    @Autowired
    private ChatIndividualRepository chatIndividualRepository;

    @Autowired
    private ChatGrupalRepository chatGrupalRepository;

    @Autowired
    private SecurityUtils securityUtils;

    @Override
    @Transactional
    public MensajeDTO guardarMensajeIndividual(MensajeDTO dto) {
        Long authenticatedUserId = securityUtils.getAuthenticatedUserId();
        System.out.println(
                "Guardando mensaje individual: emisor=" + authenticatedUserId + " receptor=" + dto.getReceptorId());

        UsuarioEntity emisor = usuarioRepository.findById(authenticatedUserId).orElseThrow();
        UsuarioEntity receptor = usuarioRepository.findById(dto.getReceptorId()).orElseThrow();

        ChatIndividualEntity chat = chatIndividualRepository.findByUsuario1AndUsuario2(emisor, receptor)
                .or(() -> chatIndividualRepository.findByUsuario1AndUsuario2(receptor, emisor))
                .orElseThrow(() -> new RuntimeException(Constantes.MSG_CHAT_INDIVIDUAL_NO_ENCONTRADO));

        if (emisor.getBloqueados().contains(receptor) || receptor.getBloqueados().contains(emisor)) {
            throw new RuntimeException(Constantes.MSG_NO_PUEDE_ENVIAR_MENSAJES);
        }

        // === AUDIO ===
        if (dto.getAudioDataUrl() != null && dto.getAudioDataUrl().startsWith(Constantes.DATA_AUDIO_PREFIX)) {
            // Guardar a disco (voice/)
            String publicUrl = Utils.saveDataUrlToUploads(dto.getAudioDataUrl(), Constantes.DIR_VOICE, uploadsRoot, uploadsBaseUrl);
            dto.setAudioUrl(publicUrl);
            // inferir mime del dataURL
            String mime = dto.getAudioDataUrl().substring(5, dto.getAudioDataUrl().indexOf(';')); // "audio/webm"
            dto.setAudioMime(mime);
            dto.setTipo("AUDIO");
        } else if (dto.getAudioUrl() != null && !dto.getAudioUrl().isBlank()) {
            dto.setTipo("AUDIO");
        } else {
            // si no hay audio, asumimos texto
            dto.setTipo("TEXT");
        }

        MensajeEntity mensaje = MappingUtils.mensajeDtoAEntity(dto, emisor, receptor);
        mensaje.setContenido(E2EPayloadUtils.normalizeForStorage(mensaje.getContenido()));
        mensaje.setChat(chat);
        mensaje.setFechaEnvio(LocalDateTime.now());
        mensaje.setActivo(true);
        mensaje.setLeido(false);

        MensajeEntity saved = mensajeRepository.save(mensaje);
        return MappingUtils.mensajeEntityADto(saved);
    }

    @Override
    @Transactional
    public MensajeDTO guardarMensajeGrupal(MensajeDTO dto) {
        Long authenticatedUserId = securityUtils.getAuthenticatedUserId();
        UsuarioEntity emisor = usuarioRepository.findById(authenticatedUserId).orElseThrow();

        // ⚠️ dto.receptorId llega con el id del chat grupal
        ChatGrupalEntity chatGrupal = chatGrupalRepository.findById(dto.getReceptorId())
                .orElseThrow(() -> new RuntimeException(Constantes.MSG_CHAT_GRUPAL_NO_ENCONTRADO));

        MensajeEntity mensaje = MappingUtils.mensajeDtoAEntity(dto, emisor, null);
        mensaje.setContenido(E2EPayloadUtils.normalizeForStorage(mensaje.getContenido()));
        mensaje.setChat(chatGrupal);
        mensaje.setFechaEnvio(LocalDateTime.now());
        mensaje.setActivo(true);
        mensaje.setLeido(false);

        MensajeEntity saved = mensajeRepository.save(mensaje);
        MensajeDTO out = MappingUtils.mensajeEntityADto(saved);

        // Enriquecer con datos del emisor para que el front no tenga que resolver nada
        out.setEmisorNombre(emisor.getNombre());
        out.setEmisorApellido(emisor.getApellido());
        if (emisor.getFotoUrl() != null) {
            out.setEmisorFoto(Utils.toDataUrlFromUrl(emisor.getFotoUrl(), uploadsRoot)); // o devuelve URL si prefieres
        }
        return out;
    }

    @Override
    public void marcarMensajesComoLeidos(List<Long> ids) {
        List<MensajeEntity> mensajes = mensajeRepository.findAllById(ids);

        mensajes.forEach(mensaje -> {
            mensaje.setLeido(true);
        });

        mensajeRepository.saveAll(mensajes);

        // Notificar por WebSocket al emisor de cada mensaje
        mensajes.forEach(mensaje -> {
            Long emisorId = mensaje.getEmisor().getId();
            Map<String, Long> payload = new HashMap<>();
            payload.put(Constantes.KEY_MENSAJE_ID, mensaje.getId());

            messagingTemplate.convertAndSend(Constantes.WS_TOPIC_LEIDO + emisorId, payload);
        });
    }

    @Override
    public boolean eliminarMensajePropio(MensajeDTO mensajeDTO) {
        Optional<MensajeEntity> optMensaje = mensajeRepository.findById(mensajeDTO.getId());

        if (optMensaje.isPresent()) {
            MensajeEntity mensaje = optMensaje.get();
            Long authenticatedUserId = securityUtils.getAuthenticatedUserId();

            // Validar que el mensaje pertenece al emisor autenticado
            if (!mensaje.getEmisor().getId().equals(authenticatedUserId)) {
                return false; // ❌ No autorizado
            }

            mensaje.setActivo(false);
            mensajeRepository.save(mensaje);
            return true;
        }
        return false;
    }
}
