package com.chat.chat.Service.StickerService;

import com.chat.chat.DTO.StickerDTO;
import com.chat.chat.Entity.StickerEntity;
import com.chat.chat.Entity.UsuarioEntity;
import com.chat.chat.Exceptions.ConflictoException;
import com.chat.chat.Exceptions.RecursoNoEncontradoException;
import com.chat.chat.Repository.StickerRepository;
import com.chat.chat.Repository.UsuarioRepository;
import com.chat.chat.Utils.SecurityUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class StickerServiceImplTest {

    @TempDir
    Path tempDir;

    private StickerRepository stickerRepository;
    private UsuarioRepository usuarioRepository;
    private StickerServiceImpl service;

    @BeforeEach
    void setUp() {
        stickerRepository = mock(StickerRepository.class);
        usuarioRepository = mock(UsuarioRepository.class);
        SecurityUtils securityUtils = mock(SecurityUtils.class);
        service = new StickerServiceImpl(
                tempDir.toString(),
                "/uploads",
                5_242_880L,
                200,
                stickerRepository,
                usuarioRepository,
                securityUtils
        );
    }

    @Test
    void isOwnedByUser_true_false_404() {
        StickerEntity owned = sticker(7L, 9L, true, "/uploads/stickers/a.png", "image/png", 8L);
        when(stickerRepository.findByIdAndActivoTrue(7L)).thenReturn(Optional.of(owned));
        assertTrue(service.isOwnedByUser(7L, 9L));
        assertFalse(service.isOwnedByUser(7L, 10L));
        when(stickerRepository.findByIdAndActivoTrue(8L)).thenReturn(Optional.empty());
        assertThrows(RecursoNoEncontradoException.class, () -> service.isOwnedByUser(8L, 9L));
    }

    @Test
    void saveStickerToUser_ok_duplicate_404() throws Exception {
        Long userId = 5L;
        Long sourceId = 11L;
        UsuarioEntity owner = new UsuarioEntity();
        owner.setId(1L);
        owner.setActivo(true);
        UsuarioEntity current = new UsuarioEntity();
        current.setId(userId);
        current.setActivo(true);

        Path stickersDir = tempDir.resolve("stickers");
        Files.createDirectories(stickersDir);
        Path sourceFile = stickersDir.resolve("s.webp");
        byte[] content = "RIFFxxxxWEBPVP8 ".getBytes();
        Files.write(sourceFile, content);

        StickerEntity source = new StickerEntity();
        source.setId(sourceId);
        source.setUsuario(owner);
        source.setActivo(true);
        source.setArchivoUrl("/uploads/stickers/s.webp");
        source.setTipoMime("image/webp");
        source.setTamano((long) content.length);
        source.setNombre("s.webp");

        when(usuarioRepository.findById(userId)).thenReturn(Optional.of(current));
        when(stickerRepository.countByUsuarioIdAndActivoTrue(userId)).thenReturn(0L);
        when(stickerRepository.findByIdAndActivoTrue(sourceId)).thenReturn(Optional.of(source));
        when(stickerRepository.existsByUsuarioIdAndSourceStickerIdAndActivoTrue(userId, sourceId)).thenReturn(false);
        when(stickerRepository.save(any(StickerEntity.class))).thenAnswer(invocation -> {
            StickerEntity e = invocation.getArgument(0);
            e.setId(99L);
            return e;
        });

        StickerDTO created = service.saveStickerToUser(sourceId, userId);
        assertNotNull(created);
        assertEquals(99L, created.getId());

        when(stickerRepository.existsByUsuarioIdAndSourceStickerIdAndActivoTrue(userId, sourceId)).thenReturn(true);
        assertThrows(ConflictoException.class, () -> service.saveStickerToUser(sourceId, userId));

        when(stickerRepository.findByIdAndActivoTrue(222L)).thenReturn(Optional.empty());
        assertThrows(RecursoNoEncontradoException.class, () -> service.saveStickerToUser(222L, userId));
    }

    private StickerEntity sticker(Long id, Long ownerId, boolean active, String url, String mime, Long size) {
        UsuarioEntity owner = new UsuarioEntity();
        owner.setId(ownerId);
        owner.setActivo(true);
        StickerEntity sticker = new StickerEntity();
        sticker.setId(id);
        sticker.setUsuario(owner);
        sticker.setActivo(active);
        sticker.setArchivoUrl(url);
        sticker.setTipoMime(mime);
        sticker.setTamano(size);
        sticker.setNombre("s");
        return sticker;
    }
}
