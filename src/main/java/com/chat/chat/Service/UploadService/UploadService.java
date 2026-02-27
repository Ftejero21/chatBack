package com.chat.chat.Service.UploadService;

import com.chat.chat.DTO.AudioUploadResponseDTO;
import com.chat.chat.DTO.FileUploadResponseDTO;
import org.springframework.web.multipart.MultipartFile;

public interface UploadService {

    AudioUploadResponseDTO uploadAudio(MultipartFile file, Integer durMs);

    FileUploadResponseDTO uploadEncryptedFile(MultipartFile file);
}
