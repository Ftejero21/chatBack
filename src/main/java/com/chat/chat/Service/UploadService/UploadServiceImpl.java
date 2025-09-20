package com.chat.chat.Service.UploadService;

import com.chat.chat.DTO.AudioUploadResponseDTO;
import com.chat.chat.Utils.Utils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.UUID;

@Service
public class UploadServiceImpl implements UploadService {

    @Value("${app.uploads.root:uploads}")
    private String uploadsRoot;

    @Value("${app.uploads.base-url:/uploads}")
    private String uploadsBaseUrl;

    @Override
    public AudioUploadResponseDTO uploadAudio(MultipartFile file, Integer durMs) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Archivo vacío");
        }

        try {
            String contentType = file.getContentType();
            String ext = Utils.extensionFor(contentType);
            Path dir = Paths.get(uploadsRoot, "voice");
            Files.createDirectories(dir);

            String name = UUID.randomUUID().toString() + ext;
            Path path = dir.resolve(name);

            // Sobrescribir si existiera (no debería)
            Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);

            String publicUrl = uploadsBaseUrl + "/voice/" + name;

            AudioUploadResponseDTO dto = new AudioUploadResponseDTO();
            dto.setUrl(publicUrl);
            dto.setMime(contentType);
            dto.setDurMs(durMs);
            return dto;
        } catch (IOException e) {
            throw new RuntimeException("Error guardando audio", e);
        }
    }
}
