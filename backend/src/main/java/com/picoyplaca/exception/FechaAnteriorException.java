package com.picoyplaca.exception;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class FechaAnteriorException extends RuntimeException {

    public FechaAnteriorException(LocalDateTime fechaHora) {
        super("La fecha y hora ingresada (" +
              fechaHora.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")) +
              ") es anterior a la fecha y hora actual.");
    }
}
