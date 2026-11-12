package com.rf.panel;

import com.rf.panel.dto.ConteoRequest;
import com.rf.panel.dto.OperacionRequest;
import com.rf.panel.dto.OperacionRequest.Tipo;
import com.rf.panel.dto.PedidoRequest;
import com.rf.panel.dto.ProductoRequest;
import com.rf.panel.entity.Pedido;
import com.rf.panel.entity.Ubicacion;
import com.rf.panel.exception.ReglaNegocioException;
import com.rf.panel.repository.ConteoRepository;
import com.rf.panel.repository.ExistenciaRepository;
import com.rf.panel.repository.MovimientoStockRepository;
import com.rf.panel.repository.OperacionProcesadaRepository;
import com.rf.panel.repository.PedidoRepository;
import com.rf.panel.repository.ProductoRepository;
import com.rf.panel.repository.UbicacionRepository;
import com.rf.panel.security.Sesion;
import com.rf.panel.service.ConteoService;
import com.rf.panel.service.InventarioService;
import com.rf.panel.service.PedidoService;
import com.rf.panel.service.ProductoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Motor de stock + conteos + pedidos, contra H2 real. */
@DataJpaTest
class InventarioServiceTest {

    private static final Sesion OPERARIO = new Sesion("1001", "Operario Demo", "OPERARIO");
    private static final Sesion SUPERVISOR = new Sesion("2001", "Jefa de depósito", "SUPERVISOR");

    @Autowired private ProductoRepository productos;
    @Autowired private ExistenciaRepository existencias;
    @Autowired private UbicacionRepository ubicaciones;
    @Autowired private MovimientoStockRepository movimientos;
    @Autowired private OperacionProcesadaRepository operaciones;
    @Autowired private ConteoRepository conteos;
    @Autowired private PedidoRepository pedidos;

    private InventarioService inventario;
    private ProductoService productoService;
    private ConteoService conteoService;
    private PedidoService pedidoService;

    @BeforeEach
    void setUp() {
        inventario = new InventarioService(productos, existencias, ubicaciones, movimientos, operaciones);
        productoService = new ProductoService(productos, movimientos, existencias, inventario);
        conteoService = new ConteoService(conteos, existencias, productos, inventario);
        pedidoService = new PedidoService(pedidos, existencias, inventario);
        ubicaciones.save(new Ubicacion("A-01", "", ""));
        ubicaciones.save(new Ubicacion("B-01", "", ""));
    }

    private long producto(String sku, int cantidad, boolean conLote) {
        return productoService.crear(new ProductoRequest("Prod " + sku, sku, cantidad, "A-01", null,
                null, null, null, null, null, conLote, null), OPERARIO).id();
    }

    private OperacionRequest op(Tipo tipo, String codigo, int cantidad, String ubicacion, String destino,
                                String lote, LocalDate vence, String clientOpId) {
        return new OperacionRequest(tipo, null, codigo, cantidad, ubicacion, destino, lote, vence, null, null, clientOpId);
    }

    @Test
    void entradaYSalidaActualizanElTotal() {
        producto("P1", 0, false);

        inventario.operar(op(Tipo.ENTRADA, "P1", 10, "B-01", null, null, null, null), OPERARIO);
        var r = inventario.operar(op(Tipo.SALIDA, "P1", 4, null, null, null, null, null), OPERARIO);

        assertThat(r.producto().cantidad()).isEqualTo(6);
        assertThat(r.movimientos()).singleElement().satisfies(m -> {
            assertThat(m.delta()).isEqualTo(-4);
            assertThat(m.ubicacion()).isEqualTo("B-01");
        });
    }

    @Test
    void laSalidaSinLoteDescuentaFefo() {
        producto("LECHE", 0, true);
        inventario.operar(op(Tipo.ENTRADA, "LECHE", 5, "A-01", null, "L-TARDE", LocalDate.now().plusDays(60), null), OPERARIO);
        inventario.operar(op(Tipo.ENTRADA, "LECHE", 5, "B-01", null, "L-PRONTO", LocalDate.now().plusDays(5), null), OPERARIO);

        var r = inventario.operar(op(Tipo.SALIDA, "LECHE", 7, null, null, null, null, null), OPERARIO);

        // Primero se agota el lote que vence antes (5 de L-PRONTO) y después 2 de L-TARDE.
        assertThat(r.movimientos()).extracting("lote").containsExactly("L-PRONTO", "L-TARDE");
        assertThat(r.movimientos()).extracting("delta").containsExactly(-5, -2);
        assertThat(r.producto().cantidad()).isEqualTo(3);
    }

    @Test
    void productoConLoteExigeLoteEnLaEntrada() {
        producto("VAC", 0, true);

        assertThatThrownBy(() -> inventario.operar(op(Tipo.ENTRADA, "VAC", 1, null, null, null, null, null), OPERARIO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("lote");
    }

    @Test
    void noSePuedeSacarMasDeLoQueHay() {
        producto("P2", 3, false);

        assertThatThrownBy(() -> inventario.operar(op(Tipo.SALIDA, "P2", 4, null, null, null, null, null), OPERARIO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("Stock insuficiente");
    }

    @Test
    void transferenciaMueveSinCambiarElTotal() {
        long id = producto("P3", 8, false);

        var r = inventario.operar(op(Tipo.TRANSFERENCIA, "P3", 3, "A-01", "B-01", null, null, null), OPERARIO);

        assertThat(r.producto().cantidad()).isEqualTo(8);
        assertThat(productoService.existencias(id))
                .extracting("ubicacion", "cantidad")
                .containsExactlyInAnyOrder(org.assertj.core.groups.Tuple.tuple("A-01", 5),
                        org.assertj.core.groups.Tuple.tuple("B-01", 3));
    }

    @Test
    void unaOperacionRepetidaNoDuplicaStock() {
        producto("P4", 0, false);
        var entrada = op(Tipo.ENTRADA, "P4", 2, null, null, null, null, "op-123");

        inventario.operar(entrada, OPERARIO);
        var reintento = inventario.operar(entrada, OPERARIO);

        assertThat(reintento.repetida()).isTrue();
        assertThat(reintento.producto().cantidad()).isEqualTo(2);
    }

    @Test
    void conteoDeUbicacionAjustaDiferenciasYLoNoContadoQuedaEnCero() {
        producto("C1", 10, false);
        producto("C2", 4, false);

        var conteo = conteoService.abrir(new ConteoRequest.Abrir("A-01", true, null), OPERARIO);
        // Ciego: el operario no ve lo esperado.
        assertThat(conteo.lineas()).allMatch(l -> l.esperado() == null);

        conteoService.registrar(conteo.id(), new ConteoRequest.Linea("C1", 7, null, null, true, null), OPERARIO);
        conteoService.registrar(conteo.id(), new ConteoRequest.Linea("C1", 1, null, null, true, null), OPERARIO);
        conteoService.cerrar(conteo.id(), OPERARIO);
        var aplicado = conteoService.aplicar(conteo.id(), SUPERVISOR);

        assertThat(aplicado.estado()).isEqualTo("APLICADO");
        assertThat(productoService.obtenerPorSku("C1").cantidad()).isEqualTo(8);
        assertThat(productoService.obtenerPorSku("C2").cantidad()).isZero();
        assertThat(aplicado.unidadesDiferencia()).isEqualTo(-6);
    }

    @Test
    void recepcionYPickingContraPedido() {
        producto("R1", 0, false);

        var compra = pedidoService.crear(new PedidoRequest.Crear(Pedido.Tipo.COMPRA, "OC-1", "Proveedor", null,
                List.of(new PedidoRequest.Linea("R1", 10))), SUPERVISOR);
        pedidoService.procesar(compra.id(), new PedidoRequest.Escaneo("R1", 10, "B-01", null, null, null), OPERARIO);

        assertThatThrownBy(() -> pedidoService.procesar(compra.id(),
                new PedidoRequest.Escaneo("R1", 1, null, null, null, null), OPERARIO))
                .isInstanceOf(ReglaNegocioException.class);

        var venta = pedidoService.crear(new PedidoRequest.Crear(Pedido.Tipo.VENTA, "PV-1", "Cliente", null,
                List.of(new PedidoRequest.Linea("R1", 4))), SUPERVISOR);
        assertThat(venta.lineas().get(0).sugerencias()).extracting("ubicacion").containsExactly("B-01");

        var hecho = pedidoService.procesar(venta.id(), new PedidoRequest.Escaneo("R1", 4, null, null, null, null), OPERARIO);

        assertThat(hecho.unidadesHechas()).isEqualTo(4);
        assertThat(productoService.obtenerPorSku("R1").cantidad()).isEqualTo(6);
    }

    @Test
    void escanearUnProductoQueNoEstaEnElPedidoRechaza() {
        producto("X1", 5, false);
        producto("X2", 5, false);
        var venta = pedidoService.crear(new PedidoRequest.Crear(Pedido.Tipo.VENTA, "PV-2", null, null,
                List.of(new PedidoRequest.Linea("X1", 1))), SUPERVISOR);

        assertThatThrownBy(() -> pedidoService.procesar(venta.id(),
                new PedidoRequest.Escaneo("X2", 1, null, null, null, null), OPERARIO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("no está en el pedido");
    }
}
