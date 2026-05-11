package com.chat.chat.DTO;

import java.time.Instant;

public class AiSearchProgressWS {

    private String requestId;
    private String step;
    private String status;
    private String message;
    private String timestamp;
    private Boolean hasApproximateResult;
    private String target;
    private String tipoReporte;
    private String complaintDirection;

    public AiSearchProgressWS(String requestId, String step, String status, String message, Boolean hasApproximateResult) {
        this.requestId = requestId;
        this.step = step;
        this.status = status;
        this.message = message;
        this.timestamp = Instant.now().toString();
        this.hasApproximateResult = hasApproximateResult;
    }

    public AiSearchProgressWS(String requestId, String step, String status, String message,
                              Boolean hasApproximateResult, String target, String tipoReporte) {
        this(requestId, step, status, message, hasApproximateResult);
        this.target = target;
        this.tipoReporte = tipoReporte;
    }

    public AiSearchProgressWS(String requestId, String step, String status, String message,
                              Boolean hasApproximateResult, String target, String tipoReporte, String complaintDirection) {
        this(requestId, step, status, message, hasApproximateResult, target, tipoReporte);
        this.complaintDirection = complaintDirection;
    }

    public String getTarget() { return target; }
    public String getTipoReporte() { return tipoReporte; }
    public String getComplaintDirection() { return complaintDirection; }

    public String getRequestId() {
        return requestId;
    }

    public String getStep() {
        return step;
    }

    public String getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public Boolean getHasApproximateResult() {
        return hasApproximateResult;
    }
}
