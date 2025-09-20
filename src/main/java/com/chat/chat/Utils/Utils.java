package com.chat.chat.Utils;

import com.chat.chat.DTO.ChatGrupalDTO;
import com.chat.chat.Entity.UsuarioEntity;
import com.chat.chat.Repository.UsuarioRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.data.repository.CrudRepository;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class Utils {


    // ObjectMapper thread-safe si lo reutilizas así
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    /**
     * Convierte una URL pública tipo "/uploads/avatars/xx.png" en dataURL "data:image/...;base64,..."
     * @param publicUrl   ej: "/uploads/avatars/xx.png"
     * @param uploadsRoot ej: "uploads" (ruta raíz física en disco)
     */
    public static String toDataUrlFromUrl(String publicUrl, String uploadsRoot) {
        try {
            if (publicUrl == null || publicUrl.isBlank()) return null;

            String relative = publicUrl.replaceFirst("^/uploads/?", ""); // "avatars/xx.png"
            Path file = Paths.get(uploadsRoot, relative).normalize().toAbsolutePath();

            byte[] bytes = Files.readAllBytes(file);

            String mime = Files.probeContentType(file);
            if (mime == null) {
                String name = file.getFileName().toString().toLowerCase();
                if (name.endsWith(".jpg") || name.endsWith(".jpeg")) mime = "image/jpeg";
                else if (name.endsWith(".png")) mime = "image/png";
                else if (name.endsWith(".webp")) mime = "image/webp";
                else mime = "application/octet-stream";
            }

            String base64 = Base64.getEncoder().encodeToString(bytes);
            return "data:" + mime + ";base64," + base64;
        } catch (Exception e) {
            return null;
        }
    }

    public static UsuarioEntity getCreadorOrThrow(ChatGrupalDTO dto, UsuarioRepository usuarioRepo) {
        if (dto == null) throw new IllegalArgumentException("El DTO no puede ser null");
        Long idCreador = dto.getIdCreador();
        if (idCreador == null) {
            throw new IllegalArgumentException("idCreador es obligatorio");
        }
        return usuarioRepo.findById(idCreador)
                .orElseThrow(() -> new IllegalArgumentException("Creador no existe: " + idCreador));
    }

    /** Carga una entidad por id o lanza IllegalArgumentException con mensaje claro. */
    public static <T, ID> T getByIdOrThrow(CrudRepository<T, ID> repo, ID id, String label) {
        return optionalOrThrow(repo.findById(id), label + " no existe: " + id);
    }

    public static boolean isPublicUrl(String v) {
        if (v == null) return false;
        String s = v.trim();
        return s.startsWith("/uploads/") || s.startsWith("http://") || s.startsWith("https://");
    }

    public static <T> T optionalOrThrow(Optional<T> opt, String msg) {
        return opt.orElseThrow(() -> new IllegalArgumentException(msg));
    }

    private static final Map<String,String> EXT_BY_MIME = Map.of(
            "audio/webm", ".webm",
            "audio/webm;codecs=opus", ".webm",
            "audio/ogg", ".ogg",
            "audio/ogg;codecs=opus", ".ogg",
            "audio/mpeg", ".mp3",
            "audio/mp4", ".m4a",
            "audio/aac", ".aac"
    );

    public static String extensionFor(String contentType, String defaultExt) {
        if (contentType == null) return defaultExt;
        String ext = EXT_BY_MIME.get(contentType.toLowerCase());
        return (ext != null ? ext : defaultExt);
    }

    public static String extensionFor(String contentType) {
        return extensionFor(contentType, ".webm");
    }

    public static String mmss(Integer ms) {
        if (ms == null || ms <= 0) return "";
        int total = ms / 1000;
        int m = total / 60;
        int s = total % 60;
        return String.format("%02d:%02d", m, s);
    }

    // evita NPE si contenido es null antes de truncar
    public static String truncarSafe(String s, int max) {
        if (s == null) return "";
        if (s.length() <= max) return s;
        return s.substring(0, Math.max(0, max)).trim() + "…";
    }

    /** Envía a /topic/notifications.{userId} */
    public static void sendNotif(SimpMessagingTemplate messagingTemplate, Long userId, Object payload) {
        if (userId == null) throw new IllegalArgumentException("userId requerido");
        messagingTemplate.convertAndSend("/topic/notifications." + userId, payload);
    }

    public static String writeJson(Object o) {
        try {
            return MAPPER.writeValueAsString(o);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error serializando JSON", e);
        }
    }

    /**
     * Guarda un dataURL (ej: "data:image/png;base64,AAA...") en /{uploadsRoot}/{folder}/
     * y devuelve la URL pública (ej: "/uploads/{folder}/{uuid}.png").
     *
     * @param dataUrl         data URL completo ("data:image/...;base64,...")
     * @param folder          subcarpeta (ej: "avatars")
     * @param uploadsRoot     raíz física (ej: "uploads")
     * @param uploadsBaseUrl  raíz pública (ej: "/uploads")
     */
    public static String saveDataUrlToUploads(String dataUrl, String folder, String uploadsRoot, String uploadsBaseUrl) {
        try {
            if (dataUrl == null || !dataUrl.startsWith("data:") || !dataUrl.contains(",")) {
                throw new IllegalArgumentException("Data URL inválido");
            }

            // data:image/png;base64,xxxx
            String[] parts = dataUrl.split(",", 2);
            String meta = parts[0];   // "data:image/png;base64"
            String base64 = parts[1];

            String ext = guessExtFromMeta(meta);
            byte[] bytes = Base64.getDecoder().decode(base64);

            // evitar path traversal en folder
            String safeFolder = folder.replace("\\", "/").replace("..", "").replaceAll("^/+", "").replaceAll("/+$", "");

            Path dir = Paths.get(uploadsRoot, safeFolder).normalize().toAbsolutePath();
            Files.createDirectories(dir);

            String filename = UUID.randomUUID().toString() + "." + ext;
            Path file = dir.resolve(filename);
            Files.write(file, bytes);

            String baseClean = uploadsBaseUrl.replaceAll("/+$", "");
            return baseClean + "/" + safeFolder + "/" + filename;
        } catch (Exception e) {
            throw new RuntimeException("No se pudo guardar la imagen", e);
        }
    }

    private static String guessExtFromMeta(String meta) {
        String m = meta.toLowerCase();
        if (m.contains("image/jpeg") || m.contains("image/jpg")) return "jpg";
        if (m.contains("image/webp")) return "webp";
        if (m.contains("image/png")) return "png";
        return "png"; // por defecto
    }
}
