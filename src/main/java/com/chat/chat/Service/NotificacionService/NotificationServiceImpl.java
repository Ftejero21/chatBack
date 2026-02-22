package com.chat.chat.Service.NotificacionService;

import com.chat.chat.DTO.NotificationDTO;
import com.chat.chat.DTO.UnseenCountWS;
import com.chat.chat.Entity.NotificationEntity;
import com.chat.chat.Repository.NotificationRepo;
import com.chat.chat.Utils.MappingUtils;
import com.chat.chat.Utils.Constantes;
import com.chat.chat.Utils.Utils;
import com.chat.chat.Utils.ExceptionConstants;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.*;

import java.util.List;
import java.util.Objects;

@Service
public class NotificationServiceImpl implements NotificationService {

    @Autowired
    private NotificationRepo notificationRepo;
    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Override
    public long unseenCount(Long userId) {
        return notificationRepo.countByUserIdAndSeenFalse(userId);
    }

    @Override
    public List<NotificationDTO> list(Long userId) {
        return MappingUtils.notificationEntityListADto(
                notificationRepo.findByUserIdOrderByCreatedAtDesc(userId));
    }

    @Override
    @Transactional
    public void markSeen(Long userId, Long notificationId) {
        NotificationEntity n = notificationRepo.findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException(Constantes.MSG_NOTIFICACION_NO_EXISTE + notificationId));
        if (!Objects.equals(n.getUserId(), userId))
            throw new IllegalArgumentException(ExceptionConstants.ERROR_NOT_AUTHORIZED_MARK);

        n.setSeen(true);
        notificationRepo.save(n);

        int count = (int) notificationRepo.countByUserIdAndSeenFalse(userId);
        Utils.sendNotif(messagingTemplate, userId, new UnseenCountWS(userId, count));
    }

    @Override
    @Transactional
    public void markAllSeen(Long userId) {
        // notificationRepo.markAllSeenByUserId(userId); // opción eficiente
        List<NotificationEntity> list = notificationRepo.findByUserIdOrderByCreatedAtDesc(userId);
        list.forEach(n -> n.setSeen(true));
        notificationRepo.saveAll(list);

        int count = (int) notificationRepo.countByUserIdAndSeenFalse(userId);
        Utils.sendNotif(messagingTemplate, userId, new UnseenCountWS(userId, count));
    }

    @Override
    public List<NotificationDTO> listPending(Long userId) {
        return MappingUtils.notificationEntityListADto(
                notificationRepo.findByUserIdAndResolvedFalseOrderByCreatedAtDesc(userId));
    }

    @Override
    @Transactional
    public void resolve(Long userId, Long notificationId) {
        NotificationEntity n = notificationRepo.findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException(Constantes.MSG_NOTIFICACION_NO_EXISTE + notificationId));
        if (!Objects.equals(n.getUserId(), userId))
            throw new IllegalArgumentException(ExceptionConstants.ERROR_NOT_AUTHORIZED_RESOLVE);

        n.setResolved(true);
        notificationRepo.save(n);
    }
}
