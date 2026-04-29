package com.chat.chat.Service.UsuarioService;

import com.chat.chat.DTO.UsuarioDTO;
import com.chat.chat.DTO.ActualizarPerfilDTO;
import com.chat.chat.DTO.E2EPrivateKeyBackupDTO;
import com.chat.chat.DTO.E2ERekeyRequestDTO;
import com.chat.chat.DTO.E2EStateDTO;
import com.chat.chat.DTO.GoogleAuthRequestDTO;
import com.chat.chat.DTO.GoogleTokenPayloadDTO;
import com.chat.chat.Entity.E2EPrivateKeyBackupEntity;
import com.chat.chat.Entity.SolicitudDesbaneoEntity;
import com.chat.chat.Entity.UserBlockRelationEntity;
import com.chat.chat.Entity.UserModerationHistoryEntity;
import com.chat.chat.Entity.UsuarioEntity;
import com.chat.chat.Exceptions.EmailNoRegistradoException;
import com.chat.chat.Exceptions.EmailYaExisteException;
import com.chat.chat.Exceptions.E2ERekeyConflictException;
import com.chat.chat.Exceptions.GoogleAuthException;
import com.chat.chat.Exceptions.PasswordIncorrectaException;
import com.chat.chat.Exceptions.SemanticApiException;
import com.chat.chat.Exceptions.UsuarioInactivoException;
import com.chat.chat.Mapper.E2EPrivateKeyBackupMapper;
import com.chat.chat.Repository.E2EPrivateKeyBackupRepository;
import com.chat.chat.Repository.UsuarioRepository;
import com.chat.chat.Repository.SolicitudDesbaneoRepository;
import com.chat.chat.Repository.UserBlockRelationRepository;
import com.chat.chat.Repository.UserModerationHistoryRepository;
import com.chat.chat.Utils.Constantes;
import com.chat.chat.Utils.MappingUtils;
import com.chat.chat.Utils.Utils;
import com.chat.chat.Utils.ExceptionConstants;
import com.chat.chat.Utils.AdminAuditCrypto;
import com.chat.chat.Utils.E2EDiagnosticUtils;
import com.chat.chat.Utils.BlockSource;
import com.chat.chat.Utils.ModerationActionType;
import com.chat.chat.Repository.MensajeRepository;
import com.chat.chat.Repository.ChatRepository;
import com.chat.chat.DTO.DashboardStatsDTO;
import com.chat.chat.DTO.BlockedUserDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import com.chat.chat.DTO.AuthRespuestaDTO;
import com.chat.chat.Security.CustomUserDetailsService;
import com.chat.chat.Security.JwtService;
import com.chat.chat.Service.AuthService.GoogleIdTokenValidatorService;
import com.chat.chat.Service.EmailService.EmailService;
import com.chat.chat.Service.AuthService.PasswordChangeService;
import com.chat.chat.Utils.SecurityUtils;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Base64;
import java.util.Collections;
import java.util.HashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static io.micrometer.core.instrument.util.StringEscapeUtils.escapeJson;

@Service
public class UsuarioServiceImpl implements UsuarioService {
    private static final Logger LOGGER = LoggerFactory.getLogger(UsuarioServiceImpl.class);
    private static final int MAX_ENCRYPTED_PRIVATE_KEY_LEN = 65535;
    private static final int MAX_IV_LEN = 1024;
    private static final int MAX_SALT_LEN = 1024;
    private static final int MAX_KDF_LEN = 32;
    private static final int MAX_KDF_HASH_LEN = 32;
    private static final int MAX_PUBLIC_KEY_LEN = 65535;
    private static final int MAX_PUBLIC_KEY_FINGERPRINT_LEN = 256;
    private static final int MIN_KDF_ITERATIONS = 10000;
    private static final int MAX_KDF_ITERATIONS = 10000000;
    private static final int MIN_KEY_LENGTH_BITS = 128;
    private static final int MAX_KEY_LENGTH_BITS = 8192;
    private static final int MAX_MODERATION_REASON_LENGTH = 500;
    private static final int MIN_ADMIN_MODERATION_REASON_LENGTH = 10;
    private static final int MAX_MODERATION_ORIGIN_LENGTH = 80;
    private static final int MAX_MODERATION_DESCRIPTION_LENGTH = 5000;
    private static final String DEFAULT_MODERATION_ORIGIN = "panel_admin";
    private static final String DNI_NIE_CONTROL_LETTERS = "TRWAGMYFPDXBNJZSQVHLCKE";
    private static final int MAX_TELEFONO_LENGTH = 32;
    private static final int MAX_FECHA_NACIMIENTO_LENGTH = 32;
    private static final int MAX_GENERO_LENGTH = 32;
    private static final int MAX_DIRECCION_LENGTH = 255;
    private static final int MAX_NACIONALIDAD_LENGTH = 80;
    private static final int MAX_OCUPACION_LENGTH = 120;
    private static final int MAX_INSTAGRAM_LENGTH = 120;
    private static final long MAX_PROFILE_IMAGE_BYTES = 5L * 1024L * 1024L;
    private static final String DATA_IMAGE_BASE64_MARKER = ";base64,";
    private static final String PROFILE_PHOTO_INVALID_SOURCE_MSG = "foto debe ser data:image/* valida o /uploads/... del sistema";
    private static final String PROFILE_PHOTO_INVALID_FORMAT_MSG = "foto de perfil invalida: formato no permitido";
    private static final String PROFILE_PHOTO_INVALID_SIZE_MSG = "foto de perfil invalida: tamano excede el limite";
    private static final Set<String> PROFILE_IMAGE_MIME_WHITELIST = Set.of("image/jpeg", "image/jpg", "image/png", "image/webp");
    private static final Set<String> PROFILE_IMAGE_EXT_WHITELIST = Set.of("jpg", "jpeg", "png", "webp");

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

    @Autowired
    private SolicitudDesbaneoRepository solicitudDesbaneoRepository;

    @Autowired
    private E2EPrivateKeyBackupRepository e2EPrivateKeyBackupRepository;

    @Autowired
    private UserModerationHistoryRepository userModerationHistoryRepository;

    @Autowired
    private UserBlockRelationRepository userBlockRelationRepository;

    @Value("${app.uploads.root:uploads}") // carpeta base
    private String uploadsRoot;

    @Value("${app.uploads.base-url:/uploads}") // prefijo pÃºblico
    private String uploadsBaseUrl;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    // repos, encoder, etc. inyectadosâ€¦

    @Autowired
    private JwtService jwtService;
    @Autowired
    private CustomUserDetailsService customUserDetailsService;
    @Autowired
    private SecurityUtils securityUtils;
    @Autowired
    private PasswordChangeService passwordChangeService;
    @Autowired
    private GoogleIdTokenValidatorService googleIdTokenValidatorService;
    @Autowired
    private AdminAuditCrypto adminAuditCrypto;

    @Autowired
    private E2EPrivateKeyBackupMapper e2EPrivateKeyBackupMapper;

    @Override
    public AuthRespuestaDTO crearUsuarioConToken(UsuarioDTO dto) {
        dto.setEmail(normalizeEmail(dto.getEmail()));

        if (usuarioRepository.findByEmail(dto.getEmail()).isPresent()) {
            throw new EmailYaExisteException(ExceptionConstants.ERROR_EMAIL_EXISTS);
        }

        // 1) foto: si llega como dataURL, guardamos y sustituimos por URL
        String fotoUrl = null;
        if (dto.getFoto() != null) {
            String f = dto.getFoto();
            if (dto.getFoto() != null && dto.getFoto().startsWith(Constantes.DATA_IMAGE_PREFIX)) {
                String url = Utils.saveDataUrlToUploads(dto.getFoto(), Constantes.DIR_AVATARS, uploadsRoot, uploadsBaseUrl);
                fotoUrl = url;
                dto.setFoto(url); // guarda URL pÃºblica en DTO
            } else if (f.startsWith(Constantes.UPLOADS_PREFIX) || f.startsWith(Constantes.HTTP_PREFIX)) {
                fotoUrl = f; // ya es una URL vÃ¡lida
            }
            // si quieres, limpia dto para no guardar base64 por error
            dto.setFoto(fotoUrl);
        }

        // 2) mapear y ajustar campos
        UsuarioEntity entity = MappingUtils.usuarioDtoAEntity(dto);
        entity.setFechaCreacion(LocalDateTime.now());
        entity.setActivo(true);
        entity.setEmailVerificado(false);
        entity.setRoles(Collections.singleton(Constantes.USUARIO));
        if (hasPublicKey(entity.getPublicKey())) {
            entity.setPublicKeyUpdatedAt(LocalDateTime.now());
        }

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
        applyBlockedSources(savedDto, saved.getId());

        // Generar Token
        UserDetails userDetails = customUserDetailsService.loadUserByUsername(saved.getEmail());
        String jwtToken = jwtService.generateToken(userDetails);

        return new AuthRespuestaDTO(
                jwtToken,
                savedDto,
                adminAuditCrypto.getAuditPublicKeySpkiBase64(),
                requiresProfileCompletion(saved));
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
        List<UsuarioDTO> list = usuarioRepository.findByActivoTrueAndIdNot(authenticatedUserId).stream()
                .filter(u -> !isAdminUser(u.getRoles()))
                .map(MappingUtils::usuarioEntityADto)
                .collect(Collectors.toList());

        // ðŸ”„ Convertir /uploads/... a dataURL Base64 (igual que getById)
        for (UsuarioDTO dto : list) {
            String foto = dto.getFoto();
            if (foto != null && foto.startsWith(Constantes.UPLOADS_PREFIX)) {
                String dataUrl = Utils.toDataUrlFromUrl(foto, uploadsRoot);
                if (dataUrl != null) {
                    dto.setFoto(dataUrl);
                } // si devuelve null, dejamos la URL tal cual
            }
        }

        return list;
    }

    private boolean isAdminUser(Set<String> roles) {
        if (roles == null || roles.isEmpty()) {
            return false;
        }
        return roles.stream().anyMatch(role -> Constantes.ADMIN.equalsIgnoreCase(role) || Constantes.ROLE_ADMIN.equalsIgnoreCase(role));
    }

    private void validateSelfOrAdmin(Long targetUserId) {
        Long authenticatedUserId = securityUtils.getAuthenticatedUserId();
        if (authenticatedUserId != null && targetUserId != null
                && authenticatedUserId.longValue() == targetUserId.longValue()) {
            return;
        }
        boolean requesterIsAdmin = securityUtils.hasRole(Constantes.ADMIN)
                || securityUtils.hasRole(Constantes.ROLE_ADMIN)
                || usuarioRepository.findById(authenticatedUserId)
                .map(u -> isAdminUser(u.getRoles()))
                .orElse(false);
        if (!requesterIsAdmin) {
            throw new AccessDeniedException(ExceptionConstants.ERROR_NOT_AUTHORIZED_PUBLIC_KEY);
        }
    }

    private void validateSelfOrAdminForBackup(Long targetUserId) {
        Long authenticatedUserId = securityUtils.getAuthenticatedUserId();
        boolean self = authenticatedUserId != null && targetUserId != null
                && authenticatedUserId.longValue() == targetUserId.longValue();
        if (self) {
            return;
        }
        boolean admin = securityUtils.hasRole(Constantes.ADMIN)
                || securityUtils.hasRole(Constantes.ROLE_ADMIN)
                || usuarioRepository.findById(authenticatedUserId)
                .map(u -> isAdminUser(u.getRoles()))
                .orElse(false);
        if (admin) {
            return;
        }
        throw semanticError(HttpStatus.FORBIDDEN, Constantes.ERR_NO_AUTORIZADO, "No autorizado para operar este backup E2E");
    }

    private void validateUserExists(Long userId) {
        boolean exists = usuarioRepository.findFreshById(userId)
                .or(() -> usuarioRepository.findById(userId))
                .isPresent();
        if (!exists) {
            throw semanticError(HttpStatus.NOT_FOUND, Constantes.ERR_NO_ENCONTRADO, Constantes.MSG_USUARIO_NO_ENCONTRADO);
        }
    }

    private SemanticApiException semanticError(HttpStatus status, String code, String message) {
        String traceId = Optional.ofNullable(E2EDiagnosticUtils.currentTraceId()).orElse(E2EDiagnosticUtils.newTraceId());
        return new SemanticApiException(status, code, message, traceId);
    }

    private E2EStateDTO toE2EState(UsuarioEntity usuario) {
        E2EStateDTO state = new E2EStateDTO();
        boolean hasKey = hasPublicKey(usuario == null ? null : usuario.getPublicKey());
        state.setHasPublicKey(hasKey);
        state.setPublicKeyFingerprint(E2EDiagnosticUtils.fingerprint12(usuario == null ? null : usuario.getPublicKey()));
        LocalDateTime updatedAt = null;
        if (usuario != null) {
            updatedAt = usuario.getPublicKeyUpdatedAt() != null ? usuario.getPublicKeyUpdatedAt() : usuario.getFechaCreacion();
        }
        state.setUpdatedAt(updatedAt);
        return state;
    }

    private String normalizeFingerprint(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private boolean hasPublicKey(String key) {
        return key != null && !key.isBlank();
    }

    private String requireNonBlankWithinLimit(String value, String fieldName, int maxLen) {
        if (value == null || value.isBlank()) {
            throw semanticError(HttpStatus.BAD_REQUEST, Constantes.ERR_E2E_BACKUP_INVALID, fieldName + " es obligatorio");
        }
        if (value.length() > maxLen) {
            throw semanticError(HttpStatus.BAD_REQUEST, Constantes.ERR_E2E_BACKUP_INVALID,
                    fieldName + " supera longitud maxima permitida");
        }
        return value;
    }

    private int requireIntRange(Integer value, String fieldName, int min, int max) {
        if (value == null) {
            throw semanticError(HttpStatus.BAD_REQUEST, Constantes.ERR_E2E_BACKUP_INVALID, fieldName + " es obligatorio");
        }
        if (value < min || value > max) {
            throw semanticError(HttpStatus.BAD_REQUEST, Constantes.ERR_E2E_BACKUP_INVALID,
                    fieldName + " fuera de rango permitido");
        }
        return value;
    }

    private void validateBackupPayload(E2EPrivateKeyBackupDTO request) {
        if (request == null) {
            throw semanticError(HttpStatus.BAD_REQUEST, Constantes.ERR_E2E_BACKUP_INVALID, "Body requerido");
        }

        String kdf = requireNonBlankWithinLimit(request.getKdf(), "kdf", MAX_KDF_LEN).trim();
        if (!"PBKDF2".equalsIgnoreCase(kdf)) {
            throw semanticError(HttpStatus.BAD_REQUEST, Constantes.ERR_E2E_BACKUP_INVALID, "kdf debe ser PBKDF2");
        }

        String kdfHash = requireNonBlankWithinLimit(request.getKdfHash(), "kdfHash", MAX_KDF_HASH_LEN).trim();
        if (!"SHA-256".equalsIgnoreCase(kdfHash)) {
            throw semanticError(HttpStatus.BAD_REQUEST, Constantes.ERR_E2E_BACKUP_INVALID, "kdfHash debe ser SHA-256");
        }

        requireNonBlankWithinLimit(request.getEncryptedPrivateKey(), "encryptedPrivateKey", MAX_ENCRYPTED_PRIVATE_KEY_LEN);
        requireNonBlankWithinLimit(request.getIv(), "iv", MAX_IV_LEN);
        requireNonBlankWithinLimit(request.getSalt(), "salt", MAX_SALT_LEN);
        requireNonBlankWithinLimit(request.getPublicKey(), "publicKey", MAX_PUBLIC_KEY_LEN);
        requireNonBlankWithinLimit(request.getPublicKeyFingerprint(), "publicKeyFingerprint", MAX_PUBLIC_KEY_FINGERPRINT_LEN);
        requireIntRange(request.getKdfIterations(), "kdfIterations", MIN_KDF_ITERATIONS, MAX_KDF_ITERATIONS);
        requireIntRange(request.getKeyLengthBits(), "keyLengthBits", MIN_KEY_LENGTH_BITS, MAX_KEY_LENGTH_BITS);
    }

    @Override
    public AuthRespuestaDTO autenticarConGoogle(GoogleAuthRequestDTO request) {
        return autenticarConGoogle(null, request);
    }

    @Override
    public AuthRespuestaDTO autenticarConGoogle(String modeFromPath, GoogleAuthRequestDTO request) {
        validateGoogleProvider(request);
        String mode = resolveGoogleMode(modeFromPath, request == null ? null : request.getMode());
        String idToken = resolveGoogleIdToken(request);
        GoogleTokenPayloadDTO payload = googleIdTokenValidatorService.validarYExtraer(idToken);

        if (Constantes.GOOGLE_MODE_LOGIN.equals(mode)) {
            UsuarioEntity usuario = usuarioRepository.findByEmail(payload.getEmail())
                    .orElseThrow(() -> new GoogleAuthException(
                            HttpStatus.NOT_FOUND,
                            Constantes.ERR_GOOGLE_USUARIO_NO_REGISTRADO,
                            "No existe usuario registrado con ese email para login con Google"));
            if (!usuario.isActivo()) {
                throw new UsuarioInactivoException(Constantes.MSG_CUENTA_INHABILITADA);
            }
            return buildAuthResponseFromUsuario(usuario);
        }

        if (usuarioRepository.findByEmail(payload.getEmail()).isPresent()) {
            throw new EmailYaExisteException(ExceptionConstants.ERROR_EMAIL_EXISTS);
        }

        UsuarioDTO usuarioNuevo = new UsuarioDTO();
        usuarioNuevo.setEmail(payload.getEmail());
        usuarioNuevo.setNombre(payload.getNombre());
        usuarioNuevo.setApellido(payload.getApellido());
        usuarioNuevo.setFoto(payload.getFoto());
        usuarioNuevo.setPassword(UUID.randomUUID().toString());
        return crearUsuarioConToken(usuarioNuevo);
    }

    private void validateGoogleProvider(GoogleAuthRequestDTO request) {
        String provider = request == null ? null : request.getProvider();
        if (provider == null || provider.isBlank()
                || !Constantes.GOOGLE_PROVIDER.equalsIgnoreCase(provider.trim())) {
            throw new GoogleAuthException(
                    HttpStatus.BAD_REQUEST,
                    Constantes.ERR_GOOGLE_PROVIDER_INVALIDO,
                    "provider debe ser GOOGLE");
        }
    }

    private String resolveGoogleMode(String modeFromPath, String modeFromBody) {
        String pathMode = normalizeMode(modeFromPath);
        String bodyMode = normalizeMode(modeFromBody);

        if (pathMode != null && bodyMode != null && !pathMode.equals(bodyMode)) {
            throw new GoogleAuthException(
                    HttpStatus.BAD_REQUEST,
                    Constantes.ERR_GOOGLE_MODE_INVALIDO,
                    "mode en path y body no coinciden");
        }

        String mode = pathMode != null ? pathMode : bodyMode;
        if (!Constantes.GOOGLE_MODE_LOGIN.equals(mode) && !Constantes.GOOGLE_MODE_REGISTER.equals(mode)) {
            throw new GoogleAuthException(
                    HttpStatus.BAD_REQUEST,
                    Constantes.ERR_GOOGLE_MODE_INVALIDO,
                    "mode debe ser login o register");
        }
        return mode;
    }

    private String normalizeMode(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private String resolveGoogleIdToken(GoogleAuthRequestDTO request) {
        if (request == null) {
            throw new GoogleAuthException(
                    HttpStatus.BAD_REQUEST,
                    Constantes.ERR_GOOGLE_TOKEN_INVALIDO,
                    "Body requerido para autenticacion Google");
        }

        String token = request.getIdToken();
        if (token == null || token.isBlank()) {
            token = request.getCredential();
        }
        if (token == null || token.isBlank()) {
            throw new GoogleAuthException(
                    HttpStatus.BAD_REQUEST,
                    Constantes.ERR_GOOGLE_TOKEN_INVALIDO,
                    "idToken o credential es obligatorio");
        }
        return token.trim();
    }

    // Nuevo mÃ©todo login que devuelve Token
    @Override
    public AuthRespuestaDTO loginConToken(String email, String password) {
        String normalizedEmail = normalizeEmail(email);
        UsuarioEntity usuario = usuarioRepository.findByEmail(normalizedEmail)
                .orElseThrow(EmailNoRegistradoException::new);

        if (!usuario.isActivo()) {
            throw new UsuarioInactivoException(Constantes.MSG_CUENTA_INHABILITADA);
        }

        if (!passwordEncoder.matches(password, usuario.getPassword())) {
            throw new PasswordIncorrectaException();
        }

        return buildAuthResponseFromUsuario(usuario);
    }

    private String normalizeEmail(String email) {
        if (email == null || email.isBlank()) {
            return email;
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private AuthRespuestaDTO buildAuthResponseFromUsuario(UsuarioEntity usuario) {
        UsuarioDTO dto = MappingUtils.usuarioEntityADto(usuario);
        applyBlockedSources(dto, usuario == null ? null : usuario.getId());
        if (dto.getFoto() != null && dto.getFoto().startsWith(Constantes.UPLOADS_PREFIX)) {
            dto.setFoto(Utils.toDataUrlFromUrl(dto.getFoto(), uploadsRoot));
        }

        UserDetails userDetails = customUserDetailsService.loadUserByUsername(usuario.getEmail());
        String jwtToken = jwtService.generateToken(userDetails);
        return new AuthRespuestaDTO(
                jwtToken,
                dto,
                adminAuditCrypto.getAuditPublicKeySpkiBase64(),
                requiresProfileCompletion(usuario));
    }

    private boolean requiresProfileCompletion(UsuarioEntity usuario) {
        if (usuario == null) {
            return false;
        }
        boolean missingNombre = usuario.getNombre() == null || usuario.getNombre().isBlank();
        boolean missingApellido = usuario.getApellido() == null || usuario.getApellido().isBlank();
        return missingNombre || missingApellido;
    }

    @Override
    public void updatePublicKey(Long id, String publicKey) {
        if (!hasPublicKey(publicKey)) {
            throw new IllegalArgumentException("publicKey es obligatoria");
        }
        UsuarioEntity usuario = usuarioRepository.findFreshById(id)
                .or(() -> usuarioRepository.findById(id))
                .orElseThrow(() -> new RuntimeException(Constantes.MSG_USUARIO_NO_ENCONTRADO));
        // Solo el propio usuario autenticado deberÃ­a poder actualizar su propia llave
        if (!usuario.getId().equals(securityUtils.getAuthenticatedUserId())) {
            throw new RuntimeException(ExceptionConstants.ERROR_NOT_AUTHORIZED_PUBLIC_KEY);
        }
        String oldFp = E2EDiagnosticUtils.fingerprint12(usuario.getPublicKey());
        String newFp = E2EDiagnosticUtils.fingerprint12(publicKey);
        boolean replacingExistingDifferentKey = hasPublicKey(usuario.getPublicKey()) && !Objects.equals(oldFp, newFp);
        if (replacingExistingDifferentKey) {
            throw new E2ERekeyConflictException(
                    "Ya existe una publicKey distinta. Usa /api/usuarios/{id}/e2e/rekey para rotarla de forma segura.");
        }
        usuario.setPublicKey(publicKey);
        if (!Objects.equals(oldFp, newFp) || usuario.getPublicKeyUpdatedAt() == null) {
            usuario.setPublicKeyUpdatedAt(LocalDateTime.now());
        }
        usuarioRepository.save(usuario);
        LOGGER.info("[E2E_DIAG] stage=PUBLIC_KEY_UPDATE ts={} userId={} oldKeyFp={} newKeyFp={} changed={}",
                Instant.now(), usuario.getId(), oldFp, newFp, !oldFp.equals(newFp));
    }

    @Override
    public E2EStateDTO getE2EState(Long id) {
        validateSelfOrAdmin(id);
        UsuarioEntity usuario = usuarioRepository.findFreshById(id)
                .or(() -> usuarioRepository.findById(id))
                .orElseThrow(() -> new RuntimeException(Constantes.MSG_USUARIO_NO_ENCONTRADO));
        return toE2EState(usuario);
    }

    @Override
    @Transactional
    public E2EStateDTO rekeyE2E(Long id, E2ERekeyRequestDTO request) {
        validateSelfOrAdmin(id);
        if (request == null || !hasPublicKey(request.getNewPublicKey())) {
            throw new IllegalArgumentException("newPublicKey es obligatoria");
        }
        if (request.getCurrentPassword() == null || request.getCurrentPassword().isBlank()) {
            throw new IllegalArgumentException("currentPassword es obligatoria");
        }

        Long requesterId = securityUtils.getAuthenticatedUserId();
        UsuarioEntity requester = usuarioRepository.findById(requesterId)
                .orElseThrow(() -> new RuntimeException(Constantes.MSG_USUARIO_NO_ENCONTRADO));
        if (!passwordEncoder.matches(request.getCurrentPassword(), requester.getPassword())) {
            throw new PasswordIncorrectaException();
        }

        UsuarioEntity usuario = usuarioRepository.findFreshById(id)
                .or(() -> usuarioRepository.findById(id))
                .orElseThrow(() -> new RuntimeException(Constantes.MSG_USUARIO_NO_ENCONTRADO));

        String oldFp = E2EDiagnosticUtils.fingerprint12(usuario.getPublicKey());
        String newFp = E2EDiagnosticUtils.fingerprint12(request.getNewPublicKey());
        String expectedOldFingerprint = normalizeFingerprint(request.getExpectedOldFingerprint());
        if (expectedOldFingerprint != null && !Objects.equals(expectedOldFingerprint, oldFp)) {
            throw new E2ERekeyConflictException("expectedOldFingerprint no coincide con el estado actual");
        }

        usuario.setPublicKey(request.getNewPublicKey());
        usuario.setPublicKeyUpdatedAt(LocalDateTime.now());
        usuarioRepository.save(usuario);

        String traceId = Optional.ofNullable(E2EDiagnosticUtils.currentTraceId()).orElse(E2EDiagnosticUtils.newTraceId());
        MDC.put(E2EDiagnosticUtils.TRACE_ID_MDC_KEY, traceId);
        try {
            LOGGER.info("[E2E_DIAG] stage=E2E_REKEY ts={} traceId={} userId={} oldFp={} newFp={}",
                    Instant.now(), traceId, id, oldFp, newFp);
        } finally {
            MDC.remove(E2EDiagnosticUtils.TRACE_ID_MDC_KEY);
        }

        return toE2EState(usuario);
    }

    @Override
    @Transactional
    public void upsertE2EPrivateKeyBackup(Long userId, E2EPrivateKeyBackupDTO request) {
        validateSelfOrAdminForBackup(userId);
        validateUserExists(userId);
        validateBackupPayload(request);

        E2EPrivateKeyBackupEntity existing = e2EPrivateKeyBackupRepository.findByUserId(userId).orElse(null);
        E2EPrivateKeyBackupEntity entity = e2EPrivateKeyBackupMapper.toEntity(
                userId,
                request,
                existing,
                LocalDateTime.now());
        e2EPrivateKeyBackupRepository.save(entity);

        String traceId = Optional.ofNullable(E2EDiagnosticUtils.currentTraceId()).orElse(E2EDiagnosticUtils.newTraceId());
        LOGGER.info("[E2E_BACKUP] stage=UPSERT ts={} traceId={} userId={} existed={} keyFp={} encryptedLen={}",
                Instant.now(),
                traceId,
                userId,
                existing != null,
                E2EDiagnosticUtils.fingerprint12(entity.getPublicKeyFingerprint()),
                entity.getEncryptedPrivateKey() == null ? 0 : entity.getEncryptedPrivateKey().length());
    }

    @Override
    public E2EPrivateKeyBackupDTO getE2EPrivateKeyBackup(Long userId) {
        validateSelfOrAdminForBackup(userId);
        validateUserExists(userId);

        E2EPrivateKeyBackupEntity entity = e2EPrivateKeyBackupRepository.findByUserId(userId)
                .orElseThrow(() -> semanticError(
                        HttpStatus.NOT_FOUND,
                        Constantes.ERR_E2E_BACKUP_NOT_FOUND,
                        "No existe backup E2E para el usuario"));

        return e2EPrivateKeyBackupMapper.toDto(entity);
    }

    @Override
    public UsuarioDTO login(String email, String password) {
        return loginConToken(email, password).getUsuario();
    }

    @Override
    public UsuarioDTO getById(Long id) {
        UsuarioEntity u = usuarioRepository.findFreshById(id)
                .orElseThrow(() -> new RuntimeException(Constantes.MSG_USUARIO_NO_ENCONTRADO));
        LOGGER.info("[E2E_DIAG] stage=USER_GET_BY_ID ts={} userId={} publicKeyFp={} publicKeyLen={}",
                Instant.now(), id, E2EDiagnosticUtils.fingerprint12(u.getPublicKey()), u.getPublicKey() == null ? 0 : u.getPublicKey().length());
        UsuarioDTO dto = MappingUtils.usuarioEntityADto(u);
        applyBlockedSources(dto, u.getId());
        // ðŸ‘‰ Si la foto es una URL pÃºblica (/uploads/...), la convertimos a Base64 para
        // el front
        if (dto.getFoto() != null && dto.getFoto().startsWith(Constantes.UPLOADS_PREFIX)) {
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
                .filter(u -> !isAdminUser(u.getRoles()))
                .map(MappingUtils::usuarioEntityADto)
                .collect(Collectors.toList());

        // Convertir /uploads/... a dataURL Base64 (igual que en otros mÃ©todos)
        for (UsuarioDTO dto : list) {
            String foto = dto.getFoto();
            if (foto != null && foto.startsWith(Constantes.UPLOADS_PREFIX)) {
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
        bloquearUsuario(bloqueadoId, null);
    }

    @Override
    @Transactional
    public void bloquearUsuario(Long bloqueadoId, String source) {
        Long authenticatedUserId = securityUtils.getAuthenticatedUserId();
        System.out.println(Constantes.LOG_BLOCK_ATTEMPT + authenticatedUserId + " bloqueado=" + bloqueadoId);
        if (authenticatedUserId.equals(bloqueadoId)) {
            throw new RuntimeException(ExceptionConstants.ERROR_CANT_BLOCK_SELF);
        }
        BlockSource blockSource = normalizeBlockSource(source);

        UsuarioEntity user = usuarioRepository.findById(authenticatedUserId).orElseThrow();
        UsuarioEntity blocked = usuarioRepository.findById(bloqueadoId).orElseThrow();
        boolean added = user.getBloqueados().add(blocked);
        upsertBlockSource(user, blocked, blockSource);

        if (added) {
            System.out.println(Constantes.LOG_BLOCK_SUCCESS);
            usuarioRepository.save(user);
            // Notify the blocked user via STOMP that their status changed
            messagingTemplate.convertAndSend(Constantes.WS_TOPIC_USER_BLOQUEOS_PREFIX + bloqueadoId + Constantes.WS_TOPIC_USER_BLOQUEOS_SUFFIX,
                    String.format(Constantes.WS_BLOCK_STATUS_PAYLOAD_TEMPLATE, authenticatedUserId, Constantes.WS_TYPE_BLOCKED));
        } else {
            System.out.println(Constantes.LOG_BLOCK_ALREADY);
        }
    }

    @Override
    @Transactional
    public void desbloquearUsuario(Long bloqueadoId) {
        Long authenticatedUserId = securityUtils.getAuthenticatedUserId();

        UsuarioEntity user = usuarioRepository.findById(authenticatedUserId).orElseThrow();
        UsuarioEntity blocked = usuarioRepository.findById(bloqueadoId).orElseThrow();
        UserBlockRelationEntity block = userBlockRelationRepository.findByBlocker_IdAndBlocked_Id(authenticatedUserId, bloqueadoId)
                .orElse(null);
        if (block != null && block.getSource() == BlockSource.REPORT) {
            throw new IllegalStateException("No puedes desbloquear un bloqueo de tipo REPORT");
        }
        if (block != null) {
            userBlockRelationRepository.delete(block);
        }

        if (user.getBloqueados().remove(blocked)) {
            usuarioRepository.save(user);
            // Notify the unblocked user via STOMP
            messagingTemplate.convertAndSend(Constantes.WS_TOPIC_USER_BLOQUEOS_PREFIX + bloqueadoId + Constantes.WS_TOPIC_USER_BLOQUEOS_SUFFIX,
                    String.format(Constantes.WS_BLOCK_STATUS_PAYLOAD_TEMPLATE, authenticatedUserId, Constantes.WS_TYPE_UNBLOCKED));
        }
    }

    private BlockSource normalizeBlockSource(String source) {
        if (source == null || source.isBlank()) {
            return BlockSource.MANUAL;
        }
        try {
            return BlockSource.valueOf(source.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("source invalido. Usa MANUAL o REPORT");
        }
    }

    private void upsertBlockSource(UsuarioEntity blocker, UsuarioEntity blocked, BlockSource source) {
        UserBlockRelationEntity row = userBlockRelationRepository
                .findByBlocker_IdAndBlocked_Id(blocker.getId(), blocked.getId())
                .orElse(null);
        if (row == null) {
            row = new UserBlockRelationEntity();
            row.setBlocker(blocker);
            row.setBlocked(blocked);
        }
        row.setSource(source == null ? BlockSource.MANUAL : source);
        userBlockRelationRepository.save(row);
    }

    private void applyBlockedSources(UsuarioDTO dto, Long userId) {
        if (dto == null || userId == null) {
            return;
        }
        List<BlockedUserDTO> blocks = userBlockRelationRepository.findByBlocker_Id(userId)
                .stream()
                .map(row -> {
                    BlockedUserDTO item = new BlockedUserDTO();
                    item.setUserId(row.getBlocked() == null ? null : row.getBlocked().getId());
                    item.setSource(row.getSource() == null ? BlockSource.MANUAL.name() : row.getSource().name());
                    return item;
                })
                .filter(item -> item.getUserId() != null)
                .collect(Collectors.toList());
        dto.setBloqueados(blocks);
    }

    @Override
    public boolean existePorEmail(String email) {
        return usuarioRepository.findByEmail(normalizeEmail(email)).isPresent();
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
        return getDashboardStats(null);
    }

    @Override
    public DashboardStatsDTO getDashboardStats(String tz) {
        ZoneId queryZone = resolveZoneId(tz);
        ZoneId serverZone = ZoneId.systemDefault();

        ZonedDateTime nowInQueryZone = ZonedDateTime.now(queryZone);
        ZonedDateTime startOfTodayInQueryZone = nowInQueryZone.toLocalDate().atStartOfDay(queryZone);
        ZonedDateTime startOfYesterdayInQueryZone = startOfTodayInQueryZone.minusDays(1);
        ZonedDateTime endOfTodayInQueryZone = startOfTodayInQueryZone.plusDays(1);

        LocalDateTime inicioDia = startOfTodayInQueryZone.withZoneSameInstant(serverZone).toLocalDateTime();
        LocalDateTime finDia = endOfTodayInQueryZone.withZoneSameInstant(serverZone).toLocalDateTime();
        LocalDateTime inicioAyer = startOfYesterdayInQueryZone.withZoneSameInstant(serverZone).toLocalDateTime();

        long totalUsuarios = usuarioRepository.count();
        long usuariosHoy = usuarioRepository.countUsuariosRegistradosEntreFechas(inicioDia, finDia);
        long usuariosAyer = usuarioRepository.countUsuariosRegistradosEntreFechas(inicioAyer, inicioDia);
        double porcentajeUsuariosHoy = calcularPorcentajeHoyVsAyer(usuariosHoy, usuariosAyer);

        long chatsActivos = chatRepository.count();
        long chatsCreadosHoy = chatRepository.countChatsEntreFechas(inicioDia, finDia);
        long chatsAyer = chatRepository.countChatsEntreFechas(inicioAyer, inicioDia);
        double porcentajeChatsHoy = calcularPorcentajeHoyVsAyer(chatsCreadosHoy, chatsAyer);

        long reportesDiariosHoy = contarReportantesUnicos(inicioDia, finDia);
        long reportesAyer = contarReportantesUnicos(inicioAyer, inicioDia);
        double porcentajeReportesHoy = calcularPorcentajeHoyVsAyer(reportesDiariosHoy, reportesAyer);

        long mensajesHoy = mensajeRepository.countMensajesEntreFechas(inicioDia, finDia);
        long mensajesAyer = mensajeRepository.countMensajesEntreFechas(inicioAyer, inicioDia);
        double porcentajeMensajesHoy = calcularPorcentajeHoyVsAyer(mensajesHoy, mensajesAyer);

        DashboardStatsDTO dto = new DashboardStatsDTO();
        dto.setTotalUsuarios(totalUsuarios);
        dto.setPorcentajeUsuarios(porcentajeUsuariosHoy);
        dto.setPorcentajeUsuariosHoy(porcentajeUsuariosHoy);

        dto.setChatsActivos(chatsActivos);
        dto.setChatsCreadosHoy(chatsCreadosHoy);
        dto.setPorcentajeChats(porcentajeChatsHoy);
        dto.setPorcentajeChatsHoy(porcentajeChatsHoy);

        // Compatibilidad: mantener reportes y porcentajeReportes con la misma regla hoy vs ayer.
        dto.setReportes(reportesDiariosHoy);
        dto.setReportesDiariosHoy(reportesDiariosHoy);
        dto.setPorcentajeReportes(porcentajeReportesHoy);
        dto.setPorcentajeReportesHoy(porcentajeReportesHoy);

        dto.setMensajesHoy(mensajesHoy);
        dto.setPorcentajeMensajes(porcentajeMensajesHoy);
        dto.setPorcentajeMensajesHoy(porcentajeMensajesHoy);
        return dto;
    }

    private long contarReportantesUnicos(LocalDateTime from, LocalDateTime to) {
        List<SolicitudDesbaneoEntity> rows = solicitudDesbaneoRepository
                .findByCreatedAtGreaterThanEqualAndCreatedAtLessThan(from, to);
        HashSet<String> uniqueReporters = new HashSet<>();
        for (SolicitudDesbaneoEntity row : rows) {
            if (row == null) {
                continue;
            }
            if (row.getUsuarioId() != null) {
                uniqueReporters.add("u:" + row.getUsuarioId());
                continue;
            }
            String normalizedEmail = normalizeEmailNullable(row.getEmail());
            if (normalizedEmail != null) {
                uniqueReporters.add("e:" + normalizedEmail);
            }
        }
        return uniqueReporters.size();
    }

    private ZoneId resolveZoneId(String tz) {
        if (tz == null || tz.isBlank()) {
            return ZoneId.systemDefault();
        }
        try {
            return ZoneId.of(tz.trim());
        } catch (Exception ex) {
            throw new IllegalArgumentException("tz invalida: " + tz);
        }
    }

    private String normalizeEmailNullable(String emailRaw) {
        if (emailRaw == null) {
            return null;
        }
        String normalized = emailRaw.trim().toLowerCase(Locale.ROOT);
        return normalized.isBlank() ? null : normalized;
    }

    private double calcularPorcentajeHoyVsAyer(long hoy, long ayer) {
        double value;
        if (ayer > 0) {
            value = ((double) (hoy - ayer) / (double) ayer) * 100.0;
        } else if (hoy == 0) {
            value = 0.0;
        } else {
            value = 100.0;
        }

        double rounded = Math.round(value * 10.0) / 10.0;
        return rounded == -0.0d ? 0.0d : rounded;
    }

    @Override
    public Page<UsuarioDTO> listarRecientes(int page, int size) {
        int safePage = Math.max(0, page);
        int safeSize = Math.max(1, size);
        Pageable pageable = PageRequest.of(safePage, safeSize);
        Page<UsuarioEntity> entidades = usuarioRepository.findRecientesSinRol(pageable, Constantes.ADMIN);
        return entidades.map(MappingUtils::usuarioEntityADto);
    }

    @Override
    @Transactional
    public UsuarioDTO actualizarPerfil(ActualizarPerfilDTO dto) {
        if (dto == null) {
            throw new IllegalArgumentException(Constantes.MSG_FALTAN_DATOS_REQUERIDOS);
        }
        Long authenticatedUserId = securityUtils.getAuthenticatedUserId();
        UsuarioEntity usuario = usuarioRepository.findById(authenticatedUserId)
                .orElseThrow(() -> new RuntimeException(Constantes.MSG_USUARIO_NO_ENCONTRADO));

        if (dto.getNombre() != null && !dto.getNombre().isBlank()) {
            usuario.setNombre(dto.getNombre().trim());
        }
        if (dto.getApellido() != null && !dto.getApellido().isBlank()) {
            usuario.setApellido(dto.getApellido().trim());
        }
        if (dto.getFoto() != null) {
            String fotoUrl = normalizeAndValidateProfilePhoto(dto.getFoto());
            if (fotoUrl != null) {
                usuario.setFotoUrl(fotoUrl);
            }
        }
        if (dto.getDni() != null) {
            String normalizedDniNie = normalizeDniNie(dto.getDni());
            if (normalizedDniNie != null && !isSpanishDniNieValid(normalizedDniNie)) {
                throw new IllegalArgumentException("DNI/NIE no válido");
            }
            usuario.setDni(normalizedDniNie);
        }
        if (dto.getTelefono() != null) {
            usuario.setTelefono(normalizeProfileText(dto.getTelefono(), MAX_TELEFONO_LENGTH, "telefono"));
        }
        if (dto.getFechaNacimiento() != null) {
            usuario.setFechaNacimiento(normalizeProfileText(dto.getFechaNacimiento(), MAX_FECHA_NACIMIENTO_LENGTH, "fechaNacimiento"));
        }
        if (dto.getGenero() != null) {
            usuario.setGenero(normalizeProfileText(dto.getGenero(), MAX_GENERO_LENGTH, "genero"));
        }
        if (dto.getDireccion() != null) {
            usuario.setDireccion(normalizeProfileText(dto.getDireccion(), MAX_DIRECCION_LENGTH, "direccion"));
        }
        if (dto.getNacionalidad() != null) {
            usuario.setNacionalidad(normalizeProfileText(dto.getNacionalidad(), MAX_NACIONALIDAD_LENGTH, "nacionalidad"));
        }
        if (dto.getOcupacion() != null) {
            usuario.setOcupacion(normalizeProfileText(dto.getOcupacion(), MAX_OCUPACION_LENGTH, "ocupacion"));
        }
        if (dto.getInstagram() != null) {
            usuario.setInstagram(normalizeProfileText(dto.getInstagram(), MAX_INSTAGRAM_LENGTH, "instagram"));
        }

        UsuarioEntity saved = usuarioRepository.save(usuario);
        UsuarioDTO out = MappingUtils.usuarioEntityADto(saved);
        applyBlockedSources(out, saved.getId());
        if (out.getFoto() != null && out.getFoto().startsWith(Constantes.UPLOADS_PREFIX)) {
            out.setFoto(Utils.toDataUrlFromUrl(out.getFoto(), uploadsRoot));
        }
        return out;
    }

    private String normalizeAndValidateProfilePhoto(String fotoRaw) {
        String foto = fotoRaw == null ? null : fotoRaw.trim();
        if (foto == null || foto.isEmpty()) {
            return null;
        }

        if (foto.startsWith(Constantes.DATA_IMAGE_PREFIX)) {
            validateProfileImageDataUrl(foto);
            return Utils.saveDataUrlToUploads(foto, Constantes.DIR_AVATARS, uploadsRoot, uploadsBaseUrl);
        }

        if (foto.startsWith(Constantes.UPLOADS_PREFIX)) {
            validateStoredProfileImageUrl(foto);
            return foto;
        }

        throw new IllegalArgumentException(PROFILE_PHOTO_INVALID_SOURCE_MSG);
    }

    private void validateProfileImageDataUrl(String dataUrl) {
        int markerIndex = dataUrl.indexOf(DATA_IMAGE_BASE64_MARKER);
        if (markerIndex <= 5) {
            throw new IllegalArgumentException(PROFILE_PHOTO_INVALID_FORMAT_MSG);
        }

        String mime = dataUrl.substring("data:".length(), markerIndex).toLowerCase(Locale.ROOT).trim();
        if (!PROFILE_IMAGE_MIME_WHITELIST.contains(mime)) {
            throw new IllegalArgumentException(PROFILE_PHOTO_INVALID_FORMAT_MSG);
        }

        String base64Payload = dataUrl.substring(markerIndex + DATA_IMAGE_BASE64_MARKER.length()).trim();
        if (base64Payload.isEmpty()) {
            throw new IllegalArgumentException(PROFILE_PHOTO_INVALID_FORMAT_MSG);
        }

        byte[] bytes;
        try {
            bytes = Base64.getDecoder().decode(base64Payload);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException(PROFILE_PHOTO_INVALID_FORMAT_MSG);
        }

        validateProfileImageBytes(bytes, mime);
    }

    private void validateStoredProfileImageUrl(String publicUrl) {
        String cleanUrl = stripQueryAndFragment(publicUrl.trim());
        String relative = cleanUrl.substring(Constantes.UPLOADS_PREFIX.length()).replace("\\", "/");
        if (relative.isBlank() || relative.contains("..")) {
            throw new IllegalArgumentException(PROFILE_PHOTO_INVALID_FORMAT_MSG);
        }

        String extension = extractLowercaseExtension(relative);
        if (!PROFILE_IMAGE_EXT_WHITELIST.contains(extension)) {
            throw new IllegalArgumentException(PROFILE_PHOTO_INVALID_FORMAT_MSG);
        }

        Path uploadsRootPath = Paths.get(uploadsRoot).toAbsolutePath().normalize();
        Path imagePath = uploadsRootPath.resolve(relative).normalize();
        if (!imagePath.startsWith(uploadsRootPath)) {
            throw new IllegalArgumentException(PROFILE_PHOTO_INVALID_FORMAT_MSG);
        }

        try {
            if (!Files.exists(imagePath) || !Files.isRegularFile(imagePath)) {
                throw new IllegalArgumentException(PROFILE_PHOTO_INVALID_FORMAT_MSG);
            }
            long fileSize = Files.size(imagePath);
            if (fileSize <= 0 || fileSize > MAX_PROFILE_IMAGE_BYTES) {
                throw new IllegalArgumentException(PROFILE_PHOTO_INVALID_SIZE_MSG);
            }
            byte[] bytes = Files.readAllBytes(imagePath);
            validateProfileImageBytes(bytes, extensionToMime(extension));
        } catch (IllegalArgumentException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalArgumentException(PROFILE_PHOTO_INVALID_FORMAT_MSG);
        }
    }

    private void validateProfileImageBytes(byte[] bytes, String mime) {
        if (bytes == null || bytes.length == 0) {
            throw new IllegalArgumentException(PROFILE_PHOTO_INVALID_FORMAT_MSG);
        }
        if (bytes.length > MAX_PROFILE_IMAGE_BYTES) {
            throw new IllegalArgumentException(PROFILE_PHOTO_INVALID_SIZE_MSG);
        }
        if (!hasExpectedMagicHeader(bytes, mime)) {
            throw new IllegalArgumentException(PROFILE_PHOTO_INVALID_FORMAT_MSG);
        }
    }

    private boolean hasExpectedMagicHeader(byte[] bytes, String mime) {
        String lowerMime = mime == null ? "" : mime.toLowerCase(Locale.ROOT);
        if ("image/png".equals(lowerMime)) {
            return bytes.length >= 8
                    && (bytes[0] & 0xFF) == 0x89
                    && (bytes[1] & 0xFF) == 0x50
                    && (bytes[2] & 0xFF) == 0x4E
                    && (bytes[3] & 0xFF) == 0x47
                    && (bytes[4] & 0xFF) == 0x0D
                    && (bytes[5] & 0xFF) == 0x0A
                    && (bytes[6] & 0xFF) == 0x1A
                    && (bytes[7] & 0xFF) == 0x0A;
        }
        if ("image/webp".equals(lowerMime)) {
            return bytes.length >= 12
                    && bytes[0] == 'R'
                    && bytes[1] == 'I'
                    && bytes[2] == 'F'
                    && bytes[3] == 'F'
                    && bytes[8] == 'W'
                    && bytes[9] == 'E'
                    && bytes[10] == 'B'
                    && bytes[11] == 'P';
        }
        return bytes.length >= 3
                && (bytes[0] & 0xFF) == 0xFF
                && (bytes[1] & 0xFF) == 0xD8
                && (bytes[2] & 0xFF) == 0xFF;
    }

    private String stripQueryAndFragment(String url) {
        int queryIndex = url.indexOf('?');
        int fragmentIndex = url.indexOf('#');
        int cutIndex = -1;
        if (queryIndex >= 0 && fragmentIndex >= 0) {
            cutIndex = Math.min(queryIndex, fragmentIndex);
        } else if (queryIndex >= 0) {
            cutIndex = queryIndex;
        } else if (fragmentIndex >= 0) {
            cutIndex = fragmentIndex;
        }
        if (cutIndex < 0) {
            return url;
        }
        return url.substring(0, cutIndex);
    }

    private String extractLowercaseExtension(String relativePath) {
        int dotIndex = relativePath.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == relativePath.length() - 1) {
            throw new IllegalArgumentException(PROFILE_PHOTO_INVALID_FORMAT_MSG);
        }
        return relativePath.substring(dotIndex + 1).toLowerCase(Locale.ROOT);
    }

    private String extensionToMime(String extension) {
        if ("png".equals(extension)) {
            return "image/png";
        }
        if ("webp".equals(extension)) {
            return "image/webp";
        }
        return "image/jpeg";
    }

    private String normalizeProfileText(String value, int maxLength, String fieldName) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        if (normalized.isEmpty()) {
            return null;
        }
        if (normalized.length() > maxLength) {
            throw new IllegalArgumentException(fieldName + " supera la longitud máxima permitida");
        }
        return normalized;
    }

    private String normalizeDniNie(String rawValue) {
        if (rawValue == null) {
            return null;
        }
        String trimmed = rawValue.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        String normalized = trimmed
                .toUpperCase(Locale.ROOT)
                .replaceAll("[^0-9A-Z]", "");
        if (normalized.length() > 9) {
            throw new IllegalArgumentException("DNI/NIE no válido");
        }
        return normalized;
    }

    private boolean isSpanishDniNieValid(String value) {
        if (value == null || value.length() != 9) {
            return false;
        }
        char controlLetter = Character.toUpperCase(value.charAt(8));
        if (controlLetter < 'A' || controlLetter > 'Z') {
            return false;
        }

        String numericCore;
        char firstChar = Character.toUpperCase(value.charAt(0));
        if (Character.isDigit(firstChar)) {
            if (!areAllDigits(value.substring(0, 8))) {
                return false;
            }
            numericCore = value.substring(0, 8);
        } else if (firstChar == 'X' || firstChar == 'Y' || firstChar == 'Z') {
            String nieDigits = value.substring(1, 8);
            if (!areAllDigits(nieDigits)) {
                return false;
            }
            char mappedPrefix = firstChar == 'X' ? '0' : firstChar == 'Y' ? '1' : '2';
            numericCore = mappedPrefix + nieDigits;
        } else {
            return false;
        }

        int index = Integer.parseInt(numericCore) % DNI_NIE_CONTROL_LETTERS.length();
        return DNI_NIE_CONTROL_LETTERS.charAt(index) == controlLetter;
    }

    private boolean areAllDigits(String value) {
        if (value == null || value.isEmpty()) {
            return false;
        }
        for (int i = 0; i < value.length(); i++) {
            if (!Character.isDigit(value.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void solicitarCodigoCambioPassword(String currentPassword, String newPassword) {
        Long authenticatedUserId = securityUtils.getAuthenticatedUserId();
        UsuarioEntity usuario = usuarioRepository.findById(authenticatedUserId)
                .orElseThrow(() -> new RuntimeException(Constantes.MSG_USUARIO_NO_ENCONTRADO));
        if (currentPassword == null || currentPassword.isBlank()) {
            throw new IllegalArgumentException("La contrase\u00f1a actual es obligatoria.");
        }
        if (!passwordEncoder.matches(currentPassword, usuario.getPassword())) {
            throw new PasswordIncorrectaException("La contrase\u00f1a actual es incorrecta.");
        }
        if (newPassword != null && !newPassword.isBlank()
                && passwordEncoder.matches(newPassword, usuario.getPassword())) {
            throw new IllegalArgumentException("La nueva contrase\u00f1a no puede ser igual a la actual.");
        }
        passwordChangeService.generateAndSendChangeCode(usuario.getEmail(), usuario.getNombre());
    }

    @Override
    @Transactional
    public void cambiarPasswordConCodigo(String code, String newPassword) {
        Long authenticatedUserId = securityUtils.getAuthenticatedUserId();
        UsuarioEntity usuario = usuarioRepository.findById(authenticatedUserId)
                .orElseThrow(() -> new RuntimeException(Constantes.MSG_USUARIO_NO_ENCONTRADO));

        if (code == null || code.isBlank() || newPassword == null || newPassword.isBlank()) {
            throw new IllegalArgumentException(Constantes.MSG_FALTAN_DATOS_REQUERIDOS);
        }

        boolean isValid = passwordChangeService.isCodeValid(usuario.getEmail(), code);
        if (!isValid) {
            throw new IllegalArgumentException(Constantes.MSG_CODIGO_INVALIDO_O_EXPIRADO);
        }

        if (passwordEncoder.matches(newPassword, usuario.getPassword())) {
            throw new IllegalArgumentException("La nueva contrase\u00f1a no puede ser igual a la actual.");
        }

        String encryptedPassword = passwordEncoder.encode(newPassword);
        usuario.setPassword(encryptedPassword);
        usuarioRepository.save(usuario);
        passwordChangeService.invalidateCode(usuario.getEmail());
    }
    @Override
    @Transactional
    public void banearUsuario(Long id, String motivo) {
        banearUsuario(id, motivo, null, null);
    }

    @Override
    @Transactional
    public void banearUsuario(Long id, String motivo, String origen, String descripcion) {
        UsuarioEntity bloqueado = usuarioRepository.findById(id).orElseThrow();
        UsuarioEntity admin = resolveAdminActorOrThrow();
        Long adminId = admin.getId();
        validateAdminTargetRulesOrThrow(admin, bloqueado, "BAN");

        String motivoFinal = resolveAdminActionReasonOrThrow(motivo, Constantes.BAN_MOTIVO_DEFAULT, "BAN");
        String origenFinal = normalizeModerationOrigin(origen);
        String descripcionFinal = normalizeModerationDescription(descripcion);
        logAdminStateChangeAttempt("BAN", adminId, bloqueado.getId(), bloqueado.isActivo(), isAdminUser(bloqueado.getRoles()), motivoFinal, origenFinal);

        bloqueado.setActivo(false);
        usuarioRepository.save(bloqueado);

        messagingTemplate.convertAndSendToUser(
                bloqueado.getEmail(),
                Constantes.WS_QUEUE_BANEOS,
                String.format(Constantes.WS_BAN_PAYLOAD_TEMPLATE, escapeJson(motivoFinal))
        );

        Map<String, String> vars = new HashMap<>();
        vars.put(Constantes.EMAIL_VAR_NOMBRE, bloqueado.getNombre());
        vars.put(Constantes.EMAIL_VAR_MOTIVO, motivoFinal);

        emailService.sendHtmlEmail(
                bloqueado.getEmail(),
                Constantes.EMAIL_SUBJECT_BAN,
                Constantes.EMAIL_TEMPLATE_BAN,
                vars
        );

        registrarModeracion(
                bloqueado,
                admin,
                ModerationActionType.SUSPENSION,
                motivoFinal,
                descripcionFinal,
                origenFinal);
        logAdminStateChangeSuccess("BAN", adminId, bloqueado.getId(), false, motivoFinal, origenFinal);
    }

    @Override
    @Transactional
    public void desbanearAdministrativamente(Long id) {
        desbanearAdministrativamente(id, null);
    }

    @Override
    @Transactional
    public void desbanearAdministrativamente(Long id, String motivo) {
        UsuarioEntity vetado = usuarioRepository.findById(id).orElseThrow();
        UsuarioEntity admin = resolveAdminActorOrThrow();
        Long adminId = admin.getId();
        validateAdminTargetRulesOrThrow(admin, vetado, "UNBAN");
        String motivoFinal = resolveAdminActionReasonOrThrow(motivo, Constantes.UNBAN_MOTIVO_DEFAULT, "UNBAN");
        String origenFinal = normalizeModerationOrigin(DEFAULT_MODERATION_ORIGIN);
        logAdminStateChangeAttempt("UNBAN", adminId, vetado.getId(), vetado.isActivo(), isAdminUser(vetado.getRoles()), motivoFinal, origenFinal);

        vetado.setActivo(true);
        usuarioRepository.save(vetado);

        Map<String, String> vars = new HashMap<>();
        vars.put(Constantes.EMAIL_VAR_NOMBRE, vetado.getNombre());
        vars.put(Constantes.EMAIL_VAR_MOTIVO, motivoFinal);

        emailService.sendHtmlEmail(
                vetado.getEmail(),
                Constantes.EMAIL_SUBJECT_UNBAN,
                Constantes.EMAIL_TEMPLATE_UNBAN,
                vars);

        registrarModeracion(
                vetado,
                admin,
                ModerationActionType.UNBAN,
                motivoFinal,
                null,
                origenFinal);
        logAdminStateChangeSuccess("UNBAN", adminId, vetado.getId(), true, motivoFinal, origenFinal);
    }

    private UsuarioEntity resolveAdminActorOrThrow() {
        Long adminId = securityUtils.getAuthenticatedUserId();
        UsuarioEntity actor = usuarioRepository.findById(adminId)
                .orElseThrow(() -> new AccessDeniedException(Constantes.MSG_SOLO_ADMIN));
        if (!isAdminUser(actor.getRoles())) {
            throw new AccessDeniedException(Constantes.MSG_SOLO_ADMIN);
        }
        return actor;
    }

    private void validateAdminTargetRulesOrThrow(UsuarioEntity actor, UsuarioEntity target, String action) {
        if (actor == null || actor.getId() == null) {
            throw new AccessDeniedException(Constantes.MSG_SOLO_ADMIN);
        }
        if (target == null || target.getId() == null) {
            throw new IllegalArgumentException(Constantes.MSG_USUARIO_NO_ENCONTRADO);
        }
        if (Objects.equals(actor.getId(), target.getId())) {
            logAdminStateChangeDenied(action, actor.getId(), target.getId(), isAdminUser(target.getRoles()), "SELF_ACTION_BLOCKED");
            throw new AccessDeniedException("No puedes aplicarte " + action + " a ti mismo");
        }
        if (isAdminUser(target.getRoles())) {
            logAdminStateChangeDenied(action, actor.getId(), target.getId(), true, "TARGET_ADMIN_PROTECTED");
            throw new AccessDeniedException("No se permite " + action + " sobre cuentas administrativas");
        }
    }

    private String resolveAdminActionReasonOrThrow(String rawReason, String defaultReason, String action) {
        boolean callerSentReason = rawReason != null && !rawReason.trim().isEmpty();
        String baseReason = callerSentReason ? rawReason : defaultReason;
        String normalized = normalizeModerationReason(baseReason);
        if (callerSentReason && normalized.length() < MIN_ADMIN_MODERATION_REASON_LENGTH) {
            throw new IllegalArgumentException("motivo demasiado corto para " + action);
        }
        return normalized;
    }

    private void logAdminStateChangeAttempt(String action,
                                            Long actorId,
                                            Long targetId,
                                            boolean targetWasActive,
                                            boolean targetIsAdmin,
                                            String reason,
                                            String origin) {
        LOGGER.info("[ADMIN_USER_STATE_ATTEMPT] action={} actorId={} targetId={} targetWasActive={} targetIsAdmin={} reasonLen={} reasonHash={} origin={}",
                action,
                actorId,
                targetId,
                targetWasActive,
                targetIsAdmin,
                reason == null ? 0 : reason.length(),
                E2EDiagnosticUtils.fingerprint12(reason),
                origin);
    }

    private void logAdminStateChangeSuccess(String action,
                                            Long actorId,
                                            Long targetId,
                                            boolean targetActiveNow,
                                            String reason,
                                            String origin) {
        LOGGER.info("[ADMIN_USER_STATE_SUCCESS] action={} actorId={} targetId={} targetActiveNow={} reasonLen={} reasonHash={} origin={}",
                action,
                actorId,
                targetId,
                targetActiveNow,
                reason == null ? 0 : reason.length(),
                E2EDiagnosticUtils.fingerprint12(reason),
                origin);
    }

    private void logAdminStateChangeDenied(String action,
                                           Long actorId,
                                           Long targetId,
                                           boolean targetIsAdmin,
                                           String cause) {
        LOGGER.warn("[ADMIN_USER_STATE_DENIED] action={} actorId={} targetId={} targetIsAdmin={} cause={}",
                action,
                actorId,
                targetId,
                targetIsAdmin,
                cause);
    }

    private void registrarModeracion(UsuarioEntity targetUser,
                                     UsuarioEntity admin,
                                     ModerationActionType actionType,
                                     String reason,
                                     String description,
                                     String origin) {
        UserModerationHistoryEntity row = new UserModerationHistoryEntity();
        row.setUser(targetUser);
        row.setAdmin(admin);
        row.setActionType(actionType);
        row.setReason(normalizeModerationReason(reason));
        row.setDescription(normalizeModerationDescription(description));
        row.setOrigin(normalizeModerationOrigin(origin));
        userModerationHistoryRepository.save(row);
    }

    private String normalizeModerationReason(String value) {
        if (value == null) {
            return "Accion administrativa";
        }
        String normalized = value.replaceAll("[\\p{Cntrl}&&[^\\r\\n\\t]]", " ").trim();
        if (normalized.isEmpty()) {
            return "Accion administrativa";
        }
        return normalized.length() <= MAX_MODERATION_REASON_LENGTH
                ? normalized
                : normalized.substring(0, MAX_MODERATION_REASON_LENGTH);
    }

    private String normalizeModerationOrigin(String value) {
        if (value == null) {
            return DEFAULT_MODERATION_ORIGIN;
        }
        String normalized = value.replaceAll("[\\p{Cntrl}&&[^\\r\\n\\t]]", " ").trim();
        if (normalized.isEmpty()) {
            return DEFAULT_MODERATION_ORIGIN;
        }
        return normalized.length() <= MAX_MODERATION_ORIGIN_LENGTH
                ? normalized
                : normalized.substring(0, MAX_MODERATION_ORIGIN_LENGTH);
    }

    private String normalizeModerationDescription(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.replaceAll("[\\p{Cntrl}&&[^\\r\\n\\t]]", " ").trim();
        if (normalized.isEmpty()) {
            return null;
        }
        return normalized.length() <= MAX_MODERATION_DESCRIPTION_LENGTH
                ? normalized
                : normalized.substring(0, MAX_MODERATION_DESCRIPTION_LENGTH);
    }
}
