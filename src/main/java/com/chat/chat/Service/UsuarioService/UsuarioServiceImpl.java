package com.chat.chat.Service.UsuarioService;

import com.chat.chat.DTO.UsuarioDTO;
import com.chat.chat.Entity.UsuarioEntity;
import com.chat.chat.Exceptions.EmailNoRegistradoException;
import com.chat.chat.Exceptions.PasswordIncorrectaException;
import com.chat.chat.Repository.UsuarioRepository;
import com.chat.chat.Utils.Constantes;
import com.chat.chat.Utils.MappingUtils;
import com.chat.chat.Utils.Utils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class UsuarioServiceImpl implements UsuarioService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Value("${app.uploads.root:uploads}")      // carpeta base
    private String uploadsRoot;

    @Value("${app.uploads.base-url:/uploads}") // prefijo público
    private String uploadsBaseUrl;

    // repos, encoder, etc. inyectados…

    @Override
    public UsuarioDTO crearUsuario(UsuarioDTO dto) {

        if (usuarioRepository.findByEmail(dto.getEmail()).isPresent()) {
            throw new RuntimeException("Ya existe un usuario con ese email");
        }

        // 1) foto: si llega como dataURL, guardamos y sustituimos por URL
        String fotoUrl = null;
        if (dto.getFoto() != null) {
            String f = dto.getFoto();
            if (dto.getFoto() != null && dto.getFoto().startsWith("data:image")) {
                String url = Utils.saveDataUrlToUploads(dto.getFoto(), "avatars", uploadsRoot, uploadsBaseUrl);
                dto.setFoto(url);                 // guarda URL pública en DTO
            } else if (f.startsWith("/uploads/") || f.startsWith("http")) {
                fotoUrl = f; // ya es una URL válida
            }
            // si quieres, limpia dto para no guardar base64 por error
            dto.setFoto(fotoUrl);
        }

        // 2) mapear y ajustar campos
        UsuarioEntity entity = MappingUtils.usuarioDtoAEntity(dto);
        entity.setFechaCreacion(LocalDateTime.now());
        entity.setActivo(true);
        entity.setRoles(Collections.singleton(Constantes.USUARIO));

        String encryptedPassword = passwordEncoder.encode(dto.getPassword());
        entity.setPassword(encryptedPassword);

        // setear fotoUrl en la entidad (si procede)
        if (fotoUrl != null) {
            entity.setFotoUrl(fotoUrl);
        } else if (entity.getFotoUrl() == null) {
            // deja null si no hay foto
            entity.setFotoUrl(null);
        }

        UsuarioEntity saved = usuarioRepository.save(entity);
        return MappingUtils.usuarioEntityADto(saved);
    }



    @Override
    public List<UsuarioDTO> listarUsuariosActivos() {
        List<UsuarioDTO> list = usuarioRepository.findAll().stream()
                .filter(UsuarioEntity::isActivo)
                .map(MappingUtils::usuarioEntityADto)
                .collect(Collectors.toList());

        // 🔄 Convertir /uploads/... a dataURL Base64 (igual que getById)
        for (UsuarioDTO dto : list) {
            String foto = dto.getFoto();
            if (foto != null && foto.startsWith("/uploads/")) {
                String dataUrl = Utils.toDataUrlFromUrl(foto, uploadsRoot);
                if (dataUrl != null) {
                    dto.setFoto(dataUrl);
                } // si devuelve null, dejamos la URL tal cual
            }
        }

        return list;
    }

    @Override
    public UsuarioDTO login(String email, String password) {
        UsuarioEntity usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(EmailNoRegistradoException::new);

        if (!passwordEncoder.matches(password, usuario.getPassword())) {
            throw new PasswordIncorrectaException();
        }

        UsuarioDTO dto = MappingUtils.usuarioEntityADto(usuario);
        if (dto.getFoto() != null && dto.getFoto().startsWith("/uploads/")) {
            dto.setFoto(Utils.toDataUrlFromUrl(dto.getFoto(), uploadsRoot)); // data:image/...;base64,...
        }
        return dto;
    }

    @Override
    public UsuarioDTO getById(Long id) {
        UsuarioEntity u = usuarioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        UsuarioDTO dto = MappingUtils.usuarioEntityADto(u);
        // 👉 Si la foto es una URL pública (/uploads/...), la convertimos a Base64 para el front
        if (dto.getFoto() != null && dto.getFoto().startsWith("/uploads/")) {
            dto.setFoto(Utils.toDataUrlFromUrl(dto.getFoto(), uploadsRoot));
        }
        return dto;
    }


    @Override
    public List<UsuarioDTO> buscarPorNombre(String q) {
        if (q == null || q.trim().isEmpty()) {
            return Collections.emptyList();
        }
        String query = q.trim();

        List<UsuarioDTO> list = usuarioRepository.searchActivosByNombre(query)
                .stream()
                .map(MappingUtils::usuarioEntityADto)
                .collect(Collectors.toList());

        // Convertir /uploads/... a dataURL Base64 (igual que en otros métodos)
        for (UsuarioDTO dto : list) {
            String foto = dto.getFoto();
            if (foto != null && foto.startsWith("/uploads/")) {
                String dataUrl = Utils.toDataUrlFromUrl(foto, uploadsRoot);
                if (dataUrl != null) {
                    dto.setFoto(dataUrl);
                }
            }
        }

        return list;
    }


}
