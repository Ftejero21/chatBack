package com.chat.chat.Service.UploadService;

import com.chat.chat.DTO.AudioUploadResponseDTO;
import com.chat.chat.Utils.Utils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import com.chat.chat.Utils.ExceptionConstants;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
public class UploadServiceImpl implements UploadService {

    // Lista blanca de tipos MIME permitidos
    private static final List<String> ALLOWED_AUDIO_TYPES = Arrays.asList(
            "audio/mpeg", "audio/mp3", "audio/ogg", "audio/wav", "audio/webm", "audio/m4a");

    private final String uploadsRoot;
    private final String uploadsBaseUrl;

    // Inyección por constructor (Mejor testabilidad)
    public UploadServiceImpl(@Value("${app.uploads.root:uploads}") String uploadsRoot,
            @Value("${app.uploads.base-url:/uploads}") String uploadsBaseUrl) {
        this.uploadsRoot = uploadsRoot;
        this.uploadsBaseUrl = uploadsBaseUrl;
    }

    @Override
    public AudioUploadResponseDTO uploadAudio(MultipartFile file, Integer durMs) {
        // 1. Validaciones
        validateFile(file);

        try {
            // 2. Preparar rutas y nombres
            String subDirectory = "voice";
            Path destinationDir = prepareDirectory(subDirectory);

            // Generar nombre seguro
            String originalExtension = Utils.extensionFor(file.getContentType());
            // Nota: Asegúrate de que Utils.extensionFor maneje nulos o tipos desconocidos

            String fileName = generateUniqueFileName(originalExtension);
            Path destinationPath = destinationDir.resolve(fileName);

            // 3. Guardar el archivo (Try-with-resources para seguridad de memoria)
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, destinationPath, StandardCopyOption.REPLACE_EXISTING);
            }

            // 4. Construir respuesta
            String publicUrl = buildPublicUrl(subDirectory, fileName);

            AudioUploadResponseDTO dto = new AudioUploadResponseDTO();
            dto.setUrl(publicUrl);
            dto.setMime(file.getContentType());
            dto.setDurMs(durMs);

            return dto;

        } catch (IOException e) {
            // Loguear el error real es importante (usando slf4j por ejemplo)
            throw new RuntimeException(ExceptionConstants.ERROR_AUDIO_SAVE_FAILED, e);
        }
    }

    // --- Métodos Auxiliares (Clean Code) ---

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException(ExceptionConstants.ERROR_AUDIO_EMPTY);
        }

        // Validación de seguridad básica por Content-Type
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_AUDIO_TYPES.contains(contentType.toLowerCase())) {
            throw new IllegalArgumentException(ExceptionConstants.ERROR_FILE_TYPE_NOT_ALLOWED + contentType);
        }

        // Validación extra opcional: Tamaño máximo (si no se maneja en
        // application.properties)
        if (file.getSize() > 10 * 1024 * 1024) { // 10MB
            throw new IllegalArgumentException(ExceptionConstants.ERROR_FILE_SIZE_EXCEEDED);
        }
    }

    private Path prepareDirectory(String subDir) throws IOException {
        Path dir = Paths.get(uploadsRoot, subDir).toAbsolutePath().normalize();
        if (!Files.exists(dir)) {
            Files.createDirectories(dir);
        }
        return dir;
    }

    private String generateUniqueFileName(String extension) {
        return UUID.randomUUID().toString() + extension;
    }

    private String buildPublicUrl(String subDir, String fileName) {
        // Asegura que no falten barras diagonales, pero evita duplicarlas
        String baseUrl = uploadsBaseUrl.endsWith("/") ? uploadsBaseUrl : uploadsBaseUrl + "/";
        return baseUrl + subDir + "/" + fileName;
    }
}
