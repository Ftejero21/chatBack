package com.chat.chat.Service.MensajeriaService;

import com.chat.chat.DTO.MensajeDTO;
import com.chat.chat.DTO.MensajeReaccionDTO;

import java.util.List;
import java.util.Set;

public interface MensajeriaService {
    MensajeDTO guardarMensajeIndividual(MensajeDTO dto);
    MensajeDTO guardarMensajeGrupal(MensajeDTO dto);
    ReactionDispatchResult procesarReaccion(MensajeReaccionDTO request);

    void marcarMensajesComoLeidos(List<Long> ids);

    public boolean eliminarMensajePropio(MensajeDTO mensajeDTO);

    record ReactionDispatchResult(
            MensajeReaccionDTO event,
            Set<Long> recipientUserIds,
            Long chatId,
            boolean groupChat
    ) {
    }
}
