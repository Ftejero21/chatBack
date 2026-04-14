package com.chat.chat.Service.PresenceService;

import com.chat.chat.DTO.PresenceEventDTO;
import com.chat.chat.Repository.ChatGrupalRepository;
import com.chat.chat.Repository.ChatIndividualRepository;
import com.chat.chat.Utils.Constantes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class PresenceBroadcastService {
    private static final Logger LOGGER = LoggerFactory.getLogger(PresenceBroadcastService.class);

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private ChatIndividualRepository chatIndividualRepository;

    @Autowired
    private ChatGrupalRepository chatGrupalRepository;

    public void publishPresenceToAuthorized(Long sourceUserId, String estado, String sessionId) {
        if (sourceUserId == null) {
            LOGGER.warn("[WS][ESTADO] action=PUBLISH result=REJECT authUserId={} destination={} reason={} sessionId={}",
                    null,
                    "-",
                    "USER_ID_NULL",
                    safeSessionId(sessionId));
            return;
        }

        String normalizedEstado = normalizeEstado(estado);
        PresenceEventDTO payload = new PresenceEventDTO(sourceUserId, normalizedEstado);

        for (Long recipientId : resolveAuthorizedRecipients(sourceUserId)) {
            String destination = Constantes.TOPIC_ESTADO + recipientId;
            messagingTemplate.convertAndSend(destination, payload);
            LOGGER.info("[WS][ESTADO][OUT] toUserId={} changedUserId={} estado={} destination={}",
                    recipientId,
                    sourceUserId,
                    normalizedEstado,
                    destination);
            LOGGER.info("[WS][ESTADO] action=PUBLISH result=OK authUserId={} destination={} reason={} sessionId={}",
                    sourceUserId,
                    destination,
                    normalizedEstado,
                    safeSessionId(sessionId));
        }
    }

    public String normalizeEstado(String estado) {
        if (estado == null || estado.isBlank()) {
            return Constantes.ESTADO_DESCONECTADO;
        }
        String normalizedInput = estado.trim();
        if (Constantes.ESTADO_CONECTADO.equalsIgnoreCase(normalizedInput)) {
            return Constantes.ESTADO_CONECTADO;
        }
        if (Constantes.ESTADO_AUSENTE.equalsIgnoreCase(normalizedInput)) {
            return Constantes.ESTADO_AUSENTE;
        }
        if (Constantes.ESTADO_DESCONECTADO.equalsIgnoreCase(normalizedInput)) {
            return Constantes.ESTADO_DESCONECTADO;
        }
        return Constantes.ESTADO_DESCONECTADO;
    }

    private Set<Long> resolveAuthorizedRecipients(Long sourceUserId) {
        Set<Long> recipients = new LinkedHashSet<>();
        recipients.add(sourceUserId);

        List<Long> contactos = chatIndividualRepository.findVisibleContactIdsByUserId(sourceUserId);
        if (contactos != null) {
            for (Long id : contactos) {
                if (id != null) {
                    recipients.add(id);
                }
            }
        }

        List<Long> miembrosGrupos = chatGrupalRepository.findVisibleMemberIdsByUserId(sourceUserId);
        if (miembrosGrupos != null) {
            for (Long id : miembrosGrupos) {
                if (id != null) {
                    recipients.add(id);
                }
            }
        }

        return recipients;
    }

    private String safeSessionId(String sessionId) {
        return sessionId == null || sessionId.isBlank() ? "-" : sessionId;
    }
}
