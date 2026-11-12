package com.rf.panel.exception;

import com.rf.panel.entity.Producto;

/**
 * Alguien más guardó el producto entre que este operario lo leyó y lo editó
 * (optimistic locking sobre `actualizadoEn`). Lleva el estado actual del
 * producto para que el cliente pueda mostrarlo sin pedirlo de nuevo.
 */
public class ConflictoEdicionException extends RuntimeException {

    private final transient Producto actual;

    public ConflictoEdicionException(String mensaje, Producto actual) {
        super(mensaje);
        this.actual = actual;
    }

    public Producto getActual() {
        return actual;
    }
}
