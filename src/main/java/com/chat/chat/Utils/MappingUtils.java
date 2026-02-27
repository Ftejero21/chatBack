package com.chat.chat.Utils;

import com.chat.chat.DTO.ChatGrupalDTO;
import com.chat.chat.DTO.ChatIndividualDTO;
import com.chat.chat.DTO.MensajeDTO;
import com.chat.chat.DTO.NotificationDTO;
import com.chat.chat.DTO.UsuarioDTO;
import com.chat.chat.Entity.ChatGrupalEntity;
import com.chat.chat.Entity.ChatIndividualEntity;
import com.chat.chat.Entity.MensajeEntity;
import com.chat.chat.Entity.NotificationEntity;
import com.chat.chat.Entity.UsuarioEntity;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.stream.Collectors;

public class MappingUtils {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static String safeFotoUrl(String url) {
        return (url == null || url.isBlank()) ? null : url;
    }

    public static NotificationDTO notificationEntityADto(NotificationEntity e) {
        if (e == null) {
            return null;
        }
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
        if (dto.getFoto() != null && (dto.getFoto().startsWith("/uploads/") || dto.getFoto().startsWith("http"))) {
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
        dto.setHasPublicKey(e.getPublicKey() != null && !e.getPublicKey().isBlank());
        dto.setFoto(safeFotoUrl(e.getFotoUrl()));
        dto.setRoles(e.getRoles());

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
        e.setReenviado(dto.isReenviado());
        e.setMensajeOriginalId(dto.getMensajeOriginalId());
        e.setReplyToMessageId(dto.getReplyToMessageId());
        e.setReplySnippet(dto.getReplySnippet());
        e.setReplyAuthorName(dto.getReplyAuthorName());

        MessageType t = MessageType.TEXT;
        if (Constantes.TIPO_AUDIO.equalsIgnoreCase(dto.getTipo())) {
            t = MessageType.AUDIO;
        } else if (Constantes.TIPO_IMAGE.equalsIgnoreCase(dto.getTipo())) {
            t = MessageType.IMAGE;
        } else if (Constantes.TIPO_VIDEO.equalsIgnoreCase(dto.getTipo())) {
            t = MessageType.VIDEO;
        } else if (Constantes.TIPO_FILE.equalsIgnoreCase(dto.getTipo())) {
            t = MessageType.FILE;
        } else if (Constantes.TIPO_SYSTEM.equalsIgnoreCase(dto.getTipo())) {
            t = MessageType.SYSTEM;
        }
        e.setTipo(t);

        if (t == MessageType.AUDIO) {
            e.setMediaUrl(dto.getAudioUrl());
            e.setMediaMime(dto.getAudioMime());
            e.setMediaDuracionMs(dto.getAudioDuracionMs());
        } else if (t == MessageType.IMAGE) {
            String imageUrl = firstNonBlank(dto.getImageUrl(), jsonTextField(dto.getContenido(), "imageUrl"));
            String imageMime = firstNonBlank(dto.getImageMime(), jsonTextField(dto.getContenido(), "imageMime"));
            e.setMediaUrl(imageUrl);
            e.setMediaMime(imageMime);
            e.setMediaDuracionMs(null);
        }

        e.setActivo(dto.isActivo());
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
            dto.setAudioUrl(e.getMediaUrl());
            dto.setAudioMime(e.getMediaMime());
            dto.setAudioDuracionMs(e.getMediaDuracionMs());
        } else if (e.getTipo() == MessageType.IMAGE) {
            String imageUrl = firstNonBlank(e.getMediaUrl(), jsonTextField(e.getContenido(), "imageUrl"));
            String imageMime = firstNonBlank(e.getMediaMime(), jsonTextField(e.getContenido(), "imageMime"));
            String imageNombre = firstNonBlank(
                    jsonTextField(e.getContenido(), "imageNombre"),
                    extractFileNameFromUrl(imageUrl));
            dto.setImageUrl(imageUrl);
            dto.setImageMime(imageMime);
            dto.setImageNombre(imageNombre);
        }

        dto.setChatId(e.getChat() != null ? e.getChat().getId() : null);

        dto.setActivo(e.isActivo());
        dto.setLeido(e.isLeido());
        dto.setFechaEnvio(e.getFechaEnvio());
        dto.setReenviado(e.isReenviado());
        dto.setMensajeOriginalId(e.getMensajeOriginalId());
        dto.setReplyToMessageId(e.getReplyToMessageId());
        dto.setReplySnippet(e.getReplySnippet());
        dto.setReplyAuthorName(e.getReplyAuthorName());

        if (e.getEmisor() != null) {
            dto.setEmisorNombre(e.getEmisor().getNombre());
            dto.setEmisorApellido(e.getEmisor().getApellido());
            String nombre = e.getEmisor().getNombre();
            String apellido = e.getEmisor().getApellido();
            String fullName = ((nombre == null ? "" : nombre)
                    + (apellido == null || apellido.trim().isEmpty() ? "" : " " + apellido)).trim();
            if (!fullName.isEmpty()) {
                dto.setEmisorNombreCompleto(fullName);
            }
            dto.setEmisorFoto(e.getEmisor().getFotoUrl());
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
        dto.setReceptor(null);
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

    private static String jsonTextField(String json, String field) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            JsonNode root = OBJECT_MAPPER.readTree(json);
            JsonNode node = root == null ? null : root.get(field);
            if (node == null || node.isNull() || !node.isTextual()) {
                return null;
            }
            String value = node.asText();
            return value == null || value.isBlank() ? null : value;
        } catch (Exception ex) {
            return null;
        }
    }

    private static String extractFileNameFromUrl(String url) {
        if (url == null || url.isBlank()) {
            return null;
        }
        int slash = url.lastIndexOf('/');
        return slash >= 0 && slash + 1 < url.length() ? url.substring(slash + 1) : url;
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }
}
