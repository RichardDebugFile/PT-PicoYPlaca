package com.picoyplaca.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ReglasResponse {

    private List<FranjaHoraria> horarios;
    private List<RestriccionDia> restriccionesPorDia;
    private String fuenteNormativa;

    @Data
    @Builder
    public static class FranjaHoraria {
        private String franja;
        private String inicio;
        private String fin;
    }

    @Data
    @Builder
    public static class RestriccionDia {
        private String dia;
        private List<Integer> digitos;
    }
}
