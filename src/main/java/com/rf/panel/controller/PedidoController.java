package com.rf.panel.controller;

import com.rf.panel.dto.PedidoRequest;
import com.rf.panel.dto.PedidoResponse;
import com.rf.panel.entity.Pedido;
import com.rf.panel.security.Sesion;
import com.rf.panel.service.PedidoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/pedidos")
@Tag(name = "Pedidos", description = "Recepción (COMPRA) y picking (VENTA) guiados por escaneo")
@SecurityRequirement(name = "bearer")
public class PedidoController {

    private final PedidoService service;

    public PedidoController(PedidoService service) {
        this.service = service;
    }

    @Operation(summary = "Listar", description = "Por defecto solo los abiertos (pendientes o en proceso), más viejo primero.")
    @GetMapping
    public List<PedidoResponse> listar(@RequestParam(required = false) Pedido.Tipo tipo,
                                       @RequestParam(defaultValue = "true") boolean abiertos) {
        return service.listar(tipo, abiertos);
    }

    @Operation(summary = "Detalle", description = "En VENTA incluye de dónde sacar cada línea (FEFO).")
    @GetMapping("/{id}")
    public PedidoResponse obtener(@PathVariable long id) {
        return service.obtener(id);
    }

    @Operation(summary = "Alta", description = "Solo supervisores (o integrado desde el ERP vía panel).")
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    @PreAuthorize("hasAnyRole('PANEL','SUPERVISOR')")
    public PedidoResponse crear(@Valid @RequestBody PedidoRequest.Crear peticion,
                                @AuthenticationPrincipal Sesion sesion) {
        return service.crear(peticion, sesion);
    }

    @Operation(summary = "Escanear", description = "Recibe o pickea `cantidad` del producto escaneado. Idempotente con `clientOpId`.")
    @PostMapping("/{id}/escaneos")
    public PedidoResponse procesar(@PathVariable long id, @Valid @RequestBody PedidoRequest.Escaneo peticion,
                                   @AuthenticationPrincipal Sesion sesion) {
        return service.procesar(id, peticion, sesion);
    }

    @Operation(summary = "Completar", description = "Cierra el pedido (si quedan pendientes, queda como entrega parcial).")
    @PostMapping("/{id}/completar")
    public PedidoResponse completar(@PathVariable long id) {
        return service.completar(id);
    }

    @Operation(summary = "Cancelar", description = "Solo supervisores y solo sin movimientos.")
    @PostMapping("/{id}/cancelar")
    @PreAuthorize("hasAnyRole('PANEL','SUPERVISOR')")
    public PedidoResponse cancelar(@PathVariable long id) {
        return service.cancelar(id);
    }
}
