package com.chat.chat.Service.GroupInviteService;

import com.chat.chat.DTO.GroupInviteResponseWS;
import com.chat.chat.DTO.UnseenCountWS;
import com.chat.chat.Entity.ChatGrupalEntity;
import com.chat.chat.Entity.GroupInviteEntity;
import com.chat.chat.Entity.NotificationEntity;
import com.chat.chat.Entity.UsuarioEntity;
import com.chat.chat.Repository.ChatGrupalRepository;
import com.chat.chat.Repository.GroupInviteRepo;
import com.chat.chat.Repository.NotificationRepo;
import com.chat.chat.Repository.UsuarioRepository;
import com.chat.chat.Utils.InviteStatus;
import com.chat.chat.Utils.NotificationType;
import com.chat.chat.Utils.Utils;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Objects;

@Service
public class GroupInviteServiceImpl implements GroupInviteService {

    @Autowired
    private GroupInviteRepo inviteRepo;
    @Autowired private ChatGrupalRepository chatRepo;
    @Autowired private UsuarioRepository usuarioRepo;
    @Autowired private NotificationRepo notificationRepo;
    @Autowired private SimpMessagingTemplate messagingTemplate;

    @Override
    @Transactional
    public void accept(Long inviteId, Long userId) {
        GroupInviteEntity inv = Utils.getByIdOrThrow(inviteRepo, inviteId, "Invitación");
        if (!Objects.equals(inv.getInvitee().getId(), userId))
            throw new IllegalArgumentException("Esta invitación no corresponde al usuario");

        if (inv.getStatus() != InviteStatus.PENDING) return;

        inv.setStatus(InviteStatus.ACCEPTED);
        inv.setRespondedAt(LocalDateTime.now());
        inviteRepo.save(inv);

        // añadir al grupo
        ChatGrupalEntity chat = inv.getChat();
        UsuarioEntity invitee = Utils.getByIdOrThrow(usuarioRepo, userId, "Usuario");
        chat.getUsuarios().add(invitee);
        chatRepo.save(chat);

        // notificar al creador
        GroupInviteResponseWS ws = new GroupInviteResponseWS();
        ws.inviteId = inv.getId();
        ws.groupId = chat.getId();
        ws.groupName = chat.getNombreGrupo();
        ws.inviteeId = invitee.getId();
        ws.inviteeNombre = invitee.getNombre();
        ws.status = InviteStatus.ACCEPTED;

        NotificationEntity notif = new NotificationEntity();
        notif.setUserId(inv.getInviter().getId());
        notif.setType(NotificationType.GROUP_INVITE_RESPONSE);
        notif.setPayloadJson(Utils.writeJson(ws));
        notif.setSeen(false);
        notificationRepo.save(notif);

        int unseenForCreator = (int) notificationRepo.countByUserIdAndSeenFalse(inv.getInviter().getId());
        ws.unseenCount = unseenForCreator;
        Utils.sendNotif(messagingTemplate, inv.getInviter().getId(), ws);

        // marcar como vista la notificación de invitación del invitado (si existe)
        notificationRepo.findFirstByUserIdAndTypeAndPayloadJsonContaining(
                userId, NotificationType.GROUP_INVITE, "\"inviteId\":" + inviteId
        ).ifPresent(n -> { n.setSeen(true); notificationRepo.save(n); });

        // actualizar contador del invitado
        int unseenForInvitee = (int) notificationRepo.countByUserIdAndSeenFalse(userId);
        Utils.sendNotif(messagingTemplate, userId, new UnseenCountWS(userId, unseenForInvitee));
    }

    @Override
    @Transactional
    public void decline(Long inviteId, Long userId) {
        GroupInviteEntity inv = Utils.getByIdOrThrow(inviteRepo, inviteId, "Invitación");
        if (!Objects.equals(inv.getInvitee().getId(), userId))
            throw new IllegalArgumentException("Esta invitación no corresponde al usuario");

        if (inv.getStatus() != InviteStatus.PENDING) return;

        inv.setStatus(InviteStatus.DECLINED);
        inv.setRespondedAt(LocalDateTime.now());
        inviteRepo.save(inv);

        // notificar al creador
        GroupInviteResponseWS ws = new GroupInviteResponseWS();
        ws.inviteId = inv.getId();
        ws.groupId = inv.getChat().getId();
        ws.groupName = inv.getChat().getNombreGrupo();
        ws.inviteeId = inv.getInvitee().getId();
        ws.inviteeNombre = inv.getInvitee().getNombre();
        ws.status = InviteStatus.DECLINED;

        NotificationEntity notif = new NotificationEntity();
        notif.setUserId(inv.getInviter().getId());
        notif.setType(NotificationType.GROUP_INVITE_RESPONSE);
        notif.setPayloadJson(Utils.writeJson(ws));
        notif.setSeen(false);
        notificationRepo.save(notif);

        int unseenForCreator = (int) notificationRepo.countByUserIdAndSeenFalse(inv.getInviter().getId());
        ws.unseenCount = unseenForCreator;
        Utils.sendNotif(messagingTemplate, inv.getInviter().getId(), ws);

        // marcar vista la notificación del invitado
        notificationRepo.findFirstByUserIdAndTypeAndPayloadJsonContaining(
                userId, NotificationType.GROUP_INVITE, "\"inviteId\":" + inviteId
        ).ifPresent(n -> { n.setSeen(true); notificationRepo.save(n); });

        int unseenForInvitee = (int) notificationRepo.countByUserIdAndSeenFalse(userId);
        Utils.sendNotif(messagingTemplate, userId, new UnseenCountWS(userId, unseenForInvitee));
    }
}
