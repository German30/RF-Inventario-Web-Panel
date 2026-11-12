package com.rf.panel.repository;

import com.rf.panel.entity.Pedido;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PedidoRepository extends JpaRepository<Pedido, Long> {

    boolean existsByNumeroIgnoreCase(String numero);

    List<Pedido> findByTipoAndEstadoInOrderByCreadoEnAsc(Pedido.Tipo tipo, Collection<Pedido.Estado> estados);

    List<Pedido> findByEstadoInOrderByCreadoEnAsc(Collection<Pedido.Estado> estados);

    List<Pedido> findTop100ByOrderByCreadoEnDesc();

    /** Serializa los escaneos concurrentes sobre el mismo pedido. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select x from Pedido x where x.id = :id")
    Optional<Pedido> findByIdForUpdate(@Param("id") Long id);
}
