package com.picoyplaca.exception;

public class PlacaInvalidaException extends RuntimeException {

    public PlacaInvalidaException(String placa) {
        super("El formato de la placa '" + placa + "' no es valido. Use el formato: ABC-1234 (particular) o ABC-123 (moto).");
    }
}
