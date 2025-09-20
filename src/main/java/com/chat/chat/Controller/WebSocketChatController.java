package com.chat.chat.Controller;

import com.chat.chat.Call.DTO.CallAnswerDTO;
import com.chat.chat.Call.DTO.CallEndDTO;
import com.chat.chat.Call.DTO.CallInviteDTO;
import com.chat.chat.Configuracion.EstadoUsuarioManager;
import com.chat.chat.DTO.EscribiendoDTO;
import com.chat.chat.DTO.EscribiendoGrupoDTO;
import com.chat.chat.DTO.EstadoDTO;
import com.chat.chat.DTO.MensajeDTO;
import com.chat.chat.Repository.UsuarioRepository;
import com.chat.chat.Service.CallService.CallService;
import com.chat.chat.Service.MensajeriaService.MensajeriaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
public class WebSocketChatController {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private EstadoUsuarioManager estadoUsuarioManager;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private MensajeriaService mensajeriaService;

    @Autowired
    private CallService callService;

    // 1) A → INVITE → B
    @MessageMapping("/call.start")
    public void startCall(@Payload CallInviteDTO dto) {
        callService.startCall(dto);
    }

    // 2) B → ANSWER → A
    @MessageMapping("/call.answer")
    public void answer(@Payload CallAnswerDTO dto) {
        callService.answerCall(dto);
    }

    // 3) END desde cualquiera
    @MessageMapping("/call.end")
    public void end(@Payload CallEndDTO dto) {
        callService.endCall(dto);
    }

    @MessageMapping("/chat.individual")
    public void enviarMensajeIndividual(@Payload MensajeDTO mensajeDTO) {
        MensajeDTO guardado = mensajeriaService.guardarMensajeIndividual(mensajeDTO);

        System.out.println("[WS] send to receptor " + guardado.getReceptorId() +
                " from " + guardado.getEmisorId() +
                " tipo=" + guardado.getTipo());
        messagingTemplate.convertAndSend("/topic/chat." + guardado.getReceptorId(), guardado);
        // ✅ Enviar también al emisor (para que lo vea con id y estado)
        messagingTemplate.convertAndSend("/topic/chat." + guardado.getEmisorId(), guardado);
    }

    @MessageMapping("/chat.eliminar")
    public void eliminarMensaje(@Payload MensajeDTO mensajeDTO) {
         boolean eliminado = mensajeriaService.eliminarMensajePropio(mensajeDTO);
        if (eliminado) {
            mensajeDTO.setActivo(false);                     // 👈 asegúralo en el payload
            // (opcional) incluye chatId si tu front lo usa para preview
            // dto.setChatId(chatIdDelMensaje);

            messagingTemplate.convertAndSend("/topic/chat." + mensajeDTO.getEmisorId(), mensajeDTO);
            messagingTemplate.convertAndSend("/topic/chat." + mensajeDTO.getReceptorId(), mensajeDTO);
        }
    }

    @MessageMapping("/mensajes.marcarLeidos")
    public void marcarMensajesLeidos(@Payload List<Long> ids) {
        mensajeriaService.marcarMensajesComoLeidos(ids);
    }

    @MessageMapping("/escribiendo")
    public void indicarEscribiendo(@Payload EscribiendoDTO dto) {

        Map<String, Object> payload = new HashMap<>();
        payload.put("emisorId", dto.getEmisorId());
        payload.put("escribiendo", dto.isEscribiendo());

        messagingTemplate.convertAndSend("/topic/escribiendo." + dto.getReceptorId(), payload);
    }

    @MessageMapping("/escribiendo.grupo")
    public void indicarEscribiendoGrupo(@Payload EscribiendoGrupoDTO dto) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("emisorId", dto.getEmisorId());
        payload.put("chatId", dto.getChatId());
        payload.put("escribiendo", dto.isEscribiendo());

        usuarioRepository.findById(dto.getEmisorId()).ifPresent(u -> {
            payload.put("emisorNombre", u.getNombre());
            payload.put("emisorApellido", u.getApellido());
        });

        messagingTemplate.convertAndSend("/topic/escribiendo.grupo." + dto.getChatId(), payload);
    }


    @MessageMapping("/estado")
    public void actualizarEstadoUsuario(@Payload EstadoDTO dto) {
        if ("Conectado".equalsIgnoreCase(dto.getEstado())) {
            estadoUsuarioManager.marcarConectado(dto.getUsuarioId());
        } else {
            estadoUsuarioManager.marcarDesconectado(dto.getUsuarioId());
        }

        messagingTemplate.convertAndSend("/topic/estado." + dto.getUsuarioId(), dto.getEstado());
    }

    @MessageMapping("/chat.grupal")
    public void enviarMensajeGrupal(@Payload MensajeDTO mensajeDTO) {
        MensajeDTO guardado = mensajeriaService.guardarMensajeGrupal(mensajeDTO);
        messagingTemplate.convertAndSend("/topic/chat.grupal." + mensajeDTO.getReceptorId(), guardado);
    }
}
