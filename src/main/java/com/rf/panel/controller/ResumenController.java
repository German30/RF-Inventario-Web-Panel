package com.rf.panel.controller;

import com.rf.panel.dto.DashboardResponse;
import com.rf.panel.dto.ErrorClienteRequest;
import com.rf.panel.dto.SyncResponse;
import com.rf.panel.entity.AccionMovimiento;
import com.rf.panel.security.Sesion;
import com.rf.panel.service.DashboardService;
import com.rf.panel.service.ReporteService;
import com.rf.panel.service.SyncService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

@RestController
@RequestMapping("/api")
@Tag(name = "Resumen", description = "Dashboard, reportes CSV, sync offline y diagnóstico")
@SecurityRequirement(name = "bearer")
public class ResumenController {

    private static final Logger log = LoggerFactory.getLogger(ResumenController.class);

    private final DashboardService dashboard;
    private final ReporteService reportes;
    private final SyncService sync;

    public ResumenController(DashboardService dashboard, ReporteService reportes, SyncService sync) {
        this.dashboard = dashboard;
        this.reportes = reportes;
        this.sync = sync;
    }

    @Operation(summary = "Dashboard", description = "KPIs y alertas (bajo mínimo, próximos a vencer).")
    @GetMapping("/dashboard")
    public DashboardResponse dashboard() {
        return dashboard.resumen();
    }

    @Operation(summary = "Sync incremental",
            description = "`desde=0` trae todo el catálogo (`completa=true`); después mandar el `cursor` recibido.")
    @GetMapping("/sync")
    public SyncResponse sync(@RequestParam(defaultValue = "0") long desde) {
        return sync.cambios(desde);
    }

    @Operation(summary = "CSV de inventario")
    @GetMapping("/reportes/inventario.csv")
    public ResponseEntity<byte[]> inventarioCsv() {
        return csv("inventario", reportes.inventario());
    }

    @Operation(summary = "CSV de existencias por ubicación y lote")
    @GetMapping("/reportes/existencias.csv")
    public ResponseEntity<byte[]> existenciasCsv() {
        return csv("existencias", reportes.existencias());
    }

    @Operation(summary = "CSV de movimientos", description = "Mismos filtros que /api/movimientos (hasta 50.000 filas).")
    @GetMapping("/reportes/movimientos.csv")
    public ResponseEntity<byte[]> movimientosCsv(@RequestParam(required = false) Long desde,
                                                 @RequestParam(required = false) Long hasta,
                                                 @RequestParam(required = false) String sku,
                                                 @RequestParam(required = false) String operador,
                                                 @RequestParam(required = false) AccionMovimiento accion) {
        return csv("movimientos", reportes.movimientos(desde, hasta, sku, operador, accion));
    }

    @Operation(summary = "Reportar crash de la app", description = "Se escribe en el log del servidor.")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @PostMapping("/diagnostico/errores")
    public void error(@Valid @RequestBody ErrorClienteRequest error, @AuthenticationPrincipal Sesion sesion) {
        log.error("Crash en la app [{} · {} · operario {}]: {}\n{}", error.version(), error.dispositivo(),
                sesion == null ? "?" : sesion.codigo(), error.mensaje(), error.stack());
    }

    private static ResponseEntity<byte[]> csv(String nombre, String contenido) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + nombre + "-" + LocalDate.now() + ".csv\"")
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(contenido.getBytes(StandardCharsets.UTF_8));
    }
}
