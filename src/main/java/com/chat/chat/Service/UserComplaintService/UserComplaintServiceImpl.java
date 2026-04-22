package com.chat.chat.Service.UserComplaintService;

import com.chat.chat.DTO.UserComplaintCreateDTO;
import com.chat.chat.DTO.UserComplaintDTO;
import com.chat.chat.DTO.UserComplaintStatsDTO;
import com.chat.chat.DTO.UserComplaintWsDTO;
import com.chat.chat.DTO.UserExpedienteDTO;
import com.chat.chat.DTO.UserModerationHistoryItemDTO;
import com.chat.chat.Entity.ChatIndividualEntity;
import com.chat.chat.Entity.UserBlockRelationEntity;
import com.chat.chat.Entity.UserComplaintEntity;
import com.chat.chat.Entity.UserModerationHistoryEntity;
import com.chat.chat.Entity.UsuarioEntity;
import com.chat.chat.Exceptions.RecursoNoEncontradoException;
import com.chat.chat.Mapper.UserComplaintMapper;
import com.chat.chat.Repository.ChatIndividualRepository;
import com.chat.chat.Repository.UserBlockRelationRepository;
import com.chat.chat.Repository.UserComplaintRepository;
import com.chat.chat.Repository.UserModerationHistoryRepository;
import com.chat.chat.Repository.UsuarioRepository;
import com.chat.chat.Security.HttpRateLimitService;
import com.chat.chat.Utils.BlockSource;
import com.chat.chat.Utils.Constantes;
import com.chat.chat.Utils.SecurityUtils;
import com.chat.chat.Utils.UserComplaintEstado;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class UserComplaintServiceImpl implements UserComplaintService {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 8;
    private static final int MAX_SIZE = 100;
    private static final int MAX_MOTIVO_LENGTH = 120;
    private static final int MAX_NOMBRE_LENGTH = 190;
    private static final String EVENT_CREATED = "USER_COMPLAINT_CREATED";
    private static final String EVENT_UPDATED = "USER_COMPLAINT_UPDATED";

    private final UserComplaintRepository userComplaintRepository;
    private final UserModerationHistoryRepository userModerationHistoryRepository;
    private final UserBlockRelationRepository userBlockRelationRepository;
    private final UsuarioRepository usuarioRepository;
    private final ChatIndividualRepository chatIndividualRepository;
    private final SecurityUtils securityUtils;
    private final HttpRateLimitService httpRateLimitService;
    private final UserComplaintMapper userComplaintMapper;
    private final SimpMessagingTemplate messagingTemplate;

    public UserComplaintServiceImpl(UserComplaintRepository userComplaintRepository,
                                    UserModerationHistoryRepository userModerationHistoryRepository,
                                    UserBlockRelationRepository userBlockRelationRepository,
                                    UsuarioRepository usuarioRepository,
                                    ChatIndividualRepository chatIndividualRepository,
                                    SecurityUtils securityUtils,
                                    HttpRateLimitService httpRateLimitService,
                                    UserComplaintMapper userComplaintMapper,
                                    SimpMessagingTemplate messagingTemplate) {
        this.userComplaintRepository = userComplaintRepository;
        this.userModerationHistoryRepository = userModerationHistoryRepository;
        this.userBlockRelationRepository = userBlockRelationRepository;
        this.usuarioRepository = usuarioRepository;
        this.chatIndividualRepository = chatIndividualRepository;
        this.securityUtils = securityUtils;
        this.httpRateLimitService = httpRateLimitService;
        this.userComplaintMapper = userComplaintMapper;
        this.messagingTemplate = messagingTemplate;
    }

    @Override
    @Transactional
    public UserComplaintDTO createComplaint(UserComplaintCreateDTO request) {
        if (request == null) {
            throw new IllegalArgumentException("Payload de denuncia vacio");
        }

        Long denuncianteId = securityUtils.getAuthenticatedUserId();
        UsuarioEntity denunciante = usuarioRepository.findById(denuncianteId)
                .orElseThrow(() -> new RecursoNoEncontradoException(Constantes.MSG_USUARIO_NO_ENCONTRADO));

        Long denunciadoId = request.getDenunciadoId();
        if (denunciadoId == null) {
            throw new IllegalArgumentException("denunciadoId es obligatorio");
        }
        if (Objects.equals(denuncianteId, denunciadoId)) {
            throw new IllegalArgumentException("No puedes denunciarte a ti mismo");
        }

        UsuarioEntity denunciado = usuarioRepository.findById(denunciadoId)
                .orElseThrow(() -> new RecursoNoEncontradoException(Constantes.MSG_USUARIO_NO_ENCONTRADO));

        httpRateLimitService.checkUserComplaintCreate(denuncianteId, denunciadoId);

        if (userComplaintRepository.existsByDenuncianteIdAndDenunciadoId(denuncianteId, denunciadoId)) {
            throw new IllegalArgumentException("Ya denunciaste a este usuario");
        }

        Long chatId = request.getChatId();
        if (chatId != null) {
            validateChatOwnership(chatId, denuncianteId, denunciadoId);
        }

        UserComplaintEntity entity = new UserComplaintEntity();
        entity.setDenuncianteId(denuncianteId);
        entity.setDenunciadoId(denunciadoId);
        entity.setChatId(chatId);
        entity.setMotivo(normalizeRequired(request.getMotivo(), "motivo", MAX_MOTIVO_LENGTH));
        entity.setDetalle(normalizeRequired(request.getDetalle(), "detalle", 10000));
        entity.setEstado(UserComplaintEstado.PENDIENTE);
        entity.setLeida(false);
        entity.setLeidaAt(null);
        entity.setDenuncianteNombre(resolveDisplayName(denunciante, null));
        entity.setDenunciadoNombre(resolveDisplayName(denunciado, request.getDenunciadoNombre()));
        entity.setChatNombreSnapshot(trimToNullable(request.getChatNombreSnapshot(), MAX_NOMBRE_LENGTH));

        UserComplaintEntity saved = userComplaintRepository.save(entity);
        ensureReportBlock(denunciante, denunciado);
        publishWsEvent(EVENT_CREATED, saved);
        return userComplaintMapper.toDto(saved);
    }

    @Override
    public Page<UserComplaintDTO> listComplaints(int page, int size) {
        int safePage = Math.max(DEFAULT_PAGE, page);
        int requestedSize = size <= 0 ? DEFAULT_SIZE : size;
        int safeSize = Math.min(MAX_SIZE, requestedSize);
        Pageable pageable = PageRequest.of(safePage, safeSize);
        Page<UserComplaintEntity> result = userComplaintRepository.findAllByOrderByCreatedAtDescIdDesc(pageable);
        List<UserComplaintDTO> content = result.getContent().stream()
                .map(userComplaintMapper::toDto)
                .collect(Collectors.toList());
        return new PageImpl<>(content, pageable, result.getTotalElements());
    }

    @Override
    public UserComplaintStatsDTO getStats() {
        UserComplaintStatsDTO dto = new UserComplaintStatsDTO();
        dto.setTotal(userComplaintRepository.count());
        dto.setUnread(userComplaintRepository.countByLeidaFalse());
        dto.setPendientes(userComplaintRepository.countByEstado(UserComplaintEstado.PENDIENTE));
        return dto;
    }

    @Override
    @Transactional
    public UserComplaintDTO markAsRead(Long id) {
        UserComplaintEntity entity = userComplaintRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Denuncia no encontrada"));

        if (!entity.isLeida()) {
            entity.setLeida(true);
            entity.setLeidaAt(LocalDateTime.now());
            entity = userComplaintRepository.save(entity);
            publishWsEvent(EVENT_UPDATED, entity);
        }

        return userComplaintMapper.toDto(entity);
    }

    @Override
    public UserExpedienteDTO getExpediente(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("userId es obligatorio");
        }

        UsuarioEntity usuario = usuarioRepository.findById(userId)
                .orElseThrow(() -> new RecursoNoEncontradoException(Constantes.MSG_USUARIO_NO_ENCONTRADO));

        UserExpedienteDTO dto = new UserExpedienteDTO();
        dto.setUserId(usuario.getId());
        dto.setNombre(resolveDisplayName(usuario, null));
        dto.setFechaRegistro(toIsoUtc(usuario.getFechaCreacion()));
        dto.setTotalDenunciasRecibidas(userComplaintRepository.countByDenunciadoId(userId));
        dto.setTotalDenunciasRealizadas(userComplaintRepository.countByDenuncianteId(userId));
        dto.setConteoPorMotivo(buildMotivoCounts(userId));
        dto.setUltimasCincoDenuncias(userComplaintRepository.findTop5ByDenunciadoIdOrderByCreatedAtDescIdDesc(userId).stream()
                .map(userComplaintMapper::toDto)
                .collect(Collectors.toList()));
        dto.setCuentaActiva(usuario.isActivo());
        dto.setEstadoCuenta(usuario.isActivo() ? "ACTIVE" : "SUSPENDED");
        dto.setHistorialModeracion(userModerationHistoryRepository.findByUser_IdOrderByCreatedAtDescIdDesc(userId).stream()
                .map(this::toModerationHistoryDto)
                .collect(Collectors.toList()));
        return dto;
    }

    private UserModerationHistoryItemDTO toModerationHistoryDto(UserModerationHistoryEntity row) {
        UserModerationHistoryItemDTO dto = new UserModerationHistoryItemDTO();
        dto.setId(row.getId());
        dto.setTipo(row.getActionType() == null ? null : row.getActionType().name());
        dto.setMotivo(row.getReason());
        dto.setDescripcion(row.getDescription());
        dto.setOrigen(row.getOrigin());
        dto.setAdminId(row.getAdmin() == null ? null : row.getAdmin().getId());
        dto.setAdminNombre(resolveDisplayName(row.getAdmin(), null));
        dto.setCreatedAt(row.getCreatedAt());
        return dto;
    }

    private String toIsoUtc(LocalDateTime value) {
        LocalDateTime safe = value == null ? LocalDateTime.now(ZoneOffset.UTC) : value;
        return safe.atOffset(ZoneOffset.UTC).toInstant().toString();
    }

    private void validateChatOwnership(Long chatId, Long denuncianteId, Long denunciadoId) {
        ChatIndividualEntity chat = chatIndividualRepository.findById(chatId)
                .orElseThrow(() -> new RecursoNoEncontradoException(Constantes.MSG_CHAT_INDIVIDUAL_NO_ENCONTRADO));

        Long usuario1Id = chat.getUsuario1() == null ? null : chat.getUsuario1().getId();
        Long usuario2Id = chat.getUsuario2() == null ? null : chat.getUsuario2().getId();
        boolean requesterParticipates = Objects.equals(usuario1Id, denuncianteId) || Objects.equals(usuario2Id, denuncianteId);
        if (!requesterParticipates) {
            throw new AccessDeniedException(Constantes.MSG_NO_PERTENECE_CHAT);
        }

        Long counterpartId = Objects.equals(usuario1Id, denuncianteId) ? usuario2Id : usuario1Id;
        if (!Objects.equals(counterpartId, denunciadoId)) {
            throw new IllegalArgumentException("chatId no corresponde con denunciadoId");
        }
    }

    private void publishWsEvent(String eventName, UserComplaintEntity entity) {
        UserComplaintWsDTO payload = userComplaintMapper.toWsDto(eventName, entity);
        messagingTemplate.convertAndSend(Constantes.TOPIC_ADMIN_DENUNCIAS, payload);
    }

    private Map<String, Long> buildMotivoCounts(Long userId) {
        LinkedHashMap<String, Long> counts = new LinkedHashMap<>();
        for (UserComplaintRepository.ComplaintMotivoCountView row : userComplaintRepository.countReceivedGroupedByMotivo(userId)) {
            if (row == null || row.getMotivo() == null) {
                continue;
            }
            counts.put(row.getMotivo(), row.getTotal() == null ? 0L : row.getTotal());
        }
        return counts;
    }

    private String normalizeRequired(String raw, String fieldName, int maxLength) {
        if (raw == null) {
            throw new IllegalArgumentException(fieldName + " es obligatorio");
        }
        if (raw.indexOf('\u0000') >= 0) {
            throw new IllegalArgumentException(fieldName + " contiene null bytes no permitidos");
        }
        String normalized = raw.replaceAll("[\\p{Cntrl}&&[^\\r\\n\\t]]", " ").trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " es obligatorio");
        }
        if (normalized.length() > maxLength) {
            throw new IllegalArgumentException(fieldName + " supera el maximo de " + maxLength + " caracteres");
        }
        return normalized;
    }

    private String trimToNullable(String raw, int maxLength) {
        if (raw == null) {
            return null;
        }
        String normalized = raw.replaceAll("[\\p{Cntrl}&&[^\\r\\n\\t]]", " ").trim();
        if (normalized.isEmpty()) {
            return null;
        }
        return normalized.length() <= maxLength ? normalized : normalized.substring(0, maxLength);
    }

    private String resolveDisplayName(UsuarioEntity usuario, String fallback) {
        if (usuario != null) {
            String fullName = joinName(usuario.getNombre(), usuario.getApellido());
            if (fullName != null) {
                return trimToNullable(fullName, MAX_NOMBRE_LENGTH);
            }
        }
        return trimToNullable(fallback, MAX_NOMBRE_LENGTH);
    }

    private String joinName(String nombre, String apellido) {
        String left = nombre == null ? "" : nombre.trim();
        String right = apellido == null ? "" : apellido.trim();
        String full = (left + " " + right).trim();
        return full.isEmpty() ? null : full;
    }

    private void ensureReportBlock(UsuarioEntity blocker, UsuarioEntity blocked) {
        if (blocker == null || blocked == null || blocker.getId() == null || blocked.getId() == null) {
            return;
        }
        blocker.getBloqueados().add(blocked);
        usuarioRepository.save(blocker);
        UserBlockRelationEntity row = userBlockRelationRepository
                .findByBlocker_IdAndBlocked_Id(blocker.getId(), blocked.getId())
                .orElse(null);
        if (row == null) {
            row = new UserBlockRelationEntity();
            row.setBlocker(blocker);
            row.setBlocked(blocked);
        }
        row.setSource(BlockSource.REPORT);
        userBlockRelationRepository.save(row);
    }
}
