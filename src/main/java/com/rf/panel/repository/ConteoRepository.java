package com.rf.panel.repository;

import com.rf.panel.entity.Conteo;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ConteoRepository extends JpaRepository<Conteo, Long> {

    List<Conteo> findByEstadoInOrderByCreadoEnDesc(Collection<Conteo.Estado> estados);

    List<Conteo> findTop100ByOrderByCreadoEnDesc();

    /** Serializa los escaneos concurrentes sobre el mismo conteo. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select x from Conteo x where x.id = :id")
    Optional<Conteo> findByIdForUpdate(@Param("id") Long id);
}
