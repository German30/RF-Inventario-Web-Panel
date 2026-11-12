package com.rf.panel.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/** Lee el `Authorization: Bearer ...` y deja la {@link Sesion} en el contexto. */
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwt;

    public JwtAuthFilter(JwtService jwt) {
        this.jwt = jwt;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest peticion,
                                    @NonNull HttpServletResponse respuesta,
                                    @NonNull FilterChain cadena) throws ServletException, IOException {
        String token = extraer(peticion);

        if (token != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            jwt.leer(token).ifPresent(sesion -> {
                // Dos identidades separadas, una autoridad por token: ROLE_PANEL
                // para el login del panel web (tabla panel_admins), ROLE_OPERARIO
                // para el de la app Android (tabla users).
                // Un operario SUPERVISOR suma ROLE_SUPERVISOR (aprobar conteos,
                // borrar, gestionar ubicaciones/pedidos), sin acceso al panel.
                var roles = sesion.esPanel()
                        ? AuthorityUtils.createAuthorityList("ROLE_PANEL")
                        : Sesion.ROL_SUPERVISOR.equals(sesion.rol())
                        ? AuthorityUtils.createAuthorityList("ROLE_OPERARIO", "ROLE_SUPERVISOR")
                        : AuthorityUtils.createAuthorityList("ROLE_OPERARIO");
                var auth = new UsernamePasswordAuthenticationToken(sesion, null, roles);
                auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(peticion));
                SecurityContextHolder.getContext().setAuthentication(auth);
            });
        }

        cadena.doFilter(peticion, respuesta);
    }

    private String extraer(HttpServletRequest peticion) {
        String cabecera = peticion.getHeader("Authorization");
        if (cabecera != null && cabecera.startsWith("Bearer ")) {
            return cabecera.substring(7);
        }
        return null;
    }
}
