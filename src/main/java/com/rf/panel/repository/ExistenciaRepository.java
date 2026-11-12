package com.rf.panel.repository;

import com.rf.panel.entity.Existencia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ExistenciaRepository extends JpaRepository<Existencia, Long> {

    @Query("select e from Existencia e where e.productoId = :productoId and e.cantidad > 0 "
            + "order by e.ubicacion.codigo, e.lote")
    List<Existencia> conStock(@Param("productoId") Long productoId);

    @Query("select e from Existencia e where e.productoId = :productoId and e.ubicacion.id = :ubicacionId "
            + "and upper(e.lote) = upper(:lote)")
    Optional<Existencia> buscar(@Param("productoId") Long productoId, @Param("ubicacionId") Long ubicacionId,
                                @Param("lote") String lote);

    /**
     * Candidatas para descontar en orden FEFO (First Expired, First Out): primero
     * lo que vence antes, lo sin vencimiento al final; a igual vencimiento, la
     * existencia más chica primero para liberar ubicaciones.
     */
    @Query("select e from Existencia e "
            + "where e.productoId = :productoId and e.cantidad > 0 "
            + "and (:ubicacionId is null or e.ubicacion.id = :ubicacionId) "
            + "and (:lote is null or upper(e.lote) = upper(:lote)) "
            + "order by case when e.vence is null then 1 else 0 end, e.vence, e.cantidad")
    List<Existencia> paraDescontar(@Param("productoId") Long productoId, @Param("ubicacionId") Long ubicacionId,
                                   @Param("lote") String lote);

    @Query("select coalesce(sum(e.cantidad), 0) from Existencia e where e.productoId = :productoId")
    int total(@Param("productoId") Long productoId);

    @Query("select e from Existencia e where e.ubicacion.id = :ubicacionId and e.cantidad > 0 order by e.productoId")
    List<Existencia> enUbicacion(@Param("ubicacionId") Long ubicacionId);

    @Query("select e from Existencia e where e.vence is not null and e.vence <= :limite and e.cantidad > 0 "
            + "order by e.vence")
    List<Existencia> porVencer(@Param("limite") LocalDate limite);

    boolean existsByUbicacionIdAndCantidadGreaterThan(Long ubicacionId, int cantidad);

    void deleteByProductoId(Long productoId);
}
