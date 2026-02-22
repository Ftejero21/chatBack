package com.chat.chat.Controller;

import com.chat.chat.DTO.NotificationDTO;
import com.chat.chat.Service.NotificacionService.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import com.chat.chat.Utils.Constantes;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@CrossOrigin("*")
public class NotificationController {

    @Autowired
    private NotificationService notificationService;

    // 👇 como no usas SecurityContext, pasamos userId por query (ajústalo si luego
    // añades seguridad)
    @GetMapping(Constantes.NOTIFICACIONES_COUNT)
    public Map<String, Object> unseenCount(@RequestParam("userId") Long userId) {
        long count = notificationService.unseenCount(userId);
        return Map.of("unseenCount", count);
    }

    @GetMapping(Constantes.NOTIFICACIONES_PENDIENTES)
    public List<NotificationDTO> listPending(@PathVariable("userId") Long userId) {
        return notificationService.listPending(userId);
    }

    @PostMapping(Constantes.NOTIFICACIONES_RESOLVER)
    public void resolve(@PathVariable("notifId") Long notifId, @RequestParam("userId") Long userId) {
        notificationService.resolve(userId, notifId);
    }

    @GetMapping
    public List<NotificationDTO> list(@RequestParam("userId") Long userId) {
        return notificationService.list(userId);
    }

    @PostMapping(Constantes.NOTIFICACIONES_VISTA)
    public void markSeen(@RequestParam("userId") Long userId, @PathVariable("id") Long id) {
        notificationService.markSeen(userId, id);
    }

    @PostMapping(Constantes.NOTIFICACIONES_VISTAS_TODAS)
    public void markAllSeen(@RequestParam("userId") Long userId) {
        notificationService.markAllSeen(userId);
    }
}
