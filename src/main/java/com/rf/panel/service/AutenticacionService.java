package com.rf.panel.service;

import com.rf.panel.dto.LoginRequest;
import com.rf.panel.dto.SesionResponse;
import com.rf.panel.entity.Operario;
import com.rf.panel.exception.CredencialesInvalidasException;
import com.rf.panel.repository.OperarioRepository;
import com.rf.panel.security.IntentosLogin;
import com.rf.panel.security.JwtService;
import com.rf.panel.security.Sesion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AutenticacionService {

    private static final Logger log = LoggerFactory.getLogger(AutenticacionService.class);

    /**
     * Hash BCrypt "de relleno" contra el que se compara cuando el código no
     * existe, para que un login con código inválido tarde lo mismo que uno
     * con PIN incorrecto: si no, el tiempo de respuesta delata cuál de los
     * dos casos fue.
     */
    private static final String HASH_DE_RELLENO =
            "$2a$10$7iouW00273DGpOovJ34CteFaUwlXZlqqQqnXhZyvcaOgkroLSf6Ze";

    private final OperarioRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwt;
    private final IntentosLogin intentos;

    /** Para tests: bloqueo con los valores por defecto. */
    public AutenticacionService(OperarioRepository repository, PasswordEncoder passwordEncoder, JwtService jwt) {
        this(repository, passwordEncoder, jwt, new IntentosLogin(5, 5));
    }

    @Autowired
    public AutenticacionService(OperarioRepository repository, PasswordEncoder passwordEncoder, JwtService jwt,
                 IntentosLogin intentos) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
        this.jwt = jwt;
        this.intentos = intentos;
    }

    public SesionResponse entrar(LoginRequest peticion) {
        String clave = "app:" + peticion.codigo().trim();
        intentos.verificar(clave);
        Operario operario = repository
                .findByCodigoAndActivoTrue(peticion.codigo().trim())
                .orElse(null);

        String hashAComparar = operario != null ? operario.getPinHash() : HASH_DE_RELLENO;
        boolean pinOk = passwordEncoder.matches(peticion.pin().trim(), hashAComparar);

        // Mismo mensaje (y prácticamente el mismo tiempo) para código
        // inexistente y PIN malo: decir cuál de los dos falló le regala al
        // atacante la mitad del trabajo.
        if (operario == null || !pinOk) {
            log.warn("Intento de acceso fallido para el código '{}'", peticion.codigo());
            intentos.fallo(clave);
            throw new CredencialesInvalidasException();
        }

        intentos.exito(clave);
        String token = jwt.emitir(operario.getCodigo(), operario.getNombre(), operario.getRol().name());
        Sesion sesion = new Sesion(operario.getCodigo(), operario.getNombre(), operario.getRol().name());
        log.info("Entró el operario {} ({})", sesion.nombre(), sesion.codigo());
        return SesionResponse.de(token, jwt.segundosDeVida(), sesion);
    }
}
