package com.chat.chat.Security;

import com.chat.chat.Utils.Constantes;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    public static final String ATTR_AUTH_FAILURE_REASON = "security.auth.failure.reason";

    private static final Logger LOGGER = LoggerFactory.getLogger(JwtAuthFilter.class);

    @Autowired
    private JwtService jwtService;

    @Autowired
    private CustomUserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        final String authHeader = request.getHeader(Constantes.HEADER_AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith(Constantes.BEARER_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        final String jwt = authHeader.substring(Constantes.BEARER_PREFIX.length());

        try {
            String userEmail = jwtService.extractUsername(jwt);

            if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = userDetailsService.loadUserByUsername(userEmail);

                if (jwtService.isTokenValid(jwt, userDetails)) {
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities());
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);

                    if (isUploadsApi(request)) {
                        LOGGER.info("[SEC_403_DEBUG] type=JWT_OK method={} uri={} user={}",
                                request.getMethod(),
                                request.getRequestURI(),
                                userEmail);
                    }
                } else {
                    request.setAttribute(ATTR_AUTH_FAILURE_REASON, "JWT_INVALID");
                    if (isUploadsApi(request)) {
                        LOGGER.warn("[SEC_403_DEBUG] type=JWT_FAIL method={} uri={} reason=JWT_INVALID user={}",
                                request.getMethod(),
                                request.getRequestURI(),
                                userEmail);
                    }
                }
            }
        } catch (ExpiredJwtException ex) {
            request.setAttribute(ATTR_AUTH_FAILURE_REASON, "JWT_EXPIRED");
            if (isUploadsApi(request)) {
                LOGGER.warn("[SEC_403_DEBUG] type=JWT_FAIL method={} uri={} reason=JWT_EXPIRED message={}",
                        request.getMethod(),
                        request.getRequestURI(),
                        ex.getMessage());
            }
        } catch (UsernameNotFoundException ex) {
            request.setAttribute(ATTR_AUTH_FAILURE_REASON, "JWT_USER_NOT_FOUND");
            if (isUploadsApi(request)) {
                LOGGER.warn("[SEC_403_DEBUG] type=JWT_FAIL method={} uri={} reason=JWT_USER_NOT_FOUND message={}",
                        request.getMethod(),
                        request.getRequestURI(),
                        ex.getMessage());
            }
        } catch (JwtException | IllegalArgumentException ex) {
            request.setAttribute(ATTR_AUTH_FAILURE_REASON, "JWT_INVALID");
            if (isUploadsApi(request)) {
                LOGGER.warn("[SEC_403_DEBUG] type=JWT_FAIL method={} uri={} reason=JWT_INVALID message={}",
                        request.getMethod(),
                        request.getRequestURI(),
                        ex.getMessage());
            }
        }

        filterChain.doFilter(request, response);
    }

    private boolean isUploadsApi(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return uri != null && uri.startsWith(Constantes.API_UPLOADS_ALL);
    }
}
