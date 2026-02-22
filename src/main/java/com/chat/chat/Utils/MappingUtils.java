package com.chat.chat.Utils;

import com.chat.chat.Configuracion.DTO.*;
import com.chat.chat.DTO.*;
import com.chat.chat.Entity.*;

import java.util.List;
import java.util.stream.Collectors;

public class MappingUtils {

    private static String safeFotoUrl(String url) {
        return (url == null || url.isBlank())
                ? null // o pon aquí un placeholder, p. ej. "/uploads/avatars/default.png"
                : url;
    }

    public static NotificationDTO notificationEntityADto(NotificationEntity e) {
        if (e == null)
            return null;
        NotificationDTO dto = new NotificationDTO();
        dto.setId(e.getId());
        dto.setUserId(e.getUserId());
        dto.setType(e.getType());
        dto.setPayloadJson(e.getPayloadJson());
        dto.setSeen(e.isSeen());
        dto.setResolved(e.isResolved());
        dto.setCreatedAt(e.getCreatedAt());
        return dto;
    }

    public static List<NotificationDTO> notificationEntityListADto(List<NotificationEntity> list) {
        return list == null ? List.of()
                : list.stream().map(MappingUtils::notificationEntityADto).collect(Collectors.toList());
    }

    public static UsuarioEntity usuarioDtoAEntity(UsuarioDTO dto) {
        UsuarioEntity e = new UsuarioEntity();
        e.setId(dto.getId());
        e.setNombre(dto.getNombre());
        e.setApellido(dto.getApellido());
        e.setEmail(dto.getEmail());
        e.setPublicKey(dto.getPublicKey());
        // Si ya viene una URL (no base64), la mapeamos
        if (dto.getFoto() != null &&
                (dto.getFoto().startsWith("/uploads/") || dto.getFoto().startsWith("http"))) {
            e.setFotoUrl(dto.getFoto());
        }
        return e;
    }

    public static UsuarioDTO usuarioEntityADto(UsuarioEntity e) {
        UsuarioDTO dto = new UsuarioDTO();
        dto.setId(e.getId());
        dto.setNombre(e.getNombre());
        dto.setApellido(e.getApellido());
        dto.setEmail(e.getEmail());
        dto.setActivo(e.isActivo());
        dto.setPublicKey(e.getPublicKey());
        dto.setFoto(safeFotoUrl(e.getFotoUrl())); // 👈 URL pública (o null si no hay)
        dto.setRoles(e.getRoles()); // Asignar los roles para el Frontend

        if (e.getBloqueados() != null) {
            dto.setBloqueadosIds(e.getBloqueados().stream()
                    .map(UsuarioEntity::getId)
                    .collect(Collectors.toSet()));
        }

        if (e.getMeHanBloqueado() != null) {
            dto.setMeHanBloqueadoIds(e.getMeHanBloqueado().stream()
                    .map(UsuarioEntity::getId)
                    .collect(Collectors.toSet()));
        }

        return dto;
    }

    public static MensajeEntity mensajeDtoAEntity(MensajeDTO dto,
                                                  UsuarioEntity emisor,
                                                  UsuarioEntity receptor) {
        MensajeEntity e = new MensajeEntity();
        e.setId(dto.getId());
        e.setEmisor(emisor);
        e.setReceptor(receptor);
        e.setContenido(dto.getContenido());

        // tipo
        MessageType t = MessageType.TEXT;
        if ("AUDIO".equalsIgnoreCase(dto.getTipo()))
            t = MessageType.AUDIO;
        e.setTipo(t);

        if (t == MessageType.AUDIO) {
            e.setMediaUrl(dto.getAudioUrl()); // lo definimos en service si viene dataURL
            e.setMediaMime(dto.getAudioMime());
            e.setMediaDuracionMs(dto.getAudioDuracionMs());
        }

        e.setActivo(dto.isActivo()); // boolean primitivo => usa isActivo()
        e.setLeido(dto.isLeido());
        e.setFechaEnvio(dto.getFechaEnvio());
        return e;
    }

    public static MensajeDTO mensajeEntityADto(MensajeEntity e) {
        MensajeDTO dto = new MensajeDTO();
        dto.setId(e.getId());
        dto.setEmisorId(e.getEmisor() != null ? e.getEmisor().getId() : null);
        dto.setReceptorId(e.getReceptor() != null ? e.getReceptor().getId() : null);
        dto.setContenido(e.getContenido());
        dto.setTipo(e.getTipo().name());

        if (e.getTipo() == MessageType.AUDIO) {
            dto.setAudioUrl(e.getMediaUrl()); // devolvemos URL pública
            dto.setAudioMime(e.getMediaMime());
            dto.setAudioDuracionMs(e.getMediaDuracionMs());
        }

        dto.setChatId(e.getChat() != null ? e.getChat().getId() : null);

        dto.setActivo(e.isActivo());
        dto.setLeido(e.isLeido());
        dto.setFechaEnvio(e.getFechaEnvio());

        // si ya envías nombre/foto del emisor:
        if (e.getEmisor() != null) {
            dto.setEmisorNombre(e.getEmisor().getNombre());
            dto.setEmisorFoto(
                    // si guardas foto como /uploads/, puedes convertir a dataURL si quieres
                    e.getEmisor().getFotoUrl());
        }
        return dto;
    }

    public static ChatIndividualDTO chatIndividualEntityADto(ChatIndividualEntity entity,
                                                             UsuarioEntity usuarioLogueado) {
        ChatIndividualDTO dto = new ChatIndividualDTO();
        dto.setId(entity.getId());

        UsuarioEntity receptor = entity.getUsuario1().getId().equals(usuarioLogueado.getId())
                ? entity.getUsuario2()
                : entity.getUsuario1();

        dto.setReceptor(usuarioEntityADto(receptor));

        return dto;
    }

    public static ChatIndividualDTO chatIndividualEntityADto(ChatIndividualEntity entity) {
        ChatIndividualDTO dto = new ChatIndividualDTO();
        dto.setId(entity.getId());

        // Opcional: podrías devolver ambos usuarios si quieres
        dto.setReceptor(null); // o usuario2 por defecto

        return dto;
    }

    public static ChatGrupalEntity chatGrupalDtoAEntity(ChatGrupalDTO dto) {
        ChatGrupalEntity e = new ChatGrupalEntity();
        dto.setId(e.getId());
        dto.setNombreGrupo(e.getNombreGrupo());
        List<UsuarioDTO> usuarios = e.getUsuarios().stream()
                .map(MappingUtils::usuarioEntityADto)
                .collect(Collectors.toList());
        dto.setUsuarios(usuarios);
        dto.setFotoGrupo(safeFotoUrl(e.getFotoUrl()));
        return e;
    }

    public static ChatGrupalDTO chatGrupalEntityADto(ChatGrupalEntity entity) {
        ChatGrupalDTO dto = new ChatGrupalDTO();
        dto.setId(entity.getId());
        dto.setNombreGrupo(entity.getNombreGrupo());
        List<UsuarioDTO> usuarios = entity.getUsuarios().stream()
                .map(MappingUtils::usuarioEntityADto)
                .collect(Collectors.toList());
        dto.setUsuarios(usuarios);
        dto.setFotoGrupo(safeFotoUrl(entity.getFotoUrl()));
        return dto;
    }
}
