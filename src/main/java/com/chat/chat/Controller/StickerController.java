package com.chat.chat.Controller;

import com.chat.chat.DTO.CrearStickerRequestDTO;
import com.chat.chat.DTO.StickerDTO;
import com.chat.chat.Service.StickerService.StickerService;
import com.chat.chat.Utils.Constantes;
import com.chat.chat.Utils.SecurityUtils;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping(Constantes.API_STICKERS)
@Validated
public class StickerController {

    private final StickerService stickerService;
    private final SecurityUtils securityUtils;

    public StickerController(StickerService stickerService, SecurityUtils securityUtils) {
        this.stickerService = stickerService;
        this.securityUtils = securityUtils;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<StickerDTO> crearSticker(@RequestParam(value = Constantes.KEY_FILE, required = false) MultipartFile file,
                                                   @RequestParam(value = "archivo", required = false) MultipartFile archivo,
                                                   @RequestParam(value = "foto", required = false) MultipartFile foto,
                                                   @Valid @ModelAttribute CrearStickerRequestDTO request) {
        MultipartFile effectiveFile = firstPresent(file, archivo, foto);
        return ResponseEntity.ok(stickerService.guardarSticker(effectiveFile, request == null ? null : request.getNombre()));
    }

    @GetMapping(Constantes.STICKERS_MIS_STICKERS)
    public ResponseEntity<List<StickerDTO>> listarMisStickers() {
        return ResponseEntity.ok(stickerService.listarMisStickers());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> eliminarSticker(@PathVariable("id") Long stickerId) {
        stickerService.eliminarSticker(stickerId);
        return ResponseEntity.ok(Map.of(Constantes.KEY_MENSAJE, "Sticker eliminado"));
    }

    @GetMapping("/{id}/archivo")
    public ResponseEntity<Resource> descargarSticker(@PathVariable("id") @Min(1) Long stickerId) {
        return stickerService.descargarSticker(stickerId);
    }

    @GetMapping("/{stickerId}/owned-by-me")
    public ResponseEntity<Map<String, Boolean>> ownedByMe(@PathVariable("stickerId") @Min(1) Long stickerId) {
        Long currentUserId = securityUtils.getAuthenticatedUserId();
        return ResponseEntity.ok(Map.of("owned", stickerService.isOwnedByUser(stickerId, currentUserId)));
    }

    @PostMapping("/{stickerId}/save-to-me")
    public ResponseEntity<StickerDTO> saveToMe(@PathVariable("stickerId") @Min(1) Long stickerId) {
        Long currentUserId = securityUtils.getAuthenticatedUserId();
        return ResponseEntity.status(201).body(stickerService.saveStickerToUser(stickerId, currentUserId));
    }

    private MultipartFile firstPresent(MultipartFile... files) {
        if (files == null) {
            return null;
        }
        for (MultipartFile f : files) {
            if (f != null && !f.isEmpty()) {
                return f;
            }
        }
        return null;
    }
}
