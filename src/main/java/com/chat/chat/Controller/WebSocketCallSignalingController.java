package com.chat.chat.Controller;

import com.chat.chat.Call.DTO.IceCandidateDTO;
import com.chat.chat.Call.DTO.SdpAnswerDTO;
import com.chat.chat.Call.DTO.SdpOfferDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

@RestController
public class WebSocketCallSignalingController {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    // Caller -> Offer -> Callee
    @MessageMapping("/call.sdp.offer")
    public void sdpOffer(@Payload SdpOfferDTO dto) {
        if (dto == null || dto.getToUserId() == null) return;
        messagingTemplate.convertAndSend("/topic/call.sdp.offer." + dto.getToUserId(), dto);
    }

    // Callee -> Answer -> Caller
    @MessageMapping("/call.sdp.answer")
    public void sdpAnswer(@Payload SdpAnswerDTO dto) {
        if (dto == null || dto.getToUserId() == null) return;
        messagingTemplate.convertAndSend("/topic/call.sdp.answer." + dto.getToUserId(), dto);
    }

    // ICE candidates (ambos sentidos)
    @MessageMapping("/call.ice")
    public void ice(@Payload IceCandidateDTO dto) {
        if (dto == null || dto.getToUserId() == null) return;
        messagingTemplate.convertAndSend("/topic/call.ice." + dto.getToUserId(), dto);
    }
}
