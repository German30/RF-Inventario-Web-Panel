package com.rf.panel.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

@RestController
@Tag(name = "Servicio", description = "Estado del backend")
public class HealthController {

    /**
     * Versión del contrato de la API. Subirla cuando la app Android necesite
     * endpoints nuevos: la app compara este número al arrancar y se niega a
     * trabajar contra un backend viejo (que respondería 404 en esas rutas).
     * 6 = WMS (ubicaciones, existencias, conteos, pedidos, /api/sync).
     */
    public static final int API_VERSION = 6;

    @Operation(
            summary = "Healthcheck",
            description = "Responde si el servicio está arriba y qué versión de la API expone. "
                    + "Lo usan el HEALTHCHECK del contenedor y la app al arrancar.")
    @GetMapping("/api/ping")
    public Map<String, Object> ping() {
        return Map.of(
                "status", "UP",
                "service", "rf-inventario-panel-api",
                "apiVersion", API_VERSION,
                "timestamp", Instant.now()
        );
    }
}
