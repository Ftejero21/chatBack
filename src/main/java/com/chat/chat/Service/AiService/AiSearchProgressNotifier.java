package com.chat.chat.Service.AiService;

import com.chat.chat.Utils.AiSearchProgressStep;

public interface AiSearchProgressNotifier {

    void notifyStarted(String userEmail, String requestId, AiSearchProgressStep step);

    void notifyCompleted(String userEmail, String requestId, AiSearchProgressStep step);

    void notifyCompleted(String userEmail, String requestId, AiSearchProgressStep step, Boolean hasApproximateResult);

    void notifyError(String userEmail, String requestId);

    /** APP_REPORT progress events. tipoReporte goes in payload (no message body). */
    void notifyAppReportStarted(String userEmail, String requestId, String tipoReporte);
    void notifyAppReportCompleted(String userEmail, String requestId, String tipoReporte);
    void notifyAppReportCompleted(String userEmail, String requestId, String tipoReporte, boolean hasImage);
    void notifyAppReportFailed(String userEmail, String requestId, String tipoReporte);

    /** APP_REPORT_STATUS progress events. */
    void notifyAppReportStatusStarted(String userEmail, String requestId, String tipoReporte);
    void notifyAppReportStatusCompleted(String userEmail, String requestId, String tipoReporte);
    void notifyAppReportStatusFailed(String userEmail, String requestId, String tipoReporte);

    /** COMPLAINTS_SEARCH progress events. */
    void notifyComplaintsSearchStarted(String userEmail, String requestId, String target, String complaintDirection);
    void notifyComplaintsSearchCompleted(String userEmail, String requestId, String target, String complaintDirection, Boolean hasApproximateResult);
    void notifyComplaintsSearchFailed(String userEmail, String requestId, String target, String complaintDirection);
}
