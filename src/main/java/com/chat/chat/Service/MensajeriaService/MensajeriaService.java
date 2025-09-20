package com.chat.chat.Service.MensajeriaService;

import com.chat.chat.DTO.MensajeDTO;

import java.util.List;

public interface MensajeriaService {
    MensajeDTO guardarMensajeIndividual(MensajeDTO dto);
    MensajeDTO guardarMensajeGrupal(MensajeDTO dto);

    void marcarMensajesComoLeidos(List<Long> ids);

    public boolean eliminarMensajePropio(MensajeDTO mensajeDTO);
}
