package com.rf.panel.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;
import java.util.Optional;

/** Firma y lee el token de sesión del panel. */
@Service
public class JwtService {

    private final SecretKey clave;
    private final Duration duracion;

    public JwtService(
            @Value("${rf.panel.jwt.secreto}") String secreto,
            @Value("${rf.panel.jwt.duracion-horas:12}") long duracionHoras
    ) {
        byte[] bytes = secreto.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalStateException(
                    "rf.panel.jwt.secreto necesita al menos 32 caracteres para firmar con HS256.");
        }
        this.clave = Keys.hmacShaKeyFor(bytes);
        this.duracion = Duration.ofHours(duracionHoras);
    }

    /**
     * Firma un token. `rol` viaja tal cual en el claim y es lo que después
     * separa un token del panel ({@link Sesion#ROL_PANEL}) de uno de la app.
     */
    public String emitir(String codigo, String nombre, String rol) {
        Date ahora = new Date();
        return Jwts.builder()
                .subject(codigo)
                .claim("nombre", nombre)
                .claim("rol", rol)
                .issuedAt(ahora)
                .expiration(new Date(ahora.getTime() + duracion.toMillis()))
                .signWith(clave)
                .compact();
    }

    /** Devuelve la sesión si el token es válido y no expiró; vacío en cualquier otro caso. */
    public Optional<Sesion> leer(String token) {
        try {
            Claims c = Jwts.parser().verifyWith(clave).build()
                    .parseSignedClaims(token).getPayload();
            return Optional.of(new Sesion(c.getSubject(), c.get("nombre", String.class), c.get("rol", String.class)));
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    public long segundosDeVida() {
        return duracion.toSeconds();
    }
}
