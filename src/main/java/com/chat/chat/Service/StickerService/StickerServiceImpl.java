package com.chat.chat.Service.StickerService;

import com.chat.chat.DTO.StickerDTO;
import com.chat.chat.Entity.StickerEntity;
import com.chat.chat.Entity.UsuarioEntity;
import com.chat.chat.Exceptions.ConflictoException;
import com.chat.chat.Exceptions.RecursoNoEncontradoException;
import com.chat.chat.Repository.StickerRepository;
import com.chat.chat.Repository.UsuarioRepository;
import com.chat.chat.Utils.Constantes;
import com.chat.chat.Utils.SecurityUtils;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Service
public class StickerServiceImpl implements StickerService {
    private static final Logger LOGGER = LoggerFactory.getLogger(StickerServiceImpl.class);

    private static final long DEFAULT_MAX_STICKER_BYTES = 5L * 1024L * 1024L;
    private static final int DEFAULT_MAX_STICKERS = 200;
    private static final int MAX_NAME_LENGTH = 100;
    private static final Set<String> ALLOWED_MIME = Set.of(
            "image/png",
            "image/jpeg",
            "image/webp",
            "image/gif"
    );
    private static final Set<String> ALLOWED_EXT = Set.of(".png", ".jpg", ".jpeg", ".webp", ".gif");

    private final String uploadsRoot;
    private final String uploadsBaseUrl;
    private final long maxStickerBytes;
    private final int maxStickersPerUser;
    private final StickerRepository stickerRepository;
    private final UsuarioRepository usuarioRepository;
    private final SecurityUtils securityUtils;

    public StickerServiceImpl(@Value(Constantes.PROP_UPLOADS_ROOT) String uploadsRoot,
                              @Value(Constantes.PROP_UPLOADS_BASE_URL) String uploadsBaseUrl,
                              @Value("${app.stickers.max-file-bytes:5242880}") Long maxStickerBytes,
                              @Value("${app.stickers.max-per-user:200}") Integer maxStickersPerUser,
                              StickerRepository stickerRepository,
                              UsuarioRepository usuarioRepository,
                              SecurityUtils securityUtils) {
        this.uploadsRoot = uploadsRoot;
        this.uploadsBaseUrl = uploadsBaseUrl;
        this.maxStickerBytes = maxStickerBytes == null || maxStickerBytes <= 0 ? DEFAULT_MAX_STICKER_BYTES : maxStickerBytes;
        this.maxStickersPerUser = maxStickersPerUser == null || maxStickersPerUser <= 0 ? DEFAULT_MAX_STICKERS : maxStickersPerUser;
        this.stickerRepository = stickerRepository;
        this.usuarioRepository = usuarioRepository;
        this.securityUtils = securityUtils;
    }

    @Override
    public StickerDTO guardarSticker(MultipartFile archivo, String nombre) {
        Long userId = securityUtils.getAuthenticatedUserId();
        UsuarioEntity usuario = usuarioRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException(Constantes.MSG_USUARIO_AUTENTICADO_NO_ENCONTRADO));
        if (!usuario.isActivo()) {
            throw new AccessDeniedException("Usuario inactivo");
        }
        if (stickerRepository.countByUsuarioIdAndActivoTrue(userId) >= maxStickersPerUser) {
            throw new IllegalArgumentException("Limite de stickers alcanzado");
        }

        byte[] bytes = readBytes(archivo);
        validateSize(bytes.length);
        String sanitizedName = normalizeName(nombre);
        String originalName = safeClientName(archivo == null ? null : archivo.getOriginalFilename());
        String declaredMime = normalizeMime(archivo == null ? null : archivo.getContentType());
        String detectedMime = detectMime(bytes, declaredMime, originalName);
        String extension = resolveExtension(originalName, detectedMime);
        validateMimeAndExtension(detectedMime, extension);

        try {
            String fileName = UUID.randomUUID() + extension;
            Path dir = prepareDirectory(Constantes.DIR_STICKERS);
            Path target = dir.resolve(fileName);
            try (InputStream in = new ByteArrayInputStream(bytes)) {
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            }

            StickerEntity entity = new StickerEntity();
            entity.setNombre(resolveStickerName(sanitizedName, originalName));
            entity.setArchivoUrl(buildPublicUrl(Constantes.DIR_STICKERS, fileName));
            entity.setTipoMime(detectedMime);
            entity.setTamano((long) bytes.length);
            entity.setUsuario(usuario);
            entity.setActivo(true);
            return toDto(stickerRepository.save(entity));
        } catch (IOException ex) {
            throw new RuntimeException("No se pudo guardar sticker", ex);
        }
    }

    @Override
    public List<StickerDTO> listarMisStickers() {
        Long userId = securityUtils.getAuthenticatedUserId();
        return stickerRepository.findByUsuarioIdAndActivoTrueOrderByFechaCreacionDesc(userId)
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    public void eliminarSticker(Long stickerId) {
        StickerEntity sticker = resolveOwnedSticker(stickerId);
        sticker.setActivo(false);
        stickerRepository.save(sticker);
    }

    @Override
    public StickerDTO obtenerSticker(Long stickerId) {
        return toDto(resolveOwnedSticker(stickerId));
    }

    @Override
    public ResponseEntity<Resource> descargarSticker(Long stickerId) {
        StickerEntity sticker = resolveOwnedSticker(stickerId);
        Path filePath = resolveStickerPath(sticker.getArchivoUrl());
        try {
            byte[] bytes = Files.readAllBytes(filePath);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filePath.getFileName() + "\"")
                    .header("X-Content-Type-Options", "nosniff")
                    .contentType(MediaType.parseMediaType(sticker.getTipoMime()))
                    .contentLength(bytes.length)
                    .body(new ByteArrayResource(bytes));
        } catch (IOException ex) {
            throw new RuntimeException("No se pudo leer sticker", ex);
        }
    }

    @Override
    public boolean isOwnedByUser(Long stickerId, Long userId) {
        if (stickerId == null || stickerId <= 0) {
            throw new IllegalArgumentException("stickerId invalido");
        }
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("userId invalido");
        }
        StickerEntity sticker = stickerRepository.findByIdAndActivoTrue(stickerId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Sticker no encontrado"));
        return Objects.equals(sticker.getUsuario() == null ? null : sticker.getUsuario().getId(), userId);
    }

    @Override
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public StickerDTO saveStickerToUser(Long stickerId, Long userId) {
        if (stickerId == null || stickerId <= 0) {
            throw new IllegalArgumentException("stickerId invalido");
        }
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("userId invalido");
        }

        UsuarioEntity usuario = usuarioRepository.findById(userId)
                .orElseThrow(() -> new RecursoNoEncontradoException(Constantes.MSG_USUARIO_AUTENTICADO_NO_ENCONTRADO));
        if (!usuario.isActivo()) {
            throw new AccessDeniedException("Usuario inactivo");
        }
        if (stickerRepository.countByUsuarioIdAndActivoTrue(userId) >= maxStickersPerUser) {
            throw new IllegalArgumentException("Limite de stickers alcanzado");
        }

        StickerEntity source = stickerRepository.findByIdAndActivoTrue(stickerId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Sticker no encontrado"));

        if (stickerRepository.existsByUsuarioIdAndSourceStickerIdAndActivoTrue(userId, source.getId())
                || Objects.equals(source.getUsuario() == null ? null : source.getUsuario().getId(), userId)) {
            LOGGER.info("[STICKER_SAVE_TO_ME] userId={} sourceStickerId={} result=conflict", userId, stickerId);
            throw new ConflictoException("Sticker ya añadido");
        }

        byte[] bytes = readStickerBytes(source);
        validateSize(bytes.length);
        String detectedMime = detectMime(bytes, normalizeMime(source.getTipoMime()), source.getNombre());
        String extension = resolveExtension(source.getNombre(), detectedMime);
        validateMimeAndExtension(detectedMime, extension);

        try {
            String fileName = UUID.randomUUID() + extension;
            Path dir = prepareDirectory(Constantes.DIR_STICKERS);
            Path target = dir.resolve(fileName);
            try (InputStream in = new ByteArrayInputStream(bytes)) {
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            }

            StickerEntity clone = new StickerEntity();
            clone.setNombre(resolveStickerName(source.getNombre(), source.getNombre()));
            clone.setArchivoUrl(buildPublicUrl(Constantes.DIR_STICKERS, fileName));
            clone.setTipoMime(detectedMime);
            clone.setTamano((long) bytes.length);
            clone.setUsuario(usuario);
            clone.setSourceSticker(source);
            clone.setActivo(true);
            StickerDTO created = toDto(stickerRepository.save(clone));
            LOGGER.info("[STICKER_SAVE_TO_ME] userId={} sourceStickerId={} result=created newStickerId={}", userId, stickerId, created.getId());
            return created;
        } catch (IOException ex) {
            throw new RuntimeException("No se pudo guardar sticker", ex);
        }
    }

    private byte[] readBytes(MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty()) {
            throw new IllegalArgumentException("archivo requerido");
        }
        try {
            return archivo.getBytes();
        } catch (IOException ex) {
            throw new RuntimeException("No se pudo leer archivo", ex);
        }
    }

    private void validateSize(long bytes) {
        if (bytes <= 0) {
            throw new IllegalArgumentException("archivo vacio");
        }
        if (bytes > maxStickerBytes) {
            throw new IllegalArgumentException("archivo excede tamano maximo permitido");
        }
    }

    private String normalizeName(String nombre) {
        if (nombre == null) {
            return null;
        }
        String cleaned = nombre.replaceAll("[\\p{Cntrl}&&[^\\r\\n\\t]]", " ");
        cleaned = Normalizer.normalize(cleaned, Normalizer.Form.NFKC).trim();
        if (cleaned.length() > MAX_NAME_LENGTH) {
            throw new IllegalArgumentException("nombre excede longitud maxima");
        }
        return cleaned.isBlank() ? null : cleaned;
    }

    private String safeClientName(String originalFilename) {
        if (originalFilename == null || originalFilename.isBlank()) {
            return "sticker";
        }
        String cleaned = originalFilename.replace("\\", "/");
        int slash = cleaned.lastIndexOf('/');
        String base = slash >= 0 ? cleaned.substring(slash + 1) : cleaned;
        base = base.replaceAll("[\\r\\n\\t]", "_").trim();
        if (base.isBlank()) {
            return "sticker";
        }
        return base.length() > 200 ? base.substring(0, 200) : base;
    }

    private String detectMime(byte[] bytes, String clientMime, String originalName) {
        String detected = null;
        try (InputStream in = new ByteArrayInputStream(bytes)) {
            detected = URLConnection.guessContentTypeFromStream(in);
        } catch (IOException ignored) {
            detected = null;
        }
        if (detected == null || detected.isBlank()) {
            detected = extToMime(extensionFromName(originalName));
        }
        if (detected == null || detected.isBlank()) {
            detected = clientMime;
        }
        return normalizeMime(detected);
    }

    private void validateMimeAndExtension(String mime, String ext) {
        if (!ALLOWED_MIME.contains(mime)) {
            throw new IllegalArgumentException("mime no permitido");
        }
        if (!ALLOWED_EXT.contains(ext)) {
            throw new IllegalArgumentException("extension no permitida");
        }
        if ("image/svg+xml".equalsIgnoreCase(mime) || ".svg".equalsIgnoreCase(ext)) {
            throw new IllegalArgumentException("svg no permitido");
        }
    }

    private String resolveExtension(String originalName, String mime) {
        String ext = extensionFromName(originalName);
        if (ext != null && ALLOWED_EXT.contains(ext)) {
            return ext;
        }
        String fromMime = mimeToExt(mime);
        if (fromMime != null && ALLOWED_EXT.contains(fromMime)) {
            return fromMime;
        }
        throw new IllegalArgumentException("extension no permitida");
    }

    private String extensionFromName(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        int dot = name.lastIndexOf('.');
        if (dot < 0 || dot == name.length() - 1) {
            return null;
        }
        String ext = name.substring(dot).toLowerCase(Locale.ROOT);
        return ext.matches("\\.[a-z0-9]{1,10}") ? ext : null;
    }

    private String extToMime(String ext) {
        if (ext == null) {
            return null;
        }
        if (".png".equals(ext)) return "image/png";
        if (".jpg".equals(ext) || ".jpeg".equals(ext)) return "image/jpeg";
        if (".webp".equals(ext)) return "image/webp";
        if (".gif".equals(ext)) return "image/gif";
        return null;
    }

    private String mimeToExt(String mime) {
        if (mime == null) {
            return null;
        }
        if ("image/png".equals(mime)) return ".png";
        if ("image/jpeg".equals(mime)) return ".jpg";
        if ("image/webp".equals(mime)) return ".webp";
        if ("image/gif".equals(mime)) return ".gif";
        return null;
    }

    private String normalizeMime(String mime) {
        return mime == null ? "" : mime.trim().toLowerCase(Locale.ROOT);
    }

    private Path prepareDirectory(String subDir) throws IOException {
        Path dir = Paths.get(uploadsRoot, subDir).toAbsolutePath().normalize();
        if (!Files.exists(dir)) {
            Files.createDirectories(dir);
        }
        return dir;
    }

    private String buildPublicUrl(String subDir, String fileName) {
        String base = uploadsBaseUrl.endsWith("/") ? uploadsBaseUrl : uploadsBaseUrl + "/";
        return base + subDir + "/" + fileName;
    }

    private String resolveStickerName(String nombre, String originalName) {
        if (nombre != null && !nombre.isBlank()) {
            return nombre;
        }
        String fallback = originalName == null ? "sticker" : originalName;
        int dot = fallback.lastIndexOf('.');
        String base = dot > 0 ? fallback.substring(0, dot) : fallback;
        base = base.trim();
        if (base.isBlank()) {
            base = "sticker";
        }
        return base.length() > MAX_NAME_LENGTH ? base.substring(0, MAX_NAME_LENGTH) : base;
    }

    private StickerDTO toDto(StickerEntity entity) {
        StickerDTO dto = new StickerDTO();
        dto.setId(entity.getId());
        dto.setNombre(entity.getNombre());
        dto.setArchivoUrl(Constantes.API_STICKERS + "/" + entity.getId() + "/archivo");
        dto.setTipoMime(entity.getTipoMime());
        dto.setTamano(entity.getTamano());
        dto.setFechaCreacion(entity.getFechaCreacion());
        dto.setActivo(entity.isActivo());
        return dto;
    }

    private StickerEntity resolveOwnedSticker(Long stickerId) {
        if (stickerId == null || stickerId <= 0) {
            throw new IllegalArgumentException("stickerId invalido");
        }
        Long userId = securityUtils.getAuthenticatedUserId();
        StickerEntity sticker = stickerRepository.findByIdAndUsuarioIdAndActivoTrue(stickerId, userId)
                .orElseThrow(() -> new AccessDeniedException("Sticker no encontrado o sin permisos"));
        if (!Objects.equals(sticker.getUsuario().getId(), userId)) {
            throw new AccessDeniedException("No autorizado");
        }
        return sticker;
    }

    private Path resolveStickerPath(String publicUrl) {
        if (publicUrl == null || !publicUrl.startsWith(Constantes.UPLOADS_PREFIX)) {
            throw new IllegalArgumentException("ruta de sticker invalida");
        }
        String relative = publicUrl.substring(Constantes.UPLOADS_PREFIX.length());
        Path root = Paths.get(uploadsRoot).toAbsolutePath().normalize();
        Path path = root.resolve(relative).normalize();
        if (!path.startsWith(root)) {
            throw new IllegalArgumentException("ruta de sticker invalida");
        }
        if (!Files.exists(path) || !Files.isRegularFile(path)) {
            throw new IllegalArgumentException("sticker no encontrado");
        }
        return path;
    }

    private byte[] readStickerBytes(StickerEntity sticker) {
        Path path = resolveStickerPath(sticker.getArchivoUrl());
        try {
            return Files.readAllBytes(path);
        } catch (Exception ex) {
            try (InputStream in = new URL(sticker.getArchivoUrl()).openStream()) {
                return in.readAllBytes();
            } catch (Exception remoteEx) {
                throw new RuntimeException("No se pudo leer sticker origen", remoteEx);
            }
        }
    }
}
