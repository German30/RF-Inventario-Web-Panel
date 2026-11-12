package com.rf.panel.dto;

import com.rf.panel.entity.Conteo;
import com.rf.panel.entity.ConteoLinea;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(name = "Conteo", description = "Sesión de conteo cíclico con sus líneas y diferencias")
public record ConteoResponse(
        Long id,
        @Schema(description = "Vacío = conteo libre") String ubicacion,
        boolean ciego,
        @Schema(description = "ABIERTO | CERRADO | APLICADO | CANCELADO") String estado,
        String nota,
        String creadoPorCodigo,
        String creadoPorNombre,
        long creadoEn,
        Long cerradoEn,
        Long aplicadoEn,
        String aplicadoPor,
        int lineasTotales,
        int lineasContadas,
        @Schema(description = "Líneas cuyo contado difiere de lo esperado (null si está oculto)") Integer lineasConDiferencia,
        @Schema(description = "Suma de diferencias en unidades (null si está oculto)") Integer unidadesDiferencia,
        List<Linea> lineas
) {

    @Schema(name = "ConteoLinea")
    public record Linea(
            Long id,
            Long productoId,
            String sku,
            String nombre,
            String ubicacion,
            String lote,
            @Schema(description = "null mientras el conteo ciego siga abierto para un operario") Integer esperado,
            @Schema(description = "null = no contado todavía") Integer contado,
            Integer diferencia,
            String contadoPor,
            Long contadoEn
    ) {
    }

    /** @param mostrarEsperado false para un operario en un conteo ciego todavía abierto. */
    public static ConteoResponse from(Conteo c, boolean mostrarEsperado, boolean incluirLineas) {
        List<ConteoLinea> ls = c.getLineas();
        int contadas = (int) ls.stream().filter(l -> l.getContado() != null).count();
        Integer conDif = null;
        Integer unidades = null;
        if (mostrarEsperado) {
            conDif = (int) ls.stream().filter(l -> diferencia(c, l) != null && diferencia(c, l) != 0).count();
            unidades = ls.stream().map(l -> diferencia(c, l)).filter(d -> d != null).mapToInt(Integer::intValue).sum();
        }
        List<Linea> lineas = incluirLineas
                ? ls.stream().map(l -> new Linea(l.getId(), l.getProductoId(), l.getSku(), l.getNombre(),
                        l.getUbicacion(), l.getLote(),
                        mostrarEsperado ? l.getEsperado() : null,
                        l.getContado(),
                        mostrarEsperado ? diferencia(c, l) : null,
                        l.getContadoPor(), l.getContadoEn())).toList()
                : List.of();
        return new ConteoResponse(c.getId(), c.getUbicacion(), c.isCiego(), c.getEstado().name(), c.getNota(),
                c.getCreadoPorCodigo(), c.getCreadoPorNombre(), c.getCreadoEn(), c.getCerradoEn(),
                c.getAplicadoEn(), c.getAplicadoPor(), ls.size(), contadas, conDif, unidades, lineas);
    }

    /**
     * Diferencia contado - esperado. En un conteo de ubicación lo no contado
     * vale 0 (falta); en uno libre, lo no contado no tiene diferencia.
     */
    private static Integer diferencia(Conteo c, ConteoLinea l) {
        Integer contado = l.getContado();
        if (contado == null) {
            return c.esDeUbicacion() ? -l.getEsperado() : null;
        }
        return contado - l.getEsperado();
    }
}
