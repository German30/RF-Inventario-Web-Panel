package com.rf.panel.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Marca de "esta operación de la app ya se aplicó". La app genera un UUID por
 * operación y lo reenvía tal cual en cada reintento de su cola offline: si la
 * respuesta se perdió en la red, el reintento NO vuelve a mover stock.
 */
@Entity
@Table(name = "processed_operations")
public class OperacionProcesada {

    @Id
    @Column(name = "client_op_id", length = 64)
    private String id;

    @Column(name = "operator_code", nullable = false, length = 32)
    private String operadorCodigo;

    @Column(name = "created_at", nullable = false)
    private long creadoEn;

    protected OperacionProcesada() {
        // JPA
    }

    public OperacionProcesada(String id, String operadorCodigo) {
        this.id = id;
        this.operadorCodigo = operadorCodigo;
        this.creadoEn = System.currentTimeMillis();
    }

    public String getId() {
        return id;
    }
}
