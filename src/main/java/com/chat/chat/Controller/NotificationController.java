package com.chat.chat.Controller;

import com.chat.chat.DTO.NotificationDTO;
import com.chat.chat.Service.NotificacionService.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@CrossOrigin("*")
public class NotificationController {

    @Autowired
    private NotificationService notificationService;

    // 👇 como no usas SecurityContext, pasamos userId por query (ajústalo si luego añades seguridad)
    @GetMapping("/count")
    public Map<String, Object> unseenCount(@RequestParam Long userId) {
        long count = notificationService.unseenCount(userId);
        return Map.of("unseenCount", count);
    }


    @GetMapping("/{userId}/pending")
    public List<NotificationDTO> listPending(@PathVariable Long userId) {
        return notificationService.listPending(userId);
    }


    @PostMapping("/{notifId}/resolve")
    public void resolve(@PathVariable Long notifId, @RequestParam Long userId) {
        notificationService.resolve(userId, notifId);
    }

    @GetMapping
    public List<NotificationDTO> list(@RequestParam Long userId) {
        return notificationService.list(userId);
    }

    @PostMapping("/{id}/seen")
    public void markSeen(@RequestParam Long userId, @PathVariable Long id) {
        notificationService.markSeen(userId, id);
    }

    @PostMapping("/seen-all")
    public void markAllSeen(@RequestParam Long userId) {
        notificationService.markAllSeen(userId);
    }
}
