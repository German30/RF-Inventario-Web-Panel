package com.rf.panel.service;

import com.rf.panel.dto.LoginRequest;
import com.rf.panel.dto.SesionResponse;
import com.rf.panel.entity.Administrador;
import com.rf.panel.exception.CredencialesInvalidasException;
import com.rf.panel.repository.AdministradorRepository;
import com.rf.panel.security.IntentosLogin;
import com.rf.panel.security.JwtService;
import com.rf.panel.security.Sesion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Login del panel web contra la tabla `panel_admins`. Es el gemelo de
 * {@link AutenticacionService} pero para la otra identidad: mismo formato de
 * credencial (código + PIN), token con {@code rol = }{@link Sesion#ROL_PANEL}.
 */
@Service
@Transactional(readOnly = true)
public class AutenticacionPanelService {

    private static final Logger log = LoggerFactory.getLogger(AutenticacionPanelService.class);

    /**
     * Igual que en {@link AutenticacionService}: se compara siempre contra un
     * hash real —este de relleno si el código no existe— para que un código
     * inválido tarde lo mismo que un PIN incorrecto y el tiempo no delate cuál
     * de los dos falló.
     */
    private static final String HASH_DE_RELLENO =
            "$2a$10$7iouW00273DGpOovJ34CteFaUwlXZlqqQqnXhZyvcaOgkroLSf6Ze";

    private final AdministradorRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwt;
    private final IntentosLogin intentos;

    /** Para tests: bloqueo con los valores por defecto. */
    public AutenticacionPanelService(AdministradorRepository repository, PasswordEncoder passwordEncoder, JwtService jwt) {
        this(repository, passwordEncoder, jwt, new IntentosLogin(5, 5));
    }

    @Autowired
    public AutenticacionPanelService(AdministradorRepository repository, PasswordEncoder passwordEncoder, JwtService jwt,
                 IntentosLogin intentos) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
        this.jwt = jwt;
        this.intentos = intentos;
    }

    public SesionResponse entrar(LoginRequest peticion) {
        String clave = "panel:" + peticion.codigo().trim();
        intentos.verificar(clave);
        Administrador admin = repository
                .findByCodigoAndActivoTrue(peticion.codigo().trim())
                .orElse(null);

        String hashAComparar = admin != null ? admin.getPinHash() : HASH_DE_RELLENO;
        boolean pinOk = passwordEncoder.matches(peticion.pin().trim(), hashAComparar);

        if (admin == null || !pinOk) {
            log.warn("Intento de acceso al panel fallido para el código '{}'", peticion.codigo());
            intentos.fallo(clave);
            throw new CredencialesInvalidasException();
        }

        intentos.exito(clave);
        String token = jwt.emitir(admin.getCodigo(), admin.getNombre(), Sesion.ROL_PANEL);
        Sesion sesion = new Sesion(admin.getCodigo(), admin.getNombre(), Sesion.ROL_PANEL);
        log.info("Entró al panel el administrador {} ({})", sesion.nombre(), sesion.codigo());
        return SesionResponse.de(token, jwt.segundosDeVida(), sesion);
    }
}
