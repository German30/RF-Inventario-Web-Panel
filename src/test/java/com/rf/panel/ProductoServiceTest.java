package com.rf.panel;

import com.rf.panel.dto.ProductoRequest;
import com.rf.panel.entity.AccionMovimiento;
import com.rf.panel.entity.MovimientoStock;
import com.rf.panel.entity.Producto;
import com.rf.panel.exception.ConflictoEdicionException;
import com.rf.panel.exception.ReglaNegocioException;
import com.rf.panel.repository.MovimientoStockRepository;
import com.rf.panel.repository.ExistenciaRepository;
import com.rf.panel.repository.OperacionProcesadaRepository;
import com.rf.panel.repository.ProductoRepository;
import com.rf.panel.repository.UbicacionRepository;
import com.rf.panel.service.InventarioService;
import com.rf.panel.security.Sesion;
import com.rf.panel.service.ProductoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Corre contra H2 real (vía {@code @DataJpaTest}) en vez de mocks: el
 * optimistic locking depende de que Hibernate/H2 asignen id y timestamps de
 * verdad, no de valores inventados a mano.
 */
@DataJpaTest
class ProductoServiceTest {

    private static final Sesion OPERARIO = new Sesion("1001", "Operario Demo", "OPERARIO");

    @Autowired
    private ProductoRepository repository;

    @Autowired
    private MovimientoStockRepository movimientos;

    @Autowired
    private ExistenciaRepository existencias;

    @Autowired
    private UbicacionRepository ubicaciones;

    @Autowired
    private OperacionProcesadaRepository operaciones;

    private ProductoService service;

    @BeforeEach
    void setUp() {
        var inventario = new InventarioService(repository, existencias, ubicaciones, movimientos, operaciones);
        service = new ProductoService(repository, movimientos, existencias, inventario);
    }

    @Test
    void editarConVersionDesactualizadaRechaza() throws InterruptedException {
        Producto guardado = repository.save(new Producto("Cable", "CBL-1", 5, "A1"));
        Long versionLeida = guardado.getActualizadoEn();

        Thread.sleep(2); // asegura que el próximo actualizadoEn sea distinto

        // Alguien más lo edita primero, con la misma versión que "leyó" este test.
        service.actualizar(guardado.getId(), new ProductoRequest("Cable HDMI", "CBL-1", 8, "A1", versionLeida), OPERARIO);

        assertThatThrownBy(() -> service.actualizar(
                guardado.getId(), new ProductoRequest("Cable VGA", "CBL-1", 3, "A1", versionLeida), OPERARIO))
                .isInstanceOf(ConflictoEdicionException.class);
    }

    @Test
    void editarSinVersionNoChequeaConflicto() {
        Producto guardado = repository.save(new Producto("Cable", "CBL-2", 5, "A1"));

        var respuesta = service.actualizar(
                guardado.getId(), new ProductoRequest("Cable actualizado", "CBL-2", 9, "A1", null), OPERARIO);

        assertThat(respuesta.nombre()).isEqualTo("Cable actualizado");
        assertThat(respuesta.cantidad()).isEqualTo(9);
    }

    @Test
    void ajustarStockDejandoloNegativoRechaza() {
        Producto guardado = repository.save(new Producto("Cable", "CBL-3", 2, "A1"));

        assertThatThrownBy(() -> service.ajustarStock(guardado.getId(), -5, OPERARIO))
                .isInstanceOf(ReglaNegocioException.class);
    }

    @Test
    void ajustarStockValidoSuma() {
        Producto guardado = repository.save(new Producto("Cable", "CBL-5", 2, "A1"));

        var respuesta = service.ajustarStock(guardado.getId(), 3, OPERARIO);

        assertThat(respuesta.cantidad()).isEqualTo(5);
    }

    @Test
    void crearConSkuRepetidoRechaza() {
        repository.save(new Producto("Cable", "CBL-4", 1, "A1"));

        assertThatThrownBy(() -> service.crear(new ProductoRequest("Otro cable", "CBL-4", 1, "A1", null), OPERARIO))
                .isInstanceOf(ReglaNegocioException.class);
    }

    @Test
    void cadaMutacionQuedaRegistradaEnElHistorial() {
        var creado = service.crear(new ProductoRequest("Cable", "CBL-6", 10, "A1", null), OPERARIO);

        service.ajustarStock(creado.id(), -3, OPERARIO);
        // actualizadoEn null: no chequea versión, no hace falta encadenar timings.
        service.actualizar(creado.id(), new ProductoRequest("Cable renombrado", "CBL-6", 7, "A1", null), OPERARIO);

        List<MovimientoStock> historial = movimientos.findTop50ByProductoIdOrderByCreadoEnDesc(creado.id());

        assertThat(historial).hasSize(3);
        assertThat(historial).allMatch(m -> m.getOperadorCodigo().equals("1001"));
        assertThat(historial).anyMatch(m -> m.getAccion() == AccionMovimiento.CREACION && m.getDelta() == 10);
        assertThat(historial).anyMatch(m -> m.getAccion() == AccionMovimiento.AJUSTE_STOCK && m.getDelta() == -3);
        assertThat(historial).anyMatch(m -> m.getAccion() == AccionMovimiento.EDICION && m.getDelta() == 0);
    }

    @Test
    void eliminarRegistraElMovimientoAntesDeBorrar() {
        Producto guardado = repository.save(new Producto("Cable", "CBL-7", 4, "A1"));

        service.eliminar(guardado.getId(), OPERARIO);

        List<MovimientoStock> historial = movimientos.findTop50ByProductoIdOrderByCreadoEnDesc(guardado.getId());
        assertThat(historial).hasSize(1);
        assertThat(historial.get(0).getAccion()).isEqualTo(AccionMovimiento.ELIMINACION);
        assertThat(historial.get(0).getDelta()).isEqualTo(-4);
        assertThat(historial.get(0).getCantidadDespues()).isZero();
    }

    @Test
    void elAltaAsientaElStockEnLaUbicacionPrincipal() {
        var creado = service.crear(new ProductoRequest("Mouse", "MOU-1", 6, "a-01", null), OPERARIO);

        assertThat(creado.ubicacion()).isEqualTo("A-01");
        assertThat(service.existencias(creado.id()))
                .singleElement()
                .satisfies(e -> {
                    assertThat(e.ubicacion()).isEqualTo("A-01");
                    assertThat(e.cantidad()).isEqualTo(6);
                });
    }

    @Test
    void seEncuentraPorCodigoAlternativo() {
        var req = new ProductoRequest("Teclado", "TEC-1", 0, "A1", null, null, null, null, 2, null, null,
                List.of("7790001112223"));
        service.crear(req, OPERARIO);

        assertThat(service.obtenerPorSku("7790001112223").sku()).isEqualTo("TEC-1");
    }

    @Test
    void codigoAlternativoRepetidoEntreProductosRechaza() {
        service.crear(new ProductoRequest("A", "SKU-A", 0, "A1", null, null, null, null, null, null, null,
                List.of("EAN-1")), OPERARIO);

        assertThatThrownBy(() -> service.crear(new ProductoRequest("B", "SKU-B", 0, "A1", null, null, null, null,
                null, null, null, List.of("EAN-1")), OPERARIO))
                .isInstanceOf(ReglaNegocioException.class);
    }

    @Test
    void bajoMinimoSeMarca() {
        var creado = service.crear(new ProductoRequest("Toner", "TON-1", 2, "A1", null, null, null, null, 5, null,
                null, null), OPERARIO);

        assertThat(creado.bajoMinimo()).isTrue();
        assertThat(service.pagina(null, null, "BAJO_MINIMO", 0, 10).contenido())
                .extracting("sku").contains("TON-1");
    }
}
