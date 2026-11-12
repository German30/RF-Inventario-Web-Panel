package com.rf.panel.controller;

import com.rf.panel.dto.ActualizarOperarioRequest;
import com.rf.panel.dto.ErrorResponse;
import com.rf.panel.dto.OperarioRequest;
import com.rf.panel.dto.OperarioResponse;
import com.rf.panel.dto.ResetPinRequest;
import com.rf.panel.security.Sesion;
import com.rf.panel.service.OperarioAdminService;
import io.swagger.v3.oas.annotations.Operation;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/operarios")
@Tag(name = "Operarios", description = "Alta y gestión de cuentas de la app. Exclusivo del panel web.")
@SecurityRequirement(name = "bearer")
@PreAuthorize("hasRole('PANEL')")
@ApiResponses({
        @ApiResponse(responseCode = "401", description = "Sin token o expirado",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "403", description = "El token no es del panel web",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
})
public class OperarioAdminController {

    private final OperarioAdminService service;

    public OperarioAdminController(OperarioAdminService service) {
        this.service = service;
    }

    @Operation(summary = "Listar operarios", description = "Incluye los desactivados.")
    @GetMapping
    public List<OperarioResponse> listar() {
        return service.listar();
    }

    @Operation(summary = "Alta de operario")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Creado"),
            @ApiResponse(responseCode = "409", description = "Código repetido",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    public OperarioResponse crear(@Valid @RequestBody OperarioRequest peticion) {
        return service.crear(peticion);
    }

    @Operation(summary = "Editar nombre, rol o si está activo",
            description = "El PIN se cambia por separado en PATCH /{codigo}/pin. No podés desactivar tu propia cuenta.")
    @ApiResponses({
            @ApiResponse(responseCode = "404", description = "No existe",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Intento de autodesactivarse",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PutMapping("/{codigo}")
    public OperarioResponse actualizar(@PathVariable String codigo,
                                       @Valid @RequestBody ActualizarOperarioRequest peticion,
                                       @AuthenticationPrincipal Sesion sesion) {
        return service.actualizar(codigo, peticion, sesion);
    }

    @Operation(summary = "Resetear PIN")
    @ApiResponse(responseCode = "404", description = "No existe",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @PatchMapping("/{codigo}/pin")
    public OperarioResponse resetearPin(@PathVariable String codigo, @Valid @RequestBody ResetPinRequest peticion) {
        return service.resetearPin(codigo, peticion.pin());
    }
}
