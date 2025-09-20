package com.chat.chat.Service.ChatService;


import com.chat.chat.DTO.*;
import com.chat.chat.Entity.*;
import com.chat.chat.Repository.*;
import com.chat.chat.Utils.*;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ChatServiceImpl implements ChatService {

    @Autowired
    private UsuarioRepository usuarioRepo;

    @Autowired
    private ChatIndividualRepository chatIndRepo;

     @Autowired
     SimpMessagingTemplate messagingTemplate;

    @Value("${app.uploads.root:uploads}")
    private String uploadsRoot;

    @Value("${app.uploads.base-url:/uploads}")
    private String uploadsBaseUrl;

    @Autowired
    private MensajeRepository mensajeRepository;

    @Autowired
    private ChatRepository chatRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private  GroupInviteRepo inviteRepo;
    @Autowired
    private  NotificationRepo notificationRepo;

    @Autowired
    private ChatGrupalRepository chatGrupalRepo;

    @Override
    public ChatIndividualDTO crearChatIndividual(Long usuario1Id, Long usuario2Id) {
        UsuarioEntity u1 = usuarioRepo.findById(usuario1Id).orElseThrow();
        UsuarioEntity u2 = usuarioRepo.findById(usuario2Id).orElseThrow();

        ChatIndividualEntity chat = new ChatIndividualEntity();
        chat.setUsuario1(u1);
        chat.setUsuario2(u2);

        return MappingUtils.chatIndividualEntityADto(chatIndRepo.save(chat));
    }

    @Override
    @Transactional
    public ChatGrupalDTO crearChatGrupal(ChatGrupalDTO dto) {
        // 1) Creador desde util
        UsuarioEntity creador = Utils.getCreadorOrThrow(dto, usuarioRepo); // ya lo tenías

        // 2) Procesar fotoGrupo: dataURL -> guardar; URL pública -> usar
        String fotoUrl = null;
        if (dto.getFotoGrupo() != null) {
            String f = dto.getFotoGrupo().trim();
            if (f.startsWith("data:image")) {
                fotoUrl = Utils.saveDataUrlToUploads(f, "group-photos", uploadsRoot, uploadsBaseUrl);
            } else if (Utils.isPublicUrl(f)) {
                fotoUrl = f;
            }
        }

        // 3) Crear el chat con el creador como único miembro inicial
        ChatGrupalEntity chat = new ChatGrupalEntity();
        chat.setNombreGrupo(dto.getNombreGrupo());
        chat.setFotoUrl(fotoUrl);
        chat.setUsuarios(new ArrayList<>(List.of(creador)));
        chat = chatGrupalRepo.save(chat);

        // 4) Preparar invitados (los que vienen en dto, excepto el creador, sin duplicados)
        Set<Long> inviteeIds = (dto.getUsuarios() == null) ? Set.of() :
                dto.getUsuarios().stream()
                        .map(UsuarioDTO::getId)
                        .filter(Objects::nonNull)
                        .filter(id -> !Objects.equals(id, dto.getIdCreador()))
                        .collect(Collectors.toCollection(LinkedHashSet::new));

        // 5) Crear invitaciones
        List<GroupInviteEntity> invites = new ArrayList<>();
        for (Long inviteeId : inviteeIds) {
            UsuarioEntity invitee = usuarioRepo.findById(inviteeId)
                    .orElseThrow(() -> new IllegalArgumentException("Usuario invitado no existe: " + inviteeId));
            GroupInviteEntity inv = new GroupInviteEntity();
            inv.setChat(chat);
            inv.setInviter(creador);
            inv.setInvitee(invitee);
            inv.setStatus(InviteStatus.PENDING);
            invites.add(inv);
        }
        inviteRepo.saveAll(invites);

        // 6) Guardar notificación + enviar WS a cada invitado
        for (GroupInviteEntity inv : invites) {
            GroupInviteWS wsPayload = new GroupInviteWS();
            wsPayload.inviteId = inv.getId();
            wsPayload.groupId = chat.getId();
            wsPayload.groupName = chat.getNombreGrupo();
            wsPayload.inviterId = creador.getId();
            wsPayload.inviterNombre = creador.getNombre(); // añade apellidos si quieres

            NotificationEntity notif = new NotificationEntity();
            notif.setUserId(inv.getInvitee().getId());
            notif.setType(NotificationType.GROUP_INVITE);
            notif.setPayloadJson(Utils.writeJson(wsPayload));
            notif.setSeen(false);
            notificationRepo.save(notif);

            long unseen = notificationRepo.countByUserIdAndSeenFalse(inv.getInvitee().getId());
            wsPayload.unseenCount = (int) unseen;

            messagingTemplate.convertAndSend("/topic/notifications." + inv.getInvitee().getId(), wsPayload);
        }

        // 7) DTO de salida (con foto)
        ChatGrupalDTO out = MappingUtils.chatGrupalEntityADto(chat);
        out.setUltimaMensaje("Grupo creado");
        out.setUltimaFecha(LocalDateTime.now());
        out.setIdCreador(creador.getId());
        out.setFotoGrupo(fotoUrl); // redundante si el mapper ya lo pone, pero ok
        return out;
    }


    @Override
    @Transactional
    public MessagueSalirGrupoDTO salirDeChatGrupal(Long groupId, Long userId) {
        // Validaciones básicas (según tu requerimiento, userId nunca será null)
        if (groupId == null) {
            return new MessagueSalirGrupoDTO(false, "groupId es obligatorio.", null, userId, false);
        }

        ChatGrupalEntity chat = chatGrupalRepo.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("No existe el grupo con id: " + groupId));

        UsuarioEntity usuario = usuarioRepo.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("No existe el usuario con id: " + userId));

        // Comprobar membresía
        boolean eraMiembro = chat.getUsuarios() != null &&
                chat.getUsuarios().stream().anyMatch(u -> Objects.equals(u.getId(), userId));
        if (!eraMiembro) {
            return new MessagueSalirGrupoDTO(false, "No perteneces a este grupo.", groupId, userId, false);
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

        // Si queda vacío → eliminar grupo
        if (chat.getUsuarios() == null || chat.getUsuarios().isEmpty()) {
            chatGrupalRepo.delete(chat);

            return new MessagueSalirGrupoDTO(true,
                    "Has salido del grupo. El grupo se ha eliminado por quedar vacío.",
                    groupId, userId, true);
        }

        // Persistir cambios cuando no se elimina
        chatGrupalRepo.save(chat);



        return new MessagueSalirGrupoDTO(true, "Has salido del grupo.", groupId, userId, false);
    }

    @Override
    public EsMiembroDTO esMiembroDeChatGrupal(Long groupId, Long userId) {
        var opt = chatGrupalRepo.findById(groupId);
        if (opt.isEmpty()) return new EsMiembroDTO(false, true);
        var chat = opt.get();
        boolean esMiembro = chat.getUsuarios() != null &&
                chat.getUsuarios().stream().anyMatch(u -> u.getId().equals(userId));
        return new EsMiembroDTO(esMiembro, false);
    }



    @Override
    public List<ChatGrupalDTO> listarChatsGrupalesPorUsuario(Long usuarioId) {
        UsuarioEntity usuario = usuarioRepo.findById(usuarioId).orElseThrow();
        return chatGrupalRepo.findAll().stream()
                .filter(chat -> chat.getUsuarios().contains(usuario))
                .map(MappingUtils::chatGrupalEntityADto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public AddUsuariosGrupoWSResponse anadirUsuariosAGrupo(AddUsuariosGrupoDTO dto) {
        // 1) Validar grupo e invitador
        ChatGrupalEntity chat = chatGrupalRepo.findById(dto.getGroupId())
                .orElseThrow(() -> new IllegalArgumentException("Chat grupal no existe: " + dto.getGroupId()));

        UsuarioEntity inviter = usuarioRepo.findById(dto.getInviterId())
                .orElseThrow(() -> new IllegalArgumentException("Invitador no existe: " + dto.getInviterId()));

        // Comprobar que el invitador pertenece al grupo
        boolean inviterEsMiembro = chat.getUsuarios() != null
                && chat.getUsuarios().stream().anyMatch(u -> Objects.equals(u.getId(), inviter.getId()));
        if (!inviterEsMiembro) {
            throw new IllegalStateException("El invitador no pertenece al grupo");
        }

        // 2) Normalizar IDs a invitar (sin duplicados, sin incluir al invitador)
        Set<Long> solicitados = (dto.getUsuarios() == null) ? Set.of() :
                dto.getUsuarios().stream()
                        .map(UsuarioDTO::getId)
                        .filter(Objects::nonNull)
                        .filter(id -> !Objects.equals(id, inviter.getId()))
                        .collect(Collectors.toCollection(LinkedHashSet::new));

        // 3) Filtrar ya miembros
        Set<Long> miembrosIds = chat.getUsuarios() == null ? Set.of()
                : chat.getUsuarios().stream().map(UsuarioEntity::getId).collect(Collectors.toSet());

        Set<Long> noMiembros = solicitados.stream()
                .filter(id -> !miembrosIds.contains(id))
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
                    .orElseThrow(() -> new IllegalArgumentException("Usuario invitado no existe: " + inviteeId));

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
            wsPayload.setInviterNombre(inviter.getNombre()); // añade apellidos si quieres

            NotificationEntity notif = new NotificationEntity();
            notif.setUserId(inv.getInvitee().getId());
            notif.setType(NotificationType.GROUP_INVITE);
            notif.setPayloadJson(Utils.writeJson(wsPayload));
            notif.setSeen(false);
            notificationRepo.save(notif);

            long unseen = notificationRepo.countByUserIdAndSeenFalse(inv.getInvitee().getId());
            wsPayload.setUnseenCount((int) unseen);

            // Emitir WS
            messagingTemplate.convertAndSend("/topic/notifications." + inv.getInvitee().getId(), wsPayload);

            // Añadir a respuesta
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
                        r -> (Long) r[0],  // chatId
                        r -> (Long) r[1]   // count
                ));

        // === INDIVIDUALES ===
        List<ChatIndividualDTO> individuales = chatIndRepo.findAll().stream()
                .filter(chat -> chat.getUsuario1().getId().equals(usuarioId) || chat.getUsuario2().getId().equals(usuarioId))
                .map(chat -> {
                    ChatIndividualDTO dto = MappingUtils.chatIndividualEntityADto(chat, usuario);

                    // 👇 convertir la foto del receptor a Base64 (dataURL) para el front
                    if (dto.getReceptor() != null && dto.getReceptor().getFoto() != null) {
                        dto.getReceptor().setFoto(Utils.toDataUrlFromUrl(dto.getReceptor().getFoto(), uploadsRoot));

                    }

                    dto.setUnreadCount(unreadMap.getOrDefault(dto.getId(), 0L));

                    // Último mensaje por chatId
                    MensajeEntity last = mensajeRepository.findTopByChatIdOrderByFechaEnvioDesc(dto.getId());

                    if (last == null) {
                        dto.setUltimaMensaje("Sin mensajes aún");
                        dto.setUltimaFecha(null);
                        // dto.setUltimaMensajeId(null); // si tienes este campo
                    } else if (!last.isActivo()) {
                        dto.setUltimaMensaje("Mensaje eliminado");
                        dto.setUltimaFecha(last.getFechaEnvio());
                        // dto.setUltimaMensajeId(last.getId());
                    } else {
                        boolean soyYo = last.getEmisor() != null && last.getEmisor().getId().equals(usuarioId);
                        String pref = soyYo ? "Tú: " : "";

                        MessageType tipo = (last.getTipo() != null) ? last.getTipo() : MessageType.TEXT;
                        String preview;

                        if (tipo == MessageType.AUDIO) {
                            String dur = Utils.mmss(last.getMediaDuracionMs());
                            // ✅ sin "Mensaje de voz", solo micro + duración
                            preview = pref + "🎤" + (dur.isEmpty() ? "" : " (" + dur + ")");
                        } else {
                            preview = pref + Utils.truncarSafe(last.getContenido(), 60);
                        }

                        dto.setUltimaMensaje(preview);
                        dto.setUltimaFecha(last.getFechaEnvio());
                            // dto.setUltimaMensajeId(last.getId());
                    }
                    return dto;
                })
                .collect(Collectors.toList());

        // === GRUPALES ===
        List<ChatGrupalDTO> grupales = chatGrupalRepo.findAll().stream()
                .filter(chat -> chat.getUsuarios().contains(usuario))
                .map(chat -> {
                    ChatGrupalDTO dto = MappingUtils.chatGrupalEntityADto(chat);

                    // Foto del GRUPO (soporta fotoGrupo o foto, según tu DTO)
                    String fg = dto.getFotoGrupo();
                    if (fg != null && !fg.startsWith("data:") && fg.startsWith("/uploads/")) {
                        String data = Utils.toDataUrlFromUrl(fg, uploadsRoot);
                        dto.setFotoGrupo(data != null ? data : fg);
                    }
                    String f2 = dto.getFotoGrupo();
                    if (f2 != null && !f2.startsWith("data:") && f2.startsWith("/uploads/")) {
                        String data = Utils.toDataUrlFromUrl(f2, uploadsRoot);
                        dto.setFotoGrupo(data != null ? data : f2);
                    }

                    // Fotos de miembros (opcional)
                    if (dto.getUsuarios() != null) {
                        dto.getUsuarios().forEach(u -> {
                            String fu = u.getFoto();
                            if (fu != null && !fu.startsWith("data:") && fu.startsWith("/uploads/")) {
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

                    // Último mensaje por chatId
                    MensajeEntity last = mensajeRepository.findTopByChatIdOrderByFechaEnvioDesc(dto.getId());

                    if (last == null) {
                        dto.setUltimaMensaje("Sin mensajes aún");
                        dto.setUltimaFecha(null);
                        // dto.setUltimaMensajeId(null);
                    } else if (!last.isActivo()) {
                        dto.setUltimaMensaje("Mensaje eliminado");
                        dto.setUltimaFecha(last.getFechaEnvio());
                        // dto.setUltimaMensajeId(last.getId());
                    } else {
                        String emisorNombre = last.getEmisor() != null ? last.getEmisor().getNombre() : "";
                        String prefix = (emisorNombre != null && !emisorNombre.isBlank()) ? emisorNombre + ": " : "";

                        MessageType tipo = (last.getTipo() != null) ? last.getTipo() : MessageType.TEXT;
                        String preview;

                        if (tipo == MessageType.AUDIO) {
                            String dur = Utils.mmss(last.getMediaDuracionMs());
                            // ✅ “Nombre: 🎤 (mm:ss)”
                            preview = prefix + "🎤" + (dur.isEmpty() ? "" : " (" + dur + ")");
                        } else {
                            preview = prefix + Utils.truncarSafe(last.getContenido(), 60);
                        }

                        dto.setUltimaMensaje(preview);
                        dto.setUltimaFecha(last.getFechaEnvio());
                        // dto.setUltimaMensajeId(last.getId());
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

            if (fa == null && fb == null) return 0;
            if (fa == null) return 1;
            if (fb == null) return -1;
            return fb.compareTo(fa); // DESC
        });

        return resultado;
    }



    @Override
    public List<MensajeDTO> listarMensajesPorChatId(Long chatId) {
        ChatEntity chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new RuntimeException("Chat no encontrado con ID: " + chatId));

        List<MensajeEntity> mensajes = chat.getMensajes();

        return mensajes.stream()
                .map(MappingUtils::mensajeEntityADto)
                .collect(Collectors.toList());
    }

    @Override
    public List<MensajeDTO> listarMensajesPorChatGrupal(Long chatId) {
        ChatGrupalEntity chat = chatGrupalRepo.findById(chatId)
                .orElseThrow(() -> new RuntimeException("Chat grupal no encontrado con ID: " + chatId));

        // Si tienes un repo con este método, úsalo; si no, ordenamos en memoria:
        // List<MensajeEntity> mensajes = mensajeRepository.findByChatIdOrderByFechaEnvioAsc(chatId);
        List<MensajeEntity> mensajes = chat.getMensajes().stream()
                .sorted(java.util.Comparator.comparing(MensajeEntity::getFechaEnvio))
                .collect(java.util.stream.Collectors.toList());

        return mensajes.stream()
                .map(e -> {
                    MensajeDTO dto = MappingUtils.mensajeEntityADto(e);
                    // enriquecer con datos del emisor para que el front pinte nombre/foto en grupos
                    UsuarioEntity emisor = e.getEmisor();
                    if (emisor != null) {
                        dto.setEmisorNombre(emisor.getNombre());
                        dto.setEmisorApellido(emisor.getApellido());
                        if (emisor.getFotoUrl() != null) {
                            dto.setEmisorFoto(Utils.toDataUrlFromUrl(emisor.getFotoUrl(), uploadsRoot));
                        }
                    }
                    return dto;
                })
                .collect(java.util.stream.Collectors.toList());
    }

    /** Trunca y normaliza espacios para previews. */
    private String truncar(String s, int max) {
        if (s == null) return "";
        s = s.trim().replaceAll("\\s+", " ");
        return (s.length() <= max) ? s : s.substring(0, Math.max(0, max - 1)) + "…";
    }


}
