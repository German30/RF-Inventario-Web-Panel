package com.rf.panel.security;

import com.rf.panel.exception.DemasiadosIntentosException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Bloqueo temporal por fuerza bruta: tras N PINs fallidos seguidos para el
 * mismo código, ese código queda bloqueado unos minutos. Un PIN de 4 dígitos
 * son solo 10.000 combinaciones; sin esto se prueban todas en minutos.
 *
 * En memoria a propósito (sin tabla): con una sola instancia alcanza, y un
 * reinicio que desbloquea no es un riesgo real. Si el backend se replica,
 * mover esto a una tabla o a Redis.
 */
@Component
public class IntentosLogin {

    private record Estado(int fallos, long bloqueadoHasta) {
    }

    private final ConcurrentHashMap<String, Estado> porCodigo = new ConcurrentHashMap<>();
    private final int maxFallos;
    private final long bloqueoMs;
    private final Clock reloj;

    @Autowired
    public IntentosLogin(@Value("${rf.panel.login.max-fallos:5}") int maxFallos,
                         @Value("${rf.panel.login.bloqueo-minutos:5}") long bloqueoMinutos) {
        this(maxFallos, bloqueoMinutos, Clock.systemUTC());
    }

    IntentosLogin(int maxFallos, long bloqueoMinutos, Clock reloj) {
        this.maxFallos = maxFallos;
        this.bloqueoMs = bloqueoMinutos * 60_000;
        this.reloj = reloj;
    }

    /** @throws DemasiadosIntentosException si el código está bloqueado ahora. */
    public void verificar(String clave) {
        Estado e = porCodigo.get(clave);
        long ahora = reloj.millis();
        if (e != null && e.bloqueadoHasta() > ahora) {
            long minutos = Math.max(1, (e.bloqueadoHasta() - ahora + 59_999) / 60_000);
            throw new DemasiadosIntentosException(
                    "Demasiados intentos fallidos. Probá de nuevo en " + minutos + " min.");
        }
    }

    public void fallo(String clave) {
        long ahora = reloj.millis();
        porCodigo.compute(clave, (k, e) -> {
            int fallos = (e == null || e.bloqueadoHasta() != 0 && e.bloqueadoHasta() <= ahora) ? 1 : e.fallos() + 1;
            return new Estado(fallos, fallos >= maxFallos ? ahora + bloqueoMs : 0);
        });
    }

    public void exito(String clave) {
        porCodigo.remove(clave);
    }
}
