package com.rf.panel;

import com.rf.panel.dto.LoginRequest;
import com.rf.panel.dto.SesionResponse;
import com.rf.panel.entity.Operario;
import com.rf.panel.exception.CredencialesInvalidasException;
import com.rf.panel.repository.OperarioRepository;
import com.rf.panel.security.JwtService;
import com.rf.panel.service.AutenticacionService;
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

class AutenticacionServiceTest {

    private OperarioRepository repository;
    private PasswordEncoder passwordEncoder;
    private AutenticacionService service;

    @BeforeEach
    void setUp() {
        repository = mock(OperarioRepository.class);
        passwordEncoder = new BCryptPasswordEncoder();
        JwtService jwt = new JwtService("secreto-de-pruebas-con-mas-de-treinta-y-dos-caracteres", 12);
        service = new AutenticacionService(repository, passwordEncoder, jwt);
    }

    @Test
    void loginOkDevuelveTokenYCodigo() {
        Operario operario = new Operario("1001", passwordEncoder.encode("1234"), "Operario Demo");
        when(repository.findByCodigoAndActivoTrue("1001")).thenReturn(Optional.of(operario));

        SesionResponse sesion = service.entrar(new LoginRequest("1001", "1234"));

        assertThat(sesion.token()).isNotBlank();
        assertThat(sesion.codigo()).isEqualTo("1001");
        assertThat(sesion.nombre()).isEqualTo("Operario Demo");
        assertThat(sesion.rol()).isEqualTo("OPERARIO");
        assertThat(sesion.expiraEn()).isEqualTo(12 * 3600L);
    }

    @Test
    void pinIncorrectoRechaza() {
        Operario operario = new Operario("1001", passwordEncoder.encode("1234"), "Operario Demo");
        when(repository.findByCodigoAndActivoTrue("1001")).thenReturn(Optional.of(operario));

        assertThatThrownBy(() -> service.entrar(new LoginRequest("1001", "0000")))
                .isInstanceOf(CredencialesInvalidasException.class);
    }

    @Test
    void codigoInexistenteRechaza() {
        when(repository.findByCodigoAndActivoTrue(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.entrar(new LoginRequest("9999", "1234")))
                .isInstanceOf(CredencialesInvalidasException.class);
    }
}
