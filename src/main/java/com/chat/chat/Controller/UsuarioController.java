package com.chat.chat.Controller;

import com.chat.chat.DTO.AuthRespuestaDTO;
import com.chat.chat.DTO.LoginRequestDTO;
import com.chat.chat.DTO.UsuarioDTO;
import com.chat.chat.DTO.DashboardStatsDTO;
import com.chat.chat.Service.UsuarioService.UsuarioService;
import com.chat.chat.Utils.Constantes;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import com.chat.chat.Service.AuthService.PasswordResetService;

@RestController
@RequestMapping(Constantes.USUARIO_API)
@CrossOrigin("*")
public class UsuarioController {

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private PasswordResetService passwordResetService;

    @PostMapping(Constantes.LOGIN)
    public AuthRespuestaDTO login(@RequestBody LoginRequestDTO dto) {
        return usuarioService.loginConToken(dto.getEmail(), dto.getPassword());
    }

    @PostMapping(Constantes.REGISTRO)
    public AuthRespuestaDTO crearUsuario(@RequestBody UsuarioDTO dto) {
        return usuarioService.crearUsuarioConToken(dto);
    }

    @GetMapping(Constantes.USUARIOS_ACTIVOS)
    public List<UsuarioDTO> listarActivos() {
        return usuarioService.listarUsuariosActivos();
    }

    @GetMapping(Constantes.USUARIO_POR_ID)
    public UsuarioDTO getById(@PathVariable("id") Long id) {
        return usuarioService.getById(id);
    }

    @GetMapping(Constantes.USUARIO_BUSCAR)
    public List<UsuarioDTO> buscar(@RequestParam("q") String q) {
        return usuarioService.buscarPorNombre(q);
    }

    @PutMapping(Constantes.USUARIO_PUBLIC_KEY)
    public void updatePublicKey(@PathVariable("id") Long id, @RequestBody java.util.Map<String, String> payload) {
        String publicKey = payload.get("publicKey");
        usuarioService.updatePublicKey(id, publicKey);
    }

    @PostMapping(Constantes.USUARIO_BLOQUEAR)
    public ResponseEntity<java.util.Map<String, String>> bloquearUsuario(
            @PathVariable("bloqueadoId") Long bloqueadoId) {
        usuarioService.bloquearUsuario(bloqueadoId);
        return ResponseEntity.ok(java.util.Map.of("mensaje", "Usuario bloqueado"));
    }

    @PostMapping(Constantes.USUARIO_DESBLOQUEAR)
    public ResponseEntity<java.util.Map<String, String>> desbloquearUsuario(
            @PathVariable("bloqueadoId") Long bloqueadoId) {
        usuarioService.desbloquearUsuario(bloqueadoId);
        return ResponseEntity.ok(java.util.Map.of("mensaje", "Usuario desbloqueado"));
    }

    @PostMapping(Constantes.RECUPERAR_PASSWORD_SOLICITAR)
    public ResponseEntity<Map<String, String>> solicitarRecuperacion(@RequestBody Map<String, String> payload) {
        String email = payload.get("email");
        if (email == null || email.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("mensaje", "Email es requerido"));
        }

        if (!usuarioService.existePorEmail(email)) {
            return ResponseEntity.badRequest()
                    .body(Map.of("mensaje", "El email proporcionado no está registrado en el sistema."));
        }

        try {
            passwordResetService.generateAndSendResetCode(email);
            return ResponseEntity.ok(Map.of("mensaje", "Código enviado al correo"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("mensaje", "Error enviando correo"));
        }
    }

    @PostMapping(Constantes.RECUPERAR_PASSWORD_VERIFICAR)
    public ResponseEntity<Map<String, String>> verificarYCambiarPassword(@RequestBody Map<String, String> payload) {
        String email = payload.get("email");
        String code = payload.get("code");
        String newPassword = payload.get("newPassword");

        if (email == null || code == null || newPassword == null) {
            return ResponseEntity.badRequest().body(Map.of("mensaje", "Faltan datos requeridos"));
        }

        boolean isValid = passwordResetService.isCodeValid(email, code);
        if (!isValid) {
            return ResponseEntity.badRequest().body(Map.of("mensaje", "Código inválido o expirado"));
        }

        try {
            usuarioService.actualizarPasswordPorEmail(email, newPassword);
            passwordResetService.invalidateCode(email);
            return ResponseEntity.ok(Map.of("mensaje", "Contraseña actualizada exitosamente"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("mensaje", e.getMessage()));
        }
    }

    @GetMapping("/admin/dashboard-stats")
    public DashboardStatsDTO getDashboardStats() {
        return usuarioService.getDashboardStats();
    }

    @GetMapping("/admin/recientes")
    public List<UsuarioDTO> listarRecientes() {
        return usuarioService.listarRecientes();
    }

    @PostMapping("/admin/{id}/ban")
    public ResponseEntity<?> banear(
        @PathVariable("id") Long id, 
        @RequestParam("motivo") String motivo) { // Recibimos el motivo aquí
        
        usuarioService.banearUsuario(id, motivo);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/admin/{id}/unban")
    public ResponseEntity<Map<String, String>> desbanearUsuario(@PathVariable("id") Long id) {
        usuarioService.desbanearAdministrativamente(id);
        return ResponseEntity.ok(Map.of("mensaje", "Usuario reactivado exitosamente"));
    }
}
