package com.rf.panel.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ReporteServiceTest {

    @Test
    void neutralizaFormulasYEscapaSeparadores() {
        assertThat(ReporteService.celda("=HYPERLINK(\"x\")")).isEqualTo("\"'=HYPERLINK(\"\"x\"\")\"");
        assertThat(ReporteService.celda("a;b")).isEqualTo("\"a;b\"");
        assertThat(ReporteService.celda(-3)).isEqualTo("-3");
        assertThat(ReporteService.celda(null)).isEmpty();
    }
}
