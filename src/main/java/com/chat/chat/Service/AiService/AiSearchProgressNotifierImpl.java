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
        sendAppReport(userEmail, requestId, "COMPLETED", tipoReporte);
    }

    @Override
    public void notifyAppReportFailed(String userEmail, String requestId, String tipoReporte) {
        sendAppReport(userEmail, requestId, "FAILED", tipoReporte);
    }

    private void sendAppReport(String userEmail, String requestId, String status, String tipoReporte) {
        if (userEmail == null || requestId == null || status == null) {
            return;
        }
        try {
            String message = resolveAppReportMessage(status);
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

    private String resolveAppReportMessage(String status) {
        return switch (status) {
            case "STARTED" -> "Generando reporte...";
            case "COMPLETED" -> "Reporte generado";
            case "FAILED" -> "No se pudo generar el reporte";
            default -> "Reporte";
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
            case MESSAGE_FOUND -> "Mensaje encontrado";
            case MESSAGE_NOT_FOUND -> "No se encontró una coincidencia clara";
            case APP_REPORT -> "STARTED".equals(status) ? "Generando reporte..." : "Reporte generado";
            case ERROR -> "Error al procesar la búsqueda";
        };
    }
}
