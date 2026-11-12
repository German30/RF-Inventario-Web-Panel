package com.rf.panel.repository;

import com.rf.panel.entity.Operario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OperarioRepository extends JpaRepository<Operario, String> {

    Optional<Operario> findByCodigoAndActivoTrue(String codigo);

    List<Operario> findAllByOrderByCodigoAsc();
}
