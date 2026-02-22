package com.chat.chat.Controller;

import com.chat.chat.DTO.AudioUploadResponseDTO;
import com.chat.chat.Service.UploadService.UploadService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.chat.chat.Utils.Constantes;

@RestController
@RequestMapping(Constantes.API_UPLOADS_ALL)
@CrossOrigin("*")
public class UploadController {

    @Autowired
    private UploadService uploadService;

    @PostMapping(value = Constantes.UPLOAD_AUDIO, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public AudioUploadResponseDTO uploadAudio(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "durMs", required = false) Integer durMs) {
        return uploadService.uploadAudio(file, durMs);
    }
}
