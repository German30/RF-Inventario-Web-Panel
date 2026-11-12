package com.rf.panel.service;

import com.rf.panel.dto.ActualizarOperarioRequest;
import com.rf.panel.dto.OperarioRequest;
import com.rf.panel.dto.OperarioResponse;
import com.rf.panel.entity.Operario;
import com.rf.panel.exception.RecursoNoEncontradoException;
import com.rf.panel.exception.ReglaNegocioException;
import com.rf.panel.repository.OperarioRepository;
import com.rf.panel.security.Sesion;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Alta/edición/reseteo de PIN de operarios de la app. Lo usa el panel web: el
 * gateo real ({@code hasRole('PANEL')}) está en
 * {@link com.rf.panel.controller.OperarioAdminController}, esto asume que quien
 * llega hasta acá es un administrador del panel.
 */
@Service
@Transactional(readOnly = true)
public class OperarioAdminService {

    private final OperarioRepository repository;
    private final PasswordEncoder passwordEncoder;

    public OperarioAdminService(OperarioRepository repository, PasswordEncoder passwordEncoder) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
    }

    public List<OperarioResponse> listar() {
        return repository.findAllByOrderByCodigoAsc().stream().map(OperarioResponse::from).toList();
    }

    @Transactional
    public OperarioResponse crear(OperarioRequest req) {
        if (repository.existsById(req.codigo().trim())) {
            throw new ReglaNegocioException("Ya existe un operario con el código " + req.codigo().trim() + ".");
        }
        Operario guardado = repository.save(new Operario(
                req.codigo().trim(), passwordEncoder.encode(req.pin().trim()), req.nombre().trim(), req.rol()));
        return OperarioResponse.from(guardado);
    }

    @Transactional
    public OperarioResponse actualizar(String codigo, ActualizarOperarioRequest req, Sesion quienEdita) {
        Operario operario = buscar(codigo);
        if (!req.activo() && operario.getCodigo().equals(quienEdita.codigo())) {
            throw new ReglaNegocioException("No podés desactivar tu propia cuenta.");
        }
        operario.actualizar(req.nombre().trim(), req.rol(), req.activo());
        return OperarioResponse.from(operario);
    }

    @Transactional
    public OperarioResponse resetearPin(String codigo, String nuevoPin) {
        Operario operario = buscar(codigo);
        operario.cambiarPin(passwordEncoder.encode(nuevoPin.trim()));
        return OperarioResponse.from(operario);
    }

    private Operario buscar(String codigo) {
        return repository.findById(codigo)
                .orElseThrow(() -> new RecursoNoEncontradoException("No hay un operario con código " + codigo + "."));
    }
}
