package com.rf.panel.service;

import com.rf.panel.dto.ProductoResponse;
import com.rf.panel.dto.SyncResponse;
import com.rf.panel.dto.UbicacionResponse;
import com.rf.panel.entity.AccionMovimiento;
import com.rf.panel.repository.MovimientoStockRepository;
import com.rf.panel.repository.ProductoRepository;
import com.rf.panel.repository.UbicacionRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Sync incremental para el caché offline de la app: devuelve solo lo que
 * cambió desde el cursor. El cursor se devuelve con un solapamiento de 2
 * minutos: una transacción que fijó `actualizadoEn` antes de esta consulta
 * pero confirmó después no se pierde (a lo sumo llega dos veces, y el upsert
 * del lado de la app es idempotente).
 */
@Service
@Transactional(readOnly = true)
public class SyncService {

    static final long SOLAPAMIENTO_MS = 120_000;

    private final ProductoRepository productos;
    private final MovimientoStockRepository movimientos;
    private final UbicacionRepository ubicaciones;

    public SyncService(ProductoRepository productos, MovimientoStockRepository movimientos,
                       UbicacionRepository ubicaciones) {
        this.productos = productos;
        this.movimientos = movimientos;
        this.ubicaciones = ubicaciones;
    }

    public SyncResponse cambios(long desde) {
        long inicio = System.currentTimeMillis();
        boolean completa = desde <= 0;

        List<ProductoResponse> cambiados = (completa
                ? productos.findAll(Sort.by("id"))
                : productos.findByActualizadoEnGreaterThanOrderByActualizadoEnAsc(desde))
                .stream().map(ProductoResponse::from).toList();

        List<Long> eliminados = completa
                ? List.of()
                : movimientos.productosConAccionDesde(AccionMovimiento.ELIMINACION, desde).stream()
                        .filter(id -> !productos.existsById(id))
                        .toList();

        return new SyncResponse(
                cambiados,
                eliminados,
                ubicaciones.findAllByOrderByCodigoAsc().stream().map(UbicacionResponse::from).toList(),
                inicio - SOLAPAMIENTO_MS,
                completa);
    }
}
