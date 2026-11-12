package com.rf.panel.entity;

/** Qué le pasó al producto en una fila de {@link MovimientoStock}. */
public enum AccionMovimiento {
    CREACION,
    EDICION,
    /** Ajuste de stock con signo (corrección, merma, rotura...). */
    AJUSTE_STOCK,
    ELIMINACION,
    /** Ingreso de mercadería sin pedido. */
    ENTRADA,
    /** Egreso de mercadería sin pedido (consumo, venta mostrador...). */
    SALIDA,
    /** Cambio de ubicación: una fila por origen con delta negativo y otra por destino con delta positivo. */
    TRANSFERENCIA,
    /** Diferencia aplicada al cerrar un conteo cíclico. */
    CONTEO,
    /** Ingreso contra un pedido de compra. */
    RECEPCION,
    /** Egreso contra un pedido de venta. */
    PICKING
}
