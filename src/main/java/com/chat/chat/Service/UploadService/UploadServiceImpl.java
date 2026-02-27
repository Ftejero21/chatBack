package com.chat.chat.Service.UploadService;

import com.chat.chat.DTO.AudioUploadResponseDTO;
import com.chat.chat.DTO.FileUploadResponseDTO;
import com.chat.chat.Utils.Constantes;
import com.chat.chat.Utils.ExceptionConstants;
import com.chat.chat.Utils.Utils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class UploadServiceImpl implements UploadService {

    private static final long MAX_UPLOAD_BYTES = 25L * 1024L * 1024L;

    private final String uploadsRoot;
    private final String uploadsBaseUrl;

    public UploadServiceImpl(@Value(Constantes.PROP_UPLOADS_ROOT) String uploadsRoot,
                             @Value(Constantes.PROP_UPLOADS_BASE_URL) String uploadsBaseUrl) {
        this.uploadsRoot = uploadsRoot;
        this.uploadsBaseUrl = uploadsBaseUrl;
    }

    @Override
    public AudioUploadResponseDTO uploadAudio(MultipartFile file, Integer durMs) {
        validateAudioFile(file);

        try {
            String mime = normalizeMime(file.getContentType());
            String originalExtension = Utils.extensionFor(mime);
            StoredFile stored = storeRawFile(file, Constantes.DIR_VOICE, originalExtension);

            AudioUploadResponseDTO dto = new AudioUploadResponseDTO();
            dto.setUrl(stored.url());
            dto.setMime(stored.mime());
            dto.setDurMs(durMs);
            return dto;

        } catch (IOException e) {
            throw new RuntimeException(ExceptionConstants.ERROR_AUDIO_SAVE_FAILED, e);
        }
    }

    @Override
    public FileUploadResponseDTO uploadEncryptedFile(MultipartFile file) {
        validateEncryptedFile(file);

        try {
            StoredFile stored = storeRawFile(file, Constantes.DIR_MEDIA, ".bin");
            FileUploadResponseDTO dto = new FileUploadResponseDTO();
            dto.setUrl(stored.url());
            dto.setMime(stored.mime());
            dto.setFileName(stored.fileName());
            dto.setSizeBytes(stored.sizeBytes());
            return dto;
        } catch (IOException e) {
            throw new RuntimeException(ExceptionConstants.ERROR_FILE_SAVE_FAILED, e);
        }
    }

    private void validateAudioFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException(ExceptionConstants.ERROR_AUDIO_EMPTY);
        }

        String normalizedType = normalizeMime(file.getContentType()).toLowerCase();
        boolean allowed = normalizedType.startsWith("audio/")
                || Constantes.MIME_APPLICATION_OCTET_STREAM.equals(normalizedType);
        if (!allowed) {
            throw new IllegalArgumentException(ExceptionConstants.ERROR_FILE_TYPE_NOT_ALLOWED + file.getContentType());
        }

        validateSize(file.getSize());
    }

    private void validateEncryptedFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException(ExceptionConstants.ERROR_FILE_EMPTY);
        }

        String normalizedType = normalizeMime(file.getContentType()).toLowerCase();
        boolean allowed = Constantes.MIME_APPLICATION_OCTET_STREAM.equals(normalizedType)
                || normalizedType.startsWith("image/");
        if (!allowed) {
            throw new IllegalArgumentException(ExceptionConstants.ERROR_FILE_TYPE_NOT_ALLOWED + file.getContentType());
        }

        validateSize(file.getSize());
    }

    private void validateSize(long sizeBytes) {
        if (sizeBytes > MAX_UPLOAD_BYTES) {
            throw new IllegalArgumentException(ExceptionConstants.ERROR_FILE_SIZE_EXCEEDED);
        }
    }

    private StoredFile storeRawFile(MultipartFile file, String subDir, String defaultExtension) throws IOException {
        Path destinationDir = prepareDirectory(subDir);

        String safeExtension = detectExtension(file, defaultExtension);
        String storedFileName = generateUniqueFileName(safeExtension);
        Path destinationPath = destinationDir.resolve(storedFileName);

        // Importante para E2E: persistir bytes exactamente como llegan.
        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, destinationPath, StandardCopyOption.REPLACE_EXISTING);
        }

        String publicUrl = buildPublicUrl(subDir, storedFileName);
        return new StoredFile(
                publicUrl,
                normalizeMime(file.getContentType()),
                storedFileName,
                file.getSize());
    }

    private String detectExtension(MultipartFile file, String fallback) {
        String fromOriginal = extensionFromOriginalName(file.getOriginalFilename());
        if (fromOriginal != null) {
            return fromOriginal;
        }
        String fromMime = Utils.extensionFor(normalizeMime(file.getContentType()), fallback);
        return fromMime == null || fromMime.isBlank() ? fallback : fromMime;
    }

    private String extensionFromOriginalName(String originalFilename) {
        if (originalFilename == null || originalFilename.isBlank()) {
            return null;
        }
        String cleaned = originalFilename.trim();
        int dot = cleaned.lastIndexOf('.');
        if (dot < 0 || dot == cleaned.length() - 1) {
            return null;
        }
        String ext = cleaned.substring(dot).toLowerCase();
        if (!ext.matches("\\.[a-z0-9]{1,10}")) {
            return null;
        }
        return ext;
    }

    private Path prepareDirectory(String subDir) throws IOException {
        Path dir = Paths.get(uploadsRoot, subDir).toAbsolutePath().normalize();
        if (!Files.exists(dir)) {
            Files.createDirectories(dir);
        }
        return dir;
    }

    private String generateUniqueFileName(String extension) {
        String safeExt = (extension == null || extension.isBlank()) ? ".bin" : extension;
        return UUID.randomUUID() + safeExt;
    }

    private String buildPublicUrl(String subDir, String fileName) {
        String baseUrl = uploadsBaseUrl.endsWith("/") ? uploadsBaseUrl : uploadsBaseUrl + "/";
        return baseUrl + subDir + "/" + fileName;
    }

    private String normalizeMime(String mime) {
        if (mime == null || mime.isBlank()) {
            return Constantes.MIME_APPLICATION_OCTET_STREAM;
        }
        return mime.trim();
    }

    private record StoredFile(String url, String mime, String fileName, Long sizeBytes) {
    }
}
