package com.rf.panel.service;

import com.rf.panel.entity.Operario;
import com.rf.panel.repository.OperarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Crea un operario la primera vez, y solo entonces.
 *
 * La tabla `users` la gestiona este backend (la app Android ya no escribe
 * MySQL directo, habla con esta API). Esto cubre el caso de arrancar contra
 * una base recién creada, con el esquema puesto por Flyway pero sin
 * operarios todavía. Si ya hay alguno, no toca nada.
 */
@Component
public class OperarioSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(OperarioSeeder.class);

    private final OperarioRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final String codigo;
    private final String pin;
    private final String nombre;

    public OperarioSeeder(OperarioRepository repository,
                          PasswordEncoder passwordEncoder,
                          @Value("${rf.panel.seed.codigo:1001}") String codigo,
                          @Value("${rf.panel.seed.pin:1234}") String pin,
                          @Value("${rf.panel.seed.nombre:Operario Demo}") String nombre) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
        this.codigo = codigo;
        this.pin = pin;
        this.nombre = nombre;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (repository.count() > 0) {
            return;
        }
        repository.save(new Operario(codigo, passwordEncoder.encode(pin), nombre));
        log.warn("Sembrado el operario {} ({}) con PIN inicial. Cambialo antes de usar esto en serio.",
                codigo, nombre);
    }
}
