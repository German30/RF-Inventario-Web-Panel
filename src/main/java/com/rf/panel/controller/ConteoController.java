package com.rf.panel.controller;

import com.rf.panel.dto.ConteoRequest;
import com.rf.panel.dto.ConteoResponse;
import com.rf.panel.security.Sesion;
import com.rf.panel.service.ConteoService;
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
@RequestMapping("/api/conteos")
@Tag(name = "Conteo cíclico", description = "Abrir → contar → cerrar → (supervisor) aplicar diferencias")
@SecurityRequirement(name = "bearer")
public class ConteoController {

    private final ConteoService service;

    public ConteoController(ConteoService service) {
        this.service = service;
    }

    @Operation(summary = "Listar", description = "Por defecto solo los abiertos/cerrados sin aplicar.")
    @GetMapping
    public List<ConteoResponse> listar(@RequestParam(defaultValue = "true") boolean abiertos,
                                       @AuthenticationPrincipal Sesion sesion) {
        return service.listar(abiertos, sesion);
    }

    @Operation(summary = "Detalle", description = "En un conteo ciego abierto, el operario no ve lo esperado.")
    @GetMapping("/{id}")
    public ConteoResponse obtener(@PathVariable long id, @AuthenticationPrincipal Sesion sesion) {
        return service.obtener(id, sesion);
    }

    @Operation(summary = "Abrir", description = "Con `ubicacion`, toma la foto de lo esperado en esa ubicación.")
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    public ConteoResponse abrir(@Valid @RequestBody ConteoRequest.Abrir peticion,
                                @AuthenticationPrincipal Sesion sesion) {
        return service.abrir(peticion, sesion);
    }

    @Operation(summary = "Registrar lectura", description = "Idempotente con `clientOpId`.")
    @PostMapping("/{id}/lineas")
    public ConteoResponse registrar(@PathVariable long id, @Valid @RequestBody ConteoRequest.Linea peticion,
                                    @AuthenticationPrincipal Sesion sesion) {
        return service.registrar(id, peticion, sesion);
    }

    @Operation(summary = "Cerrar", description = "El operario terminó de contar; queda para que un supervisor lo apruebe.")
    @PostMapping("/{id}/cerrar")
    public ConteoResponse cerrar(@PathVariable long id, @AuthenticationPrincipal Sesion sesion) {
        return service.cerrar(id, sesion);
    }

    @Operation(summary = "Reabrir", description = "Solo supervisores: para recontar antes de aplicar.")
    @PostMapping("/{id}/reabrir")
    @PreAuthorize("hasAnyRole('PANEL','SUPERVISOR')")
    public ConteoResponse reabrir(@PathVariable long id, @AuthenticationPrincipal Sesion sesion) {
        return service.reabrir(id, sesion);
    }

    @Operation(summary = "Aplicar", description = "Solo supervisores: lleva el stock a lo contado (movimientos CONTEO).")
    @PostMapping("/{id}/aplicar")
    @PreAuthorize("hasAnyRole('PANEL','SUPERVISOR')")
    public ConteoResponse aplicar(@PathVariable long id, @AuthenticationPrincipal Sesion sesion) {
        return service.aplicar(id, sesion);
    }

    @Operation(summary = "Cancelar", description = "Solo supervisores. No toca stock.")
    @PostMapping("/{id}/cancelar")
    @PreAuthorize("hasAnyRole('PANEL','SUPERVISOR')")
    public ConteoResponse cancelar(@PathVariable long id, @AuthenticationPrincipal Sesion sesion) {
        return service.cancelar(id, sesion);
    }
}
