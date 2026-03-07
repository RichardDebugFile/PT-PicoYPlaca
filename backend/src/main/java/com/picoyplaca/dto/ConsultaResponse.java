package com.picoyplaca.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ConsultaResponse {

    private String placa;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime fechaHora;

    private String diaSemana;
    private boolean puedeCircular;
    private String mensaje;
    private String digitosRestringidosHoy;
    private String franjaHorariaRestriccion;
}
