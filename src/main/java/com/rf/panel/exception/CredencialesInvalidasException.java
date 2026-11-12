package com.rf.panel.exception;

/** Código o PIN incorrectos. Nunca dice cuál de los dos. */
public class CredencialesInvalidasException extends RuntimeException {

    public CredencialesInvalidasException() {
        super("Código o PIN incorrectos.");
    }
}
