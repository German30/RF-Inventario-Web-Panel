package com.rf.panel.controller;

import com.rf.panel.dto.ErrorResponse;
import com.rf.panel.dto.LoginRequest;
import com.rf.panel.dto.SesionResponse;
import com.rf.panel.security.Sesion;
import com.rf.panel.service.AutenticacionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Acceso", description = "Login del operario contra la tabla users de RF Inventario")
public class AuthController {

    private final AutenticacionService service;

    public AuthController(AutenticacionService service) {
        this.service = service;
    }

    @Operation(
            summary = "Entrar",
            description = """
                    Devuelve el token de sesión. El operario entra con el MISMO código y
                    PIN que usa en la app Android; todo lo demás de la API va firmado con
                    ese token.
                    """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Sesión iniciada"),
            @ApiResponse(responseCode = "401", description = "Código o PIN incorrectos",
                         content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @SecurityRequirements
    @PostMapping("/login")
    public SesionResponse entrar(@Valid @RequestBody LoginRequest peticion) {
        return service.entrar(peticion);
    }

    @Operation(
            summary = "Quién soy",
            description = "Para que el front sepa si el token guardado sigue vivo al recargar.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "El token sigue vigente"),
            @ApiResponse(responseCode = "401", description = "Sin token, o expirado",
                         content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @SecurityRequirement(name = "bearer")
    @GetMapping("/yo")
    public SesionResponse yo(@AuthenticationPrincipal Sesion sesion) {
        return SesionResponse.de(sesion);
    }
}
