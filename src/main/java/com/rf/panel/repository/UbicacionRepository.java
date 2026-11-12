package com.rf.panel.repository;

import com.rf.panel.entity.Ubicacion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UbicacionRepository extends JpaRepository<Ubicacion, Long> {

    Optional<Ubicacion> findByCodigoIgnoreCase(String codigo);

    List<Ubicacion> findAllByOrderByCodigoAsc();
}
