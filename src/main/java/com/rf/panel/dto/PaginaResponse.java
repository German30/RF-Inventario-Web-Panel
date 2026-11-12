package com.rf.panel.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

@Schema(name = "Pagina", description = "Una página de resultados")
public record PaginaResponse<T>(
        List<T> contenido,
        @Schema(description = "Número de página, desde 0") int pagina,
        int tamanio,
        long total,
        int totalPaginas
) {
    public static <E, T> PaginaResponse<T> de(Page<E> page, Function<E, T> mapper) {
        return new PaginaResponse<>(page.getContent().stream().map(mapper).toList(),
                page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }
}
