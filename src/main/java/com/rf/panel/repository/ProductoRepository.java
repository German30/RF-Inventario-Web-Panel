package com.rf.panel.repository;

import com.rf.panel.entity.Producto;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface ProductoRepository extends JpaRepository<Producto, Long> {

    Optional<Producto> findBySkuIgnoreCase(String sku);

    boolean existsBySkuIgnoreCase(String sku);

    List<Producto> findByNombreContainingIgnoreCaseOrSkuContainingIgnoreCase(
            String nombre, String sku, Sort sort);

    /** Busca por código alternativo (EAN del proveedor, etc.). */
    @Query("select p from Producto p join p.codigosAlternativos c where upper(c) = upper(:codigo)")
    Optional<Producto> findByCodigoAlternativo(@Param("codigo") String codigo);

    /** ¿Algún OTRO producto usa este código como SKU o como código alternativo? */
    default boolean codigoEnUso(String codigo, Long excluirId) {
        return contarCodigoEnUso(codigo, excluirId == null ? -1L : excluirId) > 0;
    }

    @Query("select count(p) from Producto p left join p.codigosAlternativos c "
            + "where (upper(p.sku) = upper(:codigo) or upper(c) = upper(:codigo)) "
            + "and p.id <> :excluirId")
    long contarCodigoEnUso(@Param("codigo") String codigo, @Param("excluirId") Long excluirId);

    /**
     * Listado paginado con filtros opcionales. `filtro`: null/TODOS, BAJO_MINIMO
     * (stock <= mínimo, con mínimo configurado) o SIN_STOCK.
     */
    @Query("select p from Producto p "
            + "where (:q is null or lower(p.nombre) like lower(concat('%', :q, '%')) "
            + "                  or lower(p.sku) like lower(concat('%', :q, '%'))) "
            + "and (:categoria is null or p.categoria = :categoria) "
            + "and (:filtro is null or :filtro = 'TODOS' "
            + "     or (:filtro = 'BAJO_MINIMO' and p.stockMinimo > 0 and p.cantidad <= p.stockMinimo) "
            + "     or (:filtro = 'SIN_STOCK' and p.cantidad = 0))")
    Page<Producto> buscar(@Param("q") String q, @Param("categoria") String categoria,
                          @Param("filtro") String filtro, Pageable pageable);

    /** Para la sync incremental de la app: lo modificado desde el cursor. */
    List<Producto> findByActualizadoEnGreaterThanOrderByActualizadoEnAsc(long desde);

    @Query("select distinct p.categoria from Producto p where p.categoria <> '' order by p.categoria")
    List<String> categorias();

    @Query("select p from Producto p where p.stockMinimo > 0 and p.cantidad <= p.stockMinimo order by p.cantidad asc")
    List<Producto> bajoMinimo();

    long countByCantidad(int cantidad);

    @Query("select coalesce(sum(p.cantidad), 0) from Producto p")
    long unidadesTotales();

    @Query("select coalesce(sum(p.cantidad * p.costoUnitario), 0) from Producto p")
    BigDecimal valorTotal();

    /**
     * Igual que {@code findById}, pero toma un lock de fila (SELECT ... FOR
     * UPDATE) hasta que termine la transacción. Toda operación de stock pasa
     * por acá: sin esto, dos movimientos concurrentes sobre el mismo producto
     * pueden leer la misma cantidad de partida y perderse uno ("lost update").
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Producto p where p.id = :id")
    Optional<Producto> findByIdForUpdate(@Param("id") Long id);
}
