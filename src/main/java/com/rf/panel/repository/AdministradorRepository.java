package com.rf.panel.repository;

import com.rf.panel.entity.Administrador;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AdministradorRepository extends JpaRepository<Administrador, String> {

    Optional<Administrador> findByCodigoAndActivoTrue(String codigo);
}
