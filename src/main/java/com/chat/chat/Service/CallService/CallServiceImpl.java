package com.chat.chat.Service.CallService;

import com.chat.chat.Call.*;
import com.chat.chat.Call.DTO.CallAnswerDTO;
import com.chat.chat.Call.DTO.CallEndDTO;
import com.chat.chat.Call.DTO.CallInviteDTO;
import com.chat.chat.Entity.UsuarioEntity;
import com.chat.chat.Repository.UsuarioRepository;
import com.chat.chat.Utils.Constantes;
import com.chat.chat.Utils.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
public class CallServiceImpl implements CallService {
    private static final Logger LOGGER = LoggerFactory.getLogger(CallServiceImpl.class);

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private CallManager callManager;

    @Autowired
    private SecurityUtils securityUtils;

    @Override
    public CallInviteWS startCall(CallInviteDTO dto) {
        if (dto == null || dto.getCalleeId() == null) {
            LOGGER.warn("[WS_CALL] op=START stage=REJECT reason=PAYLOAD_INVALID callerId={} calleeId={} chatId={}",
                    dto == null ? null : dto.getCallerId(),
                    dto == null ? null : dto.getCalleeId(),
                    dto == null ? null : dto.getChatId());
            throw new IllegalArgumentException(Constantes.ERR_RESPUESTA_INVALIDA);
        }

        Long authUserId = resolveAuthenticatedUserId("START",
                dto == null ? null : dto.getCallerId(),
                dto.getCalleeId(),
                null,
                dto.getChatId(),
                null,
                null);
        LOGGER.info("[WS_CALL] op=START stage=IN authUserId={} payloadCallerId={} calleeId={} chatId={}",
                authUserId,
                dto.getCallerId(),
                dto.getCalleeId(),
                dto.getChatId());

        if (dto.getCallerId() != null && !Objects.equals(dto.getCallerId(), authUserId)) {
            LOGGER.warn("[WS_CALL] op=START stage=REJECT reason=CALLER_SPOOF authUserId={} payloadCallerId={} calleeId={} chatId={}",
                    authUserId,
                    dto.getCallerId(),
                    dto.getCalleeId(),
                    dto.getChatId());
            throw new AccessDeniedException(Constantes.ERR_RESPUESTA_NO_AUTORIZADA);
        }

        CallSession session = callManager.create(authUserId, dto.getCalleeId());

        // Obtener datos del caller (nombre/apellido)
        String nombre = Constantes.DEFAULT_CALLER_NAME;
        String apellido = "";
        try {
            UsuarioEntity u = usuarioRepository.findById(authUserId).orElse(null);
            if (u != null) {
                if (u.getNombre() != null) nombre = u.getNombre();
                if (u.getApellido() != null) apellido = u.getApellido();
            }
        } catch (Exception ignored) {}

        CallInviteWS invite = new CallInviteWS();
        invite.setCallId(session.getCallId());
        invite.setCallerId(authUserId);
        invite.setCallerNombre(nombre);
        invite.setCallerApellido(apellido);
        invite.setCalleeId(dto.getCalleeId());
        invite.setChatId(dto.getChatId());

        // Notificar al CALLEE
        messagingTemplate.convertAndSend(Constantes.TOPIC_CALL_INVITE + dto.getCalleeId(), invite);

        // Feedback al CALLER: "RINGING"
        CallAnswerWS ringing = new CallAnswerWS();
        ringing.setCallId(session.getCallId());
        ringing.setAccepted(false);
        ringing.setFromUserId(dto.getCalleeId());
        ringing.setToUserId(authUserId);
        ringing.setReason(Constantes.RINGING);
        messagingTemplate.convertAndSend(Constantes.TOPIC_CALL_ANSWER + authUserId, ringing);

        LOGGER.info("[WS_CALL] op=START stage=OUT callId={} inviteTopic={} answerTopic={} callerId={} calleeId={} chatId={}",
                session.getCallId(),
                Constantes.TOPIC_CALL_INVITE + dto.getCalleeId(),
                Constantes.TOPIC_CALL_ANSWER + authUserId,
                authUserId,
                dto.getCalleeId(),
                dto.getChatId());

        return invite;
    }

    @Override
    public CallAnswerWS answerCall(CallAnswerDTO dto) {
        if (dto == null || dto.getCallId() == null || dto.getCallId().isBlank()) {
            LOGGER.warn("[WS_CALL] op=ANSWER stage=REJECT reason=PAYLOAD_INVALID callId={} callerId={} calleeId={} accepted={}",
                    dto == null ? null : dto.getCallId(),
                    dto == null ? null : dto.getCallerId(),
                    dto == null ? null : dto.getCalleeId(),
                    dto != null && dto.isAccepted());
            throw new IllegalArgumentException(Constantes.ERR_RESPUESTA_INVALIDA);
        }
        Long authUserId = resolveAuthenticatedUserId("ANSWER",
                dto.getCallerId(),
                dto.getCalleeId(),
                null,
                null,
                dto.getCallId(),
                dto.isAccepted());
        LOGGER.info("[WS_CALL] op=ANSWER stage=IN callId={} authUserId={} payloadCallerId={} payloadCalleeId={} accepted={}",
                dto.getCallId(),
                authUserId,
                dto.getCallerId(),
                dto.getCalleeId(),
                dto.isAccepted());

        CallSession session = callManager.get(dto.getCallId());
        if (session == null) {
            LOGGER.warn("[WS_CALL] op=ANSWER stage=REJECT reason=CALL_NOT_FOUND callId={} authUserId={}",
                    dto.getCallId(),
                    authUserId);
            throw new IllegalArgumentException("callId invalido");
        }
        if (session.getStatus() == CallSession.Status.ENDED) {
            LOGGER.warn("[WS_CALL] op=ANSWER stage=REJECT reason=CALL_ENDED callId={} authUserId={} callerId={} calleeId={}",
                    dto.getCallId(),
                    authUserId,
                    session.getCallerId(),
                    session.getCalleeId());
            throw new AccessDeniedException(Constantes.ERR_NO_AUTORIZADO);
        }
        if (!Objects.equals(authUserId, session.getCalleeId())) {
            LOGGER.warn("[WS_CALL] op=ANSWER stage=REJECT reason=NOT_CALLEE callId={} authUserId={} callerId={} calleeId={}",
                    dto.getCallId(),
                    authUserId,
                    session.getCallerId(),
                    session.getCalleeId());
            throw new AccessDeniedException(Constantes.ERR_NO_AUTORIZADO);
        }
        if (dto.getCallerId() != null && !Objects.equals(dto.getCallerId(), session.getCallerId())) {
            LOGGER.warn("[WS_CALL] op=ANSWER stage=REJECT reason=CALLER_MISMATCH callId={} authUserId={} payloadCallerId={} expectedCallerId={}",
                    dto.getCallId(),
                    authUserId,
                    dto.getCallerId(),
                    session.getCallerId());
            throw new AccessDeniedException(Constantes.ERR_RESPUESTA_NO_AUTORIZADA);
        }
        if (dto.getCalleeId() != null && !Objects.equals(dto.getCalleeId(), session.getCalleeId())) {
            LOGGER.warn("[WS_CALL] op=ANSWER stage=REJECT reason=CALLEE_MISMATCH callId={} authUserId={} payloadCalleeId={} expectedCalleeId={}",
                    dto.getCallId(),
                    authUserId,
                    dto.getCalleeId(),
                    session.getCalleeId());
            throw new AccessDeniedException(Constantes.ERR_RESPUESTA_NO_AUTORIZADA);
        }

        // Actualizar estado
        callManager.setStatus(dto.getCallId(),
                dto.isAccepted() ? CallSession.Status.ACTIVE : CallSession.Status.ENDED);

        CallAnswerWS answer = new CallAnswerWS();
        answer.setCallId(dto.getCallId());
        answer.setAccepted(dto.isAccepted());
        answer.setFromUserId(authUserId);  // quien responde
        answer.setToUserId(session.getCallerId());    // a quien se notifica
        answer.setReason(dto.getReason());

        // Notificar al CALLER (iniciador)
        messagingTemplate.convertAndSend(Constantes.TOPIC_CALL_ANSWER + session.getCallerId(), answer);
        // (Eco) Notificar al CALLEE
        messagingTemplate.convertAndSend(Constantes.TOPIC_CALL_ANSWER + session.getCalleeId(), answer);

        LOGGER.info("[WS_CALL] op=ANSWER stage=OUT callId={} callerTopic={} calleeTopic={} fromUserId={} callerId={} calleeId={} accepted={}",
                dto.getCallId(),
                Constantes.TOPIC_CALL_ANSWER + session.getCallerId(),
                Constantes.TOPIC_CALL_ANSWER + session.getCalleeId(),
                authUserId,
                session.getCallerId(),
                session.getCalleeId(),
                dto.isAccepted());

        return answer;
    }

    @Override
    public CallEndWS endCall(CallEndDTO dto) {
        if (dto == null || dto.getCallId() == null || dto.getCallId().isBlank()) {
            LOGGER.warn("[WS_CALL] op=END stage=REJECT reason=PAYLOAD_INVALID callId={} byUserId={}",
                    dto == null ? null : dto.getCallId(),
                    dto == null ? null : dto.getByUserId());
            throw new IllegalArgumentException(Constantes.ERR_RESPUESTA_INVALIDA);
        }
        Long authUserId = resolveAuthenticatedUserId("END",
                null,
                null,
                dto.getByUserId(),
                null,
                dto.getCallId(),
                null);
        LOGGER.info("[WS_CALL] op=END stage=IN callId={} authUserId={} payloadByUserId={}",
                dto.getCallId(),
                authUserId,
                dto.getByUserId());
        if (dto.getByUserId() != null && !Objects.equals(dto.getByUserId(), authUserId)) {
            LOGGER.warn("[WS_CALL] op=END stage=REJECT reason=BY_USER_SPOOF callId={} authUserId={} payloadByUserId={}",
                    dto.getCallId(),
                    authUserId,
                    dto.getByUserId());
            throw new AccessDeniedException(Constantes.ERR_RESPUESTA_NO_AUTORIZADA);
        }

        CallSession session = callManager.get(dto.getCallId());
        if (session == null) {
            LOGGER.warn("[WS_CALL] op=END stage=REJECT reason=CALL_NOT_FOUND callId={} authUserId={}",
                    dto.getCallId(),
                    authUserId);
            throw new IllegalArgumentException("callId invalido");
        }
        boolean isCaller = Objects.equals(authUserId, session.getCallerId());
        boolean isCallee = Objects.equals(authUserId, session.getCalleeId());
        if (!isCaller && !isCallee) {
            LOGGER.warn("[WS_CALL] op=END stage=REJECT reason=NOT_PARTICIPANT callId={} authUserId={} callerId={} calleeId={}",
                    dto.getCallId(),
                    authUserId,
                    session.getCallerId(),
                    session.getCalleeId());
            throw new AccessDeniedException(Constantes.ERR_NO_AUTORIZADO);
        }

        callManager.end(dto.getCallId());

        Long notifyUserId = authUserId.equals(session.getCallerId())
                ? session.getCalleeId()
                : session.getCallerId();

        CallEndWS end = new CallEndWS();
        end.setCallId(dto.getCallId());
        end.setByUserId(authUserId);
        end.setNotifyUserId(notifyUserId);

        // Notificar a la otra parte
        messagingTemplate.convertAndSend(Constantes.TOPIC_CALL_END + notifyUserId, end);
        // (Eco) Notificar al que cuelga
        messagingTemplate.convertAndSend(Constantes.TOPIC_CALL_END + authUserId, end);

        LOGGER.info("[WS_CALL] op=END stage=OUT callId={} peerTopic={} actorTopic={} byUserId={} notifyUserId={}",
                dto.getCallId(),
                Constantes.TOPIC_CALL_END + notifyUserId,
                Constantes.TOPIC_CALL_END + authUserId,
                authUserId,
                notifyUserId);

        return end;
    }

    private Long resolveAuthenticatedUserId(String op,
                                            Long payloadCallerId,
                                            Long payloadCalleeId,
                                            Long payloadByUserId,
                                            Long chatId,
                                            String callId,
                                            Boolean accepted) {
        try {
            return securityUtils.getAuthenticatedUserId();
        } catch (RuntimeException ex) {
            LOGGER.warn("[WS_CALL] op={} stage=REJECT reason=AUTH_CONTEXT_MISSING payloadCallerId={} payloadCalleeId={} payloadByUserId={} chatId={} callId={} accepted={} errorClass={} message={}",
                    op,
                    payloadCallerId,
                    payloadCalleeId,
                    payloadByUserId,
                    chatId,
                    callId,
                    accepted,
                    ex.getClass().getSimpleName(),
                    ex.getMessage());
            throw new AccessDeniedException(Constantes.ERR_NO_AUTORIZADO);
        }
    }
}
