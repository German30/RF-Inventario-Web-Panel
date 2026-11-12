package com.rf.panel;

import com.rf.panel.dto.ActualizarOperarioRequest;
import com.rf.panel.dto.OperarioRequest;
import com.rf.panel.entity.RolOperario;
import com.rf.panel.exception.RecursoNoEncontradoException;
import com.rf.panel.exception.ReglaNegocioException;
import com.rf.panel.repository.OperarioRepository;
import com.rf.panel.security.Sesion;
import com.rf.panel.service.OperarioAdminService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
class OperarioAdminServiceTest {

    @Autowired
    private OperarioRepository repository;

    private OperarioAdminService service;

    @BeforeEach
    void setUp() {
        PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        service = new OperarioAdminService(repository, passwordEncoder);
    }

    @Test
    void crearAltaConCodigoNuevoQuedaActivoYSinExponerElPin() {
        var resp = service.crear(new OperarioRequest("2001", "1234", "Nueva Persona", RolOperario.OPERARIO));

        assertThat(resp.codigo()).isEqualTo("2001");
        assertThat(resp.rol()).isEqualTo("OPERARIO");
        assertThat(resp.activo()).isTrue();
    }

    @Test
    void crearConCodigoRepetidoRechaza() {
        service.crear(new OperarioRequest("2002", "1234", "Persona A", RolOperario.OPERARIO));

        assertThatThrownBy(() -> service.crear(new OperarioRequest("2002", "5678", "Persona B", RolOperario.OPERARIO)))
                .isInstanceOf(ReglaNegocioException.class);
    }

    @Test
    void unSupervisorNoPuedeDesactivarseASiMismo() {
        service.crear(new OperarioRequest("2003", "1234", "Supervisor Propio", RolOperario.SUPERVISOR));
        Sesion elMismo = new Sesion("2003", "Supervisor Propio", "SUPERVISOR");

        assertThatThrownBy(() -> service.actualizar("2003",
                new ActualizarOperarioRequest("Supervisor Propio", RolOperario.SUPERVISOR, false), elMismo))
                .isInstanceOf(ReglaNegocioException.class);
    }

    @Test
    void unSupervisorPuedeDesactivarAOtroOperario() {
        service.crear(new OperarioRequest("2004", "1234", "Alguien Más", RolOperario.OPERARIO));
        Sesion otroSupervisor = new Sesion("9999", "Otro Supervisor", "SUPERVISOR");

        var resp = service.actualizar("2004",
                new ActualizarOperarioRequest("Alguien Más", RolOperario.OPERARIO, false), otroSupervisor);

        assertThat(resp.activo()).isFalse();
    }

    @Test
    void resetearPinDeCodigoInexistenteRechaza() {
        assertThatThrownBy(() -> service.resetearPin("9999999", "1234"))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    void listarIncluyeDesactivados() {
        service.crear(new OperarioRequest("2005", "1234", "Temporal", RolOperario.OPERARIO));
        service.actualizar("2005", new ActualizarOperarioRequest("Temporal", RolOperario.OPERARIO, false),
                new Sesion("9999", "Otro Supervisor", "SUPERVISOR"));

        assertThat(service.listar()).anyMatch(o -> o.codigo().equals("2005") && !o.activo());
    }
}
