package com.chat.chat.Service.AdminAiReportService;

import com.chat.chat.Configuracion.AiProperties;
import com.chat.chat.Configuracion.DeepSeekProperties;
import com.chat.chat.DTO.AdminAiReportDataDTO;
import com.chat.chat.DTO.AdminAiReportRequestDTO;
import com.chat.chat.DTO.AdminConflictiveUserDTO;
import com.chat.chat.DTO.AdminGroupMetricDTO;
import com.chat.chat.DTO.AdminBannedUserDTO;
import com.chat.chat.DTO.AdminModerationActionMetricDTO;
import com.chat.chat.DTO.AdminModerationReasonMetricDTO;
import com.chat.chat.DTO.AdminReasonMetricDTO;
import com.chat.chat.DTO.AdminReporterUserDTO;
import com.chat.chat.DTO.AdminReportStatusMetricDTO;
import com.chat.chat.DTO.AdminReportTypeMetricDTO;
import com.chat.chat.Entity.UsuarioEntity;
import com.chat.chat.Entity.UserModerationHistoryEntity;
import com.chat.chat.Exceptions.SemanticApiException;
import com.chat.chat.Repository.ChatGrupalRepository;
import com.chat.chat.Repository.ChatIndividualRepository;
import com.chat.chat.Repository.EncuestaRepository;
import com.chat.chat.Repository.MensajeRepository;
import com.chat.chat.Repository.SolicitudDesbaneoRepository;
import com.chat.chat.Repository.UserComplaintRepository;
import com.chat.chat.Repository.UserModerationHistoryRepository;
import com.chat.chat.Repository.UsuarioRepository;
import com.chat.chat.Service.AiService.DeepSeekApiClient;
import com.chat.chat.Service.AdminReportPdfService.AdminReportPdfService;
import com.chat.chat.Utils.Constantes;
import com.chat.chat.Utils.ModerationActionType;
import com.chat.chat.Utils.ReporteTipo;
import com.chat.chat.Utils.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.util.HtmlUtils;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Validated
public class AdminAiReportServiceImpl implements AdminAiReportService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AdminAiReportServiceImpl.class);
    private static final int MAX_PERIOD_DAYS = 366;
    private static final int MAX_LIST_ITEMS = 10;
    private static final String CODE_OK = "ADMIN_AI_REPORT_OK";
    private static final String CODE_ERROR = "ADMIN_AI_REPORT_ERROR";
    private static final String CODE_NO_DATA = "ADMIN_AI_REPORT_NO_DATA";
    private static final String REPORT_TITLE = "Reporte administrativo de TejeChat";
    private static final DateTimeFormatter DISPLAY_DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final UsuarioRepository usuarioRepository;
    private final UserComplaintRepository userComplaintRepository;
    private final SolicitudDesbaneoRepository solicitudDesbaneoRepository;
    private final UserModerationHistoryRepository userModerationHistoryRepository;
    private final ChatGrupalRepository chatGrupalRepository;
    private final ChatIndividualRepository chatIndividualRepository;
    private final MensajeRepository mensajeRepository;
    private final EncuestaRepository encuestaRepository;
    private final AiProperties aiProperties;
    private final DeepSeekProperties deepSeekProperties;
    private final DeepSeekApiClient deepSeekApiClient;
    private final AdminReportPdfService adminReportPdfService;
    private final SecurityUtils securityUtils;

    public AdminAiReportServiceImpl(UsuarioRepository usuarioRepository,
                                    UserComplaintRepository userComplaintRepository,
                                    SolicitudDesbaneoRepository solicitudDesbaneoRepository,
                                    UserModerationHistoryRepository userModerationHistoryRepository,
                                    ChatGrupalRepository chatGrupalRepository,
                                    ChatIndividualRepository chatIndividualRepository,
                                    MensajeRepository mensajeRepository,
                                    EncuestaRepository encuestaRepository,
                                    AiProperties aiProperties,
                                    DeepSeekProperties deepSeekProperties,
                                    DeepSeekApiClient deepSeekApiClient,
                                    AdminReportPdfService adminReportPdfService,
                                    SecurityUtils securityUtils) {
        this.usuarioRepository = usuarioRepository;
        this.userComplaintRepository = userComplaintRepository;
        this.solicitudDesbaneoRepository = solicitudDesbaneoRepository;
        this.userModerationHistoryRepository = userModerationHistoryRepository;
        this.chatGrupalRepository = chatGrupalRepository;
        this.chatIndividualRepository = chatIndividualRepository;
        this.mensajeRepository = mensajeRepository;
        this.encuestaRepository = encuestaRepository;
        this.aiProperties = aiProperties;
        this.deepSeekProperties = deepSeekProperties;
        this.deepSeekApiClient = deepSeekApiClient;
        this.adminReportPdfService = adminReportPdfService;
        this.securityUtils = securityUtils;
    }

    @Override
    @Transactional(readOnly = true)
    public AdminAiReportDataDTO recopilarDatosReporte(LocalDate fechaInicio, LocalDate fechaFin) {
        ensureAdminAccess();
        AdminAiReportRequestDTO request = normalizeRequest(fechaInicio, fechaFin);
        LocalDateTime inicio = request.getFechaInicio().atStartOfDay();
        LocalDateTime fin = request.getFechaFin().plusDays(1).atStartOfDay();

        AdminAiReportDataDTO data = new AdminAiReportDataDTO();
        data.setFechaInicio(request.getFechaInicio());
        data.setFechaFin(request.getFechaFin());
        data.setTotalUsuarios(usuarioRepository.count());
        data.setUsuariosNuevos(usuarioRepository.countUsuariosRegistradosEntreFechas(inicio, fin));
        data.setUsuariosActivos(usuarioRepository.countByActivoTrue());
        data.setUsuariosInactivos(usuarioRepository.countByActivoFalse());
        data.setUsuariosActualmenteBaneados(resolveUsuariosActualmenteBaneados());
        data.setUsuariosBaneados(data.getUsuariosActualmenteBaneados().size());
        data.setBaneosRealizados(userModerationHistoryRepository.countByActionTypeAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                ModerationActionType.SUSPENSION, inicio, fin));
        data.setDesbaneosRealizados(userModerationHistoryRepository.countByActionTypeAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                ModerationActionType.UNBAN, inicio, fin));
        data.setTotalDenuncias(userComplaintRepository.countByCreatedAtGreaterThanEqualAndCreatedAtLessThan(inicio, fin));
        data.setTotalReportes(solicitudDesbaneoRepository.countByCreatedAtGreaterThanEqualAndCreatedAtLessThan(inicio, fin));
        data.setGruposCreados(chatGrupalRepository.countByFechaCreacionGreaterThanEqualAndFechaCreacionLessThan(inicio, fin));
        data.setGruposActivos(chatGrupalRepository.countByActivoTrueAndClosedFalse());
        data.setChatsIndividualesActivos(chatIndividualRepository.countActiveRegularChatsByMessagePeriod(inicio, fin));
        data.setMensajesEnviados(mensajeRepository.countUserMensajesEntreFechas(inicio, fin));
        data.setEncuestasCreadas(encuestaRepository.countByCreatedAtGreaterThanEqualAndCreatedAtLessThan(inicio, fin));
        data.setReportesPorTipo(resolveReportesPorTipo(inicio, fin));
        data.setReportesPorEstado(resolveReportesPorEstado(inicio, fin));
        data.setModeracionPorAccion(resolveModeracionPorAccion(inicio, fin));
        data.setMotivosModeracion(resolveMotivosModeracion(inicio, fin));
        data.setMotivosDenuncia(resolveMotivosDenuncia(inicio, fin));
        data.setUsuariosConflictivos(resolveUsuariosConflictivos(inicio, fin));
        data.setUsuariosQueMasDenuncian(resolveUsuariosQueMasDenuncian(inicio, fin));
        data.setGruposMasActivos(resolveGruposMasActivos(inicio, fin));
        data.setAlertas(buildAlertas(data));
        return data;
    }

    @Override
    public String generarReporteConIa(AdminAiReportDataDTO data) {
        ensureAdminAccess();
        if (data == null) {
            throw new IllegalArgumentException("data es obligatoria");
        }
        if (hasNoPeriodData(data)) {
            return buildFallbackReport(data, CODE_NO_DATA);
        }
        if (!canUseAi()) {
            LOGGER.warn("[ADMIN_AI_REPORT] ai-disabled-or-misconfigured start={} end={}",
                    data.getFechaInicio(), data.getFechaFin());
            return buildFallbackReport(data, CODE_ERROR);
        }
        try {
            LOGGER.info("[ADMIN_AI_REPORT] ai-request start={} end={} denuncias={} reportes={} mensajes={}",
                    data.getFechaInicio(),
                    data.getFechaFin(),
                    data.getTotalDenuncias(),
                    data.getTotalReportes(),
                    data.getMensajesEnviados());
            String response = deepSeekApiClient.completarTextoAdminReport(
                    buildSystemPrompt(),
                    buildUserContent(data),
                    deepSeekProperties.getAdminReportMaxOutputTokens()
            );
            if (response == null || response.isBlank()) {
                return buildFallbackReport(data, CODE_ERROR);
            }
            return response.trim();
        } catch (SemanticApiException ex) {
            LOGGER.warn("[ADMIN_AI_REPORT] ai-provider-error start={} end={} code={} status={}",
                    data.getFechaInicio(),
                    data.getFechaFin(),
                    ex.getCode(),
                    ex.getStatus().value());
            return buildFallbackReport(data, CODE_ERROR);
        } catch (Exception ex) {
            LOGGER.error("[ADMIN_AI_REPORT] ai-unexpected-error start={} end={} type={}",
                    data.getFechaInicio(),
                    data.getFechaFin(),
                    ex.getClass().getSimpleName());
            return buildFallbackReport(data, CODE_ERROR);
        }
    }

    @Override
    public byte[] generarArchivoReporte(String reporte, AdminAiReportDataDTO data) {
        ensureAdminAccess();
        String html = buildHtmlDocument(reporte, data);
        return adminReportPdfService.convertHtmlToPdf(html);
    }

    @Override
    public ResponseEntity<byte[]> descargarReporteIa(LocalDate fechaInicio, LocalDate fechaFin) {
        AdminAiReportDataDTO data = recopilarDatosReporte(fechaInicio, fechaFin);
        String reporte = generarReporteConIa(data);
        byte[] bytes = generarArchivoReporte(reporte, data);
        String filename = buildFilename(data);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(ContentDisposition.attachment().filename(filename, StandardCharsets.UTF_8).build());
        headers.setContentLength(bytes.length);
        headers.setCacheControl("no-store, no-cache, must-revalidate");
        headers.add("X-Admin-AI-Report-Code", resolveHeaderCode(reporte));
        return ResponseEntity.ok().headers(headers).body(bytes);
    }

    private List<AdminReasonMetricDTO> resolveMotivosDenuncia(LocalDateTime inicio, LocalDateTime fin) {
        return userComplaintRepository.countGroupedByMotivoAndCreatedAtBetween(inicio, fin, PageRequest.of(0, MAX_LIST_ITEMS))
                .stream()
                .map(row -> {
                    AdminReasonMetricDTO dto = new AdminReasonMetricDTO();
                    dto.setMotivo(safeText(row.getMotivo(), 120));
                    dto.setTotal(defaultLong(row.getTotal()));
                    return dto;
                })
                .filter(dto -> dto.getMotivo() != null && dto.getTotal() > 0)
                .collect(Collectors.toList());
    }

    private List<AdminReportTypeMetricDTO> resolveReportesPorTipo(LocalDateTime inicio, LocalDateTime fin) {
        return solicitudDesbaneoRepository.countByTipoReporteGroupedByCreatedAtBetween(inicio, fin)
                .stream()
                .map(row -> {
                    AdminReportTypeMetricDTO dto = new AdminReportTypeMetricDTO();
                    dto.setTipo(row.getTipoReporte() == null ? "NO_DISPONIBLE" : row.getTipoReporte().name());
                    dto.setTotal(defaultLong(row.getTotal()));
                    return dto;
                })
                .collect(Collectors.toList());
    }

    private List<AdminReportStatusMetricDTO> resolveReportesPorEstado(LocalDateTime inicio, LocalDateTime fin) {
        return solicitudDesbaneoRepository.countByEstadoGroupedByCreatedAtBetween(inicio, fin)
                .stream()
                .map(row -> {
                    AdminReportStatusMetricDTO dto = new AdminReportStatusMetricDTO();
                    dto.setEstado(row.getEstado() == null ? "NO_DISPONIBLE" : row.getEstado().name());
                    dto.setTotal(defaultLong(row.getTotal()));
                    return dto;
                })
                .collect(Collectors.toList());
    }

    private List<AdminModerationActionMetricDTO> resolveModeracionPorAccion(LocalDateTime inicio, LocalDateTime fin) {
        return userModerationHistoryRepository.countByActionTypeGroupedByCreatedAtBetween(inicio, fin)
                .stream()
                .map(row -> {
                    AdminModerationActionMetricDTO dto = new AdminModerationActionMetricDTO();
                    dto.setAccion(row.getActionType() == null ? "NO_DISPONIBLE" : row.getActionType().name());
                    dto.setTotal(defaultLong(row.getTotal()));
                    return dto;
                })
                .collect(Collectors.toList());
    }

    private List<AdminModerationReasonMetricDTO> resolveMotivosModeracion(LocalDateTime inicio, LocalDateTime fin) {
        return userModerationHistoryRepository.countByReasonGroupedByCreatedAtBetween(inicio, fin, PageRequest.of(0, MAX_LIST_ITEMS))
                .stream()
                .map(row -> {
                    AdminModerationReasonMetricDTO dto = new AdminModerationReasonMetricDTO();
                    dto.setMotivo(safeText(row.getReason(), 190));
                    dto.setTotal(defaultLong(row.getTotal()));
                    return dto;
                })
                .filter(row -> row.getMotivo() != null)
                .collect(Collectors.toList());
    }

    private List<AdminBannedUserDTO> resolveUsuariosActualmenteBaneados() {
        List<UsuarioEntity> inactiveUsers = usuarioRepository.findAll().stream()
                .filter(user -> user != null && !user.isActivo())
                .collect(Collectors.toList());
        List<AdminBannedUserDTO> result = new ArrayList<>();
        for (UsuarioEntity user : inactiveUsers) {
            UserModerationHistoryEntity latest = userModerationHistoryRepository
                    .findFirstByUser_IdOrderByCreatedAtDescIdDesc(user.getId())
                    .orElse(null);
            if (latest == null || latest.getActionType() != ModerationActionType.SUSPENSION) {
                continue;
            }
            AdminBannedUserDTO dto = new AdminBannedUserDTO();
            dto.setUsuarioId(user.getId());
            dto.setNombre(resolveDisplayName(user, user.getId()));
            dto.setEmail(safeText(user.getEmail(), 190));
            dto.setMotivo(safeText(latest.getReason(), 190));
            result.add(dto);
        }
        return result;
    }

    private List<AdminConflictiveUserDTO> resolveUsuariosConflictivos(LocalDateTime inicio, LocalDateTime fin) {
        List<UserComplaintRepository.DenunciadoComplaintCountView> rows =
                userComplaintRepository.findTopDenunciadosByCreatedAtBetween(inicio, fin, PageRequest.of(0, MAX_LIST_ITEMS));
        if (rows.isEmpty()) {
            return List.of();
        }
        List<Long> userIds = rows.stream()
                .map(UserComplaintRepository.DenunciadoComplaintCountView::getDenunciadoId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        List<UsuarioEntity> users = usuarioRepository.findAllById(userIds);
        java.util.Map<Long, UsuarioEntity> usersById = users.stream()
                .collect(Collectors.toMap(UsuarioEntity::getId, u -> u));

        List<AdminConflictiveUserDTO> result = new ArrayList<>();
        for (UserComplaintRepository.DenunciadoComplaintCountView row : rows) {
            if (row == null || row.getDenunciadoId() == null) {
                continue;
            }
            UsuarioEntity usuario = usersById.get(row.getDenunciadoId());
            AdminConflictiveUserDTO dto = new AdminConflictiveUserDTO();
            dto.setUsuarioId(row.getDenunciadoId());
            dto.setNombre(resolveDisplayName(usuario, row.getDenunciadoId()));
            dto.setEmail(usuario == null ? null : safeText(usuario.getEmail(), 190));
            dto.setDenunciasRecibidas(defaultLong(row.getTotal()));
            dto.setReportesRecibidos(0L);
            dto.setMotivoPrincipal(resolveMotivoPrincipal(row.getDenunciadoId(), inicio, fin));
            result.add(dto);
        }
        return result;
    }

    private List<AdminReporterUserDTO> resolveUsuariosQueMasDenuncian(LocalDateTime inicio, LocalDateTime fin) {
        List<UserComplaintRepository.DenuncianteComplaintCountView> rows =
                userComplaintRepository.findTopDenunciantesByCreatedAtBetween(inicio, fin, PageRequest.of(0, MAX_LIST_ITEMS));
        if (rows.isEmpty()) {
            return List.of();
        }
        List<Long> userIds = rows.stream()
                .map(UserComplaintRepository.DenuncianteComplaintCountView::getDenuncianteId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        List<UsuarioEntity> users = usuarioRepository.findAllById(userIds);
        java.util.Map<Long, UsuarioEntity> usersById = users.stream()
                .collect(Collectors.toMap(UsuarioEntity::getId, u -> u));

        List<AdminReporterUserDTO> result = new ArrayList<>();
        for (UserComplaintRepository.DenuncianteComplaintCountView row : rows) {
            if (row == null || row.getDenuncianteId() == null) {
                continue;
            }
            UsuarioEntity usuario = usersById.get(row.getDenuncianteId());
            AdminReporterUserDTO dto = new AdminReporterUserDTO();
            dto.setUsuarioId(row.getDenuncianteId());
            dto.setNombre(resolveDisplayName(usuario, row.getDenuncianteId()));
            dto.setDenunciasRealizadas(defaultLong(row.getTotal()));
            result.add(dto);
        }
        return result;
    }

    private List<AdminGroupMetricDTO> resolveGruposMasActivos(LocalDateTime inicio, LocalDateTime fin) {
        return chatGrupalRepository.findTopActiveGroupsByPeriod(inicio, fin, PageRequest.of(0, MAX_LIST_ITEMS))
                .stream()
                .map(row -> {
                    AdminGroupMetricDTO dto = new AdminGroupMetricDTO();
                    dto.setGrupoId(row.getGrupoId());
                    dto.setNombreGrupo(safeText(row.getNombreGrupo(), 190));
                    dto.setTotalMensajes(defaultLong(row.getTotalMensajes()));
                    dto.setTotalMiembros(defaultLong(row.getTotalMiembros()));
                    return dto;
                })
                .collect(Collectors.toList());
    }

    private List<String> buildAlertas(AdminAiReportDataDTO data) {
        List<String> alertas = new ArrayList<>();
        if (data.getTotalDenuncias() == 0 && data.getTotalReportes() == 0) {
            alertas.add("No se registraron denuncias ni reportes administrativos en el periodo.");
        }
        if (!data.getMotivosDenuncia().isEmpty()) {
            AdminReasonMetricDTO top = data.getMotivosDenuncia().get(0);
            alertas.add("Motivo de denuncia mas frecuente: " + safeText(top.getMotivo(), 120) + " (" + top.getTotal() + ").");
        }
        if (!data.getUsuariosConflictivos().isEmpty()) {
            AdminConflictiveUserDTO top = data.getUsuariosConflictivos().get(0);
            alertas.add("Usuario a revisar con mas denuncias recibidas: " + top.getNombre()
                    + " [ID " + top.getUsuarioId() + "] (" + top.getDenunciasRecibidas() + ").");
        }
        if (!data.getUsuariosQueMasDenuncian().isEmpty()) {
            AdminReporterUserDTO top = data.getUsuariosQueMasDenuncian().get(0);
            alertas.add("Usuario con mayor volumen de denuncias emitidas: " + top.getNombre()
                    + " [ID " + top.getUsuarioId() + "] (" + top.getDenunciasRealizadas() + ").");
        }
        if (!data.getGruposMasActivos().isEmpty()) {
            AdminGroupMetricDTO top = data.getGruposMasActivos().get(0);
            alertas.add("Grupo mas activo por mensajes visibles en el periodo: " + safeText(top.getNombreGrupo(), 190)
                    + " [ID " + top.getGrupoId() + "] (" + top.getTotalMensajes() + ").");
        }
        if (!data.getReportesPorTipo().isEmpty()) {
            AdminReportTypeMetricDTO topTipo = data.getReportesPorTipo().get(0);
            alertas.add("Tipo de reporte administrativo mas frecuente: " + safeText(topTipo.getTipo(), 80)
                    + " (" + topTipo.getTotal() + ").");
        }
        if (!data.getReportesPorEstado().isEmpty()) {
            AdminReportStatusMetricDTO topEstado = data.getReportesPorEstado().get(0);
            alertas.add("Estado mas frecuente dentro de reportes creados en el periodo: " + safeText(topEstado.getEstado(), 80)
                    + " (" + topEstado.getTotal() + ").");
        }
        if (data.getUsuariosBaneados() > 0) {
            alertas.add("Usuarios actualmente baneados identificados por historial de moderacion y estado inactivo: " + data.getUsuariosBaneados() + ".");
        }
        if (!data.getMotivosModeracion().isEmpty()) {
            AdminModerationReasonMetricDTO topMotivoModeracion = data.getMotivosModeracion().get(0);
            alertas.add("Motivo de moderacion mas frecuente: " + safeText(topMotivoModeracion.getMotivo(), 190)
                    + " (" + topMotivoModeracion.getTotal() + ").");
        }
        if (data.getTotalReportes() > 0) {
            alertas.add("Los reportes administrativos se contabilizan de forma agregada; no se atribuyen a usuarios conflictivos para evitar inferencias no verificadas.");
        }
        return alertas.stream()
                .filter(text -> text != null && !text.isBlank())
                .distinct()
                .limit(MAX_LIST_ITEMS)
                .collect(Collectors.toList());
    }

    private String resolveMotivoPrincipal(Long userId, LocalDateTime inicio, LocalDateTime fin) {
        return userComplaintRepository.countReceivedGroupedByMotivoAndCreatedAtBetween(userId, inicio, fin, PageRequest.of(0, 1))
                .stream()
                .findFirst()
                .map(UserComplaintRepository.ComplaintMotivoCountView::getMotivo)
                .map(value -> safeText(value, 120))
                .orElse("No disponible");
    }

    private AdminAiReportRequestDTO normalizeRequest(LocalDate fechaInicio, LocalDate fechaFin) {
        LocalDate today = LocalDate.now();
        AdminAiReportRequestDTO request = new AdminAiReportRequestDTO();
        if ((fechaInicio == null) != (fechaFin == null)) {
            throw new IllegalArgumentException("fechaInicio y fechaFin deben enviarse juntas o ambas omitirse");
        }
        if (fechaInicio == null) {
            request.setFechaFin(today);
            request.setFechaInicio(today.withDayOfMonth(1));
        } else {
            request.setFechaInicio(fechaInicio);
            request.setFechaFin(fechaFin);
        }
        if (request.getFechaFin().isBefore(request.getFechaInicio())) {
            throw new IllegalArgumentException("fechaFin no puede ser anterior a fechaInicio");
        }
        if (request.getFechaFin().isAfter(today) || request.getFechaInicio().isAfter(today)) {
            throw new IllegalArgumentException("Las fechas no pueden estar en el futuro");
        }
        long days = ChronoUnit.DAYS.between(request.getFechaInicio(), request.getFechaFin()) + 1L;
        if (days > MAX_PERIOD_DAYS) {
            throw new IllegalArgumentException("El rango maximo permitido es de " + MAX_PERIOD_DAYS + " dias");
        }
        return request;
    }

    private void ensureAdminAccess() {
        Long userId = securityUtils.getAuthenticatedUserId();
        boolean admin = securityUtils.hasRole(Constantes.ADMIN)
                || securityUtils.hasRole(Constantes.ROLE_ADMIN)
                || usuarioRepository.findById(userId).map(u -> hasAdminRole(u.getRoles())).orElse(false);
        if (!admin) {
            throw new SemanticApiException(HttpStatus.FORBIDDEN, "ADMIN_AI_REPORT_FORBIDDEN",
                    Constantes.MSG_SOLO_ADMIN, null);
        }
    }

    private boolean hasAdminRole(Set<String> roles) {
        if (roles == null || roles.isEmpty()) {
            return false;
        }
        return roles.stream().anyMatch(role ->
                Constantes.ADMIN.equalsIgnoreCase(role) || Constantes.ROLE_ADMIN.equalsIgnoreCase(role));
    }

    private boolean canUseAi() {
        return aiProperties.isEnabled()
                && "deepseek".equalsIgnoreCase(aiProperties.getProvider())
                && deepSeekProperties.getApiKey() != null
                && !deepSeekProperties.getApiKey().isBlank();
    }

    private boolean hasNoPeriodData(AdminAiReportDataDTO data) {
        return data.getUsuariosNuevos() == 0
                && data.getTotalDenuncias() == 0
                && data.getTotalReportes() == 0
                && data.getGruposCreados() == 0
                && data.getChatsIndividualesActivos() == 0
                && data.getMensajesEnviados() == 0
                && data.getEncuestasCreadas() == 0;
    }

    private String buildSystemPrompt() {
        return "Genera un reporte administrativo profesional para TejeChat usando exclusivamente los datos proporcionados. "
                + "No inventes cifras ni eventos. Si un dato aparece como 0 o no disponible, indicalo con prudencia o no lo destaques. "
                + "El reporte debe ser claro, ejecutivo y util para administracion.\n\n"
                + "Estructura obligatoria:\n\n"
                + "# Reporte administrativo de TejeChat\n"
                + "Periodo analizado: <fechaInicio> - <fechaFin>\n\n"
                + "## 1. Resumen ejecutivo\n"
                + "## 2. Actividad de usuarios\n"
                + "## 3. Denuncias y reportes\n"
                + "## 4. Usuarios mas conflictivos\n"
                + "## 5. Actividad en grupos y chats\n"
                + "## 6. Riesgos detectados\n"
                + "## 7. Recomendaciones administrativas\n"
                + "## 8. Conclusion\n\n"
                + "Reglas:\n"
                + "- No inventes datos.\n"
                + "- No acuses de forma definitiva a usuarios.\n"
                + "- Usa lenguaje prudente: posible, conviene revisar, segun los datos disponibles.\n"
                + "- No incluyas informacion privada innecesaria.\n"
                + "- No incluyas mensajes privados completos.\n"
                + "- No uses tono informal.\n"
                + "- No añadas explicaciones fuera del reporte.";
    }

    private String buildUserContent(AdminAiReportDataDTO data) {
        StringBuilder builder = new StringBuilder();
        builder.append("Periodo: ").append(data.getFechaInicio()).append(" a ").append(data.getFechaFin()).append('\n');
        builder.append("Metricas globales:\n");
        builder.append("- totalUsuarios: ").append(data.getTotalUsuarios()).append('\n');
        builder.append("- usuariosNuevos: ").append(data.getUsuariosNuevos()).append('\n');
        builder.append("- usuariosActivos: ").append(data.getUsuariosActivos()).append('\n');
        builder.append("- usuariosInactivos: ").append(data.getUsuariosInactivos()).append('\n');
        builder.append("- usuariosBaneados: ").append(formatMetricValue(data.getUsuariosBaneados())).append('\n');
        builder.append("- baneosRealizados: ").append(data.getBaneosRealizados()).append('\n');
        builder.append("- desbaneosRealizados: ").append(data.getDesbaneosRealizados()).append('\n');
        builder.append("- totalDenuncias: ").append(data.getTotalDenuncias()).append('\n');
        builder.append("- totalReportes: ").append(data.getTotalReportes()).append('\n');
        builder.append("Reportes por tipo:\n");
        appendReportTypeMetrics(builder, data.getReportesPorTipo());
        builder.append("Reportes por estado:\n");
        appendReportStatusMetrics(builder, data.getReportesPorEstado());
        builder.append("Moderacion por accion:\n");
        appendModerationActionMetrics(builder, data.getModeracionPorAccion());
        builder.append("Motivos de moderacion:\n");
        appendModerationReasonMetrics(builder, data.getMotivosModeracion());
        builder.append("- gruposCreados: ").append(data.getGruposCreados()).append('\n');
        builder.append("- gruposActivos: ").append(data.getGruposActivos()).append('\n');
        builder.append("- chatsIndividualesActivos: ").append(data.getChatsIndividualesActivos()).append('\n');
        builder.append("- mensajesEnviados: ").append(data.getMensajesEnviados()).append('\n');
        builder.append("- encuestasCreadas: ").append(data.getEncuestasCreadas()).append('\n');
        builder.append("Motivos de denuncia:\n");
        appendReasonMetrics(builder, data.getMotivosDenuncia());
        builder.append("Usuarios conflictivos:\n");
        appendConflictiveUsers(builder, data.getUsuariosConflictivos());
        builder.append("Usuarios que mas denuncian:\n");
        appendReporterUsers(builder, data.getUsuariosQueMasDenuncian());
        builder.append("Grupos mas activos:\n");
        appendGroupMetrics(builder, data.getGruposMasActivos());
        builder.append("Usuarios actualmente baneados:\n");
        appendBannedUsers(builder, data.getUsuariosActualmenteBaneados());
        builder.append("Alertas/limitaciones:\n");
        appendAlerts(builder, data.getAlertas());
        return builder.toString();
    }

    private void appendReasonMetrics(StringBuilder builder, List<AdminReasonMetricDTO> items) {
        if (items == null || items.isEmpty()) {
            builder.append("- No disponible\n");
            return;
        }
        for (AdminReasonMetricDTO item : items) {
            builder.append("- ").append(safeText(item.getMotivo(), 120))
                    .append(": ").append(item.getTotal()).append('\n');
        }
    }

    private void appendReportTypeMetrics(StringBuilder builder, List<AdminReportTypeMetricDTO> items) {
        if (items == null || items.isEmpty()) {
            builder.append("- No disponible\n");
            return;
        }
        for (AdminReportTypeMetricDTO item : items) {
            builder.append("- ").append(safeText(item.getTipo(), 80))
                    .append(": ").append(item.getTotal()).append('\n');
        }
    }

    private void appendReportStatusMetrics(StringBuilder builder, List<AdminReportStatusMetricDTO> items) {
        if (items == null || items.isEmpty()) {
            builder.append("- No disponible\n");
            return;
        }
        for (AdminReportStatusMetricDTO item : items) {
            builder.append("- ").append(safeText(item.getEstado(), 80))
                    .append(": ").append(item.getTotal()).append('\n');
        }
    }

    private void appendModerationActionMetrics(StringBuilder builder, List<AdminModerationActionMetricDTO> items) {
        if (items == null || items.isEmpty()) {
            builder.append("- No disponible\n");
            return;
        }
        for (AdminModerationActionMetricDTO item : items) {
            builder.append("- ").append(safeText(item.getAccion(), 80))
                    .append(": ").append(item.getTotal()).append('\n');
        }
    }

    private void appendModerationReasonMetrics(StringBuilder builder, List<AdminModerationReasonMetricDTO> items) {
        if (items == null || items.isEmpty()) {
            builder.append("- No disponible\n");
            return;
        }
        for (AdminModerationReasonMetricDTO item : items) {
            builder.append("- ").append(safeText(item.getMotivo(), 190))
                    .append(": ").append(item.getTotal()).append('\n');
        }
    }

    private void appendConflictiveUsers(StringBuilder builder, List<AdminConflictiveUserDTO> items) {
        if (items == null || items.isEmpty()) {
            builder.append("- No disponible\n");
            return;
        }
        for (AdminConflictiveUserDTO item : items) {
            builder.append("- ID ").append(item.getUsuarioId())
                    .append(" | nombre: ").append(safeText(item.getNombre(), 190))
                    .append(" | denunciasRecibidas: ").append(item.getDenunciasRecibidas())
                    .append(" | reportesRecibidos: ").append(item.getReportesRecibidos())
                    .append(" | motivoPrincipal: ").append(safeText(item.getMotivoPrincipal(), 120))
                    .append('\n');
        }
    }

    private void appendReporterUsers(StringBuilder builder, List<AdminReporterUserDTO> items) {
        if (items == null || items.isEmpty()) {
            builder.append("- No disponible\n");
            return;
        }
        for (AdminReporterUserDTO item : items) {
            builder.append("- ID ").append(item.getUsuarioId())
                    .append(" | nombre: ").append(safeText(item.getNombre(), 190))
                    .append(" | denunciasRealizadas: ").append(item.getDenunciasRealizadas())
                    .append('\n');
        }
    }

    private void appendGroupMetrics(StringBuilder builder, List<AdminGroupMetricDTO> items) {
        if (items == null || items.isEmpty()) {
            builder.append("- No disponible\n");
            return;
        }
        for (AdminGroupMetricDTO item : items) {
            builder.append("- ID ").append(item.getGrupoId())
                    .append(" | nombreGrupo: ").append(safeText(item.getNombreGrupo(), 190))
                    .append(" | totalMensajes: ").append(item.getTotalMensajes())
                    .append(" | totalMiembros: ").append(item.getTotalMiembros())
                    .append('\n');
        }
    }

    private void appendBannedUsers(StringBuilder builder, List<AdminBannedUserDTO> items) {
        if (items == null || items.isEmpty()) {
            builder.append("- No disponible\n");
            return;
        }
        for (AdminBannedUserDTO item : items) {
            builder.append("- ID ").append(item.getUsuarioId())
                    .append(" | nombre: ").append(safeText(item.getNombre(), 190))
                    .append(" | email: ").append(safeText(item.getEmail(), 190))
                    .append(" | motivo: ").append(safeText(item.getMotivo(), 190))
                    .append('\n');
        }
    }

    private void appendAlerts(StringBuilder builder, List<String> items) {
        if (items == null || items.isEmpty()) {
            builder.append("- No disponible\n");
            return;
        }
        for (String item : items) {
            builder.append("- ").append(safeText(item, 300)).append('\n');
        }
    }

    private String buildFallbackReport(AdminAiReportDataDTO data, String code) {
        StringBuilder builder = new StringBuilder();
        builder.append(REPORT_TITLE).append('\n');
        builder.append("Periodo analizado: ").append(data.getFechaInicio()).append(" - ").append(data.getFechaFin()).append("\n\n");
        builder.append("## 1. Resumen ejecutivo\n");
        if (data.getTotalUsuarios() == 0 && data.getTotalDenuncias() == 0 && data.getTotalReportes() == 0
                && data.getMensajesEnviados() == 0) {
            builder.append("No se identifican datos operativos relevantes en el periodo segun las metricas disponibles.\n\n");
        } else {
            builder.append("Se presenta un resumen administrativo basado exclusivamente en metricas reales del backend. ");
            builder.append("Esta version se genera sin redaccion IA avanzada por indisponibilidad o configuracion del proveedor.\n\n");
        }
        builder.append("## 2. Actividad de usuarios\n");
        builder.append("- Usuarios totales: ").append(data.getTotalUsuarios()).append('\n');
        builder.append("- Usuarios nuevos en el periodo: ").append(data.getUsuariosNuevos()).append('\n');
        builder.append("- Usuarios activos: ").append(data.getUsuariosActivos()).append('\n');
        builder.append("- Usuarios inactivos: ").append(data.getUsuariosInactivos()).append('\n');
        builder.append("- Usuarios baneados: ").append(formatMetricValue(data.getUsuariosBaneados())).append('\n');
        builder.append("- Nota: el backend no dispone de una metrica separada de baneados respecto a inactivos.\n\n");
        builder.append("## 3. Denuncias y reportes\n");
        builder.append("- Denuncias recibidas: ").append(data.getTotalDenuncias()).append('\n');
        builder.append("- Reportes administrativos recibidos: ").append(data.getTotalReportes()).append('\n');
        if (data.getMotivosDenuncia().isEmpty()) {
            builder.append("- Motivos principales: no disponibles\n\n");
        } else {
            builder.append("- Motivos principales:\n");
            for (AdminReasonMetricDTO item : data.getMotivosDenuncia()) {
                builder.append("  - ").append(safeText(item.getMotivo(), 120)).append(": ").append(item.getTotal()).append('\n');
            }
            builder.append('\n');
        }
        builder.append("## 4. Usuarios mas conflictivos\n");
        if (data.getUsuariosConflictivos().isEmpty()) {
            builder.append("No hay usuarios destacados por volumen de denuncias en el periodo.\n\n");
        } else {
            for (AdminConflictiveUserDTO item : data.getUsuariosConflictivos()) {
                builder.append("- ID ").append(item.getUsuarioId())
                        .append(" | ").append(safeText(item.getNombre(), 190))
                        .append(" | denuncias: ").append(item.getDenunciasRecibidas())
                        .append(" | motivo principal: ").append(safeText(item.getMotivoPrincipal(), 120))
                        .append('\n');
            }
            builder.append('\n');
        }
        builder.append("## 5. Actividad en grupos y chats\n");
        builder.append("- Grupos creados: ").append(data.getGruposCreados()).append('\n');
        builder.append("- Grupos activos: ").append(data.getGruposActivos()).append('\n');
        builder.append("- Chats individuales activos: ").append(data.getChatsIndividualesActivos()).append('\n');
        builder.append("- Mensajes enviados: ").append(data.getMensajesEnviados()).append('\n');
        builder.append("- Encuestas creadas: ").append(data.getEncuestasCreadas()).append('\n');
        if (!data.getGruposMasActivos().isEmpty()) {
            builder.append("- Grupos mas activos:\n");
            for (AdminGroupMetricDTO item : data.getGruposMasActivos()) {
                builder.append("  - ID ").append(item.getGrupoId())
                        .append(" | ").append(safeText(item.getNombreGrupo(), 190))
                        .append(" | mensajes: ").append(item.getTotalMensajes())
                        .append(" | miembros: ").append(item.getTotalMiembros())
                        .append('\n');
            }
        }
        builder.append("\n## 6. Riesgos detectados\n");
        appendFallbackAlerts(builder, data.getAlertas());
        builder.append("\n## 7. Recomendaciones administrativas\n");
        builder.append("- Revisar manualmente los usuarios con mayor volumen de denuncias antes de cualquier accion.\n");
        builder.append("- Priorizar los motivos de denuncia mas repetidos para ajustes de moderacion.\n");
        builder.append("- Supervisar grupos con mayor volumen de actividad cuando coincidan con incidencias.\n\n");
        builder.append("## 8. Conclusion\n");
        builder.append("Reporte generado con metricas verificadas del backend. Codigo de generacion: ").append(code).append(".\n");
        return builder.toString().trim();
    }

    private void appendFallbackAlerts(StringBuilder builder, List<String> alertas) {
        if (alertas == null || alertas.isEmpty()) {
            builder.append("- Sin alertas relevantes derivadas de las metricas disponibles.\n");
            return;
        }
        for (String alerta : alertas) {
            builder.append("- ").append(safeText(alerta, 300)).append('\n');
        }
    }

    private String buildHtmlDocument(String reporte, AdminAiReportDataDTO data) {
        String code = resolveHeaderCode(reporte);
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>");
        html.append("<html xmlns=\"http://www.w3.org/1999/xhtml\" lang=\"es\"><head>");
        html.append("<meta charset=\"UTF-8\" />");
        html.append("<title>").append(escapeHtml(REPORT_TITLE)).append("</title>");
        html.append("<style>");
        html.append("@page { size: A4; margin: 22mm 18mm 20mm 18mm; ");
        html.append("@bottom-center { content: 'TejeChat - Reporte administrativo | Página ' counter(page) ' de ' counter(pages); ");
        html.append("font-size: 9pt; color: #6b7280; } }");
        html.append("body { font-family: Helvetica, Arial, sans-serif; color: #1f2937; font-size: 11pt; line-height: 1.55; }");
        html.append("h1 { text-align: center; font-size: 20pt; letter-spacing: 0.6px; margin: 0 0 12px 0; color: #0f172a; }");
        html.append("h2 { font-size: 13.5pt; color: #0f172a; margin: 24px 0 8px 0; padding-bottom: 4px; border-bottom: 1px solid #dbe2ea; }");
        html.append("h3 { font-size: 11.5pt; color: #1e293b; margin: 18px 0 8px 0; }");
        html.append("p { margin: 8px 0; }");
        html.append("ul { margin: 8px 0 8px 18px; }");
        html.append("li { margin: 4px 0; }");
        html.append(".subtitle { text-align: center; color: #475569; margin-bottom: 3px; font-size: 10.5pt; }");
        html.append(".divider { border-top: 1px solid #cbd5e1; margin: 16px 0 18px 0; }");
        html.append(".section { margin-top: 12px; }");
        html.append(".cards { width: 100%; margin: 16px 0 6px 0; }");
        html.append(".card { display: inline-block; width: 31%; margin-right: 2%; vertical-align: top; border: 1px solid #dbe2ea; border-radius: 6px; padding: 10px 12px; box-sizing: border-box; background: #f8fafc; }");
        html.append(".card.last { margin-right: 0; }");
        html.append(".card-label { color: #64748b; font-size: 9pt; margin-bottom: 4px; }");
        html.append(".card-value { color: #0f172a; font-size: 15pt; font-weight: bold; }");
        html.append("table { width: 100%; border-collapse: collapse; margin: 10px 0 16px 0; font-size: 10pt; }");
        html.append("th, td { border: 1px solid #dbe2ea; padding: 7px 8px; text-align: left; vertical-align: top; }");
        html.append("th { background: #eef2f7; color: #0f172a; font-weight: bold; }");
        html.append(".muted { color: #64748b; }");
        html.append(".code { font-size: 9pt; color: #64748b; text-align: right; margin-top: 10px; }");
        html.append("</style></head><body>");
        html.append("<h1>REPORTE ADMINISTRATIVO DE TEJECHAT</h1>");
        html.append("<div class=\"subtitle\">Periodo analizado: ")
                .append(escapeHtml(formatDisplayDate(data.getFechaInicio())))
                .append(" - ")
                .append(escapeHtml(formatDisplayDate(data.getFechaFin())))
                .append("</div>");
        html.append("<div class=\"subtitle\">Fecha de generación: ")
                .append(escapeHtml(formatDisplayDate(LocalDate.now())))
                .append("</div>");
        html.append("<div class=\"divider\"></div>");
        html.append(buildSummaryCardsHtml(data));
        html.append("<div class=\"section\">").append(renderReportTextAsHtml(reporte)).append("</div>");
        html.append(buildMetricsAnnexHtml(data));
        html.append("<div class=\"code\">Código interno: ").append(escapeHtml(code)).append("</div>");
        html.append("</body></html>");
        return html.toString();
    }

    private String resolveHeaderCode(String reporte) {
        if (reporte == null || reporte.isBlank()) {
            return CODE_ERROR;
        }
        if (reporte.contains(CODE_ERROR)) {
            return CODE_ERROR;
        }
        if (reporte.contains(CODE_NO_DATA)) {
            return CODE_NO_DATA;
        }
        return CODE_OK;
    }

    private String buildFilename(AdminAiReportDataDTO data) {
        return "reporte-tejechat-ia-" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + ".pdf";
    }

    private String buildSummaryCardsHtml(AdminAiReportDataDTO data) {
        StringBuilder html = new StringBuilder();
        html.append("<div class=\"cards\">");
        html.append(buildCardHtml("Usuarios totales", String.valueOf(data.getTotalUsuarios()), false));
        html.append(buildCardHtml("Denuncias periodo", String.valueOf(data.getTotalDenuncias()), false));
        html.append(buildCardHtml("Mensajes periodo", String.valueOf(data.getMensajesEnviados()), true));
        html.append("</div>");
        return html.toString();
    }

    private String buildCardHtml(String label, String value, boolean last) {
        return "<div class=\"card" + (last ? " last" : "") + "\">"
                + "<div class=\"card-label\">" + escapeHtml(label) + "</div>"
                + "<div class=\"card-value\">" + escapeHtml(value) + "</div>"
                + "</div>";
    }

    private String renderReportTextAsHtml(String reporte) {
        if (reporte == null || reporte.isBlank()) {
            return "<p>No se pudo generar el cuerpo del reporte.</p>";
        }
        String[] lines = reporte.replace("\r\n", "\n").replace('\r', '\n').split("\n");
        StringBuilder html = new StringBuilder();
        boolean listOpen = false;
        for (String rawLine : lines) {
            String line = rawLine == null ? "" : rawLine.trim();
            if (line.isEmpty()) {
                if (listOpen) {
                    html.append("</ul>");
                    listOpen = false;
                }
                continue;
            }
            if (line.startsWith("# ")) {
                if (listOpen) {
                    html.append("</ul>");
                    listOpen = false;
                }
                html.append("<h2>").append(escapeHtml(line.substring(2).trim())).append("</h2>");
                continue;
            }
            if (line.startsWith("## ")) {
                if (listOpen) {
                    html.append("</ul>");
                    listOpen = false;
                }
                html.append("<h2>").append(escapeHtml(line.substring(3).trim())).append("</h2>");
                continue;
            }
            if (line.matches("^\\d+\\.\\s+.+$")) {
                if (listOpen) {
                    html.append("</ul>");
                    listOpen = false;
                }
                html.append("<h2>").append(escapeHtml(line)).append("</h2>");
                continue;
            }
            if (line.startsWith("- ")) {
                if (!listOpen) {
                    html.append("<ul>");
                    listOpen = true;
                }
                html.append("<li>").append(escapeHtml(line.substring(2).trim())).append("</li>");
                continue;
            }
            if (listOpen) {
                html.append("</ul>");
                listOpen = false;
            }
            html.append("<p>").append(escapeHtml(line)).append("</p>");
        }
        if (listOpen) {
            html.append("</ul>");
        }
        return html.toString();
    }

    private String buildMetricsAnnexHtml(AdminAiReportDataDTO data) {
        StringBuilder html = new StringBuilder();
        html.append("<h2>Anexo de métricas verificadas</h2>");
        html.append("<table><thead><tr><th>Métrica</th><th>Valor</th></tr></thead><tbody>");
        appendMetricRow(html, "Usuarios totales", String.valueOf(data.getTotalUsuarios()));
        appendMetricRow(html, "Usuarios nuevos en el periodo", String.valueOf(data.getUsuariosNuevos()));
        appendMetricRow(html, "Usuarios activos", String.valueOf(data.getUsuariosActivos()));
        appendMetricRow(html, "Usuarios inactivos", String.valueOf(data.getUsuariosInactivos()));
        appendMetricRow(html, "Usuarios baneados", formatMetricValue(data.getUsuariosBaneados()));
        appendMetricRow(html, "Baneos realizados en el periodo", String.valueOf(data.getBaneosRealizados()));
        appendMetricRow(html, "Desbaneos realizados en el periodo", String.valueOf(data.getDesbaneosRealizados()));
        appendMetricRow(html, "Denuncias recibidas", String.valueOf(data.getTotalDenuncias()));
        appendMetricRow(html, "Reportes recibidos", String.valueOf(data.getTotalReportes()));
        appendMetricRow(html, "Grupos creados", String.valueOf(data.getGruposCreados()));
        appendMetricRow(html, "Grupos activos", String.valueOf(data.getGruposActivos()));
        appendMetricRow(html, "Chats individuales activos", String.valueOf(data.getChatsIndividualesActivos()));
        appendMetricRow(html, "Mensajes enviados", String.valueOf(data.getMensajesEnviados()));
        appendMetricRow(html, "Encuestas creadas", String.valueOf(data.getEncuestasCreadas()));
        html.append("</tbody></table>");

        html.append("<h3>Incidencias desglosadas por tipo</h3>");
        html.append(buildReportTypeTable(data.getReportesPorTipo()));
        html.append("<h3>Incidencias desglosadas por estado</h3>");
        html.append(buildReportStatusTable(data.getReportesPorEstado()));
        html.append("<h3>Moderación por acción</h3>");
        html.append(buildModerationActionTable(data.getModeracionPorAccion()));
        html.append("<h3>Motivos de moderación</h3>");
        html.append(buildModerationReasonTable(data.getMotivosModeracion()));
        html.append("<h3>Motivos principales de denuncia</h3>");
        html.append(buildReasonTable(data.getMotivosDenuncia()));
        html.append("<h3>Usuarios a revisar</h3>");
        html.append(buildConflictiveUsersTable(data.getUsuariosConflictivos()));
        html.append("<h3>Usuarios que más denuncian</h3>");
        html.append(buildReporterUsersTable(data.getUsuariosQueMasDenuncian()));
        html.append("<h3>Grupos más activos</h3>");
        html.append(buildGroupsTable(data.getGruposMasActivos()));
        html.append("<h3>Usuarios actualmente baneados</h3>");
        html.append(buildBannedUsersTable(data.getUsuariosActualmenteBaneados()));
        html.append("<h3>Alertas y limitaciones</h3>");
        html.append(buildAlertsHtml(data.getAlertas()));
        return html.toString();
    }

    private void appendMetricRow(StringBuilder html, String label, String value) {
        html.append("<tr><td>").append(escapeHtml(label)).append("</td><td>")
                .append(escapeHtml(value)).append("</td></tr>");
    }

    private String buildReasonTable(List<AdminReasonMetricDTO> items) {
        if (items == null || items.isEmpty()) {
            return "<p class=\"muted\">No disponible.</p>";
        }
        StringBuilder html = new StringBuilder("<table><thead><tr><th>Motivo</th><th>Total</th></tr></thead><tbody>");
        for (AdminReasonMetricDTO item : items) {
            html.append("<tr><td>").append(escapeHtml(safeText(item.getMotivo(), 120))).append("</td><td>")
                    .append(item.getTotal()).append("</td></tr>");
        }
        html.append("</tbody></table>");
        return html.toString();
    }

    private String buildReportTypeTable(List<AdminReportTypeMetricDTO> items) {
        if (items == null || items.isEmpty()) {
            return "<p class=\"muted\">No disponible.</p>";
        }
        StringBuilder html = new StringBuilder("<table><thead><tr><th>Tipo</th><th>Total</th></tr></thead><tbody>");
        for (AdminReportTypeMetricDTO item : items) {
            html.append("<tr><td>").append(escapeHtml(safeText(item.getTipo(), 80))).append("</td><td>")
                    .append(item.getTotal()).append("</td></tr>");
        }
        html.append("</tbody></table>");
        return html.toString();
    }

    private String buildReportStatusTable(List<AdminReportStatusMetricDTO> items) {
        if (items == null || items.isEmpty()) {
            return "<p class=\"muted\">No disponible.</p>";
        }
        StringBuilder html = new StringBuilder("<table><thead><tr><th>Estado</th><th>Total</th></tr></thead><tbody>");
        for (AdminReportStatusMetricDTO item : items) {
            html.append("<tr><td>").append(escapeHtml(safeText(item.getEstado(), 80))).append("</td><td>")
                    .append(item.getTotal()).append("</td></tr>");
        }
        html.append("</tbody></table>");
        return html.toString();
    }

    private String buildModerationActionTable(List<AdminModerationActionMetricDTO> items) {
        if (items == null || items.isEmpty()) {
            return "<p class=\"muted\">No disponible.</p>";
        }
        StringBuilder html = new StringBuilder("<table><thead><tr><th>Acción</th><th>Total</th></tr></thead><tbody>");
        for (AdminModerationActionMetricDTO item : items) {
            html.append("<tr><td>").append(escapeHtml(safeText(item.getAccion(), 80))).append("</td><td>")
                    .append(item.getTotal()).append("</td></tr>");
        }
        html.append("</tbody></table>");
        return html.toString();
    }

    private String buildModerationReasonTable(List<AdminModerationReasonMetricDTO> items) {
        if (items == null || items.isEmpty()) {
            return "<p class=\"muted\">No disponible.</p>";
        }
        StringBuilder html = new StringBuilder("<table><thead><tr><th>Motivo</th><th>Total</th></tr></thead><tbody>");
        for (AdminModerationReasonMetricDTO item : items) {
            html.append("<tr><td>").append(escapeHtml(safeText(item.getMotivo(), 190))).append("</td><td>")
                    .append(item.getTotal()).append("</td></tr>");
        }
        html.append("</tbody></table>");
        return html.toString();
    }

    private String buildConflictiveUsersTable(List<AdminConflictiveUserDTO> items) {
        if (items == null || items.isEmpty()) {
            return "<p class=\"muted\">No disponible.</p>";
        }
        StringBuilder html = new StringBuilder("<table><thead><tr><th>Usuario</th><th>Denuncias recibidas</th><th>Reportes recibidos</th><th>Motivo principal</th><th>Recomendación</th></tr></thead><tbody>");
        for (AdminConflictiveUserDTO item : items) {
            html.append("<tr><td>")
                    .append(escapeHtml(safeText(item.getNombre(), 190))).append(" [ID ").append(item.getUsuarioId()).append("]")
                    .append("</td><td>").append(item.getDenunciasRecibidas())
                    .append("</td><td>").append(item.getReportesRecibidos())
                    .append("</td><td>").append(escapeHtml(safeText(item.getMotivoPrincipal(), 120)))
                    .append("</td><td>Conviene revisar el contexto y valorar revisión manual.</td></tr>");
        }
        html.append("</tbody></table>");
        return html.toString();
    }

    private String buildReporterUsersTable(List<AdminReporterUserDTO> items) {
        if (items == null || items.isEmpty()) {
            return "<p class=\"muted\">No disponible.</p>";
        }
        StringBuilder html = new StringBuilder("<table><thead><tr><th>Usuario</th><th>Denuncias realizadas</th></tr></thead><tbody>");
        for (AdminReporterUserDTO item : items) {
            html.append("<tr><td>")
                    .append(escapeHtml(safeText(item.getNombre(), 190))).append(" [ID ").append(item.getUsuarioId()).append("]")
                    .append("</td><td>").append(item.getDenunciasRealizadas())
                    .append("</td></tr>");
        }
        html.append("</tbody></table>");
        return html.toString();
    }

    private String buildGroupsTable(List<AdminGroupMetricDTO> items) {
        if (items == null || items.isEmpty()) {
            return "<p class=\"muted\">No disponible.</p>";
        }
        StringBuilder html = new StringBuilder("<table><thead><tr><th>Grupo</th><th>Mensajes</th><th>Miembros</th></tr></thead><tbody>");
        for (AdminGroupMetricDTO item : items) {
            html.append("<tr><td>")
                    .append(escapeHtml(safeText(item.getNombreGrupo(), 190))).append(" [ID ").append(item.getGrupoId()).append("]")
                    .append("</td><td>").append(item.getTotalMensajes())
                    .append("</td><td>").append(item.getTotalMiembros())
                    .append("</td></tr>");
        }
        html.append("</tbody></table>");
        return html.toString();
    }

    private String buildBannedUsersTable(List<AdminBannedUserDTO> items) {
        if (items == null || items.isEmpty()) {
            return "<p class=\"muted\">No disponible.</p>";
        }
        StringBuilder html = new StringBuilder("<table><thead><tr><th>Usuario</th><th>Email</th><th>Motivo</th></tr></thead><tbody>");
        for (AdminBannedUserDTO item : items) {
            html.append("<tr><td>")
                    .append(escapeHtml(safeText(item.getNombre(), 190))).append(" [ID ").append(item.getUsuarioId()).append("]")
                    .append("</td><td>").append(escapeHtml(safeText(item.getEmail(), 190)))
                    .append("</td><td>").append(escapeHtml(safeText(item.getMotivo(), 190)))
                    .append("</td></tr>");
        }
        html.append("</tbody></table>");
        return html.toString();
    }

    private String buildAlertsHtml(List<String> items) {
        if (items == null || items.isEmpty()) {
            return "<p class=\"muted\">Sin alertas relevantes derivadas de las métricas disponibles.</p>";
        }
        StringBuilder html = new StringBuilder("<ul>");
        for (String item : items) {
            html.append("<li>").append(escapeHtml(safeText(item, 300))).append("</li>");
        }
        html.append("</ul>");
        return html.toString();
    }

    private String formatMetricValue(long value) {
        return value < 0 ? "No disponible" : String.valueOf(value);
    }

    private String formatDisplayDate(LocalDate date) {
        return date == null ? "No disponible" : date.format(DISPLAY_DATE_FORMAT);
    }

    private String escapeHtml(String value) {
        return HtmlUtils.htmlEscape(value == null ? "" : value, StandardCharsets.UTF_8.name());
    }

    private String safeText(String value, int maxLen) {
        if (value == null) {
            return null;
        }
        String normalized = value.replace('\r', ' ').replace('\n', ' ').trim();
        if (normalized.isBlank()) {
            return null;
        }
        if (normalized.length() <= maxLen) {
            return normalized;
        }
        return normalized.substring(0, Math.max(0, maxLen)).trim();
    }

    private long defaultLong(Long value) {
        return value == null ? 0L : value;
    }

    private String resolveDisplayName(UsuarioEntity usuario, Long fallbackId) {
        if (usuario != null) {
            String nombre = safeText(usuario.getNombre(), 90);
            String apellido = safeText(usuario.getApellido(), 90);
            if (nombre != null && apellido != null) {
                return (nombre + " " + apellido).trim();
            }
            if (nombre != null) {
                return nombre;
            }
            if (apellido != null) {
                return apellido;
            }
        }
        return "Usuario " + fallbackId;
    }
}
