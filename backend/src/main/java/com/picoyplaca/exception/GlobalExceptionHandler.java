package com.picoyplaca.exception;

import com.picoyplaca.dto.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(FechaAnteriorException.class)
    public ResponseEntity<ErrorResponse> handleFechaAnterior(FechaAnteriorException ex) {
        return ResponseEntity.badRequest().body(
                ErrorResponse.builder()
                        .status(400)
                        .error("FECHA_ANTERIOR")
                        .mensaje(ex.getMessage())
                        .timestamp(LocalDateTime.now())
                        .build()
        );
    }

    @ExceptionHandler(PlacaInvalidaException.class)
    public ResponseEntity<ErrorResponse> handlePlacaInvalida(PlacaInvalidaException ex) {
        return ResponseEntity.badRequest().body(
                ErrorResponse.builder()
                        .status(400)
                        .error("PLACA_INVALIDA")
                        .mensaje(ex.getMessage())
                        .timestamp(LocalDateTime.now())
                        .build()
        );
    }

    // Captura errores de @Valid: recopila cada campo fallido con su mensaje de validacion
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationErrors(MethodArgumentNotValidException ex) {
        Map<String, String> errores = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            // Cast a FieldError para obtener el nombre del campo que fallo
            String campo = ((FieldError) error).getField();
            errores.put(campo, error.getDefaultMessage());
        });
        return ResponseEntity.badRequest().body(
                ErrorResponse.builder()
                        .status(400)
                        .error("VALIDACION_FALLIDA")
                        .mensaje("Errores de validacion en la solicitud.")
                        .errores(errores)
                        .timestamp(LocalDateTime.now())
                        .build()
        );
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(
                ErrorResponse.builder()
                        .status(405)
                        .error("METODO_NO_PERMITIDO")
                        .mensaje("El metodo HTTP '" + ex.getMethod() + "' no esta permitido para esta ruta.")
                        .timestamp(LocalDateTime.now())
                        .build()
        );
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMediaTypeNotSupported(HttpMediaTypeNotSupportedException ex) {
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).body(
                ErrorResponse.builder()
                        .status(415)
                        .error("TIPO_MEDIO_NO_SOPORTADO")
                        .mensaje("El Content-Type '" + ex.getContentType() + "' no esta soportado. Use application/json.")
                        .timestamp(LocalDateTime.now())
                        .build()
        );
    }

    // Fallback: captura cualquier excepcion no manejada para evitar exponer stack traces al cliente
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneral(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ErrorResponse.builder()
                        .status(500)
                        .error("ERROR_INTERNO")
                        .mensaje("Ocurrio un error inesperado. Por favor intente nuevamente.")
                        .timestamp(LocalDateTime.now())
                        .build()
        );
    }
}
