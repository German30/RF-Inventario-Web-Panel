package com.rf.panel.controller;

import com.rf.panel.dto.ExistenciaResponse;
import com.rf.panel.dto.UbicacionRequest;
import com.rf.panel.dto.UbicacionResponse;
import com.rf.panel.service.UbicacionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/ubicaciones")
@Tag(name = "Ubicaciones", description = "Depósito / pasillo / estante, con código escaneable")
@SecurityRequirement(name = "bearer")
public class UbicacionController {

    private final UbicacionService service;

    public UbicacionController(UbicacionService service) {
        this.service = service;
    }

    @GetMapping
    public List<UbicacionResponse> listar() {
        return service.listar();
    }

    @GetMapping("/{codigo}")
    public UbicacionResponse obtener(@PathVariable String codigo) {
        return service.obtener(codigo);
    }

    @Operation(summary = "Contenido", description = "Qué hay en la ubicación (al escanear la etiqueta del estante).")
    @GetMapping("/{codigo}/contenido")
    public List<ExistenciaResponse> contenido(@PathVariable String codigo) {
        return service.contenido(codigo);
    }

    @Operation(summary = "Alta", description = "Solo supervisores.")
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    @PreAuthorize("hasAnyRole('PANEL','SUPERVISOR')")
    public UbicacionResponse crear(@Valid @RequestBody UbicacionRequest peticion) {
        return service.crear(peticion);
    }

    @Operation(summary = "Editar", description = "Solo supervisores. No se puede desactivar con stock.")
    @PutMapping("/{codigo}")
    @PreAuthorize("hasAnyRole('PANEL','SUPERVISOR')")
    public UbicacionResponse actualizar(@PathVariable String codigo, @Valid @RequestBody UbicacionRequest peticion) {
        return service.actualizar(codigo, peticion);
    }

    @Operation(summary = "Desactivar", description = "Las ubicaciones no se borran (el historial las nombra).")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/{codigo}")
    @PreAuthorize("hasAnyRole('PANEL','SUPERVISOR')")
    public void desactivar(@PathVariable String codigo) {
        service.desactivar(codigo);
    }
}
