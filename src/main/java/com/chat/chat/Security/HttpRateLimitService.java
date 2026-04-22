package com.chat.chat.Security;

import com.chat.chat.Exceptions.TooManyRequestsException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Locale;

@Service
public class HttpRateLimitService {

    private static final int LOGIN_LIMIT = 8;
    private static final Duration LOGIN_WINDOW = Duration.ofMinutes(10);

    private static final int RECOVERY_REQUEST_LIMIT = 5;
    private static final Duration RECOVERY_REQUEST_WINDOW = Duration.ofMinutes(30);

    private static final int RECOVERY_VERIFY_LIMIT = 8;
    private static final Duration RECOVERY_VERIFY_WINDOW = Duration.ofMinutes(30);
    private static final int REGISTRATION_OTP_REQUEST_LIMIT = 5;
    private static final Duration REGISTRATION_OTP_REQUEST_WINDOW = Duration.ofMinutes(30);
    private static final int REGISTRATION_OTP_VERIFY_LIMIT = 12;
    private static final Duration REGISTRATION_OTP_VERIFY_WINDOW = Duration.ofMinutes(30);

    private static final int UNBAN_APPEAL_BY_IP_LIMIT = 10;
    private static final Duration UNBAN_APPEAL_BY_IP_WINDOW = Duration.ofHours(1);
    private static final int UNBAN_APPEAL_BY_EMAIL_LIMIT = 3;
    private static final Duration UNBAN_APPEAL_BY_EMAIL_WINDOW = Duration.ofHours(24);
    private static final int UNBAN_APPEAL_BY_IP_EMAIL_LIMIT = 3;
    private static final Duration UNBAN_APPEAL_BY_IP_EMAIL_WINDOW = Duration.ofHours(6);
    private static final int CHAT_CLOSED_REPORT_LIMIT = 6;
    private static final Duration CHAT_CLOSED_REPORT_WINDOW = Duration.ofHours(24);
    private static final int GROUP_INVITE_BY_INVITER_LIMIT = 20;
    private static final Duration GROUP_INVITE_BY_INVITER_WINDOW = Duration.ofMinutes(10);
    private static final int GROUP_INVITE_BY_GROUP_LIMIT = 50;
    private static final Duration GROUP_INVITE_BY_GROUP_WINDOW = Duration.ofMinutes(10);
    private static final int GROUP_INVITE_BY_INVITEE_LIMIT = 15;
    private static final Duration GROUP_INVITE_BY_INVITEE_WINDOW = Duration.ofMinutes(10);
    private static final int USER_COMPLAINT_BY_REPORTER_LIMIT = 8;
    private static final Duration USER_COMPLAINT_BY_REPORTER_WINDOW = Duration.ofHours(1);
    private static final int USER_COMPLAINT_BY_REPORTER_BURST_LIMIT = 3;
    private static final Duration USER_COMPLAINT_BY_REPORTER_BURST_WINDOW = Duration.ofMinutes(10);
    private static final int USER_COMPLAINT_BY_REPORTER_TARGET_LIMIT = 1;
    private static final Duration USER_COMPLAINT_BY_REPORTER_TARGET_WINDOW = Duration.ofHours(12);

    private static final int ADMIN_HTTP_LIMIT = 180;
    private static final Duration ADMIN_HTTP_WINDOW = Duration.ofMinutes(1);
    private static final int UPLOAD_LIMIT = 40;
    private static final Duration UPLOAD_WINDOW = Duration.ofMinutes(5);
    private static final int E2E_BACKUP_PUT_LIMIT = 15;
    private static final Duration E2E_BACKUP_PUT_WINDOW = Duration.ofMinutes(5);
    private static final int E2E_BACKUP_GET_LIMIT = 60;
    private static final Duration E2E_BACKUP_GET_WINDOW = Duration.ofMinutes(1);

    private final InMemoryRateLimiterService limiter;
    private final ClientIpResolver clientIpResolver;

    public HttpRateLimitService(InMemoryRateLimiterService limiter, ClientIpResolver clientIpResolver) {
        this.limiter = limiter;
        this.clientIpResolver = clientIpResolver;
    }

    public void checkLogin(HttpServletRequest request, String email) {
        String ip = clientIpResolver.resolve(request);
        String identity = normalizeIdentity(email);
        String key = "http:login:" + ip + ":" + identity;
        enforce(key, LOGIN_LIMIT, LOGIN_WINDOW, "Demasiados intentos de login. Intenta mas tarde.");
    }

    public void checkPasswordRecoveryRequest(HttpServletRequest request, String email) {
        String ip = clientIpResolver.resolve(request);
        String identity = normalizeIdentity(email);
        String key = "http:recovery:request:" + ip + ":" + identity;
        enforce(key, RECOVERY_REQUEST_LIMIT, RECOVERY_REQUEST_WINDOW,
                "Demasiadas solicitudes de recuperacion. Intenta mas tarde.");
    }

    public void checkPasswordRecoveryVerify(HttpServletRequest request, String email) {
        String ip = clientIpResolver.resolve(request);
        String identity = normalizeIdentity(email);
        String key = "http:recovery:verify:" + ip + ":" + identity;
        enforce(key, RECOVERY_VERIFY_LIMIT, RECOVERY_VERIFY_WINDOW,
                "Demasiados intentos de verificacion de codigo. Intenta mas tarde.");
    }

    public void checkRegistrationOtpRequest(HttpServletRequest request, String email) {
        String ip = clientIpResolver.resolve(request);
        String identity = normalizeIdentity(email);
        String key = "http:registration:otp:request:" + ip + ":" + identity;
        enforce(key, REGISTRATION_OTP_REQUEST_LIMIT, REGISTRATION_OTP_REQUEST_WINDOW,
                "Demasiadas solicitudes de verificacion de registro. Intenta mas tarde.");
    }

    public void checkRegistrationOtpVerify(HttpServletRequest request, String email) {
        String ip = clientIpResolver.resolve(request);
        String identity = normalizeIdentity(email);
        String key = "http:registration:otp:verify:" + ip + ":" + identity;
        enforce(key, REGISTRATION_OTP_VERIFY_LIMIT, REGISTRATION_OTP_VERIFY_WINDOW,
                "Demasiados intentos de verificacion de registro. Intenta mas tarde.");
    }

    public void checkUnbanAppeal(HttpServletRequest request, String email) {
        String ip = clientIpResolver.resolve(request);
        String identity = normalizeIdentity(email);
        enforce("http:unban-appeal:ip:" + ip, UNBAN_APPEAL_BY_IP_LIMIT, UNBAN_APPEAL_BY_IP_WINDOW,
                "No se pudo procesar la solicitud en este momento. Intenta mas tarde.");
        enforce("http:unban-appeal:email:" + identity, UNBAN_APPEAL_BY_EMAIL_LIMIT, UNBAN_APPEAL_BY_EMAIL_WINDOW,
                "No se pudo procesar la solicitud en este momento. Intenta mas tarde.");
        enforce("http:unban-appeal:ip-email:" + ip + ":" + identity, UNBAN_APPEAL_BY_IP_EMAIL_LIMIT, UNBAN_APPEAL_BY_IP_EMAIL_WINDOW,
                "No se pudo procesar la solicitud en este momento. Intenta mas tarde.");
    }

    public void checkChatClosedReport(HttpServletRequest request, Long chatId) {
        String ip = clientIpResolver.resolve(request);
        String userKey = authenticatedUserKey();
        String key = "http:chat-closed-report:" + normalizeIdentity(String.valueOf(chatId)) + ":" + ip + ":" + userKey;
        enforce(key, CHAT_CLOSED_REPORT_LIMIT, CHAT_CLOSED_REPORT_WINDOW,
                "Demasiados reportes de chat cerrado para este origen. Intenta mas tarde.");
    }

    public void checkGroupInviteCreate(Long inviterId, Long groupId, Long inviteeId) {
        String inviterKey = normalizeIdentity(String.valueOf(inviterId));
        String groupKey = normalizeIdentity(String.valueOf(groupId));
        String inviteeKey = normalizeIdentity(String.valueOf(inviteeId));
        String message = "Demasiadas invitaciones de grupo. Intenta mas tarde.";

        enforce("svc:group-invite:inviter:" + inviterKey,
                GROUP_INVITE_BY_INVITER_LIMIT,
                GROUP_INVITE_BY_INVITER_WINDOW,
                message);
        enforce("svc:group-invite:group:" + groupKey,
                GROUP_INVITE_BY_GROUP_LIMIT,
                GROUP_INVITE_BY_GROUP_WINDOW,
                message);
        enforce("svc:group-invite:invitee:" + inviteeKey,
                GROUP_INVITE_BY_INVITEE_LIMIT,
                GROUP_INVITE_BY_INVITEE_WINDOW,
                message);
    }

    public void checkUserComplaintCreate(Long reporterId, Long targetUserId) {
        String reporterKey = normalizeIdentity(String.valueOf(reporterId));
        String targetKey = normalizeIdentity(String.valueOf(targetUserId));
        String message = "Demasiadas denuncias en este momento. Intenta mas tarde.";

        enforce("svc:user-complaint:reporter:" + reporterKey,
                USER_COMPLAINT_BY_REPORTER_LIMIT,
                USER_COMPLAINT_BY_REPORTER_WINDOW,
                message);
        enforce("svc:user-complaint:reporter-burst:" + reporterKey,
                USER_COMPLAINT_BY_REPORTER_BURST_LIMIT,
                USER_COMPLAINT_BY_REPORTER_BURST_WINDOW,
                message);
        enforce("svc:user-complaint:reporter-target:" + reporterKey + ":" + targetKey,
                USER_COMPLAINT_BY_REPORTER_TARGET_LIMIT,
                USER_COMPLAINT_BY_REPORTER_TARGET_WINDOW,
                "Debes esperar antes de volver a denunciar al mismo usuario.");
    }

    public void checkAdminEndpoint(HttpServletRequest request, String endpointKey) {
        String ip = clientIpResolver.resolve(request);
        String userKey = authenticatedUserKey();
        String key = "http:admin:" + endpointKey + ":" + ip + ":" + userKey;
        enforce(key, ADMIN_HTTP_LIMIT, ADMIN_HTTP_WINDOW, "Demasiadas operaciones administrativas. Intenta mas tarde.");
    }

    public void checkUpload(HttpServletRequest request, String uploadType) {
        String ip = clientIpResolver.resolve(request);
        String userKey = authenticatedUserKey();
        String normalizedType = normalizeIdentity(uploadType);
        String key = "http:upload:" + normalizedType + ":" + ip + ":" + userKey;
        enforce(key, UPLOAD_LIMIT, UPLOAD_WINDOW, "Demasiadas subidas de archivo. Intenta mas tarde.");
    }

    public void checkE2EPrivateKeyBackupPut(HttpServletRequest request, Long userId) {
        String ip = clientIpResolver.resolve(request);
        String userKey = authenticatedUserKey();
        String key = "http:e2e:backup:put:" + normalizeIdentity(String.valueOf(userId)) + ":" + ip + ":" + userKey;
        enforce(key, E2E_BACKUP_PUT_LIMIT, E2E_BACKUP_PUT_WINDOW,
                "Demasiadas actualizaciones de backup E2E. Intenta mas tarde.");
    }

    public void checkE2EPrivateKeyBackupGet(HttpServletRequest request, Long userId) {
        String ip = clientIpResolver.resolve(request);
        String userKey = authenticatedUserKey();
        String key = "http:e2e:backup:get:" + normalizeIdentity(String.valueOf(userId)) + ":" + ip + ":" + userKey;
        enforce(key, E2E_BACKUP_GET_LIMIT, E2E_BACKUP_GET_WINDOW,
                "Demasiadas consultas de backup E2E. Intenta mas tarde.");
    }

    private void enforce(String key, int limit, Duration window, String baseMessage) {
        RateLimitDecision decision = limiter.consume(key, limit, window);
        if (decision.allowed()) {
            return;
        }
        long retryAfter = decision.retryAfterSeconds();
        throw new TooManyRequestsException(baseMessage + " Reintenta en " + retryAfter + " segundos.", retryAfter);
    }

    private String normalizeIdentity(String raw) {
        if (raw == null || raw.isBlank()) {
            return "-";
        }
        return raw.trim().toLowerCase(Locale.ROOT);
    }

    private String authenticatedUserKey() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return "anon";
        }
        String name = auth.getName();
        if (name == null || name.isBlank() || "anonymousUser".equalsIgnoreCase(name)) {
            return "anon";
        }
        return name.trim().toLowerCase(Locale.ROOT);
    }
}
