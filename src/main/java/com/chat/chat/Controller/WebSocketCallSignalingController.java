package com.chat.chat.Controller;

import com.chat.chat.Call.CallManager;
import com.chat.chat.Call.CallSession;
import com.chat.chat.Call.DTO.IceCandidateDTO;
import com.chat.chat.Call.DTO.SdpAnswerDTO;
import com.chat.chat.Call.DTO.SdpOfferDTO;
import com.chat.chat.Utils.Constantes;
import com.chat.chat.Utils.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

@RestController
public class WebSocketCallSignalingController {
    private static final Logger LOGGER = LoggerFactory.getLogger(WebSocketCallSignalingController.class);

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private SecurityUtils securityUtils;

    @Autowired
    private CallManager callManager;

    // Caller -> Offer -> Callee
    @MessageMapping(Constantes.WS_APP_CALL_SDP_OFFER)
    public void sdpOffer(@Payload SdpOfferDTO dto) {
        Long authUserId = validateSignalingFrame("SDP_OFFER",
                dto == null ? null : dto.getCallId(),
                dto == null ? null : dto.getToUserId(),
                dto == null ? null : dto.getFromUserId());
        dto.setFromUserId(authUserId);
        LOGGER.debug("[WS_CALL_SIGNAL] op=SDP_OFFER stage=OUT topic={} callId={} fromUserId={} toUserId={}",
                Constantes.TOPIC_CALL_SDP_OFFER + dto.getToUserId(),
                dto.getCallId(),
                authUserId,
                dto.getToUserId());
        messagingTemplate.convertAndSend(Constantes.TOPIC_CALL_SDP_OFFER + dto.getToUserId(), dto);
    }

    // Callee -> Answer -> Caller
    @MessageMapping(Constantes.WS_APP_CALL_SDP_ANSWER)
    public void sdpAnswer(@Payload SdpAnswerDTO dto) {
        Long authUserId = validateSignalingFrame("SDP_ANSWER",
                dto == null ? null : dto.getCallId(),
                dto == null ? null : dto.getToUserId(),
                dto == null ? null : dto.getFromUserId());
        dto.setFromUserId(authUserId);
        LOGGER.debug("[WS_CALL_SIGNAL] op=SDP_ANSWER stage=OUT topic={} callId={} fromUserId={} toUserId={}",
                Constantes.TOPIC_CALL_SDP_ANSWER + dto.getToUserId(),
                dto.getCallId(),
                authUserId,
                dto.getToUserId());
        messagingTemplate.convertAndSend(Constantes.TOPIC_CALL_SDP_ANSWER + dto.getToUserId(), dto);
    }

    // ICE candidates (ambos sentidos)
    @MessageMapping(Constantes.WS_APP_CALL_ICE)
    public void ice(@Payload IceCandidateDTO dto) {
        Long authUserId = validateSignalingFrame("ICE",
                dto == null ? null : dto.getCallId(),
                dto == null ? null : dto.getToUserId(),
                dto == null ? null : dto.getFromUserId());
        dto.setFromUserId(authUserId);
        LOGGER.debug("[WS_CALL_SIGNAL] op=ICE stage=OUT topic={} callId={} fromUserId={} toUserId={}",
                Constantes.TOPIC_CALL_ICE + dto.getToUserId(),
                dto.getCallId(),
                authUserId,
                dto.getToUserId());
        messagingTemplate.convertAndSend(Constantes.TOPIC_CALL_ICE + dto.getToUserId(), dto);
    }

    private Long validateSignalingFrame(String op, String callId, Long toUserId, Long payloadFromUserId) {
        if (callId == null || callId.isBlank() || toUserId == null) {
            LOGGER.debug("[WS_CALL_SIGNAL] op={} stage=REJECT reason=PAYLOAD_INVALID callId={} toUserId={} payloadFromUserId={}",
                    op, callId, toUserId, payloadFromUserId);
            throw new IllegalArgumentException(Constantes.ERR_RESPUESTA_INVALIDA);
        }

        Long authUserId;
        try {
            authUserId = securityUtils.getAuthenticatedUserId();
        } catch (RuntimeException ex) {
            LOGGER.debug("[WS_CALL_SIGNAL] op={} stage=REJECT reason=AUTH_CONTEXT_MISSING callId={} toUserId={} errorClass={} message={}",
                    op, callId, toUserId, ex.getClass().getSimpleName(), ex.getMessage());
            throw new AccessDeniedException(Constantes.ERR_NO_AUTORIZADO);
        }

        LOGGER.debug("[WS_CALL_SIGNAL] op={} stage=IN callId={} authUserId={} payloadFromUserId={} toUserId={}",
                op, callId, authUserId, payloadFromUserId, toUserId);

        if (payloadFromUserId != null && !Objects.equals(payloadFromUserId, authUserId)) {
            LOGGER.debug("[WS_CALL_SIGNAL] op={} stage=REJECT reason=FROM_USER_SPOOF callId={} authUserId={} payloadFromUserId={} toUserId={}",
                    op, callId, authUserId, payloadFromUserId, toUserId);
            throw new AccessDeniedException(Constantes.ERR_RESPUESTA_NO_AUTORIZADA);
        }

        CallSession session = callManager.get(callId);
        if (session == null) {
            LOGGER.debug("[WS_CALL_SIGNAL] op={} stage=REJECT reason=CALL_NOT_FOUND callId={} authUserId={} toUserId={}",
                    op, callId, authUserId, toUserId);
            throw new IllegalArgumentException("callId invalido");
        }
        if (session.getStatus() == CallSession.Status.ENDED) {
            LOGGER.debug("[WS_CALL_SIGNAL] op={} stage=REJECT reason=CALL_ENDED callId={} authUserId={} toUserId={}",
                    op, callId, authUserId, toUserId);
            throw new AccessDeniedException(Constantes.ERR_NO_AUTORIZADO);
        }

        boolean isCaller = Objects.equals(session.getCallerId(), authUserId);
        boolean isCallee = Objects.equals(session.getCalleeId(), authUserId);
        if (!isCaller && !isCallee) {
            LOGGER.debug("[WS_CALL_SIGNAL] op={} stage=REJECT reason=NOT_PARTICIPANT callId={} authUserId={} callerId={} calleeId={} toUserId={}",
                    op, callId, authUserId, session.getCallerId(), session.getCalleeId(), toUserId);
            throw new AccessDeniedException(Constantes.ERR_NO_AUTORIZADO);
        }

        Long expectedPeerId = isCaller ? session.getCalleeId() : session.getCallerId();
        if (!Objects.equals(toUserId, expectedPeerId)) {
            LOGGER.debug("[WS_CALL_SIGNAL] op={} stage=REJECT reason=ROUTE_MISMATCH callId={} authUserId={} expectedToUserId={} payloadToUserId={}",
                    op, callId, authUserId, expectedPeerId, toUserId);
            throw new AccessDeniedException(Constantes.ERR_RESPUESTA_NO_AUTORIZADA);
        }

        return authUserId;
    }

    @MessageExceptionHandler({IllegalArgumentException.class, AccessDeniedException.class})
    @SendToUser(Constantes.WS_QUEUE_ERRORS)
    public Map<String, Object> handleWsSignalingError(Exception ex) {
        String code = ex instanceof AccessDeniedException ? Constantes.ERR_NO_AUTORIZADO : Constantes.ERR_RESPUESTA_INVALIDA;
        LOGGER.debug("[WS_CALL_SIGNAL] op=SEMANTIC_ERROR code={} errorClass={} message={}",
                code,
                ex == null ? null : ex.getClass().getSimpleName(),
                ex == null ? null : ex.getMessage());
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("code", code);
        payload.put("message", ex == null ? null : ex.getMessage());
        payload.put("ts", LocalDateTime.now().toString());
        return payload;
    }
}
