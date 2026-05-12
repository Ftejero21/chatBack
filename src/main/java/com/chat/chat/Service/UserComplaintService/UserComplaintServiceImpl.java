package com.chat.chat.Service.UserComplaintService;

import com.chat.chat.DTO.UserComplaintCreateDTO;
import com.chat.chat.DTO.UserComplaintDTO;
import com.chat.chat.DTO.UserComplaintEstadoUpdateDTO;
import com.chat.chat.DTO.UserComplaintHistoryItemDTO;
import com.chat.chat.DTO.UserComplaintStatsDTO;
import com.chat.chat.DTO.UserComplaintWsDTO;
import com.chat.chat.DTO.UserExpedienteDTO;
import com.chat.chat.DTO.UserModerationHistoryItemDTO;
import com.chat.chat.Entity.ChatIndividualEntity;
import com.chat.chat.Entity.UserBlockRelationEntity;
import com.chat.chat.Entity.UserComplaintEntity;
import com.chat.chat.Entity.UserComplaintHistoryEntity;
import com.chat.chat.Entity.UserModerationHistoryEntity;
import com.chat.chat.Entity.UsuarioEntity;
import com.chat.chat.Exceptions.RecursoNoEncontradoException;
import com.chat.chat.Mapper.UserComplaintMapper;
import com.chat.chat.Repository.ChatIndividualRepository;
import com.chat.chat.Repository.UserBlockRelationRepository;
import com.chat.chat.Repository.UserComplaintHistoryRepository;
import com.chat.chat.Repository.UserComplaintRepository;
import com.chat.chat.Repository.UserModerationHistoryRepository;
import com.chat.chat.Repository.UsuarioRepository;
import com.chat.chat.Security.HttpRateLimitService;
import com.chat.chat.Utils.BlockSource;
import com.chat.chat.Utils.Constantes;
import com.chat.chat.Utils.SecurityUtils;
import com.chat.chat.Utils.UserComplaintEstado;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class UserComplaintServiceImpl implements UserComplaintService {
    private static final Logger LOGGER = LoggerFactory.getLogger(UserComplaintServiceImpl.class);

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 8;
    private static final int MAX_SIZE = 100;
    private static final int MAX_MOTIVO_LENGTH = 120;
    private static final int MAX_NOMBRE_LENGTH = 190;
    private static final int MAX_DETALLE_LENGTH = 10000;
    private static final int MAX_RESOLUTION_LENGTH = 1000;
    private static final String EVENT_CREATED = "USER_COMPLAINT_CREATED";
    private static final String EVENT_UPDATED = "USER_COMPLAINT_UPDATED";
    private static final String ACTION_CREACION = "CREACION";
    private static final String ACTION_CAMBIO_ESTADO = "CAMBIO_ESTADO";
    private static final String CREATION_RESOLUTION = "Denuncia registrada y pendiente de revision por administracion.";

    private final UserComplaintRepository userComplaintRepository;
    private final UserComplaintHistoryRepository userComplaintHistoryRepository;
    private final UserModerationHistoryRepository userModerationHistoryRepository;
    private final UserBlockRelationRepository userBlockRelationRepository;
    private final UsuarioRepository usuarioRepository;
    private final ChatIndividualRepository chatIndividualRepository;
    private final SecurityUtils securityUtils;
    private final HttpRateLimitService httpRateLimitService;
    private final UserComplaintMapper userComplaintMapper;
    private final SimpMessagingTemplate messagingTemplate;

    public UserComplaintServiceImpl(UserComplaintRepository userComplaintRepository,
                                    UserComplaintHistoryRepository userComplaintHistoryRepository,
                                    UserModerationHistoryRepository userModerationHistoryRepository,
                                    UserBlockRelationRepository userBlockRelationRepository,
                                    UsuarioRepository usuarioRepository,
                                    ChatIndividualRepository chatIndividualRepository,
                                    SecurityUtils securityUtils,
                                    HttpRateLimitService httpRateLimitService,
                                    UserComplaintMapper userComplaintMapper,
                                    SimpMessagingTemplate messagingTemplate) {
        this.userComplaintRepository = userComplaintRepository;
        this.userComplaintHistoryRepository = userComplaintHistoryRepository;
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
        entity.setDetalle(normalizeRequired(request.getDetalle(), "detalle", MAX_DETALLE_LENGTH));
        entity.setEstado(UserComplaintEstado.PENDIENTE);
        entity.setLeida(false);
        entity.setLeidaAt(null);
        entity.setDenuncianteNombre(resolveDisplayName(denunciante, null));
        entity.setDenunciadoNombre(resolveDisplayName(denunciado, request.getDenunciadoNombre()));
        entity.setChatNombreSnapshot(trimToNullable(request.getChatNombreSnapshot(), MAX_NOMBRE_LENGTH));

        UserComplaintEntity saved = userComplaintRepository.save(entity);
        registrarHistorialCreacion(saved);
        ensureReportBlock(denunciante, denunciado);
        publishWsEvent(EVENT_CREATED, saved);
        List<UserComplaintHistoryEntity> persistedHistory = userComplaintHistoryRepository
                .findByComplaintIdOrderByCreatedAtAsc(saved.getId());
        return userComplaintMapper.toDto(saved, buildHistoryTimeline(saved, persistedHistory));
    }

    @Override
    public Page<UserComplaintDTO> listComplaints(int page, int size) {
        int safePage = Math.max(DEFAULT_PAGE, page);
        int requestedSize = size <= 0 ? DEFAULT_SIZE : size;
        int safeSize = Math.min(MAX_SIZE, requestedSize);
        Pageable pageable = PageRequest.of(safePage, safeSize);
        Page<UserComplaintEntity> result = userComplaintRepository.findAllByOrderByCreatedAtDescIdDesc(pageable);
        Map<Long, List<UserComplaintHistoryEntity>> historyByComplaintId = loadHistoryByComplaintId(result.getContent());
        List<UserComplaintDTO> content = result.getContent().stream()
                .map(entity -> userComplaintMapper.toDto(
                        entity,
                        buildHistoryTimeline(entity, historyByComplaintId.getOrDefault(entity.getId(), List.of()))
                ))
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
    @Transactional
    public UserComplaintDTO updateStatus(Long id, UserComplaintEstadoUpdateDTO request, HttpServletRequest httpRequest) {
        if (request == null || request.getEstado() == null || request.getEstado().isBlank()) {
            throw new IllegalArgumentException("estado es obligatorio");
        }

        UserComplaintEntity entity = userComplaintRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Denuncia no encontrada"));

        UserComplaintEstado newEstado = parseEstadoRequired(request.getEstado());
        UserComplaintEstado oldEstado = entity.getEstado();
        Long adminId = securityUtils.getAuthenticatedUserId();
        String requestId = resolveRequestId(httpRequest);

        LOGGER.info("[COMPLAINT_STATUS] requestId={} denunciaId={} fromEstado={} toEstado={}",
                requestId, entity.getId(), oldEstado, newEstado);

        Map<Long, List<UserComplaintHistoryEntity>> currentHistoryByComplaintId = loadHistoryByComplaintId(List.of(entity));
        if (oldEstado == newEstado) {
            LOGGER.info("[COMPLAINT_STATUS] requestId={} denunciaId={} resolucionAutogenerada=false",
                    requestId, entity.getId());
            return userComplaintMapper.toDto(
                    entity,
                    buildHistoryTimeline(entity, currentHistoryByComplaintId.getOrDefault(entity.getId(), List.of()))
            );
        }

        validateTransition(oldEstado, newEstado);

        boolean resolucionAutogenerada = false;
        String resolucionMotivo = normalizeOptionalReason(request.getResolucionMotivo(), "resolucionMotivo", MAX_RESOLUTION_LENGTH);
        if (isBlank(resolucionMotivo)) {
            resolucionMotivo = buildFallbackResolution(newEstado);
            resolucionAutogenerada = true;
        }

        LOGGER.info("[COMPLAINT_STATUS] requestId={} denunciaId={} resolucionAutogenerada={}",
                requestId, entity.getId(), resolucionAutogenerada);

        entity.setEstado(newEstado);
        entity.setReviewedByAdminId(adminId);
        UserComplaintEntity saved = userComplaintRepository.save(entity);

        registrarHistorialCambioEstado(requestId, saved, oldEstado, newEstado, adminId, resolucionMotivo);
        publishWsEvent(EVENT_UPDATED, saved);

        List<UserComplaintHistoryEntity> persistedHistory = userComplaintHistoryRepository
                .findByComplaintIdOrderByCreatedAtAsc(saved.getId());
        return userComplaintMapper.toDto(saved, buildHistoryTimeline(saved, persistedHistory));
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

    private void registrarHistorialCreacion(UserComplaintEntity entity) {
        if (entity == null || entity.getId() == null) {
            return;
        }
        try {
            UserComplaintHistoryEntity history = new UserComplaintHistoryEntity();
            history.setComplaintId(entity.getId());
            history.setEstadoAnterior(null);
            history.setEstadoNuevo(UserComplaintEstado.PENDIENTE);
            history.setEstadoLabel(resolveEstadoLabel(UserComplaintEstado.PENDIENTE));
            history.setMotivoSnapshot(trimToNullable(entity.getMotivo(), MAX_MOTIVO_LENGTH));
            history.setDetalleSnapshot(trimToNullable(entity.getDetalle(), MAX_DETALLE_LENGTH));
            history.setResolucionMotivo(trimToNullable(CREATION_RESOLUTION, MAX_RESOLUTION_LENGTH));
            history.setAdminId(null);
            history.setAccion(ACTION_CREACION);
            history.setCreatedAt(entity.getCreatedAt() == null ? LocalDateTime.now() : entity.getCreatedAt());
            userComplaintHistoryRepository.save(history);
            LOGGER.info("[COMPLAINT_HISTORY] complaintId={} action=CREACION created=true", entity.getId());
        } catch (RuntimeException ex) {
            LOGGER.warn("[COMPLAINT_HISTORY] complaintId={} action=CREACION created=false error={}",
                    entity.getId(), safeLogValue(ex.getMessage()));
        }
    }

    private void registrarHistorialCambioEstado(String requestId,
                                                UserComplaintEntity entity,
                                                UserComplaintEstado estadoAnterior,
                                                UserComplaintEstado estadoNuevo,
                                                Long adminId,
                                                String resolucionMotivo) {
        if (entity == null || entity.getId() == null || estadoNuevo == null) {
            return;
        }
        if (estadoAnterior == estadoNuevo) {
            LOGGER.info("[COMPLAINT_HISTORY] requestId={} denunciaId={} action=CAMBIO_ESTADO created=false motivo=same-state",
                    requestId, entity == null ? null : entity.getId());
            return;
        }
        try {
            UserComplaintHistoryEntity history = new UserComplaintHistoryEntity();
            history.setComplaintId(entity.getId());
            history.setEstadoAnterior(estadoAnterior);
            history.setEstadoNuevo(estadoNuevo);
            history.setEstadoLabel(resolveEstadoLabel(estadoNuevo));
            history.setMotivoSnapshot(trimToNullable(entity.getMotivo(), MAX_MOTIVO_LENGTH));
            history.setDetalleSnapshot(trimToNullable(entity.getDetalle(), MAX_DETALLE_LENGTH));
            history.setResolucionMotivo(trimToNullable(resolucionMotivo, MAX_RESOLUTION_LENGTH));
            history.setAdminId(adminId);
            history.setAccion(ACTION_CAMBIO_ESTADO);
            history.setCreatedAt(entity.getUpdatedAt() == null ? LocalDateTime.now() : entity.getUpdatedAt());
            UserComplaintHistoryEntity saved = userComplaintHistoryRepository.save(history);
            LOGGER.info("[COMPLAINT_HISTORY] complaintId={} action=CAMBIO_ESTADO created=true", entity.getId());
            LOGGER.info("[COMPLAINT_HISTORY] requestId={} denunciaId={} action=CAMBIO_ESTADO created=true historialId={}",
                    requestId, entity.getId(), saved.getId());
        } catch (RuntimeException ex) {
            LOGGER.warn("[COMPLAINT_HISTORY] requestId={} denunciaId={} action=CAMBIO_ESTADO created=false error={}",
                    requestId, entity == null ? null : entity.getId(), safeLogValue(ex.getMessage()));
        }
    }

    private Map<Long, List<UserComplaintHistoryEntity>> loadHistoryByComplaintId(List<UserComplaintEntity> complaints) {
        if (complaints == null || complaints.isEmpty()) {
            return Map.of();
        }
        List<Long> ids = complaints.stream()
                .map(UserComplaintEntity::getId)
                .filter(Objects::nonNull)
                .toList();
        if (ids.isEmpty()) {
            return Map.of();
        }
        Map<Long, List<UserComplaintHistoryEntity>> out = new HashMap<>();
        for (UserComplaintHistoryEntity row : userComplaintHistoryRepository.findByComplaintIdInOrderByCreatedAtAsc(ids)) {
            if (row == null || row.getComplaintId() == null) {
                continue;
            }
            out.computeIfAbsent(row.getComplaintId(), key -> new ArrayList<>()).add(row);
        }
        return out;
    }

    private List<UserComplaintHistoryItemDTO> buildHistoryTimeline(UserComplaintEntity complaint,
                                                                   List<UserComplaintHistoryEntity> persistedHistory) {
        int dbSize = persistedHistory == null ? 0 : persistedHistory.size();
        List<UserComplaintHistoryItemDTO> out = mapPersistedHistory(persistedHistory);
        boolean synthetic = ensureCompleteHistoryTimeline(complaint, out);
        out.sort(historyOrderComparator());
        LOGGER.info("[COMPLAINT_HISTORY] complaintId={} dbSize={} mappedSize={} synthetic={}",
                complaint == null ? null : complaint.getId(),
                dbSize,
                out.size(),
                synthetic);
        LOGGER.info("[COMPLAINT_HISTORY] complaintId={} order={}",
                complaint == null ? null : complaint.getId(),
                out.stream().map(UserComplaintHistoryItemDTO::getEstadoNuevo).toList());
        return out;
    }

    private List<UserComplaintHistoryItemDTO> mapPersistedHistory(List<UserComplaintHistoryEntity> persistedHistory) {
        List<UserComplaintHistoryItemDTO> out = new ArrayList<>();
        if (persistedHistory == null) {
            return out;
        }
        for (UserComplaintHistoryEntity row : persistedHistory) {
            if (row == null) {
                continue;
            }
            out.add(userComplaintMapper.toHistoryDto(row));
        }
        return out;
    }

    private boolean ensureCompleteHistoryTimeline(UserComplaintEntity complaint, List<UserComplaintHistoryItemDTO> out) {
        boolean synthetic = false;
        if (!containsEstado(out, UserComplaintEstado.PENDIENTE.name())) {
            out.add(buildSyntheticHistoryItem(
                    complaint,
                    null,
                    UserComplaintEstado.PENDIENTE,
                    CREATION_RESOLUTION,
                    complaint == null ? null : complaint.getCreatedAt(),
                    null,
                    ACTION_CREACION
            ));
            synthetic = true;
        }
        if (complaint == null || complaint.getEstado() == null) {
            return synthetic;
        }
        if (complaint.getEstado() == UserComplaintEstado.EN_REVISION
                && !containsEstado(out, UserComplaintEstado.EN_REVISION.name())) {
            out.add(buildSyntheticHistoryItem(
                    complaint,
                    UserComplaintEstado.PENDIENTE,
                    UserComplaintEstado.EN_REVISION,
                    buildFallbackResolution(UserComplaintEstado.EN_REVISION),
                    complaint.getUpdatedAt(),
                    complaint.getReviewedByAdminId(),
                    ACTION_CAMBIO_ESTADO
            ));
            return true;
        }
        if ((complaint.getEstado() == UserComplaintEstado.RESUELTA || complaint.getEstado() == UserComplaintEstado.DESCARTADA)
                && !containsEstado(out, UserComplaintEstado.EN_REVISION.name())) {
            out.add(buildSyntheticHistoryItem(
                    complaint,
                    UserComplaintEstado.PENDIENTE,
                    UserComplaintEstado.EN_REVISION,
                    buildFallbackResolution(UserComplaintEstado.EN_REVISION),
                    complaint.getCreatedAt(),
                    complaint.getReviewedByAdminId(),
                    ACTION_CAMBIO_ESTADO
            ));
            synthetic = true;
        }
        if ((complaint.getEstado() == UserComplaintEstado.RESUELTA || complaint.getEstado() == UserComplaintEstado.DESCARTADA)
                && !containsEstado(out, complaint.getEstado().name())) {
            out.add(buildSyntheticHistoryItem(
                    complaint,
                    UserComplaintEstado.EN_REVISION,
                    complaint.getEstado(),
                    buildFallbackResolution(complaint.getEstado()),
                    complaint.getUpdatedAt(),
                    complaint.getReviewedByAdminId(),
                    ACTION_CAMBIO_ESTADO
            ));
            synthetic = true;
        }
        return synthetic;
    }

    private boolean containsEstado(List<UserComplaintHistoryItemDTO> history, String estadoNuevo) {
        if (history == null || estadoNuevo == null) {
            return false;
        }
        return history.stream().anyMatch(item -> item != null && estadoNuevo.equals(item.getEstadoNuevo()));
    }

    private UserComplaintHistoryItemDTO buildSyntheticHistoryItem(UserComplaintEntity complaint,
                                                                  UserComplaintEstado estadoAnterior,
                                                                  UserComplaintEstado estadoNuevo,
                                                                  String resolucionMotivo,
                                                                  LocalDateTime fecha,
                                                                  Long adminId,
                                                                  String accion) {
        UserComplaintHistoryItemDTO dto = new UserComplaintHistoryItemDTO();
        dto.setEstadoAnterior(estadoAnterior == null ? null : estadoAnterior.name());
        dto.setEstadoNuevo(estadoNuevo == null ? null : estadoNuevo.name());
        dto.setEstadoLabel(resolveEstadoLabel(estadoNuevo));
        dto.setMotivo(complaint == null ? null : complaint.getMotivo());
        dto.setDetalle(complaint == null ? null : complaint.getDetalle());
        dto.setResolucionMotivo(resolucionMotivo);
        LocalDateTime effectiveDate = fecha;
        if (effectiveDate == null && complaint != null) {
            effectiveDate = complaint.getUpdatedAt() == null ? complaint.getCreatedAt() : complaint.getUpdatedAt();
        }
        dto.setFecha(effectiveDate == null ? null : toIsoUtc(effectiveDate));
        dto.setAdminId(adminId);
        dto.setAccion(accion);
        return dto;
    }

    private Comparator<UserComplaintHistoryItemDTO> historyOrderComparator() {
        return Comparator
                .comparingInt((UserComplaintHistoryItemDTO item) -> resolveEstadoRank(item == null ? null : item.getEstadoNuevo()))
                .thenComparing((UserComplaintHistoryItemDTO item) -> parseIsoDate(item == null ? null : item.getFecha()),
                        Comparator.nullsLast(Comparator.reverseOrder()));
    }

    private int resolveEstadoRank(String estadoNuevo) {
        if ("RESUELTA".equals(estadoNuevo) || "DESCARTADA".equals(estadoNuevo)) {
            return 0;
        }
        if ("EN_REVISION".equals(estadoNuevo)) {
            return 1;
        }
        return 2;
    }

    private LocalDateTime parseIsoDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDateTime.ofInstant(java.time.Instant.parse(value), ZoneOffset.UTC);
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private void validateTransition(UserComplaintEstado oldEstado, UserComplaintEstado newEstado) {
        if (oldEstado == null || newEstado == null) {
            throw new IllegalArgumentException("estado invalido");
        }
        boolean valid = switch (oldEstado) {
            case PENDIENTE -> newEstado == UserComplaintEstado.EN_REVISION;
            case EN_REVISION -> newEstado == UserComplaintEstado.RESUELTA || newEstado == UserComplaintEstado.DESCARTADA;
            case RESUELTA, DESCARTADA -> false;
        };
        if (!valid) {
            throw new IllegalArgumentException("Transicion de estado no permitida para la denuncia");
        }
    }

    private UserComplaintEstado parseEstadoRequired(String raw) {
        try {
            return UserComplaintEstado.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (RuntimeException ex) {
            throw new IllegalArgumentException("estado invalido. Valores permitidos: EN_REVISION, RESUELTA, DESCARTADA");
        }
    }

    private String normalizeOptionalReason(String raw, String fieldName, int maxLength) {
        if (raw == null) {
            return null;
        }
        if (raw.indexOf('\u0000') >= 0) {
            throw new IllegalArgumentException(fieldName + " contiene null bytes no permitidos");
        }
        String normalized = raw.replaceAll("[\\p{Cntrl}&&[^\\r\\n\\t]]", " ").trim();
        if (normalized.isEmpty()) {
            return null;
        }
        if (normalized.length() > maxLength) {
            throw new IllegalArgumentException(fieldName + " supera el maximo de " + maxLength + " caracteres");
        }
        return normalized;
    }

    private String buildFallbackResolution(UserComplaintEstado estado) {
        return switch (estado) {
            case EN_REVISION -> "Denuncia en revision: administracion esta analizando el caso reportado.";
            case RESUELTA -> "Denuncia resuelta: administracion ha revisado el caso y lo marca como atendido.";
            case DESCARTADA -> "Denuncia descartada: administracion ha revisado el caso y no aplicara medidas adicionales por ahora.";
            case PENDIENTE -> CREATION_RESOLUTION;
        };
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String resolveRequestId(HttpServletRequest request) {
        if (request != null) {
            String requestId = trimToNullable(request.getHeader("X-Request-Id"), 120);
            if (requestId != null) {
                return requestId;
            }
            String traceId = trimToNullable(request.getHeader("X-Trace-Id"), 120);
            if (traceId != null) {
                return traceId;
            }
        }
        return UUID.randomUUID().toString();
    }

    private String resolveEstadoLabel(UserComplaintEstado estado) {
        if (estado == null) {
            return null;
        }
        return switch (estado) {
            case PENDIENTE -> "Pendiente";
            case EN_REVISION -> "En revisión";
            case RESUELTA -> "Resuelta";
            case DESCARTADA -> "Descartada";
        };
    }

    private String safeLogValue(String value) {
        if (value == null) {
            return "-";
        }
        return value.replaceAll("[\\p{Cntrl}&&[^\\r\\n\\t]]", " ").trim();
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
