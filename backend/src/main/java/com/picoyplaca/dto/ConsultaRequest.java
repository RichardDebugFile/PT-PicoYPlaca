package com.picoyplaca.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ConsultaRequest {

    @NotBlank(message = "La placa no puede estar vacia")
    private String placa;

    @NotNull(message = "La fecha y hora son obligatorias")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime fechaHora;
}
