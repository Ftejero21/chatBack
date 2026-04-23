package com.chat.chat.Controller;

import com.chat.chat.DTO.UserComplaintCreateDTO;
import com.chat.chat.DTO.UserComplaintDTO;
import com.chat.chat.DTO.UserComplaintStatsDTO;
import com.chat.chat.DTO.UserExpedienteDTO;
import com.chat.chat.Service.UserComplaintService.UserComplaintService;
import com.chat.chat.Utils.Constantes;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.Valid;

@RestController
@RequestMapping(Constantes.USUARIO_API)
@CrossOrigin(Constantes.CORS_ANY_ORIGIN)
@Tag(name = "Denuncias de Usuarios", description = "Flujo de denuncias entre usuarios y panel admin.")
public class UserComplaintController {

    private final UserComplaintService userComplaintService;

    public UserComplaintController(UserComplaintService userComplaintService) {
        this.userComplaintService = userComplaintService;
    }

    @PostMapping(Constantes.USER_COMPLAINT_CREATE)
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Crear denuncia de usuario")
    public ResponseEntity<UserComplaintDTO> createComplaint(@Valid @RequestBody UserComplaintCreateDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userComplaintService.createComplaint(request));
    }

    @GetMapping(Constantes.ADMIN_USER_COMPLAINT_LIST)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Listar denuncias (admin)")
    public ResponseEntity<Page<UserComplaintDTO>> listComplaints(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "8") int size) {
        return ResponseEntity.ok(userComplaintService.listComplaints(page, size));
    }

    @GetMapping(Constantes.ADMIN_USER_COMPLAINT_STATS)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Stats denuncias (admin)")
    public ResponseEntity<UserComplaintStatsDTO> getStats() {
        return ResponseEntity.ok(userComplaintService.getStats());
    }

    @PatchMapping(Constantes.ADMIN_USER_COMPLAINT_READ)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Marcar denuncia leida (admin)")
    public ResponseEntity<UserComplaintDTO> markAsRead(@PathVariable("id") Long id) {
        return ResponseEntity.ok(userComplaintService.markAsRead(id));
    }

    @GetMapping(Constantes.ADMIN_USER_COMPLAINT_EXPEDIENTE)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Expediente de denuncias por usuario (admin)")
    public ResponseEntity<UserExpedienteDTO> getExpediente(@PathVariable("userId") Long userId) {
        return ResponseEntity.ok(userComplaintService.getExpediente(userId));
    }
}
