package com.rf.panel.security;

import com.rf.panel.exception.DemasiadosIntentosException;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IntentosLoginTest {

    /** Reloj que el test puede adelantar. */
    private static final class RelojMovil extends Clock {
        private Instant ahora = Instant.parse("2026-11-11T10:00:00Z");

        void avanzar(Duration d) {
            ahora = ahora.plus(d);
        }

        @Override
        public ZoneOffset getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return ahora;
        }
    }

    @Test
    void bloqueaTrasNFallosYDesbloqueaAlVencer() {
        var reloj = new RelojMovil();
        var intentos = new IntentosLogin(3, 5, reloj);

        intentos.fallo("app:1001");
        intentos.fallo("app:1001");
        assertThatCode(() -> intentos.verificar("app:1001")).doesNotThrowAnyException();

        intentos.fallo("app:1001");
        assertThatThrownBy(() -> intentos.verificar("app:1001")).isInstanceOf(DemasiadosIntentosException.class);
        // Otro código no se ve afectado.
        assertThatCode(() -> intentos.verificar("app:1002")).doesNotThrowAnyException();

        reloj.avanzar(Duration.ofMinutes(6));
        assertThatCode(() -> intentos.verificar("app:1001")).doesNotThrowAnyException();
    }

    @Test
    void unLoginExitosoReiniciaLaCuenta() {
        var intentos = new IntentosLogin(2, 5, new RelojMovil());

        intentos.fallo("x");
        intentos.exito("x");
        intentos.fallo("x");

        assertThatCode(() -> intentos.verificar("x")).doesNotThrowAnyException();
    }
}
