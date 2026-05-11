package com.chat.chat.Service.AiService;

import com.chat.chat.DTO.AiSearchProgressWS;
import com.chat.chat.Utils.AiSearchProgressStep;
import com.chat.chat.Utils.Constantes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class AiSearchProgressNotifierImpl implements AiSearchProgressNotifier {

    private static final Logger LOGGER = LoggerFactory.getLogger(AiSearchProgressNotifierImpl.class);

    private final SimpMessagingTemplate messagingTemplate;

    public AiSearchProgressNotifierImpl(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @Override
    public void notifyStarted(String userEmail, String requestId, AiSearchProgressStep step) {
        send(userEmail, requestId, step, "STARTED", null);
    }

    @Override
    public void notifyCompleted(String userEmail, String requestId, AiSearchProgressStep step) {
        send(userEmail, requestId, step, "COMPLETED", null);
    }

    @Override
    public void notifyCompleted(String userEmail, String requestId, AiSearchProgressStep step, Boolean hasApproximateResult) {
        send(userEmail, requestId, step, "COMPLETED", hasApproximateResult);
    }

    @Override
    public void notifyError(String userEmail, String requestId) {
        send(userEmail, requestId, AiSearchProgressStep.ERROR, "COMPLETED", null);
    }

    @Override
    public void notifyAppReportStarted(String userEmail, String requestId, String tipoReporte) {
        sendAppReport(userEmail, requestId, "STARTED", tipoReporte);
    }

    @Override
    public void notifyAppReportCompleted(String userEmail, String requestId, String tipoReporte) {
        notifyAppReportCompleted(userEmail, requestId, tipoReporte, false);
    }

    @Override
    public void notifyAppReportCompleted(String userEmail, String requestId, String tipoReporte, boolean hasImage) {
        sendAppReport(userEmail, requestId, "COMPLETED", tipoReporte, hasImage);
    }

    @Override
    public void notifyAppReportFailed(String userEmail, String requestId, String tipoReporte) {
        sendAppReport(userEmail, requestId, "FAILED", tipoReporte, false);
    }

    private void sendAppReport(String userEmail, String requestId, String status, String tipoReporte) {
        sendAppReport(userEmail, requestId, status, tipoReporte, false);
    }

    private void sendAppReport(String userEmail, String requestId, String status, String tipoReporte, boolean hasImage) {
        if (userEmail == null || requestId == null || status == null) {
            return;
        }
        try {
            String message = resolveAppReportMessage(status, hasImage);
            AiSearchProgressWS payload = new AiSearchProgressWS(
                    requestId,
                    AiSearchProgressStep.APP_REPORT.name(),
                    status,
                    message,
                    null,
                    "APP_REPORT",
                    tipoReporte
            );
            messagingTemplate.convertAndSendToUser(userEmail, Constantes.WS_QUEUE_AI_SEARCH_PROGRESS, payload);
        } catch (Exception ex) {
            LOGGER.warn("[AI][SEARCH_PROGRESS] ws-send-error step=APP_REPORT status={} userEmail={} errorClass={}",
                    status, userEmail, ex.getClass().getSimpleName());
        }
    }

    private String resolveAppReportMessage(String status, boolean hasImage) {
        return switch (status) {
            case "STARTED" -> "Generando reporte...";
            case "COMPLETED" -> hasImage ? "Reporte generado con imagen adjunta" : "Reporte generado";
            case "FAILED" -> "No se pudo generar el reporte";
            default -> "Reporte";
        };
    }

    @Override
    public void notifyAppReportStatusStarted(String userEmail, String requestId, String tipoReporte) {
        sendAppReportStatus(userEmail, requestId, "STARTED", tipoReporte);
    }

    @Override
    public void notifyAppReportStatusCompleted(String userEmail, String requestId, String tipoReporte) {
        sendAppReportStatus(userEmail, requestId, "COMPLETED", tipoReporte);
    }

    @Override
    public void notifyAppReportStatusFailed(String userEmail, String requestId, String tipoReporte) {
        sendAppReportStatus(userEmail, requestId, "FAILED", tipoReporte);
    }

    @Override
    public void notifyComplaintsSearchStarted(String userEmail, String requestId, String target, String complaintDirection) {
        sendComplaintsSearch(userEmail, requestId, "STARTED", target, complaintDirection, null);
    }

    @Override
    public void notifyComplaintsSearchCompleted(String userEmail, String requestId, String target, String complaintDirection, Boolean hasApproximateResult) {
        sendComplaintsSearch(userEmail, requestId, "COMPLETED", target, complaintDirection, hasApproximateResult);
    }

    @Override
    public void notifyComplaintsSearchFailed(String userEmail, String requestId, String target, String complaintDirection) {
        sendComplaintsSearch(userEmail, requestId, "FAILED", target, complaintDirection, null);
    }

    private void sendAppReportStatus(String userEmail, String requestId, String status, String tipoReporte) {
        if (userEmail == null || requestId == null || status == null) return;
        try {
            String message = resolveAppReportStatusMessage(status);
            AiSearchProgressWS payload = new AiSearchProgressWS(
                    requestId,
                    AiSearchProgressStep.APP_REPORT_STATUS.name(),
                    status,
                    message,
                    null,
                    "APP_REPORT_STATUS",
                    tipoReporte
            );
            messagingTemplate.convertAndSendToUser(userEmail, Constantes.WS_QUEUE_AI_SEARCH_PROGRESS, payload);
        } catch (Exception ex) {
            LOGGER.warn("[AI][SEARCH_PROGRESS] ws-send-error step=APP_REPORT_STATUS status={} userEmail={} errorClass={}",
                    status, userEmail, ex.getClass().getSimpleName());
        }
    }

    private String resolveAppReportStatusMessage(String status) {
        return switch (status) {
            case "STARTED" -> "Buscando tus reportes...";
            case "COMPLETED" -> "Búsqueda de reportes finalizada";
            case "FAILED" -> "No se pudo consultar el estado del reporte";
            default -> "Reporte";
        };
    }

    private void sendComplaintsSearch(String userEmail,
                                      String requestId,
                                      String status,
                                      String target,
                                      String complaintDirection,
                                      Boolean hasApproximateResult) {
        if (userEmail == null || requestId == null || status == null) {
            return;
        }
        try {
            String normalizedDirection = normalizeComplaintDirection(complaintDirection);
            String normalizedTarget = normalizeComplaintTarget(target, normalizedDirection);
            String message = resolveComplaintsSearchMessage(status, normalizedDirection);
            AiSearchProgressWS payload = new AiSearchProgressWS(
                    requestId,
                    AiSearchProgressStep.COMPLAINTS_SEARCH.name(),
                    status,
                    message,
                    hasApproximateResult,
                    normalizedTarget,
                    null,
                    normalizedDirection
            );
            messagingTemplate.convertAndSendToUser(userEmail, Constantes.WS_QUEUE_AI_SEARCH_PROGRESS, payload);
        } catch (Exception ex) {
            LOGGER.warn("[AI][SEARCH_PROGRESS] ws-send-error step=COMPLAINTS_SEARCH status={} userEmail={} errorClass={}",
                    status, userEmail, ex.getClass().getSimpleName());
        }
    }

    private String normalizeComplaintDirection(String complaintDirection) {
        if (complaintDirection == null || complaintDirection.isBlank()) {
            return "ANY";
        }
        String normalized = complaintDirection.trim().toUpperCase();
        if ("COMPLAINT_RECEIVED".equals(normalized)) {
            return "RECEIVED";
        }
        if ("COMPLAINT_CREATED".equals(normalized)) {
            return "CREATED";
        }
        if ("RECEIVED".equals(normalized) || "CREATED".equals(normalized) || "ANY".equals(normalized)) {
            return normalized;
        }
        return "ANY";
    }

    private String normalizeComplaintTarget(String target, String complaintDirection) {
        if (target != null && !target.isBlank()) {
            String normalized = target.trim().toUpperCase();
            if ("MIXED".equals(normalized) || "COMPLAINTS_RECEIVED".equals(normalized) || "COMPLAINTS_CREATED".equals(normalized)) {
                return normalized;
            }
        }
        return switch (complaintDirection) {
            case "RECEIVED" -> "COMPLAINTS_RECEIVED";
            case "CREATED" -> "COMPLAINTS_CREATED";
            default -> "MIXED";
        };
    }

    private String resolveComplaintsSearchMessage(String status, String complaintDirection) {
        return switch (status) {
            case "STARTED" -> switch (complaintDirection) {
                case "RECEIVED" -> "Buscando denuncias recibidas...";
                case "CREATED" -> "Buscando denuncias realizadas...";
                default -> "Buscando denuncias...";
            };
            case "COMPLETED" -> "BÃºsqueda de denuncias finalizada";
            case "FAILED" -> "No se pudieron consultar las denuncias";
            default -> "Denuncias";
        };
    }

    private void send(String userEmail, String requestId, AiSearchProgressStep step, String status, Boolean hasApproximateResult) {
        if (userEmail == null || requestId == null || step == null) {
            return;
        }
        try {
            AiSearchProgressWS payload = new AiSearchProgressWS(
                    requestId,
                    step.name(),
                    status,
                    resolveMessage(step, status),
                    hasApproximateResult
            );
            messagingTemplate.convertAndSendToUser(userEmail, Constantes.WS_QUEUE_AI_SEARCH_PROGRESS, payload);
        } catch (Exception ex) {
            LOGGER.warn("[AI][SEARCH_PROGRESS] ws-send-error step={} status={} userEmail={} errorClass={}",
                    step, status, userEmail, ex.getClass().getSimpleName());
        }
    }

    private String resolveMessage(AiSearchProgressStep step, String status) {
        return switch (step) {
            case ANALYZING_CONTEXT -> "STARTED".equals(status) ? "Analizando contexto..." : "Contexto analizado";
            case ANALYZING_MESSAGES -> "STARTED".equals(status) ? "Analizando mensajes..." : "Mensajes analizados";
            case COMPLAINTS_SEARCH -> "STARTED".equals(status) ? "Buscando denuncias..." : "BÃºsqueda de denuncias finalizada";
            case MESSAGE_FOUND -> "Mensaje encontrado";
            case MESSAGE_NOT_FOUND -> "No se encontró una coincidencia clara";
            case APP_REPORT -> "STARTED".equals(status) ? "Generando reporte..." : "Reporte generado";
            case APP_REPORT_STATUS -> "STARTED".equals(status) ? "Buscando tus reportes..." : "Búsqueda de reportes finalizada";
            case ERROR -> "Error al procesar la búsqueda";
        };
    }
}
