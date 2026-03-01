package com.chat.chat.Service.ChatService;

import com.chat.chat.DTO.*;
import com.chat.chat.DTO.*;
import com.chat.chat.Entity.*;
import com.chat.chat.Repository.*;
import com.chat.chat.Utils.*;
import com.chat.chat.Utils.ExceptionConstants;
import com.chat.chat.Utils.ChatConstants;
import com.chat.chat.Configuracion.EstadoUsuarioManager;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.Normalizer;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class ChatServiceImpl implements ChatService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ChatServiceImpl.class);
    private static final int DEFAULT_MESSAGES_PAGE = 0;
    private static final int DEFAULT_MESSAGES_SIZE = 50;
    private static final int MAX_MESSAGES_SIZE = 50;
    private static final int DEFAULT_MEDIA_SIZE = 30;
    private static final int MAX_MEDIA_SIZE = 50;
    private static final int DEFAULT_SEARCH_PAGE = 0;
    private static final int DEFAULT_SEARCH_SIZE = 20;
    private static final int MAX_SEARCH_SIZE = 100;
    private static final int SEARCH_SNIPPET_CONTEXT_CHARS = 40;
    private static final String CURSOR_SEPARATOR = "_";
    private static final Pattern DIACRITICS_PATTERN = Pattern.compile("\\p{M}+");
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Autowired
    private UsuarioRepository usuarioRepo;

    @Autowired
    private ChatIndividualRepository chatIndRepo;

    @Autowired
    SimpMessagingTemplate messagingTemplate;

    @Value(Constantes.PROP_UPLOADS_ROOT)
    private String uploadsRoot;

    @Value(Constantes.PROP_UPLOADS_BASE_URL)
    private String uploadsBaseUrl;

    @Autowired
    private MensajeRepository mensajeRepository;

    @Autowired
    private MensajeReaccionRepository mensajeReaccionRepository;

    @Autowired
    private ChatRepository chatRepository;

    @Autowired
    private GroupInviteRepo inviteRepo;
    @Autowired
    private NotificationRepo notificationRepo;

    @Autowired
    private ChatGrupalRepository chatGrupalRepo;

    @Autowired
    private SecurityUtils securityUtils;

    @Autowired
    private AdminAuditCrypto adminAuditCrypto;

    @Autowired
    private EstadoUsuarioManager estadoUsuarioManager;

    @Override
    public ChatIndividualDTO crearChatIndividual(Long usuario1Id, Long usuario2Id) {
        Long authenticatedUserId = securityUtils.getAuthenticatedUserId();
        // Solo podemos crear chats donde participemos nosotros mismos
        if (!authenticatedUserId.equals(usuario1Id) && !authenticatedUserId.equals(usuario2Id)) {
            throw new SecurityException(ExceptionConstants.ERROR_CREATE_THIRD_PARTY_CHAT);
        }

        UsuarioEntity u1 = usuarioRepo.findById(usuario1Id).orElseThrow();
        UsuarioEntity u2 = usuarioRepo.findById(usuario2Id).orElseThrow();

        if (u1.getBloqueados().contains(u2) || u2.getBloqueados().contains(u1)) {
            throw new RuntimeException(Constantes.MSG_CHAT_INDIVIDUAL_BLOQUEADO);
        }

        ChatIndividualEntity chat = new ChatIndividualEntity();
        chat.setUsuario1(u1);
        chat.setUsuario2(u2);

        return MappingUtils.chatIndividualEntityADto(chatIndRepo.save(chat));
    }

    @Override
    @Transactional
    public ChatGrupalDTO crearChatGrupal(ChatGrupalDTO dto) {
        // En vez de usar el ID que manda el front (que se puede falsificar), usamos el
        // del token
        Long authenticatedUserId = securityUtils.getAuthenticatedUserId();

        // 1) Creador desde auth
        UsuarioEntity creador = usuarioRepo.findById(authenticatedUserId)
                .orElseThrow(() -> new RuntimeException(Constantes.MSG_CREADOR_NO_ENCONTRADO));

        // 2) Procesar fotoGrupo: dataURL -> guardar; URL pÃºblica -> usar
        String fotoUrl = null;
        if (dto.getFotoGrupo() != null) {
            String f = dto.getFotoGrupo().trim();
            if (f.startsWith(Constantes.DATA_IMAGE_PREFIX)) {
                fotoUrl = Utils.saveDataUrlToUploads(f, Constantes.DIR_GROUP_PHOTOS, uploadsRoot, uploadsBaseUrl);
            } else if (Utils.isPublicUrl(f)) {
                fotoUrl = f;
            }
        }

        // 3) Crear el chat con el creador como Ãºnico miembro inicial
        ChatGrupalEntity chat = new ChatGrupalEntity();
        chat.setNombreGrupo(dto.getNombreGrupo());
        chat.setFotoUrl(fotoUrl);
        chat.setDescripcion(dto.getDescripcion());
        if (dto.getVisibilidad() != null && !dto.getVisibilidad().isBlank()) {
            try {
                chat.setVisibilidad(GroupVisibility.valueOf(dto.getVisibilidad().trim().toUpperCase()));
            } catch (IllegalArgumentException ex) {
                chat.setVisibilidad(GroupVisibility.PRIVADO);
            }
        } else {
            chat.setVisibilidad(GroupVisibility.PRIVADO);
        }
        chat.setCreador(creador);
        chat.setUsuarios(new ArrayList<>(List.of(creador)));
        chat.getAdmins().add(creador);
        chat.setMediaCount(0);
        chat.setFilesCount(0);
        chat.setActivo(true);
        chat = chatGrupalRepo.save(chat);

        // 4) Preparar invitados (los que vienen en dto, excepto el creador, sin
        // duplicados)
        Set<Long> inviteeIds = (dto.getUsuarios() == null) ? Set.of()
                : dto.getUsuarios().stream()
                        .map(UsuarioDTO::getId)
                        .filter(id -> !Objects.equals(id, authenticatedUserId))
                        .filter(id -> {
                            UsuarioEntity potentialInvitee = usuarioRepo.findById(id).orElse(null);
                            if (potentialInvitee == null)
                                return false;
                            // Prevenir agregar si creador bloqueÃ³ al invitado, o si el invitado bloqueÃ³ al
                            // creador
                            return !creador.getBloqueados().contains(potentialInvitee) &&
                                    !potentialInvitee.getBloqueados().contains(creador);
                        })
                        .collect(Collectors.toCollection(LinkedHashSet::new));

        // 5) Crear invitaciones
        List<GroupInviteEntity> invites = new ArrayList<>();
        for (Long inviteeId : inviteeIds) {
            UsuarioEntity invitee = usuarioRepo.findById(inviteeId)
                    .orElseThrow(() -> new IllegalArgumentException(Constantes.MSG_USUARIO_INVITADO_NO_EXISTE + inviteeId));
            GroupInviteEntity inv = new GroupInviteEntity();
            inv.setChat(chat);
            inv.setInviter(creador);
            inv.setInvitee(invitee);
            inv.setStatus(InviteStatus.PENDING);
            invites.add(inv);
        }
        inviteRepo.saveAll(invites);

        // 6) Guardar notificaciÃ³n + enviar WS a cada invitado
        for (GroupInviteEntity inv : invites) {
            GroupInviteWS wsPayload = new GroupInviteWS();
            wsPayload.inviteId = inv.getId();
            wsPayload.groupId = chat.getId();
            wsPayload.groupName = chat.getNombreGrupo();
            wsPayload.inviterId = creador.getId();
            wsPayload.inviterNombre = creador.getNombre();

            NotificationEntity notif = new NotificationEntity();
            notif.setUserId(inv.getInvitee().getId());
            notif.setType(NotificationType.GROUP_INVITE);
            notif.setPayloadJson(Utils.writeJson(wsPayload));
            notif.setSeen(false);
            notificationRepo.save(notif);

            long unseen = notificationRepo.countByUserIdAndSeenFalse(inv.getInvitee().getId());
            wsPayload.unseenCount = (int) unseen;

            messagingTemplate.convertAndSend(Constantes.WS_TOPIC_NOTIFICATIONS + inv.getInvitee().getId(), wsPayload);
        }

        // 7) DTO de salida (con foto)
        ChatGrupalDTO out = MappingUtils.chatGrupalEntityADto(chat);
        out.setUltimaMensaje(ChatConstants.MSG_GRUPO_CREADO);
        out.setUltimaFecha(LocalDateTime.now());
        out.setIdCreador(creador.getId());
        out.setFotoGrupo(fotoUrl);
        return out;
    }

    @Override
    @Transactional
    public MessagueSalirGrupoDTO salirDeChatGrupal(Long groupId, Long userId) {
        // Validaciones bÃ¡sicas (segÃºn tu requerimiento, userId nunca serÃ¡ null)
        if (groupId == null) {
            return new MessagueSalirGrupoDTO(false, Constantes.MSG_GROUP_ID_OBLIGATORIO, null, userId, false);
        }

        ChatGrupalEntity chat = chatGrupalRepo.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException(Constantes.MSG_GRUPO_NO_EXISTE_ID + groupId));

        UsuarioEntity usuario = usuarioRepo.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException(Constantes.MSG_USUARIO_NO_EXISTE_ID + userId));

        // Comprobar membresÃ­a
        boolean eraMiembro = chat.getUsuarios() != null &&
                chat.getUsuarios().stream().anyMatch(u -> Objects.equals(u.getId(), userId));
        if (!eraMiembro) {
            return new MessagueSalirGrupoDTO(false, Constantes.MSG_NO_PERTENECE_GRUPO, groupId, userId, false);
        }

        // Eliminar del listado de usuarios
        Iterator<UsuarioEntity> it = chat.getUsuarios().iterator();
        while (it.hasNext()) {
            UsuarioEntity u = it.next();
            if (Objects.equals(u.getId(), userId)) {
                it.remove();
                break;
            }
        }

        // Si queda vacÃ­o â†’ eliminar grupo
        if (chat.getUsuarios() == null || chat.getUsuarios().isEmpty()) {
            chat.setActivo(false);
            chatGrupalRepo.save(chat);

            return new MessagueSalirGrupoDTO(true,
                    Constantes.MSG_SALIO_GRUPO_ELIMINADO,
                    groupId, userId, true);
        }

        // Persistir cambios cuando no se elimina
        chatGrupalRepo.save(chat);

        MensajeDTO systemMessage = crearMensajeSistemaSalidaGrupo(chat, usuario);
        messagingTemplate.convertAndSend(Constantes.TOPIC_CHAT_GRUPAL + groupId, systemMessage);
        LOGGER.info(Constantes.LOG_E2E_GROUP_MEMBER_LEFT_SYSTEM_MESSAGE_BROADCAST,
                groupId,
                userId,
                systemMessage.getId(),
                systemMessage.getTipo(),
                E2EDiagnosticUtils.fingerprint12(systemMessage.getContenido()),
                systemMessage.getContenido() == null ? 0 : systemMessage.getContenido().length());

        return new MessagueSalirGrupoDTO(true, Constantes.MSG_SALIO_GRUPO, groupId, userId, false);
    }

    @Override
    public EsMiembroDTO esMiembroDeChatGrupal(Long groupId, Long userId) {
        Optional<ChatGrupalEntity> opt = chatGrupalRepo.findById(groupId);
        if (opt.isEmpty())
            return new EsMiembroDTO(false, true);
        ChatGrupalEntity chat = opt.get();
        if (!chat.isActivo()) {
            return new EsMiembroDTO(false, true);
        }
        boolean esMiembro = chat.getUsuarios() != null &&
                chat.getUsuarios().stream().anyMatch(u -> u.getId().equals(userId));
        return new EsMiembroDTO(esMiembro, false);
    }

    @Override
    public List<ChatGrupalDTO> listarChatsGrupalesPorUsuario(Long usuarioId) {
        UsuarioEntity usuario = usuarioRepo.findById(usuarioId).orElseThrow();
        return chatGrupalRepo.findAll().stream()
                .filter(ChatGrupalEntity::isActivo)
                .filter(chat -> chat.getUsuarios().contains(usuario))
                .map(MappingUtils::chatGrupalEntityADto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public AddUsuariosGrupoWSResponse anadirUsuariosAGrupo(AddUsuariosGrupoDTO dto) {
        // 1) Validar grupo e invitador
        ChatGrupalEntity chat = chatGrupalRepo.findById(dto.getGroupId())
                .orElseThrow(() -> new IllegalArgumentException(Constantes.MSG_CHAT_GRUPAL_NO_EXISTE + dto.getGroupId()));

        UsuarioEntity inviter = usuarioRepo.findById(dto.getInviterId())
                .orElseThrow(() -> new IllegalArgumentException(Constantes.MSG_INVITADOR_NO_EXISTE + dto.getInviterId()));

        // Comprobar que el invitador pertenece al grupo
        boolean inviterEsMiembro = chat.getUsuarios() != null
                && chat.getUsuarios().stream().anyMatch(u -> Objects.equals(u.getId(), inviter.getId()));
        if (!inviterEsMiembro) {
            throw new IllegalStateException(ExceptionConstants.ERROR_NOT_GROUP_MEMBER);
        }

        // 2) Normalizar IDs a invitar (sin duplicados, sin incluir al invitador)
        Set<Long> solicitados = (dto.getUsuarios() == null) ? Set.of()
                : dto.getUsuarios().stream()
                        .map(UsuarioDTO::getId)
                        .filter(Objects::nonNull)
                        .filter(id -> !Objects.equals(id, inviter.getId()))
                        .collect(Collectors.toCollection(LinkedHashSet::new));

        // 3) Filtrar ya miembros
        Set<Long> miembrosIds = chat.getUsuarios() == null ? Set.of()
                : chat.getUsuarios().stream().map(UsuarioEntity::getId).collect(Collectors.toSet());

        Set<Long> noMiembros = solicitados.stream()
                .filter(id -> !miembrosIds.contains(id))
                .filter(id -> {
                    UsuarioEntity potentialInvitee = usuarioRepo.findById(id).orElse(null);
                    if (potentialInvitee == null)
                        return false;
                    // Prevenir agregar si invitador bloqueÃ³ al invitado, o si el invitado bloqueÃ³
                    // al invitador
                    return !inviter.getBloqueados().contains(potentialInvitee) &&
                            !potentialInvitee.getBloqueados().contains(inviter);
                })
                .collect(Collectors.toCollection(LinkedHashSet::new));

        // 4) Evitar duplicar invitaciones PENDING
        List<GroupInviteEntity> pendientesExistentes = inviteRepo
                .findAllByChatIdAndStatus(chat.getId(), InviteStatus.PENDING);

        Set<Long> yaPendientes = pendientesExistentes.stream()
                .map(inv -> inv.getInvitee().getId())
                .collect(Collectors.toSet());

        List<GroupInviteEntity> nuevasInvitaciones = new ArrayList<>();
        int contadorYaPendientes = 0;

        for (Long inviteeId : noMiembros) {
            if (yaPendientes.contains(inviteeId)) {
                contadorYaPendientes++;
                continue;
            }
            UsuarioEntity invitee = usuarioRepo.findById(inviteeId)
                    .orElseThrow(() -> new IllegalArgumentException(Constantes.MSG_USUARIO_INVITADO_NO_EXISTE + inviteeId));

            GroupInviteEntity inv = new GroupInviteEntity();
            inv.setChat(chat);
            inv.setInviter(inviter);
            inv.setInvitee(invitee);
            inv.setStatus(InviteStatus.PENDING);
            inv.setCreatedAt(LocalDateTime.now());
            nuevasInvitaciones.add(inv);
        }

        if (!nuevasInvitaciones.isEmpty()) {
            inviteRepo.saveAll(nuevasInvitaciones);
        }

        // 5) Notificar y emitir WS + construir lista WS para la respuesta
        List<GroupInviteWS> invitacionesWs = new ArrayList<>();
        for (GroupInviteEntity inv : nuevasInvitaciones) {
            GroupInviteWS wsPayload = new GroupInviteWS();
            wsPayload.setInviteId(inv.getId());
            wsPayload.setGroupId(chat.getId());
            wsPayload.setGroupName(chat.getNombreGrupo());
            wsPayload.setInviterId(inviter.getId());
            wsPayload.setInviterNombre(inviter.getNombre()); // aÃ±ade apellidos si quieres

            NotificationEntity notif = new NotificationEntity();
            notif.setUserId(inv.getInvitee().getId());
            notif.setType(NotificationType.GROUP_INVITE);
            notif.setPayloadJson(Utils.writeJson(wsPayload));
            notif.setSeen(false);
            notificationRepo.save(notif);

            long unseen = notificationRepo.countByUserIdAndSeenFalse(inv.getInvitee().getId());
            wsPayload.setUnseenCount((int) unseen);

            // Emitir WS
            messagingTemplate.convertAndSend(Constantes.WS_TOPIC_NOTIFICATIONS + inv.getInvitee().getId(), wsPayload);

            // AÃ±adir a respuesta
            invitacionesWs.add(wsPayload);
        }

        // 6) Resumen de salida (WS-friendly)
        AddUsuariosGrupoWSResponse resp = new AddUsuariosGrupoWSResponse();
        resp.setGroupId(chat.getId());
        resp.setTotalSolicitados(solicitados.size());
        resp.setYaMiembros(solicitados.size() - noMiembros.size());
        resp.setYaInvitadosPendientes(contadorYaPendientes);
        resp.setInvitadosCreados(nuevasInvitaciones.size());
        resp.setInvitaciones(invitacionesWs);

        return resp;
    }

    @Override
    public List<Object> listarTodosLosChatsDeUsuario(Long usuarioId) {
        UsuarioEntity usuario = usuarioRepo.findById(usuarioId).orElseThrow();

        Map<Long, Long> unreadMap = mensajeRepository.countUnreadByUser(usuarioId)
                .stream()
                .collect(Collectors.toMap(
                        r -> (Long) r[0], // chatId
                        r -> (Long) r[1] // count
                ));

        // === INDIVIDUALES ===
        List<ChatIndividualDTO> individuales = chatIndRepo.findAll().stream()
                .filter(chat -> chat.getUsuario1().getId().equals(usuarioId)
                        || chat.getUsuario2().getId().equals(usuarioId))
                .map(chat -> {
                    // Decide quiÃ©n es el receptor basado en el usuario actual
                    boolean soyUser1 = chat.getUsuario1().getId().equals(usuarioId);
                    UsuarioEntity peer = soyUser1 ? chat.getUsuario2() : chat.getUsuario1();

                    // Si por algÃºn motivo era un chat conmigo mismo, peer = yo mismo
                    ChatIndividualDTO dto = MappingUtils.chatIndividualEntityADto(chat,
                            soyUser1 ? chat.getUsuario1() : chat.getUsuario2());
                    dto.setReceptor(MappingUtils.usuarioEntityADto(peer));

                    // ðŸ‘‡ convertir la foto del receptor a Base64 (dataURL) para el front
                    if (dto.getReceptor() != null && dto.getReceptor().getFoto() != null) {
                        dto.getReceptor().setFoto(Utils.toDataUrlFromUrl(dto.getReceptor().getFoto(), uploadsRoot));

                    }

                    dto.setUnreadCount(unreadMap.getOrDefault(dto.getId(), 0L));

                    // Ãšltimo mensaje por chatId
                    MensajeEntity last = mensajeRepository.findTopByChatIdOrderByFechaEnvioDesc(dto.getId());

                    if (last == null) {
                        dto.setUltimaMensaje(ChatConstants.MSG_SIN_MENSAJES);
                        dto.setUltimaFecha(null);
                        applyLastMessageMeta(dto, null);
                    } else if (!last.isActivo()) {
                        dto.setUltimaMensaje(ChatConstants.MSG_MENSAJE_ELIMINADO);
                        applyLastMessageMeta(dto, last);
                    } else {
                        dto.setUltimaMensaje(buildIndividualPreview(last, usuarioId));
                        applyLastMessageMeta(dto, last);
                    }
                    return dto;
                })
                .collect(Collectors.toList());

        // === GRUPALES ===
        List<ChatGrupalDTO> grupales = chatGrupalRepo.findAll().stream()
                .filter(ChatGrupalEntity::isActivo)
                .filter(chat -> chat.getUsuarios().contains(usuario))
                .map(chat -> {
                    ChatGrupalDTO dto = MappingUtils.chatGrupalEntityADto(chat);

                    // Foto del GRUPO (soporta fotoGrupo o foto, segÃºn tu DTO)
                    String fg = dto.getFotoGrupo();
                    if (fg != null && !fg.startsWith("data:") && fg.startsWith(Constantes.UPLOADS_PREFIX)) {
                        String data = Utils.toDataUrlFromUrl(fg, uploadsRoot);
                        dto.setFotoGrupo(data != null ? data : fg);
                    }
                    String f2 = dto.getFotoGrupo();
                    if (f2 != null && !f2.startsWith("data:") && f2.startsWith(Constantes.UPLOADS_PREFIX)) {
                        String data = Utils.toDataUrlFromUrl(f2, uploadsRoot);
                        dto.setFotoGrupo(data != null ? data : f2);
                    }

                    // Fotos de miembros (opcional)
                    if (dto.getUsuarios() != null) {
                        dto.getUsuarios().forEach(u -> {
                            String fu = u.getFoto();
                            if (fu != null && !fu.startsWith("data:") && fu.startsWith(Constantes.UPLOADS_PREFIX)) {
                                String data = Utils.toDataUrlFromUrl(fu, uploadsRoot);
                                u.setFoto(data != null ? data : fu);
                            }
                        });
                    }

                    // (Opcional) convertir fotos de miembros a Base64
                    if (dto.getUsuarios() != null) {
                        dto.getUsuarios().forEach(u -> {
                            if (u.getFoto() != null) {
                                u.setFoto(Utils.toDataUrlFromUrl(u.getFoto(), uploadsRoot));
                            }
                        });
                    }

                    // Ãšltimo mensaje por chatId
                    MensajeEntity last = mensajeRepository.findTopByChatIdOrderByFechaEnvioDesc(dto.getId());

                    if (last == null) {
                        dto.setUltimaMensaje(ChatConstants.MSG_SIN_MENSAJES);
                        dto.setUltimaFecha(null);
                        applyLastMessageMeta(dto, null);
                    } else if (!last.isActivo()) {
                        dto.setUltimaMensaje(ChatConstants.MSG_MENSAJE_ELIMINADO);
                        applyLastMessageMeta(dto, last);
                    } else {
                        dto.setUltimaMensaje(buildGroupPreview(last, usuarioId));
                        applyLastMessageMeta(dto, last);
                    }
                    return dto;
                })
                .collect(Collectors.toList());

        // === COMBINAR Y ORDENAR POR FECHA DESC ===
        List<Object> resultado = new ArrayList<>();
        resultado.addAll(individuales);
        resultado.addAll(grupales);

        resultado.sort((a, b) -> {
            LocalDateTime fa = (a instanceof ChatIndividualDTO)
                    ? ((ChatIndividualDTO) a).getUltimaFecha()
                    : ((ChatGrupalDTO) a).getUltimaFecha();
            LocalDateTime fb = (b instanceof ChatIndividualDTO)
                    ? ((ChatIndividualDTO) b).getUltimaFecha()
                    : ((ChatGrupalDTO) b).getUltimaFecha();

            if (fa == null && fb == null)
                return 0;
            if (fa == null)
                return 1;
            if (fb == null)
                return -1;
            return fb.compareTo(fa); // DESC
        });

        return resultado;
    }

    private String buildIndividualPreview(MensajeEntity last, Long viewerId) {
        if (last == null) {
            return ChatConstants.MSG_SIN_MENSAJES;
        }
        boolean soyYo = last.getEmisor() != null && Objects.equals(last.getEmisor().getId(), viewerId);
        String prefix = soyYo ? "Tu: " : "";
        MessageType tipo = last.getTipo() == null ? MessageType.TEXT : last.getTipo();
        if (tipo == MessageType.IMAGE) {
            return prefix + "Imagen";
        }
        if (tipo == MessageType.AUDIO) {
            MediaMeta meta = extractMediaMeta(last);
            String dur = Utils.mmss(meta.durMs());
            return prefix + "Audio" + (dur.isEmpty() ? "" : " (" + dur + ")");
        }
        if (isEncryptedPayload(last.getContenido())) {
            return prefix + "Mensaje cifrado";
        }
        return prefix + Utils.truncarSafe(last.getContenido(), 60);
    }

    private String buildGroupPreview(MensajeEntity last, Long viewerId) {
        if (last == null) {
            return ChatConstants.MSG_SIN_MENSAJES;
        }
        Long senderId = last.getEmisor() == null ? null : last.getEmisor().getId();
        String senderName = last.getEmisor() == null ? null : last.getEmisor().getNombre();
        String prefix;
        if (Objects.equals(senderId, viewerId)) {
            prefix = "yo: ";
        } else if (senderName != null && !senderName.isBlank()) {
            prefix = senderName + ": ";
        } else {
            prefix = "usuario: ";
        }

        MessageType tipo = last.getTipo() == null ? MessageType.TEXT : last.getTipo();
        if (tipo == MessageType.IMAGE) {
            return prefix + "Imagen";
        }
        if (tipo == MessageType.AUDIO) {
            MediaMeta meta = extractMediaMeta(last);
            String dur = Utils.mmss(meta.durMs());
            return prefix + "Audio" + (dur.isEmpty() ? "" : " (" + dur + ")");
        }
        if (isEncryptedPayload(last.getContenido())) {
            return prefix + "Mensaje cifrado";
        }
        return prefix + Utils.truncarSafe(last.getContenido(), 60);
    }

    private boolean isEncryptedPayload(String content) {
        if (content == null || content.isBlank()) {
            return false;
        }
        String classification = E2EDiagnosticUtils.analyze(content).getClassification();
        return classification != null && classification.startsWith("JSON_E2E");
    }

    private void applyLastMessageMeta(ChatIndividualDTO dto, MensajeEntity last) {
        if (dto == null || last == null) {
            if (dto != null) {
                dto.setUltimaMensajeId(null);
                dto.setUltimaMensajeTipo(null);
                dto.setUltimaMensajeEmisorId(null);
                dto.setUltimaMensajeRaw(null);
                dto.setUltimaMensajeImageUrl(null);
                dto.setUltimaMensajeImageMime(null);
                dto.setUltimaMensajeImageNombre(null);
                dto.setUltimaMensajeAudioUrl(null);
                dto.setUltimaMensajeAudioMime(null);
                dto.setUltimaMensajeAudioDuracionMs(null);
            }
            return;
        }
        MessageType tipo = last.getTipo() == null ? MessageType.TEXT : last.getTipo();
        dto.setUltimaMensajeId(last.getId());
        dto.setUltimaMensajeTipo(tipo.name());
        dto.setUltimaMensajeEmisorId(last.getEmisor() == null ? null : last.getEmisor().getId());
        dto.setUltimaMensajeRaw(last.getContenido());
        dto.setUltimaFecha(last.getFechaEnvio());

        MediaMeta meta = extractMediaMeta(last);
        if (tipo == MessageType.IMAGE) {
            dto.setUltimaMensajeImageUrl(meta.mediaUrl());
            dto.setUltimaMensajeImageMime(meta.mime());
            dto.setUltimaMensajeImageNombre(meta.fileName());
            dto.setUltimaMensajeAudioUrl(null);
            dto.setUltimaMensajeAudioMime(null);
            dto.setUltimaMensajeAudioDuracionMs(null);
            return;
        }
        if (tipo == MessageType.AUDIO) {
            dto.setUltimaMensajeAudioUrl(meta.mediaUrl());
            dto.setUltimaMensajeAudioMime(meta.mime());
            dto.setUltimaMensajeAudioDuracionMs(meta.durMs());
            dto.setUltimaMensajeImageUrl(null);
            dto.setUltimaMensajeImageMime(null);
            dto.setUltimaMensajeImageNombre(null);
            return;
        }
        dto.setUltimaMensajeImageUrl(null);
        dto.setUltimaMensajeImageMime(null);
        dto.setUltimaMensajeImageNombre(null);
        dto.setUltimaMensajeAudioUrl(null);
        dto.setUltimaMensajeAudioMime(null);
        dto.setUltimaMensajeAudioDuracionMs(null);
    }

    private void applyLastMessageMeta(ChatGrupalDTO dto, MensajeEntity last) {
        if (dto == null || last == null) {
            if (dto != null) {
                dto.setUltimaMensajeId(null);
                dto.setUltimaMensajeTipo(null);
                dto.setUltimaMensajeEmisorId(null);
                dto.setUltimaMensajeRaw(null);
                dto.setUltimaMensajeImageUrl(null);
                dto.setUltimaMensajeImageMime(null);
                dto.setUltimaMensajeImageNombre(null);
                dto.setUltimaMensajeAudioUrl(null);
                dto.setUltimaMensajeAudioMime(null);
                dto.setUltimaMensajeAudioDuracionMs(null);
            }
            return;
        }
        MessageType tipo = last.getTipo() == null ? MessageType.TEXT : last.getTipo();
        dto.setUltimaMensajeId(last.getId());
        dto.setUltimaMensajeTipo(tipo.name());
        dto.setUltimaMensajeEmisorId(last.getEmisor() == null ? null : last.getEmisor().getId());
        dto.setUltimaMensajeRaw(last.getContenido());
        dto.setUltimaFecha(last.getFechaEnvio());

        MediaMeta meta = extractMediaMeta(last);
        if (tipo == MessageType.IMAGE) {
            dto.setUltimaMensajeImageUrl(meta.mediaUrl());
            dto.setUltimaMensajeImageMime(meta.mime());
            dto.setUltimaMensajeImageNombre(meta.fileName());
            dto.setUltimaMensajeAudioUrl(null);
            dto.setUltimaMensajeAudioMime(null);
            dto.setUltimaMensajeAudioDuracionMs(null);
            return;
        }
        if (tipo == MessageType.AUDIO) {
            dto.setUltimaMensajeAudioUrl(meta.mediaUrl());
            dto.setUltimaMensajeAudioMime(meta.mime());
            dto.setUltimaMensajeAudioDuracionMs(meta.durMs());
            dto.setUltimaMensajeImageUrl(null);
            dto.setUltimaMensajeImageMime(null);
            dto.setUltimaMensajeImageNombre(null);
            return;
        }
        dto.setUltimaMensajeImageUrl(null);
        dto.setUltimaMensajeImageMime(null);
        dto.setUltimaMensajeImageNombre(null);
        dto.setUltimaMensajeAudioUrl(null);
        dto.setUltimaMensajeAudioMime(null);
        dto.setUltimaMensajeAudioDuracionMs(null);
    }

    private Map<Long, List<MensajeReaccionEntity>> loadReaccionesPorMensaje(List<MensajeEntity> mensajes) {
        if (mensajes == null || mensajes.isEmpty()) {
            return Map.of();
        }
        List<Long> messageIds = mensajes.stream()
                .map(MensajeEntity::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        if (messageIds.isEmpty()) {
            return Map.of();
        }
        List<MensajeReaccionEntity> rows = mensajeReaccionRepository
                .findByMensajeIdInOrderByMensajeIdAscUpdatedAtDescIdDesc(messageIds);
        return rows.stream()
                .filter(Objects::nonNull)
                .filter(r -> r.getMensaje() != null && r.getMensaje().getId() != null)
                .collect(Collectors.groupingBy(
                        r -> r.getMensaje().getId(),
                        LinkedHashMap::new,
                        Collectors.toList()));
    }

    private void applyReacciones(MensajeDTO dto, List<MensajeReaccionEntity> reaccionesMensaje) {
        if (dto == null) {
            return;
        }
        if (reaccionesMensaje == null || reaccionesMensaje.isEmpty()) {
            dto.setReaccionEmoji(null);
            dto.setReaccionUsuarioId(null);
            dto.setReaccionFecha(null);
            dto.setReacciones(List.of());
            return;
        }
        MensajeReaccionEntity latest = reaccionesMensaje.get(0);
        dto.setReaccionEmoji(latest.getEmoji());
        dto.setReaccionUsuarioId(latest.getUsuario() == null ? null : latest.getUsuario().getId());
        dto.setReaccionFecha(latest.getUpdatedAt() != null ? latest.getUpdatedAt() : latest.getCreatedAt());
        List<MensajeReaccionResumenDTO> resumenes = reaccionesMensaje.stream()
                .map(reaccion -> {
                    MensajeReaccionResumenDTO resumen = new MensajeReaccionResumenDTO();
                    resumen.setUserId(reaccion.getUsuario() == null ? null : reaccion.getUsuario().getId());
                    resumen.setEmoji(reaccion.getEmoji());
                    resumen.setCreatedAt(reaccion.getUpdatedAt() != null ? reaccion.getUpdatedAt() : reaccion.getCreatedAt());
                    return resumen;
                })
                .collect(Collectors.toList());
        dto.setReacciones(resumenes);
    }

    @Override
    public List<MensajeDTO> listarMensajesPorChatId(Long chatId, Integer page, Integer size) {
        ChatEntity chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new RuntimeException(Constantes.MSG_CHAT_NO_ENCONTRADO_ID + chatId));
        String traceId = Optional.ofNullable(E2EDiagnosticUtils.currentTraceId()).orElse(E2EDiagnosticUtils.newTraceId());
        boolean groupChat = chat instanceof ChatGrupalEntity;

        List<MensajeEntity> mensajes = fetchMessagesPageChronological(chatId, page, size);
        Map<Long, List<MensajeReaccionEntity>> reaccionesPorMensaje = loadReaccionesPorMensaje(mensajes);

        return mensajes.stream()
                .map(e -> {
                    try {
                        MensajeDTO dto = MappingUtils.mensajeEntityADto(e);
                        applyReacciones(dto, reaccionesPorMensaje.get(e.getId()));
                        if (groupChat) {
                            E2EDiagnosticUtils.ContentDiagnostic historyDiag = E2EDiagnosticUtils.analyze(
                                    dto.getContenido(),
                                    dto.getTipo());
                            boolean senderKeyPresentAtRead = e.getEmisor() != null
                                    && e.getEmisor().getPublicKey() != null
                                    && !e.getEmisor().getPublicKey().isBlank();
                            LOGGER.info(
                                    Constantes.LOG_E2E_HISTORY_OUT_CHAT_ID,
                                    Instant.now(),
                                    traceId,
                                    chatId,
                                    dto.getId(),
                                    dto.getEmisorId(),
                                    dto.getTipo(),
                                    historyDiag.getClassification(),
                                    historyDiag.getLength(),
                                    historyDiag.getHash12(),
                                    historyDiag.hasIv(),
                                    historyDiag.hasCiphertext(),
                                    historyDiag.hasForEmisor(),
                                    historyDiag.hasForReceptores(),
                                    historyDiag.hasForAdmin(),
                                    historyDiag.getForReceptoresKeys(),
                                    senderKeyPresentAtRead);
                            if ("INVALID_JSON".equals(historyDiag.getClassification())) {
                                LOGGER.warn(Constantes.LOG_E2E_HISTORY_OUT_PARSE_WARN,
                                        Instant.now(), traceId, chatId, dto.getId(), historyDiag.getParseErrorClass());
                            }
                        }
                        return dto;
                    } catch (RuntimeException ex) {
                        if (groupChat) {
                            LOGGER.error(Constantes.LOG_E2E_HISTORY_OUT_ERROR,
                                    Instant.now(), traceId, chatId, e.getId(), ex.getClass().getSimpleName());
                        }
                        throw ex;
                    }
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<MensajeDTO> listarMensajesPorChatGrupal(Long chatId, Integer page, Integer size) {
        ChatGrupalEntity chat = chatGrupalRepo.findById(chatId)
                .orElseThrow(() -> new RuntimeException(Constantes.MSG_CHAT_GRUPAL_NO_ENCONTRADO_ID + chatId));
        String traceId = Optional.ofNullable(E2EDiagnosticUtils.currentTraceId()).orElse(E2EDiagnosticUtils.newTraceId());

        // Si tienes un repo con este mÃ©todo, Ãºsalo; si no, ordenamos en memoria:
        // List<MensajeEntity> mensajes =
        // mensajeRepository.findByChatIdOrderByFechaEnvioAsc(chatId);
        List<MensajeEntity> mensajes = fetchMessagesPageChronological(chatId, page, size);
        Map<Long, List<MensajeReaccionEntity>> reaccionesPorMensaje = loadReaccionesPorMensaje(mensajes);
        Map<Long, String> currentMemberKeyFp = new LinkedHashMap<>();
        if (chat.getUsuarios() != null) {
            chat.getUsuarios().forEach(u -> {
                if (u != null && u.getId() != null) {
                    currentMemberKeyFp.put(u.getId(), E2EDiagnosticUtils.fingerprint12(u.getPublicKey()));
                }
            });
        }

        return mensajes.stream()
                .map(e -> {
                    try {
                        MensajeDTO dto = MappingUtils.mensajeEntityADto(e);
                        applyReacciones(dto, reaccionesPorMensaje.get(e.getId()));
                        UsuarioEntity emisor = e.getEmisor();
                        if (emisor != null) {
                            dto.setEmisorNombre(emisor.getNombre());
                            dto.setEmisorApellido(emisor.getApellido());
                            if (emisor.getFotoUrl() != null) {
                                dto.setEmisorFoto(Utils.toDataUrlFromUrl(emisor.getFotoUrl(), uploadsRoot));
                            }
                        }
                        E2EDiagnosticUtils.ContentDiagnostic historyDiag = E2EDiagnosticUtils.analyze(
                                dto.getContenido(),
                                dto.getTipo());
                        boolean senderKeyPresentAtRead = e.getEmisor() != null
                                && e.getEmisor().getPublicKey() != null
                                && !e.getEmisor().getPublicKey().isBlank();
                        List<Long> recipientIdsUsed = currentMemberKeyFp.keySet().stream()
                                .filter(id -> !Objects.equals(id, dto.getEmisorId()))
                                .collect(Collectors.toList());
                        Set<String> payloadForReceptoresKeys = E2EDiagnosticUtils.extractForReceptoresKeys(dto.getContenido());
                        String senderCurrentKeyFp = currentMemberKeyFp.getOrDefault(dto.getEmisorId(), "-");
                        LOGGER.info(
                                Constantes.LOG_E2E_HISTORY_OUT_CHAT_GRUPAL,
                                Instant.now(),
                                traceId,
                                chatId,
                                dto.getId(),
                                dto.getEmisorId(),
                                dto.getTipo(),
                                historyDiag.getClassification(),
                                historyDiag.getLength(),
                                historyDiag.getHash12(),
                                historyDiag.hasIv(),
                                historyDiag.hasCiphertext(),
                                historyDiag.hasForEmisor(),
                                historyDiag.hasForReceptores(),
                                historyDiag.hasForAdmin(),
                                historyDiag.getForReceptoresKeys(),
                                recipientIdsUsed,
                                payloadForReceptoresKeys,
                                senderCurrentKeyFp,
                                currentMemberKeyFp,
                                senderKeyPresentAtRead);
                        if ("INVALID_JSON".equals(historyDiag.getClassification())) {
                            LOGGER.warn(Constantes.LOG_E2E_HISTORY_OUT_PARSE_WARN,
                                    Instant.now(), traceId, chatId, dto.getId(), historyDiag.getParseErrorClass());
                        }
                        return dto;
                    } catch (RuntimeException ex) {
                        LOGGER.error(Constantes.LOG_E2E_HISTORY_OUT_ERROR,
                                Instant.now(), traceId, chatId, e.getId(), ex.getClass().getSimpleName());
                        throw ex;
                    }
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ChatMensajeBusquedaPageDTO buscarMensajesEnChat(Long chatId, String q, Integer page, Integer size) {
        Long requesterId = securityUtils.getAuthenticatedUserId();
        ChatEntity chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new RuntimeException(Constantes.MSG_CHAT_NO_ENCONTRADO_ID + chatId));
        validateSearchAccess(chat, requesterId);

        int safePage = page == null ? DEFAULT_SEARCH_PAGE : Math.max(DEFAULT_SEARCH_PAGE, page);
        int requestedSize = size == null ? DEFAULT_SEARCH_SIZE : size;
        int safeSize = Math.max(1, Math.min(MAX_SEARCH_SIZE, requestedSize));

        ChatMensajeBusquedaPageDTO response = new ChatMensajeBusquedaPageDTO();
        response.setPage(safePage);
        response.setSize(safeSize);

        String normalizedQuery = normalizeForSearch(q == null ? "" : q.trim());
        if (normalizedQuery.isBlank()) {
            response.setItems(List.of());
            response.setTotal(0);
            response.setHasMore(false);
            return response;
        }

        List<MensajeEntity> candidates = mensajeRepository.findTextActivosByChatIdOrderByFechaEnvioDescIdDesc(chatId, MessageType.TEXT);
        List<SearchMatchCandidate> matches = new ArrayList<>();
        for (MensajeEntity mensaje : candidates) {
            String searchableContent = resolveSearchableContent(mensaje.getContenido());
            if (searchableContent == null || searchableContent.isBlank()) {
                continue;
            }

            MatchWindow window = findFirstMatchWindow(searchableContent, normalizedQuery);
            if (window == null) {
                continue;
            }

            ChatMensajeBusquedaItemDTO item = new ChatMensajeBusquedaItemDTO();
            item.setId(mensaje.getId());
            item.setChatId(mensaje.getChat() == null ? null : mensaje.getChat().getId());
            item.setEmisorId(mensaje.getEmisor() == null ? null : mensaje.getEmisor().getId());
            item.setEmisorNombre(mensaje.getEmisor() == null ? null : mensaje.getEmisor().getNombre());
            item.setEmisorApellido(mensaje.getEmisor() == null ? null : mensaje.getEmisor().getApellido());
            item.setFechaEnvio(mensaje.getFechaEnvio());
            item.setSnippet(window.snippet());
            item.setContenido(window.snippet());
            item.setMatchStart(window.matchStart());
            item.setMatchEnd(window.matchEnd());

            matches.add(new SearchMatchCandidate(
                    window.startsWithQuery(),
                    mensaje.getFechaEnvio(),
                    mensaje.getId(),
                    item));
        }

        matches.sort(
                Comparator.comparing((SearchMatchCandidate c) -> c.startsWithQuery() ? 0 : 1)
                        .thenComparing(SearchMatchCandidate::fechaEnvio, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(SearchMatchCandidate::id, Comparator.nullsLast(Comparator.reverseOrder()))
        );

        long total = matches.size();
        long offset = (long) safePage * safeSize;
        int fromIndex = (int) Math.min(offset, total);
        int toIndex = (int) Math.min(offset + safeSize, total);

        List<ChatMensajeBusquedaItemDTO> items = matches.subList(fromIndex, toIndex)
                .stream()
                .map(SearchMatchCandidate::item)
                .collect(Collectors.toList());

        response.setItems(items);
        response.setTotal(total);
        response.setHasMore(toIndex < total);
        return response;
    }

    @Override
    public GroupMediaPageDTO listarMediaPorChatGrupal(Long chatId, String cursor, Integer size, String types) {
        Long requesterId = securityUtils.getAuthenticatedUserId();
        ChatGrupalEntity chat = chatGrupalRepo.findByIdWithUsuarios(chatId)
                .orElseThrow(() -> new RuntimeException(Constantes.MSG_CHAT_GRUPAL_NO_ENCONTRADO_ID + chatId));
        if (!chat.isActivo()) {
            throw new RuntimeException(Constantes.MSG_CHAT_GRUPAL_NO_ENCONTRADO_ID + chatId);
        }

        boolean requesterIsActiveMember = chat.getUsuarios() != null
                && chat.getUsuarios().stream()
                .anyMatch(u -> u != null && Objects.equals(u.getId(), requesterId) && u.isActivo());
        if (!requesterIsActiveMember) {
            throw new AccessDeniedException(Constantes.MSG_NO_PERTENECE_GRUPO);
        }

        int safeSize = size == null ? DEFAULT_MEDIA_SIZE : Math.max(1, Math.min(MAX_MEDIA_SIZE, size));
        List<MessageType> mediaTypes = parseMediaTypes(types);
        CursorPosition cursorPosition = parseCursor(cursor, chatId);

        Pageable pageable = PageRequest.of(0, safeSize + 1);
        List<MensajeEntity> fetched = mensajeRepository.findGroupMediaPage(
                chatId,
                mediaTypes,
                cursorPosition.fechaEnvio(),
                cursorPosition.messageId(),
                pageable);

        boolean hasMore = fetched.size() > safeSize;
        List<MensajeEntity> visible = hasMore ? fetched.subList(0, safeSize) : fetched;
        List<GroupMediaItemDTO> items = visible.stream()
                .map(this::toGroupMediaItem)
                .collect(Collectors.toList());

        String nextCursor = null;
        if (hasMore && !visible.isEmpty()) {
            MensajeEntity last = visible.get(visible.size() - 1);
            nextCursor = encodeCursor(last.getFechaEnvio(), last.getId());
        }

        String traceId = Optional.ofNullable(E2EDiagnosticUtils.currentTraceId())
                .orElse(E2EDiagnosticUtils.newTraceId());
        LOGGER.info(
                Constantes.LOG_E2E_GROUP_MEDIA_LIST,
                traceId,
                chatId,
                requesterId,
                cursor,
                safeSize,
                mediaTypes.stream().map(Enum::name).collect(Collectors.joining(",")),
                items.size(),
                hasMore);
        for (GroupMediaItemDTO item : items) {
            E2EDiagnosticUtils.ContentDiagnostic diag = E2EDiagnosticUtils.analyze(item.getContenidoRaw(), item.getTipo());
            LOGGER.info(
                    Constantes.LOG_E2E_GROUP_MEDIA_ITEM,
                    traceId,
                    chatId,
                    item.getMessageId(),
                    item.getTipo(),
                    diag.getClassification(),
                    diag.getLength(),
                    diag.getHash12());
        }

        GroupMediaPageDTO out = new GroupMediaPageDTO();
        out.setItems(items);
        out.setHasMore(hasMore);
        out.setNextCursor(nextCursor);
        return out;
    }

    @Override
    public List<MensajeDTO> listarMensajesPorChatIdAdmin(Long chatId) {
        Long requesterId = securityUtils.getAuthenticatedUserId();
        boolean requesterIsAdmin = securityUtils.hasRole("ADMIN") || usuarioRepo.findById(requesterId)
                .map(this::esAdmin)
                .orElse(false);
        if (!requesterIsAdmin) {
            throw new AccessDeniedException(Constantes.MSG_SOLO_ADMIN);
        }

        ChatEntity chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new RuntimeException(Constantes.MSG_CHAT_NO_ENCONTRADO_ID + chatId));

        List<MensajeEntity> mensajes = mensajeRepository.findByChatIdOrderByFechaEnvioAsc(chat.getId());
        Map<Long, List<MensajeReaccionEntity>> reaccionesPorMensaje = loadReaccionesPorMensaje(mensajes);
        return mensajes.stream()
                .map(e -> {
                    MensajeDTO dto = MappingUtils.mensajeEntityADto(e);
                    applyReacciones(dto, reaccionesPorMensaje.get(e.getId()));
                    LOGGER.info(Constantes.LOG_E2E_ADMIN_CHAT_MESSAGES_RAW,
                            chatId,
                            e.getId(),
                            E2EDiagnosticUtils.analyze(
                                    e.getContenido(),
                                    e.getTipo() == null ? null : e.getTipo().name()).getClassification(),
                            false,
                            false);
                    return dto;
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<ChatResumenDTO> listarConversacionesDeUsuario(Long usuarioId) {
        Long requesterId = securityUtils.getAuthenticatedUserId();
        boolean requesterIsAdmin = securityUtils.hasRole("ADMIN") || usuarioRepo.findById(requesterId)
                .map(this::esAdmin)
                .orElse(false);
        if (!requesterIsAdmin) {
            throw new AccessDeniedException(Constantes.MSG_SOLO_ADMIN);
        }

        List<ChatIndividualEntity> individuales = chatIndRepo.findAllByUsuario1IdOrUsuario2Id(usuarioId, usuarioId);
        List<ChatGrupalEntity> grupales = chatGrupalRepo.findAllByUsuariosId(usuarioId);
        List<Long> chatIds = new ArrayList<>();
        individuales.forEach(ci -> chatIds.add(ci.getId()));
        grupales.forEach(cg -> chatIds.add(cg.getId()));

        Map<Long, Integer> totalMensajesPorChat = new HashMap<>();
        Map<Long, MensajeEntity> ultimoMensajePorChat = new HashMap<>();
        if (!chatIds.isEmpty()) {
            mensajeRepository.countActivosByChatIds(chatIds)
                    .forEach(row -> totalMensajesPorChat.put((Long) row[0], ((Long) row[1]).intValue()));
            mensajeRepository.findLatestByChatIds(chatIds)
                    .forEach(m -> ultimoMensajePorChat.put(m.getChat().getId(), m));
        }

        List<ChatResumenDTO> resumenes = new ArrayList<>();
        for (ChatIndividualEntity ci : individuales) {
            ChatResumenDTO dto = new ChatResumenDTO();
            dto.setId(ci.getId());
            dto.setTipo(Constantes.CHAT_TIPO_INDIVIDUAL);
            dto.setNombreChat(ci.getUsuario1().getNombre() + Constantes.MSG_Y + ci.getUsuario2().getNombre());
            dto.setTotalMensajes(totalMensajesPorChat.getOrDefault(ci.getId(), 0));
            applyAdminRawMetadata(dto, ultimoMensajePorChat.get(ci.getId()));
            resumenes.add(dto);
        }

        for (ChatGrupalEntity cg : grupales) {
            ChatResumenDTO dto = new ChatResumenDTO();
            dto.setId(cg.getId());
            dto.setTipo(Constantes.CHAT_TIPO_GRUPAL);
            dto.setNombreChat(cg.getNombreGrupo() + Constantes.MSG_GRUPO_SUFFIX);
            dto.setTotalMensajes(totalMensajesPorChat.getOrDefault(cg.getId(), 0));
            applyAdminRawMetadata(dto, ultimoMensajePorChat.get(cg.getId()));
            resumenes.add(dto);
        }

        return resumenes;
    }

    @Override
    public GroupDetailDTO obtenerDetalleGrupo(Long groupId) {
        Long authenticatedUserId = securityUtils.getAuthenticatedUserId();
        ChatGrupalEntity chat = chatGrupalRepo.findById(groupId)
                .orElseThrow(() -> new RuntimeException(Constantes.MSG_CHAT_GRUPAL_NO_ENCONTRADO_ID + groupId));
        if (!chat.isActivo()) {
            throw new RuntimeException(Constantes.MSG_CHAT_GRUPAL_NO_ENCONTRADO_ID + groupId);
        }

        boolean esMiembro = chat.getUsuarios() != null
                && chat.getUsuarios().stream().anyMatch(u -> Objects.equals(u.getId(), authenticatedUserId));
        if (!esMiembro) {
            throw new AccessDeniedException(Constantes.MSG_NO_PERTENECE_GRUPO);
        }

        if ((chat.getAdmins() == null || chat.getAdmins().isEmpty()) && chat.getCreador() != null) {
            chat.getAdmins().add(chat.getCreador());
            chat = chatGrupalRepo.save(chat);
        }

        GroupDetailDTO dto = new GroupDetailDTO();
        dto.setId(chat.getId());
        dto.setNombreGrupo(chat.getNombreGrupo());
        dto.setFotoGrupo(chat.getFotoUrl());
        dto.setDescripcion(chat.getDescripcion());
        dto.setVisibilidad(chat.getVisibilidad());
        dto.setFechaCreacion(chat.getFechaCreacion());
        dto.setMediaCount(chat.getMediaCount());
        dto.setFilesCount(chat.getFilesCount());

        UsuarioEntity creador = chat.getCreador();
        if (creador != null) {
            dto.setIdCreador(creador.getId());
            String nombreCreador = ((creador.getNombre() == null ? "" : creador.getNombre())
                    + (creador.getApellido() == null || creador.getApellido().isBlank() ? "" : " " + creador.getApellido()))
                    .trim();
            dto.setNombreCreador(nombreCreador.isEmpty() ? creador.getNombre() : nombreCreador);
        } else {
            dto.setIdCreador(null);
            dto.setNombreCreador(null);
        }

        final ChatGrupalEntity chatFinal = chat;
        List<GroupMemberDTO> miembros = (chatFinal.getUsuarios() == null ? List.<UsuarioEntity>of() : chatFinal.getUsuarios())
                .stream()
                .map(u -> {
                    GroupMemberDTO m = new GroupMemberDTO();
                    m.setId(u.getId());
                    m.setNombre(u.getNombre());
                    m.setApellido(u.getApellido());
                    String foto = u.getFotoUrl();
                    if (foto != null && foto.startsWith(Constantes.UPLOADS_PREFIX)) {
                        String dataUrl = Utils.toDataUrlFromUrl(foto, uploadsRoot);
                        m.setFoto(dataUrl != null ? dataUrl : foto);
                    } else {
                        m.setFoto(foto);
                    }
                    boolean esAdmin = chatFinal.getAdmins() != null
                            && chatFinal.getAdmins().stream().anyMatch(a -> Objects.equals(a.getId(), u.getId()));
                    if (!esAdmin && chatFinal.getCreador() != null && Objects.equals(chatFinal.getCreador().getId(), u.getId())) {
                        esAdmin = true;
                    }
                    m.setRolGrupo(esAdmin ? GroupRole.ADMIN : GroupRole.MIEMBRO);
                    if (estadoUsuarioManager != null) {
                        boolean conectado = estadoUsuarioManager.estaConectado(u.getId());
                        m.setEstado(conectado ? Constantes.ESTADO_CONECTADO : Constantes.ESTADO_DESCONECTADO);
                    }
                    return m;
                })
                .collect(Collectors.toList());

        dto.setMiembros(miembros);
        List<Long> memberIdsDetail = miembros.stream()
                .map(GroupMemberDTO::getId)
                .filter(Objects::nonNull)
                .sorted()
                .collect(Collectors.toList());
        Map<Long, String> memberKeyFp = new LinkedHashMap<>();
        if (chatFinal.getUsuarios() != null) {
            chatFinal.getUsuarios().forEach(u -> {
                if (u != null && u.getId() != null) {
                    memberKeyFp.put(u.getId(), E2EDiagnosticUtils.fingerprint12(u.getPublicKey()));
                }
            });
        }
        LOGGER.info(Constantes.LOG_E2E_GROUP_DETAIL_MEMBERS,
                Instant.now(), groupId, authenticatedUserId, memberIdsDetail, memberIdsDetail.size(), memberKeyFp);
        return dto;
    }

    @Override
    @Transactional
    public void setAdminGrupo(Long groupId, Long targetUserId, boolean makeAdmin) {
        Long requesterId = securityUtils.getAuthenticatedUserId();
        ChatGrupalEntity chat = chatGrupalRepo.findById(groupId)
                .orElseThrow(() -> new RuntimeException(Constantes.MSG_CHAT_GRUPAL_NO_ENCONTRADO_ID + groupId));
        if (!chat.isActivo()) {
            throw new RuntimeException(Constantes.MSG_CHAT_GRUPAL_NO_ENCONTRADO_ID + groupId);
        }

        boolean requesterIsMember = chat.getUsuarios() != null
                && chat.getUsuarios().stream().anyMatch(u -> Objects.equals(u.getId(), requesterId));
        if (!requesterIsMember) {
            throw new AccessDeniedException(Constantes.MSG_NO_PERTENECE_GRUPO);
        }

        if ((chat.getAdmins() == null || chat.getAdmins().isEmpty()) && chat.getCreador() != null) {
            chat.getAdmins().add(chat.getCreador());
        }

        boolean requesterIsAdmin = chat.getAdmins() != null
                && chat.getAdmins().stream().anyMatch(u -> Objects.equals(u.getId(), requesterId));
        if (!requesterIsAdmin) {
            throw new AccessDeniedException(Constantes.MSG_SOLO_ADMIN);
        }

        UsuarioEntity target = usuarioRepo.findById(targetUserId).orElseThrow();
        boolean targetIsMember = chat.getUsuarios() != null
                && chat.getUsuarios().stream().anyMatch(u -> Objects.equals(u.getId(), targetUserId));
        if (!targetIsMember) {
            throw new IllegalArgumentException(Constantes.MSG_NO_PERTENECE_GRUPO);
        }

        if (makeAdmin) {
            chat.getAdmins().add(target);
        } else {
            chat.getAdmins().removeIf(u -> Objects.equals(u.getId(), targetUserId));
            if ((chat.getAdmins() == null || chat.getAdmins().isEmpty()) && chat.getCreador() != null) {
                chat.getAdmins().add(chat.getCreador());
            }
        }

        chatGrupalRepo.save(chat);
    }

    private List<MensajeEntity> fetchMessagesPageChronological(Long chatId, Integer page, Integer size) {
        Pageable pageable = buildMessagesPageable(page, size);
        List<MensajeEntity> content = new ArrayList<>(mensajeRepository.findByChatId(chatId, pageable).getContent());
        content.sort(Comparator.comparing(MensajeEntity::getFechaEnvio).thenComparing(MensajeEntity::getId));
        return content;
    }

    private Pageable buildMessagesPageable(Integer page, Integer size) {
        int safePage = page == null ? DEFAULT_MESSAGES_PAGE : Math.max(DEFAULT_MESSAGES_PAGE, page);
        int requestedSize = size == null ? DEFAULT_MESSAGES_SIZE : size;
        int safeSize = Math.max(1, Math.min(MAX_MESSAGES_SIZE, requestedSize));
        Sort sort = Sort.by(Sort.Order.desc("fechaEnvio"), Sort.Order.desc("id"));
        return PageRequest.of(safePage, safeSize, sort);
    }

    private void validateSearchAccess(ChatEntity chat, Long requesterId) {
        if (chat instanceof ChatIndividualEntity individual) {
            boolean isMember = (individual.getUsuario1() != null && Objects.equals(individual.getUsuario1().getId(), requesterId))
                    || (individual.getUsuario2() != null && Objects.equals(individual.getUsuario2().getId(), requesterId));
            if (!isMember) {
                throw new AccessDeniedException(Constantes.MSG_NO_PERTENECE_CHAT);
            }
            return;
        }

        if (chat instanceof ChatGrupalEntity grupal) {
            if (!grupal.isActivo()) {
                throw new RuntimeException(Constantes.MSG_CHAT_GRUPAL_NO_ENCONTRADO_ID + chat.getId());
            }
            boolean isMember = grupal.getUsuarios() != null
                    && grupal.getUsuarios().stream()
                    .anyMatch(u -> u != null && Objects.equals(u.getId(), requesterId) && u.isActivo());
            if (!isMember) {
                throw new AccessDeniedException(Constantes.MSG_NO_PERTENECE_CHAT);
            }
            return;
        }

        throw new AccessDeniedException(Constantes.ERR_NO_AUTORIZADO);
    }

    private String resolveSearchableContent(String rawContent) {
        if (rawContent == null || rawContent.isBlank()) {
            return null;
        }

        E2EDiagnosticUtils.ContentDiagnostic diagnostic = E2EDiagnosticUtils.analyze(rawContent, Constantes.TIPO_TEXT);
        String classification = diagnostic.getClassification();
        if (classification != null && classification.startsWith("JSON_E2E")) {
            // Estrategia segura forAdmin: descifrado controlado en memoria, sin persistir ni loggear plaintext.
            String forAdminEnvelope = E2EPayloadUtils.getAdminEnvelope(rawContent);
            if (forAdminEnvelope == null || forAdminEnvelope.isBlank()) {
                return null;
            }
            String decrypted = adminAuditCrypto.decryptBase64Envelope(forAdminEnvelope);
            if (decrypted == null || decrypted.isBlank()) {
                return null;
            }
            return decrypted;
        }
        return rawContent;
    }

    private MatchWindow findFirstMatchWindow(String sourceText, String normalizedQuery) {
        if (sourceText == null || sourceText.isBlank() || normalizedQuery == null || normalizedQuery.isBlank()) {
            return null;
        }

        NormalizedText normalizedText = normalizeWithIndexMap(sourceText);
        if (normalizedText.normalized().isBlank() || normalizedText.indexMap().length == 0) {
            return null;
        }

        int normalizedMatchStart = normalizedText.normalized().indexOf(normalizedQuery);
        if (normalizedMatchStart < 0) {
            return null;
        }

        int normalizedMatchEnd = normalizedMatchStart + normalizedQuery.length() - 1;
        if (normalizedMatchEnd >= normalizedText.indexMap().length) {
            return null;
        }

        int originalMatchStart = normalizedText.indexMap()[normalizedMatchStart];
        int originalMatchEndExclusive = Math.min(sourceText.length(), normalizedText.indexMap()[normalizedMatchEnd] + 1);

        int snippetStart = Math.max(0, originalMatchStart - SEARCH_SNIPPET_CONTEXT_CHARS);
        int snippetEnd = Math.min(sourceText.length(), originalMatchEndExclusive + SEARCH_SNIPPET_CONTEXT_CHARS);
        String snippet = sourceText.substring(snippetStart, snippetEnd);
        int snippetMatchStart = Math.max(0, originalMatchStart - snippetStart);
        int snippetMatchEnd = Math.max(snippetMatchStart, originalMatchEndExclusive - snippetStart);

        boolean startsWithQuery = normalizedText.normalized().startsWith(normalizedQuery);
        return new MatchWindow(snippet, snippetMatchStart, snippetMatchEnd, startsWithQuery);
    }

    private NormalizedText normalizeWithIndexMap(String input) {
        if (input == null || input.isEmpty()) {
            return new NormalizedText("", new int[0]);
        }

        StringBuilder normalizedBuilder = new StringBuilder(input.length());
        int[] indexMap = new int[Math.max(16, input.length())];
        int count = 0;

        for (int i = 0; i < input.length(); i++) {
            String normalizedChar = normalizeForSearch(String.valueOf(input.charAt(i)));
            if (normalizedChar.isEmpty()) {
                continue;
            }
            for (int j = 0; j < normalizedChar.length(); j++) {
                if (count == indexMap.length) {
                    indexMap = Arrays.copyOf(indexMap, indexMap.length * 2);
                }
                normalizedBuilder.append(normalizedChar.charAt(j));
                indexMap[count] = i;
                count++;
            }
        }

        return new NormalizedText(
                normalizedBuilder.toString(),
                Arrays.copyOf(indexMap, count));
    }

    private String normalizeForSearch(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String normalized = Normalizer.normalize(value.toLowerCase(Locale.ROOT), Normalizer.Form.NFD);
        return DIACRITICS_PATTERN.matcher(normalized).replaceAll("");
    }

    private MensajeDTO crearMensajeSistemaSalidaGrupo(ChatGrupalEntity chat, UsuarioEntity usuarioQueSale) {
        String nombreCompleto = buildNombreCompleto(
                usuarioQueSale == null ? null : usuarioQueSale.getNombre(),
                usuarioQueSale == null ? null : usuarioQueSale.getApellido());
        String contenido = nombreCompleto + " ha salido del grupo";

        MensajeEntity mensaje = new MensajeEntity();
        mensaje.setChat(chat);
        mensaje.setEmisor(usuarioQueSale);
        mensaje.setReceptor(null);
        mensaje.setTipo(MessageType.SYSTEM);
        mensaje.setContenido(contenido);
        mensaje.setFechaEnvio(LocalDateTime.now());
        mensaje.setActivo(true);
        mensaje.setLeido(false);
        mensaje.setReenviado(false);

        MensajeEntity saved = mensajeRepository.save(mensaje);
        MensajeDTO dto = MappingUtils.mensajeEntityADto(saved);
        dto.setReceptorId(chat.getId());
        dto.setTipo(Constantes.TIPO_SYSTEM);
        dto.setEmisorNombre(usuarioQueSale == null ? null : usuarioQueSale.getNombre());
        dto.setEmisorApellido(usuarioQueSale == null ? null : usuarioQueSale.getApellido());
        dto.setEmisorNombreCompleto(nombreCompleto);

        LOGGER.info(Constantes.LOG_E2E_GROUP_MEMBER_LEFT_SYSTEM_MESSAGE_CREATED,
                chat == null ? null : chat.getId(),
                usuarioQueSale == null ? null : usuarioQueSale.getId(),
                saved.getId(),
                MessageType.SYSTEM.name(),
                E2EDiagnosticUtils.fingerprint12(contenido),
                contenido.length());

        return dto;
    }

    private void applyAdminRawMetadata(ChatResumenDTO dto, MensajeEntity mensaje) {
        if (mensaje == null) {
            dto.setUltimoMensaje(Constantes.MSG_SIN_DATOS);
            dto.setUltimoMensajeFecha(null);
            dto.setFechaUltimoMensaje(null);
            dto.setUltimoMensajeTipo(MessageType.TEXT.name());
            dto.setUltimoMensajeTexto(null);
            dto.setUltimoMensajePreview(null);
            dto.setUltimoMensajeDescifrado(null);
            dto.setUltimoMensajeEmisorNombre(null);
            dto.setUltimoMensajeEmisorApellido(null);
            dto.setUltimoMensajeEmisorNombreCompleto(null);
            return;
        }

        String classification = E2EDiagnosticUtils.analyze(
                mensaje.getContenido(),
                mensaje.getTipo() == null ? null : mensaje.getTipo().name()).getClassification();
        String emisorNombre = mensaje.getEmisor() == null ? null : mensaje.getEmisor().getNombre();
        String emisorApellido = mensaje.getEmisor() == null ? null : mensaje.getEmisor().getApellido();
        String emisorNombreCompleto = buildNombreCompleto(emisorNombre, emisorApellido);
        String rawContenido = mensaje.getContenido() == null ? Constantes.MSG_SIN_DATOS : mensaje.getContenido();

        dto.setUltimoMensaje(rawContenido);
        dto.setUltimoMensajeFecha(mensaje.getFechaEnvio());
        dto.setFechaUltimoMensaje(mensaje.getFechaEnvio());
        dto.setUltimoMensajeTipo(mensaje.getTipo() == null ? MessageType.TEXT.name() : mensaje.getTipo().name());
        dto.setUltimoMensajeEmisorNombre(emisorNombre);
        dto.setUltimoMensajeEmisorApellido(emisorApellido);
        dto.setUltimoMensajeEmisorNombreCompleto(emisorNombreCompleto);
        dto.setUltimoMensajeTexto(null);
        dto.setUltimoMensajePreview(null);
        dto.setUltimoMensajeDescifrado(null);

        logAdminPreviewDiag(mensaje, classification, false, false);
    }

    private void logAdminPreviewDiag(MensajeEntity mensaje, String classification, boolean decryptOk, boolean usedForAdmin) {
        LOGGER.info(Constantes.LOG_E2E_ADMIN_CHAT_LIST_PREVIEW,
                mensaje.getChat() == null ? null : mensaje.getChat().getId(),
                mensaje.getId(),
                classification,
                decryptOk,
                usedForAdmin);
    }

    private String buildNombreCompleto(String nombre, String apellido) {
        String n = nombre == null ? "" : nombre.trim();
        String a = apellido == null ? "" : apellido.trim();
        String full = (n + (a.isEmpty() ? "" : " " + a)).trim();
        return full.isEmpty() ? Constantes.DEFAULT_CALLER_NAME : full;
    }

    private GroupMediaItemDTO toGroupMediaItem(MensajeEntity mensaje) {
        GroupMediaItemDTO dto = new GroupMediaItemDTO();
        dto.setMessageId(mensaje.getId());
        dto.setChatId(mensaje.getChat() == null ? null : mensaje.getChat().getId());
        dto.setEmisorId(mensaje.getEmisor() == null ? null : mensaje.getEmisor().getId());
        dto.setEmisorNombreCompleto(buildNombreCompleto(
                mensaje.getEmisor() == null ? null : mensaje.getEmisor().getNombre(),
                mensaje.getEmisor() == null ? null : mensaje.getEmisor().getApellido()));
        dto.setTipo(mensaje.getTipo() == null ? null : mensaje.getTipo().name());
        dto.setFechaEnvio(mensaje.getFechaEnvio());
        dto.setActivo(mensaje.isActivo());
        dto.setReenviado(mensaje.isReenviado());
        dto.setContenidoRaw(mensaje.getContenido());

        MediaMeta meta = extractMediaMeta(mensaje);
        dto.setMime(meta.mime());
        dto.setDurMs(meta.durMs());
        dto.setMediaUrl(meta.mediaUrl());
        dto.setFileName(meta.fileName());
        dto.setSizeBytes(meta.sizeBytes());
        return dto;
    }

    private List<MessageType> parseMediaTypes(String csv) {
        if (csv == null || csv.isBlank()) {
            return List.of(MessageType.IMAGE, MessageType.VIDEO, MessageType.AUDIO, MessageType.FILE);
        }
        LinkedHashSet<MessageType> parsed = new LinkedHashSet<>();
        for (String token : csv.split(",")) {
            String normalized = token == null ? "" : token.trim().toUpperCase(Locale.ROOT);
            if (normalized.isBlank()) {
                continue;
            }
            MessageType type;
            try {
                type = MessageType.valueOf(normalized);
            } catch (IllegalArgumentException ex) {
                throw new IllegalArgumentException(ExceptionConstants.ERROR_MEDIA_TYPES_INVALID_PREFIX + normalized);
            }
            if (!(type == MessageType.IMAGE || type == MessageType.VIDEO || type == MessageType.AUDIO || type == MessageType.FILE)) {
                throw new IllegalArgumentException(ExceptionConstants.ERROR_MEDIA_TYPES_INVALID_PREFIX + normalized);
            }
            parsed.add(type);
        }
        if (parsed.isEmpty()) {
            return List.of(MessageType.IMAGE, MessageType.VIDEO, MessageType.AUDIO, MessageType.FILE);
        }
        return new ArrayList<>(parsed);
    }

    private CursorPosition parseCursor(String cursor, Long chatId) {
        if (cursor == null || cursor.isBlank()) {
            return CursorPosition.empty();
        }
        String normalized = cursor.trim();
        if (normalized.contains(CURSOR_SEPARATOR)) {
            String[] parts = normalized.split(CURSOR_SEPARATOR, 2);
            if (parts.length != 2) {
                throw new IllegalArgumentException(ExceptionConstants.ERROR_MEDIA_CURSOR_INVALID);
            }
            try {
                long epochMillis = Long.parseLong(parts[0]);
                long id = Long.parseLong(parts[1]);
                LocalDateTime fecha = LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMillis), ZoneOffset.UTC);
                return new CursorPosition(fecha, id);
            } catch (NumberFormatException ex) {
                throw new IllegalArgumentException(ExceptionConstants.ERROR_MEDIA_CURSOR_INVALID);
            }
        }
        try {
            long messageId = Long.parseLong(normalized);
            MensajeEntity anchor = mensajeRepository.findByIdAndChatId(messageId, chatId)
                    .orElseThrow(() -> new IllegalArgumentException(ExceptionConstants.ERROR_MEDIA_CURSOR_INVALID));
            return new CursorPosition(anchor.getFechaEnvio(), anchor.getId());
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(ExceptionConstants.ERROR_MEDIA_CURSOR_INVALID);
        }
    }

    private String encodeCursor(LocalDateTime fechaEnvio, Long id) {
        if (fechaEnvio == null || id == null) {
            return null;
        }
        long epochMillis = fechaEnvio.toInstant(ZoneOffset.UTC).toEpochMilli();
        return epochMillis + CURSOR_SEPARATOR + id;
    }

    private MediaMeta extractMediaMeta(MensajeEntity mensaje) {
        String mime = mensaje.getMediaMime();
        Integer durMs = mensaje.getMediaDuracionMs();
        String mediaUrl = mensaje.getMediaUrl();
        String fileName = null;
        Long sizeBytes = null;

        if (mediaUrl != null && !mediaUrl.isBlank()) {
            int slash = mediaUrl.lastIndexOf('/');
            fileName = slash >= 0 && slash + 1 < mediaUrl.length() ? mediaUrl.substring(slash + 1) : mediaUrl;
            sizeBytes = resolveSizeBytesFromMediaUrl(mediaUrl);
        }

        String contenido = mensaje.getContenido();
        if (contenido != null && !contenido.isBlank() && (mime == null || durMs == null || mediaUrl == null || fileName == null || sizeBytes == null)) {
            try {
                JsonNode root = OBJECT_MAPPER.readTree(contenido);
                if (mime == null) {
                    mime = firstNonBlank(
                            root.path("audioMime").asText(null),
                            root.path("imageMime").asText(null),
                            root.path("mime").asText(null));
                }
                if (durMs == null) {
                    Integer parsedDur = extractInteger(root, "audioDuracionMs");
                    if (parsedDur == null) {
                        parsedDur = extractInteger(root, "durMs");
                    }
                    durMs = parsedDur;
                }
                if (mediaUrl == null) {
                    mediaUrl = firstNonBlank(root.path("audioUrl").asText(null),
                            root.path("imageUrl").asText(null),
                            root.path("mediaUrl").asText(null),
                            root.path("url").asText(null));
                }
                if (fileName == null) {
                    fileName = firstNonBlank(
                            root.path("imageNombre").asText(null),
                            root.path("fileName").asText(null),
                            extractFileNameFromUrl(mediaUrl));
                }
                if (sizeBytes == null) {
                    Long parsedSize = extractLong(root, "sizeBytes");
                    if (parsedSize == null && mediaUrl != null) {
                        parsedSize = resolveSizeBytesFromMediaUrl(mediaUrl);
                    }
                    sizeBytes = parsedSize;
                }
            } catch (Exception ignored) {
                // Metadata parsing is best-effort; raw payload stays untouched.
            }
        }

        return new MediaMeta(mime, sizeBytes, durMs, fileName, mediaUrl);
    }

    private Integer extractInteger(JsonNode root, String field) {
        JsonNode node = root.get(field);
        if (node == null || node.isNull()) {
            return null;
        }
        if (node.isInt() || node.isLong()) {
            return node.asInt();
        }
        if (node.isTextual()) {
            try {
                return Integer.parseInt(node.asText());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private Long extractLong(JsonNode root, String field) {
        JsonNode node = root.get(field);
        if (node == null || node.isNull()) {
            return null;
        }
        if (node.isIntegralNumber()) {
            return node.asLong();
        }
        if (node.isTextual()) {
            try {
                return Long.parseLong(node.asText());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private String extractFileNameFromUrl(String mediaUrl) {
        if (mediaUrl == null || mediaUrl.isBlank()) {
            return null;
        }
        int slash = mediaUrl.lastIndexOf('/');
        return slash >= 0 && slash + 1 < mediaUrl.length() ? mediaUrl.substring(slash + 1) : mediaUrl;
    }

    private Long resolveSizeBytesFromMediaUrl(String mediaUrl) {
        if (mediaUrl == null || mediaUrl.isBlank() || !mediaUrl.startsWith(Constantes.UPLOADS_PREFIX)) {
            return null;
        }
        String relative = mediaUrl.substring(Constantes.UPLOADS_PREFIX.length());
        Path absolutePath = Paths.get(uploadsRoot).resolve(relative).normalize();
        try {
            if (Files.exists(absolutePath) && Files.isRegularFile(absolutePath)) {
                return Files.size(absolutePath);
            }
        } catch (Exception ignored) {
            return null;
        }
        return null;
    }

    private String firstNonBlank(String... values) {
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

    private record SearchMatchCandidate(boolean startsWithQuery,
                                        LocalDateTime fechaEnvio,
                                        Long id,
                                        ChatMensajeBusquedaItemDTO item) {
    }

    private record MatchWindow(String snippet,
                               int matchStart,
                               int matchEnd,
                               boolean startsWithQuery) {
    }

    private record NormalizedText(String normalized, int[] indexMap) {
    }

    private record CursorPosition(LocalDateTime fechaEnvio, Long messageId) {
        static CursorPosition empty() {
            return new CursorPosition(null, null);
        }
    }

    private record MediaMeta(String mime, Long sizeBytes, Integer durMs, String fileName, String mediaUrl) {
    }

    private boolean esAdmin(UsuarioEntity usuario) {
        if (usuario == null || usuario.getRoles() == null) {
            return false;
        }
        return usuario.getRoles().stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .anyMatch(rol -> Constantes.ROLE_ADMIN.equalsIgnoreCase(rol) || Constantes.ADMIN.equalsIgnoreCase(rol));
    }






}
