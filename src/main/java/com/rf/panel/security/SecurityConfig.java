package com.rf.panel.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rf.panel.dto.ErrorResponse;
import jakarta.servlet.DispatcherType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Quién entra y quién no.
 *
 * Sin sesión en servidor: el token lleva el operario, así que el backend puede
 * correr replicado sin sesiones pegajosas. Se apoya en {@link JwtAuthFilter}.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    /** Rutas abiertas: login, healthcheck y documentación. */
    private static final String[] PUBLICAS = {
            "/api/auth/login",
            "/api/panel/auth/login",
            "/api/ping",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html"
    };

    private final String[] origenesPermitidos;

    public SecurityConfig(@Value("${rf.cors.allowed-origins}") String[] origenesPermitidos) {
        this.origenesPermitidos = origenesPermitidos;
    }

    @Bean
    public SecurityFilterChain filtros(HttpSecurity http, JwtAuthFilter jwtAuthFilter,
                                       ObjectMapper json) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(rutas -> rutas
                        // Spring reenvía cualquier excepción (404, 500...) a /error en un
                        // dispatch aparte, donde el JwtAuthFilter no corre. Si /error exige
                        // sesión, todo error llega al cliente como 401 y la app cierra la
                        // sesión del operario por un fallo que no tiene nada que ver.
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                        .requestMatchers("/error").permitAll()
                        .requestMatchers(PUBLICAS).permitAll()
                        .anyRequest().authenticated())
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling(e -> e
                        .authenticationEntryPoint((peticion, respuesta, error) ->
                                escribir(json, respuesta, HttpStatus.UNAUTHORIZED,
                                        "Sesión no válida o expirada. Volvé a entrar."))
                        .accessDeniedHandler((peticion, respuesta, error) ->
                                escribir(json, respuesta, HttpStatus.FORBIDDEN,
                                        "Tu cuenta no tiene acceso a esto.")))
                .build();
    }

    private void escribir(ObjectMapper json, jakarta.servlet.http.HttpServletResponse respuesta,
                          HttpStatus status, String mensaje) throws java.io.IOException {
        respuesta.setStatus(status.value());
        respuesta.setContentType(MediaType.APPLICATION_JSON_VALUE);
        respuesta.setCharacterEncoding("UTF-8");
        json.writeValue(respuesta.getWriter(),
                ErrorResponse.de(status.value(), status.getReasonPhrase(), mensaje));
    }

    /** BCrypt: lento a propósito (resiste fuerza bruta) y con salt por fila. */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration cfg = new CorsConfiguration();
        cfg.setAllowedOrigins(Arrays.asList(origenesPermitidos));
        cfg.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        cfg.setAllowedHeaders(List.of("*"));
        cfg.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource fuente = new UrlBasedCorsConfigurationSource();
        fuente.registerCorsConfiguration("/api/**", cfg);
        return fuente;
    }
}
