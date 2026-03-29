package com.chat.chat.Security;

import com.chat.chat.Utils.Constantes;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class SecurityDebugAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private static final Logger LOGGER = LoggerFactory.getLogger(SecurityDebugAuthenticationEntryPoint.class);

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        String jwtReason = (String) request.getAttribute(JwtAuthFilter.ATTR_AUTH_FAILURE_REASON);
        String corsReason = (String) request.getAttribute(CorsPreflightDebugFilter.ATTR_CORS_FAILURE_REASON);
        String reason = jwtReason != null ? jwtReason : (corsReason != null ? "CORS_PREFLIGHT_" + corsReason : "AUTH_REQUIRED");

        if (isUploadsApi(request)) {
            LOGGER.warn("[SEC_403_DEBUG] type=AUTH_FAIL method={} uri={} reason={} message={}",
                    request.getMethod(),
                    request.getRequestURI(),
                    reason,
                    authException.getClass().getSimpleName());
        }

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write("{\"code\":\"" + Constantes.ERR_NO_AUTORIZADO + "\",\"message\":\"Autenticacion requerida o JWT invalido/expirado\"}");
    }

    private boolean isUploadsApi(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return uri != null && uri.startsWith(Constantes.API_UPLOADS_ALL);
    }
}
