package com.rf.panel.controller;

import com.rf.panel.dto.AjusteStockRequest;
import com.rf.panel.dto.ErrorResponse;
import com.rf.panel.dto.ExistenciaResponse;
import com.rf.panel.dto.PaginaResponse;
import com.rf.panel.dto.MovimientoStockResponse;
import com.rf.panel.dto.ProductoRequest;
import com.rf.panel.dto.ProductoResponse;
import com.rf.panel.security.Sesion;
import com.rf.panel.service.ProductoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/productos")
@Tag(name = "Inventario", description = "CRUD de productos sobre la tabla que comparte la app Android")
@SecurityRequirement(name = "bearer")
@ApiResponses(@ApiResponse(responseCode = "401", description = "Sin token o expirado",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class))))
public class ProductoController {

    private final ProductoService service;

    public ProductoController(ProductoService service) {
        this.service = service;
    }

    @Operation(summary = "Listar / buscar",
            description = "Sin `q` devuelve todo el inventario ordenado por nombre. Con `q` filtra por nombre o SKU.")
    @GetMapping
    public List<ProductoResponse> listar(
            @Parameter(description = "Texto a buscar en nombre o SKU", example = "router")
            @RequestParam(required = false) String q) {
        return service.listar(q);
    }

    @Operation(summary = "Listar paginado",
            description = "Para catálogos grandes. `filtro`: TODOS | BAJO_MINIMO | SIN_STOCK.")
    @GetMapping("/pagina")
    public PaginaResponse<ProductoResponse> pagina(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String categoria,
            @RequestParam(required = false) String filtro,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "50") int tamanio) {
        return service.pagina(q, categoria, filtro, pagina, tamanio);
    }

    @Operation(summary = "Categorías en uso")
    @GetMapping("/categorias")
    public List<String> categorias() {
        return service.categorias();
    }

    @Operation(summary = "Existencias", description = "Stock del producto por ubicación y lote (solo lo que tiene stock).")
    @GetMapping("/{id}/existencias")
    public List<ExistenciaResponse> existencias(@PathVariable long id) {
        return service.existencias(id);
    }

    @Operation(summary = "Ver uno")
    @ApiResponse(responseCode = "404", description = "No existe",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @GetMapping("/{id}")
    public ProductoResponse obtener(@PathVariable long id) {
        return service.obtener(id);
    }

    @Operation(summary = "Ver por código",
            description = "Búsqueda exacta por lo escaneado: SKU o cualquier código alternativo. 404 si no existe.")
    @ApiResponse(responseCode = "404", description = "No existe",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @GetMapping("/sku/{sku}")
    public ProductoResponse obtenerPorSku(@PathVariable String sku) {
        return service.obtenerPorSku(sku);
    }

    @Operation(summary = "Historial",
            description = "Últimos 50 movimientos del producto: quién lo creó, editó, ajustó o borró, y cuándo.")
    @GetMapping("/{id}/movimientos")
    public List<MovimientoStockResponse> movimientos(@PathVariable long id) {
        return service.listarMovimientos(id);
    }

    @Operation(summary = "Alta",
            description = "El SKU debe ser único. Devuelve 409 si ya existe.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Creado"),
            @ApiResponse(responseCode = "409", description = "SKU repetido",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    public ProductoResponse crear(@Valid @RequestBody ProductoRequest peticion,
                                  @AuthenticationPrincipal Sesion sesion) {
        return service.crear(peticion, sesion);
    }

    @Operation(summary = "Editar")
    @ApiResponse(responseCode = "404", description = "No existe",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @PutMapping("/{id}")
    public ProductoResponse actualizar(@PathVariable long id,
                                       @Valid @RequestBody ProductoRequest peticion,
                                       @AuthenticationPrincipal Sesion sesion) {
        return service.actualizar(id, peticion, sesion);
    }

    @Operation(summary = "Ajustar stock",
            description = "Suma `delta` al stock actual (negativo para descontar). "
                    + "Devuelve 409 si dejaría el stock en negativo.")
    @ApiResponse(responseCode = "409", description = "Stock negativo",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @PatchMapping("/{id}/stock")
    public ProductoResponse ajustarStock(@PathVariable long id,
                                         @Valid @RequestBody AjusteStockRequest peticion,
                                         @AuthenticationPrincipal Sesion sesion) {
        return service.ajustarStock(id, peticion.delta(), sesion);
    }

    @Operation(summary = "Eliminar", description = "Solo supervisores (operario SUPERVISOR o panel).")
    @ApiResponse(responseCode = "204", description = "Eliminado")
    @PreAuthorize("hasAnyRole('PANEL','SUPERVISOR')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/{id}")
    public void eliminar(@PathVariable long id, @AuthenticationPrincipal Sesion sesion) {
        service.eliminar(id, sesion);
    }
}
