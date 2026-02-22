package com.chat.chat.Service.UsuarioService;

import com.chat.chat.DTO.UsuarioDTO;
import com.chat.chat.Entity.UsuarioEntity;
import com.chat.chat.Exceptions.EmailNoRegistradoException;
import com.chat.chat.Exceptions.PasswordIncorrectaException;
import com.chat.chat.Exceptions.UsuarioInactivoException;
import com.chat.chat.Repository.UsuarioRepository;
import com.chat.chat.Utils.Constantes;
import com.chat.chat.Utils.MappingUtils;
import com.chat.chat.Utils.Utils;
import com.chat.chat.Utils.ExceptionConstants;
import com.chat.chat.Repository.MensajeRepository;
import com.chat.chat.Repository.ChatRepository;
import com.chat.chat.DTO.DashboardStatsDTO;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import com.chat.chat.DTO.AuthRespuestaDTO;
import com.chat.chat.Security.CustomUserDetailsService;
import com.chat.chat.Security.JwtService;
import com.chat.chat.Service.EmailService.EmailService;
import com.chat.chat.Utils.SecurityUtils;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static io.micrometer.core.instrument.util.StringEscapeUtils.escapeJson;

@Service
public class UsuarioServiceImpl implements UsuarioService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private EmailService emailService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ChatRepository chatRepository;

    @Autowired
    private MensajeRepository mensajeRepository;

    @Value("${app.uploads.root:uploads}") // carpeta base
    private String uploadsRoot;

    @Value("${app.uploads.base-url:/uploads}") // prefijo público
    private String uploadsBaseUrl;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    // repos, encoder, etc. inyectados…

    @Autowired
    private JwtService jwtService;
    @Autowired
    private CustomUserDetailsService customUserDetailsService;
    @Autowired
    private SecurityUtils securityUtils;

    @Override
    public AuthRespuestaDTO crearUsuarioConToken(UsuarioDTO dto) {

        if (usuarioRepository.findByEmail(dto.getEmail()).isPresent()) {
            throw new RuntimeException(ExceptionConstants.ERROR_EMAIL_EXISTS);
        }

        // 1) foto: si llega como dataURL, guardamos y sustituimos por URL
        String fotoUrl = null;
        if (dto.getFoto() != null) {
            String f = dto.getFoto();
            if (dto.getFoto() != null && dto.getFoto().startsWith("data:image")) {
                String url = Utils.saveDataUrlToUploads(dto.getFoto(), "avatars", uploadsRoot, uploadsBaseUrl);
                dto.setFoto(url); // guarda URL pública en DTO
            } else if (f.startsWith("/uploads/") || f.startsWith("http")) {
                fotoUrl = f; // ya es una URL válida
            }
            // si quieres, limpia dto para no guardar base64 por error
            dto.setFoto(fotoUrl);
        }

        // 2) mapear y ajustar campos
        UsuarioEntity entity = MappingUtils.usuarioDtoAEntity(dto);
        entity.setFechaCreacion(LocalDateTime.now());
        entity.setActivo(true);
        entity.setRoles(Collections.singleton(Constantes.USUARIO));

        String encryptedPassword = passwordEncoder.encode(dto.getPassword());
        entity.setPassword(encryptedPassword);

        // setear fotoUrl en la entidad (si procede)
        if (fotoUrl != null) {
            entity.setFotoUrl(fotoUrl);
        } else if (entity.getFotoUrl() == null) {
            // deja null si no hay foto
            entity.setFotoUrl(null);
        }

        UsuarioEntity saved = usuarioRepository.save(entity);
        UsuarioDTO savedDto = MappingUtils.usuarioEntityADto(saved);

        // Generar Token
        UserDetails userDetails = customUserDetailsService.loadUserByUsername(saved.getEmail());
        String jwtToken = jwtService.generateToken(userDetails);

        return new AuthRespuestaDTO(jwtToken, savedDto);
    }

    // Mantenemos este por compatibilidad interna si se necesita (aunque el de
    // arriba es el del endpoint ahora)
    @Override
    public UsuarioDTO crearUsuario(UsuarioDTO dto) {
        return crearUsuarioConToken(dto).getUsuario();
    }

    @Override
    public List<UsuarioDTO> listarUsuariosActivos() {
        Long authenticatedUserId = securityUtils.getAuthenticatedUserId();
        List<UsuarioDTO> list = usuarioRepository.findAll().stream()
                .filter(UsuarioEntity::isActivo)
                .filter(u -> !u.getId().equals(authenticatedUserId))
                .map(MappingUtils::usuarioEntityADto)
                .collect(Collectors.toList());

        // 🔄 Convertir /uploads/... a dataURL Base64 (igual que getById)
        for (UsuarioDTO dto : list) {
            String foto = dto.getFoto();
            if (foto != null && foto.startsWith("/uploads/")) {
                String dataUrl = Utils.toDataUrlFromUrl(foto, uploadsRoot);
                if (dataUrl != null) {
                    dto.setFoto(dataUrl);
                } // si devuelve null, dejamos la URL tal cual
            }
        }

        return list;
    }

    // Nuevo método login que devuelve Token
    public AuthRespuestaDTO loginConToken(String email, String password) {
        UsuarioEntity usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(EmailNoRegistradoException::new);

        if (!usuario.isActivo()) {
            throw new UsuarioInactivoException("Esta cuenta ha sido inhabilitada por un administrador.");
        }

        if (!passwordEncoder.matches(password, usuario.getPassword())) {
            throw new PasswordIncorrectaException();
        }

        UsuarioDTO dto = MappingUtils.usuarioEntityADto(usuario);
        if (dto.getFoto() != null && dto.getFoto().startsWith("/uploads/")) {
            dto.setFoto(Utils.toDataUrlFromUrl(dto.getFoto(), uploadsRoot)); // data:image/...;base64,...
        }

        UserDetails userDetails = customUserDetailsService.loadUserByUsername(usuario.getEmail());
        String jwtToken = jwtService.generateToken(userDetails);

        return new AuthRespuestaDTO(jwtToken, dto);
    }

    @Override
    public void updatePublicKey(Long id, String publicKey) {
        UsuarioEntity usuario = usuarioRepository.findById(id).orElseThrow();
        // Solo el propio usuario autenticado debería poder actualizar su propia llave
        if (!usuario.getId().equals(securityUtils.getAuthenticatedUserId())) {
            throw new RuntimeException(ExceptionConstants.ERROR_NOT_AUTHORIZED_PUBLIC_KEY);
        }
        usuario.setPublicKey(publicKey);
        usuarioRepository.save(usuario);
    }

    @Override
    public UsuarioDTO login(String email, String password) {
        return loginConToken(email, password).getUsuario();
    }

    @Override
    public UsuarioDTO getById(Long id) {
        UsuarioEntity u = usuarioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        UsuarioDTO dto = MappingUtils.usuarioEntityADto(u);
        // 👉 Si la foto es una URL pública (/uploads/...), la convertimos a Base64 para
        // el front
        if (dto.getFoto() != null && dto.getFoto().startsWith("/uploads/")) {
            dto.setFoto(Utils.toDataUrlFromUrl(dto.getFoto(), uploadsRoot));
        }
        return dto;
    }

    @Override
    public List<UsuarioDTO> buscarPorNombre(String q) {
        if (q == null || q.trim().isEmpty()) {
            return Collections.emptyList();
        }
        Long authenticatedUserId = securityUtils.getAuthenticatedUserId();
        String query = q.trim();

        List<UsuarioDTO> list = usuarioRepository.searchActivosByNombre(query)
                .stream()
                .filter(u -> !u.getId().equals(authenticatedUserId))
                .map(MappingUtils::usuarioEntityADto)
                .collect(Collectors.toList());

        // Convertir /uploads/... a dataURL Base64 (igual que en otros métodos)
        for (UsuarioDTO dto : list) {
            String foto = dto.getFoto();
            if (foto != null && foto.startsWith("/uploads/")) {
                String dataUrl = Utils.toDataUrlFromUrl(foto, uploadsRoot);
                if (dataUrl != null) {
                    dto.setFoto(dataUrl);
                }
            }
        }

        return list;
    }

    @Override
    @Transactional
    public void bloquearUsuario(Long bloqueadoId) {
        Long authenticatedUserId = securityUtils.getAuthenticatedUserId();
        System.out.println("INTENTO DE BLOQUEO: blocker=" + authenticatedUserId + " bloqueado=" + bloqueadoId);
        if (authenticatedUserId.equals(bloqueadoId)) {
            throw new RuntimeException(ExceptionConstants.ERROR_CANT_BLOCK_SELF);
        }

        UsuarioEntity user = usuarioRepository.findById(authenticatedUserId).orElseThrow();
        UsuarioEntity blocked = usuarioRepository.findById(bloqueadoId).orElseThrow();

        if (user.getBloqueados().add(blocked)) {
            System.out.println("USUARIO BLOQUEADO CON ÉXITO EN BD");
            usuarioRepository.save(user);
            // Notify the blocked user via STOMP that their status changed
            messagingTemplate.convertAndSend("/topic/user/" + bloqueadoId + "/bloqueos",
                    "{\"blockerId\":" + authenticatedUserId + ",\"type\":\"BLOCKED\"}");
        } else {
            System.out.println("USUARIO YA ESTABA BLOQUEADO EN BD");
        }
    }

    @Override
    @Transactional
    public void desbloquearUsuario(Long bloqueadoId) {
        Long authenticatedUserId = securityUtils.getAuthenticatedUserId();

        UsuarioEntity user = usuarioRepository.findById(authenticatedUserId).orElseThrow();
        UsuarioEntity blocked = usuarioRepository.findById(bloqueadoId).orElseThrow();

        if (user.getBloqueados().remove(blocked)) {
            usuarioRepository.save(user);
            // Notify the unblocked user via STOMP
            messagingTemplate.convertAndSend("/topic/user/" + bloqueadoId + "/bloqueos",
                    "{\"blockerId\":" + authenticatedUserId + ",\"type\":\"UNBLOCKED\"}");
        }
    }

    @Override
    public boolean existePorEmail(String email) {
        return usuarioRepository.findByEmail(email).isPresent();
    }

    @Override
    @Transactional
    public void actualizarPasswordPorEmail(String email, String newPassword) {
        UsuarioEntity usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(EmailNoRegistradoException::new);
        String encryptedPassword = passwordEncoder.encode(newPassword);
        usuario.setPassword(encryptedPassword);
        usuarioRepository.save(usuario);
    }

    @Override
    public DashboardStatsDTO getDashboardStats() {
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        java.time.LocalDateTime startOfToday = now.toLocalDate().atStartOfDay();
        java.time.LocalDateTime startOfYesterday = startOfToday.minusDays(1);

        // Usuarios
        long usuariosTotalesAyer = usuarioRepository.countUsuariosTotalesHasta(startOfToday);
        long usuariosTotalesHoy = usuarioRepository.countUsuariosTotalesHasta(now);
        long usuariosHoyNuevos = usuarioRepository.countUsuariosRegistradosEntreFechas(startOfToday, now);
        long usuariosAyerNuevos = usuarioRepository.countUsuariosRegistradosEntreFechas(startOfYesterday, startOfToday);
        double pctUsuarios = calcularPorcentaje(usuariosTotalesAyer, usuariosTotalesHoy);

        // Chats Activos (Totales históricos) - No tenemos baja lógica de chats así que
        // nos basamos en creación
        long chatsTotalesAyer = chatRepository.countChatsEntreFechas(java.time.LocalDateTime.MIN, startOfToday);
        long chatsTotalesHoy = chatRepository.countChatsEntreFechas(java.time.LocalDateTime.MIN, now);
        double pctChats = calcularPorcentaje(chatsTotalesAyer, chatsTotalesHoy);

        // Reportes (Mockeado a 0 como se especificó en el plan porque no hay Entidad
        // Reporte)
        long reportes = 0;
        double pctReportes = 0.0;

        // Mensajes
        long mensajesHoy = mensajeRepository.countMensajesEntreFechas(startOfToday, now);
        long mensajesAyer = mensajeRepository.countMensajesEntreFechas(startOfYesterday, startOfToday);
        double pctMensajes = calcularPorcentaje(mensajesAyer, mensajesHoy);

        return new DashboardStatsDTO(
                usuariosTotalesHoy, pctUsuarios,
                chatsTotalesHoy, pctChats,
                reportes, pctReportes,
                mensajesHoy, pctMensajes);
    }

    private double calcularPorcentaje(long viejo, long nuevo) {
        if (viejo == 0) {
            return nuevo > 0 ? 100.0 : 0.0;
        }
        return ((double) (nuevo - viejo) / viejo) * 100.0;
    }

    @Override
    public List<UsuarioDTO> listarRecientes() {
        Pageable top10 = PageRequest.of(0, 50); // Traemos más para poder filtrar en memoria
        List<UsuarioEntity> entidades = usuarioRepository.findTop10Recientes(top10);
        return entidades.stream()
                .filter(u -> u.getRoles() == null || !u.getRoles().contains(Constantes.ADMIN))
                .limit(10)
                .map(MappingUtils::usuarioEntityADto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void banearUsuario(Long id, String motivo) {
        UsuarioEntity bloqueado = usuarioRepository.findById(id).orElseThrow();

        // Motivo por defecto si viene null o vacío
        String motivoFinal = (motivo == null || motivo.trim().isEmpty())
                ? "Tu cuenta ha sido suspendida temporalmente por incumplimiento de las normas de uso de TejeChat."
                : motivo.trim();

        bloqueado.setActivo(false);
        usuarioRepository.save(bloqueado);

        // 1. WebSocket con el motivo final
        messagingTemplate.convertAndSendToUser(
                bloqueado.getEmail(),
                "/queue/baneos",
                "{\"banned\": true, \"motivo\": \"" + escapeJson(motivoFinal) + "\"}"
        );

        // 2. Enviar Email con el motivo final
        Map<String, String> vars = new java.util.HashMap<>();
        vars.put("nombre", bloqueado.getNombre());
        vars.put("motivo", motivoFinal);

        emailService.sendHtmlEmail(
                bloqueado.getEmail(),
                "Aviso de suspensión de cuenta - TejeChat",
                "templates/user-banned.html",
                vars
        );
    }

    @Override
    @Transactional
    public void desbanearAdministrativamente(Long id) {
        UsuarioEntity vetado = usuarioRepository.findById(id).orElseThrow();

        vetado.setActivo(true);
        usuarioRepository.save(vetado);

        // 3. Enviar Email de Desbaneo
        Map<String, String> vars = new java.util.HashMap<>();
        vars.put("nombre", vetado.getNombre());

        emailService.sendHtmlEmail(
                vetado.getEmail(),
                "¡Cuenta reactivada! Bienvenido de nuevo - TejeChat",
                "templates/user-unbanned.html",
                vars);
    }
}
