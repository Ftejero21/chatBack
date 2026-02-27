package com.chat.chat.Security;

import com.chat.chat.Utils.Constantes;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {
    private static final String LOG_JWT_FILTER_REQUEST = "JwtAuthFilter - Filtro Request: [";
    private static final String LOG_JWT_FILTER_SEPARATOR = "] ";
    private static final String LOG_JWT_NO_BEARER = "JwtAuthFilter - No Authorization Header found or doesn't start with Bearer";
    private static final String LOG_JWT_VALID = "JWT Validado exitosamente para usuario: ";
    private static final String LOG_JWT_INVALID = "JWT Token Invalido para usuario: ";
    private static final String LOG_JWT_PROCESS_ERROR = "Error procesando JWT: ";


    @Autowired
    private JwtService jwtService;

    @Autowired
    private CustomUserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        final String authHeader = request.getHeader(Constantes.HEADER_AUTHORIZATION);
        final String jwt;
        final String userEmail;

        System.out.println(LOG_JWT_FILTER_REQUEST + request.getMethod() + LOG_JWT_FILTER_SEPARATOR + request.getRequestURI());

        // Si no hay cabecera Authorization o no empieza con Bearer, continuamos
        // filtrando sin autenticar
        if (authHeader == null || !authHeader.startsWith(Constantes.BEARER_PREFIX)) {
            System.out.println(LOG_JWT_NO_BEARER);
            filterChain.doFilter(request, response);
            return;
        }

        jwt = authHeader.substring(Constantes.BEARER_PREFIX.length());

        try {
            userEmail = jwtService.extractUsername(jwt);

            // Si el token tiene email y el contexto de seguridad aún no está autenticado
            if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = this.userDetailsService.loadUserByUsername(userEmail);

                if (jwtService.isTokenValid(jwt, userDetails)) {
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities());
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    // Asignamos el usuario autenticado al contexto de Spring Security
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                    System.out.println(LOG_JWT_VALID + userEmail);
                } else {
                    System.err.println(LOG_JWT_INVALID + userEmail);
                }
            }
        } catch (Exception e) {
            System.err.println(LOG_JWT_PROCESS_ERROR + e.getMessage());
            e.printStackTrace();
        }

        filterChain.doFilter(request, response);
    }
}
