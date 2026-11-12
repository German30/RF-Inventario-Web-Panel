package com.rf.panel.exception;

/**
 * La petición está bien escrita pero rompe una regla del dominio: SKU repetido,
 * dejar el stock en negativo, etc. Responde 409.
 */
public class ReglaNegocioException extends RuntimeException {

    public ReglaNegocioException(String mensaje) {
        super(mensaje);
    }
}
