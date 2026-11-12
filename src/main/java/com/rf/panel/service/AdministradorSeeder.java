package com.rf.panel.service;

import com.rf.panel.entity.Administrador;
import com.rf.panel.repository.AdministradorRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Crea el primer administrador del panel, y solo entonces. El gemelo de
 * {@link OperarioSeeder} para la tabla `panel_admins`.
 *
 * En una base que venía usando el panel, V5 ya copió al supervisor 9001 desde
 * `users`, así que esto no hace nada. En una base nueva y vacía V5 no copió a
 * nadie y este seeder deja un administrador para poder entrar la primera vez.
 */
@Component
public class AdministradorSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdministradorSeeder.class);

    private final AdministradorRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final String codigo;
    private final String pin;
    private final String nombre;

    public AdministradorSeeder(AdministradorRepository repository,
                               PasswordEncoder passwordEncoder,
                               @Value("${rf.panel.admin-seed.codigo:9001}") String codigo,
                               @Value("${rf.panel.admin-seed.pin:4321}") String pin,
                               @Value("${rf.panel.admin-seed.nombre:Supervisor Demo}") String nombre) {
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
        repository.save(new Administrador(codigo, passwordEncoder.encode(pin), nombre));
        log.warn("Sembrado el administrador de panel {} ({}) con PIN inicial. Cambialo antes de usar esto en serio.",
                codigo, nombre);
    }
}
