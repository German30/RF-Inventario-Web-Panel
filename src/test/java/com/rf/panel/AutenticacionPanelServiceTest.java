package com.rf.panel;

import com.rf.panel.dto.LoginRequest;
import com.rf.panel.dto.SesionResponse;
import com.rf.panel.entity.Administrador;
import com.rf.panel.exception.CredencialesInvalidasException;
import com.rf.panel.repository.AdministradorRepository;
import com.rf.panel.security.JwtService;
import com.rf.panel.security.Sesion;
import com.rf.panel.service.AutenticacionPanelService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AutenticacionPanelServiceTest {

    private AdministradorRepository repository;
    private PasswordEncoder passwordEncoder;
    private AutenticacionPanelService service;

    @BeforeEach
    void setUp() {
        repository = mock(AdministradorRepository.class);
        passwordEncoder = new BCryptPasswordEncoder();
        JwtService jwt = new JwtService("secreto-de-pruebas-con-mas-de-treinta-y-dos-caracteres", 12);
        service = new AutenticacionPanelService(repository, passwordEncoder, jwt);
    }

    @Test
    void loginOkDevuelveTokenYRolPanel() {
        Administrador admin = new Administrador("9001", passwordEncoder.encode("4321"), "Supervisor Demo");
        when(repository.findByCodigoAndActivoTrue("9001")).thenReturn(Optional.of(admin));

        SesionResponse sesion = service.entrar(new LoginRequest("9001", "4321"));

        assertThat(sesion.token()).isNotBlank();
        assertThat(sesion.codigo()).isEqualTo("9001");
        assertThat(sesion.nombre()).isEqualTo("Supervisor Demo");
        assertThat(sesion.rol()).isEqualTo(Sesion.ROL_PANEL);
        assertThat(sesion.expiraEn()).isEqualTo(12 * 3600L);
    }

    @Test
    void pinIncorrectoRechaza() {
        Administrador admin = new Administrador("9001", passwordEncoder.encode("4321"), "Supervisor Demo");
        when(repository.findByCodigoAndActivoTrue("9001")).thenReturn(Optional.of(admin));

        assertThatThrownBy(() -> service.entrar(new LoginRequest("9001", "0000")))
                .isInstanceOf(CredencialesInvalidasException.class);
    }

    @Test
    void codigoInexistenteRechaza() {
        when(repository.findByCodigoAndActivoTrue(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.entrar(new LoginRequest("1234", "4321")))
                .isInstanceOf(CredencialesInvalidasException.class);
    }
}
