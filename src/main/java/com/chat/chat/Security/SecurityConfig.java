package com.chat.chat.Security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.http.HttpMethod;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import com.chat.chat.Utils.Constantes;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private static final String MATCH_ALL = "/**";
    private static final String SWAGGER_UI = "/swagger-ui/**";
    private static final String SWAGGER_UI_HTML = "/swagger-ui.html";
    private static final String API_DOCS = "/v3/api-docs/**";
    private static final String CORS_METHOD_GET = "GET";
    private static final String CORS_METHOD_POST = "POST";
    private static final String CORS_METHOD_PUT = "PUT";
    private static final String CORS_METHOD_DELETE = "DELETE";
    private static final String CORS_METHOD_OPTIONS = "OPTIONS";
    private static final String CORS_METHOD_PATCH = "PATCH";
    private static final String CORS_METHOD_HEAD = "HEAD";

    @Autowired
    private JwtAuthFilter jwtAuthFilter;

    @Autowired
    private CustomUserDetailsService userDetailsService;

    @Bean
    public FilterRegistrationBean<JwtAuthFilter> jwtAuthFilterRegistration(JwtAuthFilter filter) {
        FilterRegistrationBean<JwtAuthFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false); // 🔥 Evita que Spring Boot lo registre globalmente fuera de Spring Security
        return registration;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable()) // Deshabilitar CSRF porque usamos JWT
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, MATCH_ALL).permitAll() // Permite
                                                                                                         // Preflight
                                                                                                         // globalmente
                        .requestMatchers(SWAGGER_UI, SWAGGER_UI_HTML, API_DOCS).permitAll()
                        .requestMatchers(Constantes.USUARIO_API + Constantes.LOGIN, Constantes.USUARIO_API + Constantes.REGISTRO, Constantes.USUARIO_API,
                                Constantes.USUARIO_API + Constantes.RECUPERAR_PASSWORD_ALL,
                                Constantes.USUARIO_API + Constantes.SOLICITUD_DESBANEO_CREATE)
                        .permitAll() // Login, Registro y Recuperación de Password
                        // públicos
                        .requestMatchers(Constantes.WS_ENDPOINT_PATTERN).permitAll() // WebSocket endpoint inicial público (la
                                                                    // autenticación se hará en los interceptores STOMP)
                        .requestMatchers(Constantes.UPLOADS_PATTERN).permitAll() // Dejar la carpeta uploads como pública para poder
                        .requestMatchers(Constantes.USUARIO_API + Constantes.USUARIO_ADMIN_PATTERN).hasRole(Constantes.ADMIN) // Solo Administradores
                        .requestMatchers(Constantes.API_AI_PATTERN).hasRole(Constantes.ADMIN)
                        .anyRequest().authenticated())
                .sessionManagement(sess -> sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS)) // No usamos
                                                                                                        // sesiones
                                                                                                        // (Cookies)
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        // Usamos BCrypt para comparar passwords
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // Permite cualquier origen (en producción cámbialo por tu dominio)
        configuration.setAllowedOrigins(Arrays.asList(Constantes.CORS_ORIGIN_LOCALHOST_4200, Constantes.CORS_ORIGIN_127_4200));
        configuration.setAllowedOriginPatterns(Arrays.asList(Constantes.CORS_ANY_ORIGIN));
        // Permite TODOS los métodos, incluyendo OPTIONS que es crucial para Pre-flight
        // requests
        configuration.setAllowedMethods(Arrays.asList(CORS_METHOD_GET, CORS_METHOD_POST, CORS_METHOD_PUT, CORS_METHOD_DELETE, CORS_METHOD_OPTIONS, CORS_METHOD_PATCH, CORS_METHOD_HEAD));
        // Permite TODAS las cabeceras, crucial para Authorization y Content-Type
        configuration.setAllowedHeaders(Arrays.asList(Constantes.CORS_ANY_ORIGIN));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        // Aplica esta configuración a TODOS los endpoints
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
