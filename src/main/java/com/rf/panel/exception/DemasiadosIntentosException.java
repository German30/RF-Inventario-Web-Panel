package com.rf.panel.exception;

/** Login bloqueado temporalmente por PINs fallidos repetidos. Responde 429. */
public class DemasiadosIntentosException extends RuntimeException {

    public DemasiadosIntentosException(String mensaje) {
        super(mensaje);
    }
}
