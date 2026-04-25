package com.chat.chat.Service.ReportWarningBatchService;

import com.chat.chat.DTO.AdminDirectMessageRequestDTO;
import com.chat.chat.Entity.UserComplaintEntity;
import com.chat.chat.Entity.UserModerationHistoryEntity;
import com.chat.chat.Entity.UsuarioEntity;
import com.chat.chat.Repository.UserComplaintRepository;
import com.chat.chat.Repository.UserModerationHistoryRepository;
import com.chat.chat.Repository.UsuarioRepository;
import com.chat.chat.Service.ChatService.ChatService;
import com.chat.chat.Utils.ModerationActionType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class ReportWarningBatchServiceImpl implements ReportWarningBatchService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReportWarningBatchServiceImpl.class);
    private static final String FALLBACK_REPORTERS = "usuarios de la plataforma";
    private static final String FALLBACK_MOTIVOS = "comportamientos reportados por la comunidad";
    private static final String FALLBACK_USER_NAME = "usuario";
    private static final Pattern UNSAFE_CONTROL_CHARS = Pattern.compile("[\\p{Cntrl}&&[^\\r\\n\\t]]");
    private static final Pattern COMPLAINT_COUNT_PATTERN = Pattern.compile("(^|\\|)complaintCount=(\\d+)(\\||$)");

    private final UserComplaintRepository userComplaintRepository;
    private final UserModerationHistoryRepository userModerationHistoryRepository;
    private final UsuarioRepository usuarioRepository;
    private final ChatService chatService;

    @Value("${tejechat.batch.report-warning.threshold:4}")
    private int threshold;

    @Value("${tejechat.batch.report-warning.max-samples:3}")
    private int maxSamples;

    @Value("${tejechat.batch.report-warning.admin-user-id:0}")
    private Long configuredAdminUserId;

    @Value("${tejechat.batch.report-warning.moderation-origin:batch_report_warning_auto}")
    private String moderationOrigin;

    @Value("${tejechat.batch.report-warning.moderation-reason:Advertencia automatica por denuncias}")
    private String moderationReason;

    public ReportWarningBatchServiceImpl(UserComplaintRepository userComplaintRepository,
                                         UserModerationHistoryRepository userModerationHistoryRepository,
                                         UsuarioRepository usuarioRepository,
                                         ChatService chatService) {
        this.userComplaintRepository = userComplaintRepository;
        this.userModerationHistoryRepository = userModerationHistoryRepository;
        this.usuarioRepository = usuarioRepository;
        this.chatService = chatService;
    }

    @Override
    public void ejecutarBatchAdvertenciasPorDenuncias() {
        int safeThreshold = Math.max(4, threshold);
        int safeSamples = Math.max(1, Math.min(maxSamples, 5));
        String batchOrigin = normalizeBatchOrigin(moderationOrigin);

        Long adminUserId = resolveAdminUserId();
        if (adminUserId == null) {
            LOGGER.warn("[REPORT_WARNING_BATCH] start skipped=true reason=no_admin_actor");
            return;
        }

        List<UserComplaintRepository.DenunciadoComplaintCountView> candidates =
                userComplaintRepository.findDenunciadosConMasDeDenuncias(safeThreshold);

        int detected = candidates.size();
        int sent = 0;
        int skippedDuplicate = 0;
        int skippedInactive = 0;
        int skippedInvalid = 0;
        int errors = 0;

        LOGGER.info(
                "[REPORT_WARNING_BATCH] start threshold={} detectedUsers={} adminUserId={} origin={}",
                safeThreshold,
                detected,
                adminUserId,
                batchOrigin);

        for (UserComplaintRepository.DenunciadoComplaintCountView row : candidates) {
            Long targetUserId = row == null ? null : row.getDenunciadoId();
            long complaintCount = row == null || row.getTotal() == null ? 0L : row.getTotal();

            if (targetUserId == null || targetUserId <= 0 || complaintCount <= safeThreshold) {
                skippedInvalid++;
                LOGGER.warn(
                        "[REPORT_WARNING_BATCH] skip userId={} reason=invalid_candidate complaintCount={}",
                        targetUserId,
                        complaintCount);
                continue;
            }

            try {
                UsuarioEntity targetUser = usuarioRepository.findById(targetUserId).orElse(null);
                if (targetUser == null || !targetUser.isActivo()) {
                    skippedInactive++;
                    LOGGER.info(
                            "[REPORT_WARNING_BATCH] skip userId={} reason=inactive_or_missing complaintCount={}",
                            targetUserId,
                            complaintCount);
                    continue;
                }

                Long lastSentCount = readLastSentComplaintCount(targetUserId, batchOrigin);
                if (lastSentCount != null && complaintCount <= lastSentCount) {
                    skippedDuplicate++;
                    LOGGER.info(
                            "[REPORT_WARNING_BATCH] skip userId={} reason=already_warned currentCount={} lastSentCount={}",
                            targetUserId,
                            complaintCount,
                            lastSentCount);
                    continue;
                }

                List<UserComplaintEntity> recentComplaints =
                        userComplaintRepository.findTop5ByDenunciadoIdOrderByCreatedAtDescIdDesc(targetUserId);

                String userName = resolveTargetUserName(targetUser, recentComplaints);
                String reportersText = resolveReportersText(recentComplaints, safeSamples);
                String motivosText = resolveMotivosText(recentComplaints, safeSamples);
                Long latestComplaintId = recentComplaints.stream()
                        .map(UserComplaintEntity::getId)
                        .filter(Objects::nonNull)
                        .findFirst()
                        .orElse(null);

                AdminDirectMessageRequestDTO request = new AdminDirectMessageRequestDTO();
                request.setUserIds(List.of(targetUserId));
                request.setContenido(buildWarningMessage(userName, complaintCount, reportersText, motivosText));
                request.setOrigen(batchOrigin);
                request.setMotivo(moderationReason);
                request.setDescripcion(buildBatchDescription(complaintCount, latestComplaintId));

                chatService.enviarMensajeDirectoAdminComoSistema(adminUserId, request);
                sent++;
                LOGGER.info(
                        "[REPORT_WARNING_BATCH] sent userId={} complaintCount={} latestComplaintId={}",
                        targetUserId,
                        complaintCount,
                        latestComplaintId);
            } catch (Exception ex) {
                errors++;
                LOGGER.error(
                        "[REPORT_WARNING_BATCH] error userId={} complaintCount={} errorType={} error={}",
                        targetUserId,
                        complaintCount,
                        ex.getClass().getSimpleName(),
                        ex.getMessage(),
                        ex);
            }
        }

        LOGGER.info(
                "[REPORT_WARNING_BATCH] end detectedUsers={} sent={} skippedDuplicate={} skippedInactive={} skippedInvalid={} errors={}",
                detected,
                sent,
                skippedDuplicate,
                skippedInactive,
                skippedInvalid,
                errors);
    }

    private Long resolveAdminUserId() {
        if (configuredAdminUserId != null && configuredAdminUserId > 0) {
            UsuarioEntity configured = usuarioRepository.findById(configuredAdminUserId).orElse(null);
            if (configured != null && configured.isActivo() && hasAdminRole(configured)) {
                return configured.getId();
            }
            LOGGER.warn(
                    "[REPORT_WARNING_BATCH] configured admin invalid adminUserId={} reason=missing_inactive_or_non_admin",
                    configuredAdminUserId);
        }

        List<UsuarioEntity> admins = usuarioRepository.findActiveAdmins(PageRequest.of(0, 1));
        if (admins.isEmpty()) {
            return null;
        }
        return admins.get(0).getId();
    }

    private boolean hasAdminRole(UsuarioEntity user) {
        if (user == null || user.getRoles() == null) {
            return false;
        }
        return user.getRoles().stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .anyMatch(role -> "ADMIN".equalsIgnoreCase(role) || "ROLE_ADMIN".equalsIgnoreCase(role));
    }

    private Long readLastSentComplaintCount(Long userId, String origin) {
        Optional<UserModerationHistoryEntity> latest = userModerationHistoryRepository
                .findFirstByUser_IdAndActionTypeAndOriginOrderByCreatedAtDescIdDesc(
                        userId,
                        ModerationActionType.WARNING,
                        origin);
        if (latest.isEmpty()) {
            return null;
        }
        return extractComplaintCount(latest.get().getDescription());
    }

    private Long extractComplaintCount(String description) {
        if (description == null || description.isBlank()) {
            return null;
        }
        Matcher matcher = COMPLAINT_COUNT_PATTERN.matcher(description);
        if (!matcher.find()) {
            return null;
        }
        try {
            return Long.parseLong(matcher.group(2));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String resolveTargetUserName(UsuarioEntity user, List<UserComplaintEntity> recentComplaints) {
        String name = joinName(user == null ? null : user.getNombre(), user == null ? null : user.getApellido());
        if (name != null) {
            return name;
        }
        if (recentComplaints != null) {
            for (UserComplaintEntity complaint : recentComplaints) {
                String snapshot = sanitizeText(complaint == null ? null : complaint.getDenunciadoNombre(), 190);
                if (snapshot != null) {
                    return snapshot;
                }
            }
        }
        return FALLBACK_USER_NAME;
    }

    private String resolveReportersText(List<UserComplaintEntity> complaints, int maxItems) {
        if (complaints == null || complaints.isEmpty()) {
            return FALLBACK_REPORTERS;
        }

        Set<Long> reporterIds = complaints.stream()
                .map(UserComplaintEntity::getDenuncianteId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        Map<Long, String> reporterNamesById = usuarioRepository.findAllById(reporterIds).stream()
                .collect(Collectors.toMap(
                        UsuarioEntity::getId,
                        user -> joinName(user.getNombre(), user.getApellido()),
                        (left, right) -> left));

        LinkedHashSet<String> selected = new LinkedHashSet<>();
        for (UserComplaintEntity complaint : complaints) {
            if (selected.size() >= maxItems) {
                break;
            }
            if (complaint == null) {
                continue;
            }
            String reporter = sanitizeText(complaint.getDenuncianteNombre(), 190);
            if (reporter == null) {
                reporter = sanitizeText(reporterNamesById.get(complaint.getDenuncianteId()), 190);
            }
            if (reporter != null) {
                selected.add(reporter);
            }
        }

        if (selected.isEmpty()) {
            return FALLBACK_REPORTERS;
        }
        return String.join(", ", selected);
    }

    private String resolveMotivosText(List<UserComplaintEntity> complaints, int maxItems) {
        if (complaints == null || complaints.isEmpty()) {
            return FALLBACK_MOTIVOS;
        }
        LinkedHashSet<String> motivos = new LinkedHashSet<>();
        for (UserComplaintEntity complaint : complaints) {
            if (motivos.size() >= maxItems) {
                break;
            }
            String motivo = sanitizeText(complaint == null ? null : complaint.getMotivo(), 120);
            if (motivo != null) {
                motivos.add(motivo);
            }
        }
        if (motivos.isEmpty()) {
            return FALLBACK_MOTIVOS;
        }
        return String.join(", ", motivos);
    }

    private String joinName(String nombre, String apellido) {
        String first = sanitizeText(nombre, 120);
        String last = sanitizeText(apellido, 120);
        List<String> parts = new ArrayList<>(2);
        if (first != null) {
            parts.add(first);
        }
        if (last != null) {
            parts.add(last);
        }
        if (parts.isEmpty()) {
            return null;
        }
        return String.join(" ", parts);
    }

    private String sanitizeText(String raw, int maxLength) {
        if (raw == null) {
            return null;
        }
        String normalized = UNSAFE_CONTROL_CHARS.matcher(raw).replaceAll(" ").trim();
        if (normalized.isBlank()) {
            return null;
        }
        if (normalized.length() <= maxLength) {
            return normalized;
        }
        return normalized.substring(0, maxLength);
    }

    private String buildWarningMessage(String userName, long complaintCount, String reportersText, String motivosText) {
        String safeUserName = sanitizeText(userName, 190);
        String safeReporters = sanitizeText(reportersText, 500);
        String safeMotivos = sanitizeText(motivosText, 500);

        if (safeUserName == null) {
            safeUserName = FALLBACK_USER_NAME;
        }
        if (safeReporters == null) {
            safeReporters = FALLBACK_REPORTERS;
        }
        if (safeMotivos == null) {
            safeMotivos = FALLBACK_MOTIVOS;
        }

        return "Hola " + safeUserName + ". Te escribimos desde administracion de TejeChat. Hemos recibido "
                + complaintCount
                + " denuncia(s) recientes asociadas a tu actividad. Algunas fueron registradas por: "
                + safeReporters
                + ". Los motivos reportados incluyen: "
                + safeMotivos
                + ". Te pedimos mantener una conducta respetuosa y ajustar tu comportamiento a las normas de convivencia de la plataforma. Si se repiten incidencias, se podran aplicar medidas adicionales sobre la cuenta.";
    }

    private String buildBatchDescription(long complaintCount, Long latestComplaintId) {
        String latestIdValue = latestComplaintId == null ? "0" : String.valueOf(latestComplaintId);
        return "batchReportWarning|complaintCount=" + complaintCount + "|latestComplaintId=" + latestIdValue;
    }

    private String normalizeBatchOrigin(String value) {
        String normalized = value == null ? "" : UNSAFE_CONTROL_CHARS.matcher(value).replaceAll(" ").trim();
        if (normalized.isBlank()) {
            normalized = "batch_report_warning_auto";
        }
        if (normalized.length() <= 80) {
            return normalized;
        }
        return normalized.substring(0, 80);
    }
}
