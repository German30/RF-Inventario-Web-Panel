package com.rf.panel.controller;

import com.rf.panel.dto.ErrorResponse;
import com.rf.panel.dto.MovimientoStockResponse;
import com.rf.panel.dto.OperacionRequest;
import com.rf.panel.dto.OperacionResponse;
import com.rf.panel.dto.PaginaResponse;
import com.rf.panel.entity.AccionMovimiento;
import com.rf.panel.repository.MovimientoStockRepository;
import com.rf.panel.security.Sesion;
import com.rf.panel.service.InventarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@Tag(name = "Operaciones", description = "Entradas, salidas, transferencias, ajustes e historial global")
@SecurityRequirement(name = "bearer")
public class OperacionController {

    private final InventarioService inventario;
    private final MovimientoStockRepository movimientos;

    public OperacionController(InventarioService inventario, MovimientoStockRepository movimientos) {
        this.inventario = inventario;
        this.movimientos = movimientos;
    }

    @Operation(summary = "Registrar operación",
            description = "Idempotente con `clientOpId`: la cola offline de la app puede reintentar sin duplicar.")
    @ApiResponse(responseCode = "409", description = "Stock insuficiente, lote faltante, ubicación inactiva...",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @PostMapping("/operaciones")
    public OperacionResponse operar(@Valid @RequestBody OperacionRequest peticion,
                                    @AuthenticationPrincipal Sesion sesion) {
        return inventario.operar(peticion, sesion);
    }

    @Operation(summary = "Historial global", description = "Paginado, más reciente primero. Fechas en epoch millis.")
    @GetMapping("/movimientos")
    public PaginaResponse<MovimientoStockResponse> movimientos(
            @RequestParam(required = false) Long desde,
            @RequestParam(required = false) Long hasta,
            @RequestParam(required = false) String sku,
            @RequestParam(required = false) String operador,
            @RequestParam(required = false) AccionMovimiento accion,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "50") int tamanio) {
        var page = movimientos.buscar(desde, hasta, blankANull(sku), blankANull(operador), accion,
                PageRequest.of(Math.max(0, pagina), Math.min(Math.max(1, tamanio), 500)));
        return PaginaResponse.de(page, MovimientoStockResponse::from);
    }

    private static String blankANull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
