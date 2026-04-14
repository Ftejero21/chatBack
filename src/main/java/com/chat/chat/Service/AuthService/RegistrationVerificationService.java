package com.chat.chat.Service.AuthService;

import com.chat.chat.Service.EmailService.EmailService;
import com.chat.chat.Utils.Constantes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
public class RegistrationVerificationService {
    private static final Logger LOGGER = LoggerFactory.getLogger(RegistrationVerificationService.class);
    private static final SecureRandom RANDOM = new SecureRandom();

    @Autowired
    private EmailService emailService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Value("${app.auth.registration.otp-expiration-minutes:7}")
    private long otpExpirationMinutes;

    private final ConcurrentMap<String, PendingRegistration> pendingByEmail = new ConcurrentHashMap<>();

    public void startVerification(String email, String rawPassword, String ipAddress) {
        String normalizedEmail = normalizeEmail(email);
        validateCredentials(normalizedEmail, rawPassword);

        String otp = generateOtpCode();
        Instant now = Instant.now();
        PendingRegistration pending = pendingByEmail.compute(normalizedEmail, (key, existing) -> {
            PendingRegistration out = existing == null ? new PendingRegistration() : existing;
            out.email = normalizedEmail;
            out.passwordHash = passwordEncoder.encode(rawPassword);
            out.otpHash = passwordEncoder.encode(otp);
            out.expiresAt = now.plus(Duration.ofMinutes(otpExpirationMinutes));
            out.failedAttempts = 0;
            out.lockedUntil = null;
            out.updatedAt = now;
            out.createdAt = out.createdAt == null ? now : out.createdAt;
            return out;
        });

        emailService.sendHtmlEmailOrThrow(
                normalizedEmail,
                Constantes.EMAIL_SUBJECT_REGISTRATION_VERIFICATION,
                Constantes.EMAIL_TEMPLATE_REGISTRATION_VERIFICATION,
                Map.of(
                        Constantes.KEY_CODE, otp,
                        Constantes.KEY_MINUTES, String.valueOf(otpExpirationMinutes),
                        Constantes.KEY_TITLE, Constantes.TITLE_REGISTRATION_VERIFICATION
                )
        );

        LOGGER.info("[AUTH][REG_VERIFICATION] stage=OTP_SENT email={} ip={} expiresAt={} pendingSince={}",
                normalizedEmail, safe(ipAddress), pending.expiresAt, pending.createdAt);
    }

    public boolean verifyCode(String email, String rawPassword, String rawCode, String ipAddress) {
        String normalizedEmail = normalizeEmail(email);
        if (rawCode == null || rawCode.isBlank()) {
            LOGGER.warn("[AUTH][REG_VERIFICATION] stage=VERIFY_REJECT reason=CODE_MISSING email={} ip={}",
                    normalizedEmail, safe(ipAddress));
            return false;
        }

        PendingRegistration pending = pendingByEmail.get(normalizedEmail);
        if (pending == null) {
            LOGGER.warn("[AUTH][REG_VERIFICATION] stage=VERIFY_REJECT reason=PENDING_NOT_FOUND email={} ip={}",
                    normalizedEmail, safe(ipAddress));
            return false;
        }

        synchronized (pending) {
            Instant now = Instant.now();

            if (pending.lockedUntil != null && now.isBefore(pending.lockedUntil)) {
                LOGGER.warn("[AUTH][REG_VERIFICATION] stage=VERIFY_REJECT reason=LOCKED email={} ip={} lockedUntil={}",
                        normalizedEmail, safe(ipAddress), pending.lockedUntil);
                return false;
            }

            if (pending.expiresAt == null || now.isAfter(pending.expiresAt)) {
                pendingByEmail.remove(normalizedEmail);
                LOGGER.warn("[AUTH][REG_VERIFICATION] stage=VERIFY_REJECT reason=EXPIRED email={} ip={}",
                        normalizedEmail, safe(ipAddress));
                return false;
            }

            boolean passwordMatches = rawPassword != null && !rawPassword.isBlank()
                    && passwordEncoder.matches(rawPassword, pending.passwordHash);
            boolean codeMatches = passwordEncoder.matches(rawCode.trim(), pending.otpHash);

            if (!passwordMatches || !codeMatches) {
                pending.failedAttempts++;
                pending.lockedUntil = resolveLockUntil(now, pending.failedAttempts);
                LOGGER.warn("[AUTH][REG_VERIFICATION] stage=VERIFY_REJECT reason=INVALID_CREDENTIALS email={} ip={} failedAttempts={} lockedUntil={}",
                        normalizedEmail, safe(ipAddress), pending.failedAttempts, pending.lockedUntil);
                return false;
            }

            pendingByEmail.remove(normalizedEmail);
            LOGGER.info("[AUTH][REG_VERIFICATION] stage=VERIFY_OK email={} ip={} createdAt={} verifiedAt={}",
                    normalizedEmail, safe(ipAddress), pending.createdAt, now);
            return true;
        }
    }

    public void invalidatePending(String email) {
        String normalizedEmail = normalizeEmail(email);
        pendingByEmail.remove(normalizedEmail);
    }

    private String generateOtpCode() {
        return String.valueOf(100000 + RANDOM.nextInt(900000));
    }

    private Instant resolveLockUntil(Instant now, int failedAttempts) {
        if (failedAttempts >= 9) {
            return now.plus(Duration.ofMinutes(15));
        }
        if (failedAttempts >= 6) {
            return now.plus(Duration.ofMinutes(5));
        }
        if (failedAttempts >= 3) {
            return now.plus(Duration.ofMinutes(1));
        }
        return null;
    }

    private void validateCredentials(String email, String rawPassword) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException(Constantes.MSG_EMAIL_REQUERIDO);
        }
        if (rawPassword == null || rawPassword.isBlank()) {
            throw new IllegalArgumentException("Password es requerida");
        }
    }

    private String normalizeEmail(String email) {
        if (email == null) {
            return null;
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String safe(String value) {
        if (value == null || value.isBlank()) {
            return "-";
        }
        return value;
    }

    private static class PendingRegistration {
        private String email;
        private String passwordHash;
        private String otpHash;
        private Instant expiresAt;
        private int failedAttempts;
        private Instant lockedUntil;
        private Instant createdAt;
        private Instant updatedAt;
    }
}
