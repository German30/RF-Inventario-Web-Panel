package com.rf.panel.repository;

import com.rf.panel.entity.AccionMovimiento;
import com.rf.panel.entity.MovimientoStock;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MovimientoStockRepository extends JpaRepository<MovimientoStock, Long> {

    /** Los últimos 50 movimientos del producto, más reciente primero. */
    List<MovimientoStock> findTop50ByProductoIdOrderByCreadoEnDesc(Long productoId);

    /** Historial global filtrable (panel, reportes, app). Todos los filtros son opcionales. */
    @Query("select m from MovimientoStock m "
            + "where (:desde is null or m.creadoEn >= :desde) "
            + "and (:hasta is null or m.creadoEn < :hasta) "
            + "and (:sku is null or upper(m.sku) = upper(:sku)) "
            + "and (:operador is null or m.operadorCodigo = :operador) "
            + "and (:accion is null or m.accion = :accion) "
            + "order by m.creadoEn desc, m.id desc")
    Page<MovimientoStock> buscar(@Param("desde") Long desde, @Param("hasta") Long hasta,
                                 @Param("sku") String sku, @Param("operador") String operador,
                                 @Param("accion") AccionMovimiento accion, Pageable pageable);

    long countByCreadoEnGreaterThanEqual(long desde);

    /** Productos con esa acción desde el cursor (p. ej. borrados, para limpiar el caché de la app). */
    @Query("select distinct m.productoId from MovimientoStock m where m.accion = :accion and m.creadoEn > :desde")
    List<Long> productosConAccionDesde(@Param("accion") AccionMovimiento accion, @Param("desde") long desde);
}
