package com.chat.chat.Service.StickerService;

import com.chat.chat.DTO.StickerDTO;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface StickerService {
    StickerDTO guardarSticker(MultipartFile archivo, String nombre);
    List<StickerDTO> listarMisStickers();
    void eliminarSticker(Long stickerId);
    StickerDTO obtenerSticker(Long stickerId);
    ResponseEntity<Resource> descargarSticker(Long stickerId);
    boolean isOwnedByUser(Long stickerId, Long userId);
    StickerDTO saveStickerToUser(Long stickerId, Long userId);
}
