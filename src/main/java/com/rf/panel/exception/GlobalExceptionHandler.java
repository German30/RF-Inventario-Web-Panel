package com.rf.panel.exception;

import com.rf.panel.dto.ConflictoEdicionResponse;
import com.rf.panel.dto.ErrorResponse;
import com.rf.panel.dto.ProductoResponse;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    /** Login fallido. 401 y no 403: todavía no hay nadie identificado. */
    @ExceptionHandler(CredencialesInvalidasException.class)
    public ResponseEntity<ErrorResponse> credenciales(CredencialesInvalidasException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(cuerpo(HttpStatus.UNAUTHORIZED, ex.getMessage()));
    }

    /** PINs fallidos repetidos: el código queda bloqueado unos minutos. */
    @ExceptionHandler(DemasiadosIntentosException.class)
    public ResponseEntity<ErrorResponse> demasiadosIntentos(DemasiadosIntentosException ex) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .body(cuerpo(HttpStatus.TOO_MANY_REQUESTS, ex.getMessage()));
    }

    /**
     * Una restricción única de la base saltó en una carrera (dos altas con el
     * mismo SKU o el mismo clientOpId a la vez). Reintentar resuelve.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> integridad(DataIntegrityViolationException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(cuerpo(HttpStatus.CONFLICT, "El dato ya existe o cambió al mismo tiempo. Reintentá."));
    }

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<ErrorResponse> noEncontrado(RecursoNoEncontradoException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(cuerpo(HttpStatus.NOT_FOUND, ex.getMessage()));
    }

    /** SKU repetido, stock negativo... la petición es válida pero rompe una regla. */
    @ExceptionHandler(ReglaNegocioException.class)
    public ResponseEntity<ErrorResponse> reglaNegocio(ReglaNegocioException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(cuerpo(HttpStatus.CONFLICT, ex.getMessage()));
    }

    /**
     * Optimistic locking: otro operario guardó este producto primero. Va con
     * cuerpo propio (no {@link ErrorResponse}) porque el cliente necesita el
     * estado actual, no solo el mensaje.
     */
    @ExceptionHandler(ConflictoEdicionException.class)
    public ResponseEntity<ConflictoEdicionResponse> conflictoEdicion(ConflictoEdicionException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ConflictoEdicionResponse(ex.getMessage(), ProductoResponse.from(ex.getActual())));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> validacion(MethodArgumentNotValidException ex) {
        Map<String, String> campos = new HashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(error -> campos.put(error.getField(), error.getDefaultMessage()));

        return ResponseEntity.badRequest()
                .body(cuerpo(HttpStatus.BAD_REQUEST, "Datos de entrada inválidos").conCampos(campos));
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ErrorResponse> parametroInvalido(Exception ex) {
        return ResponseEntity.badRequest()
                .body(cuerpo(HttpStatus.BAD_REQUEST, "Parámetro o cuerpo de la petición inválido."));
    }

    private ErrorResponse cuerpo(HttpStatus status, String mensaje) {
        return ErrorResponse.de(status.value(), status.getReasonPhrase(), mensaje);
    }
}
