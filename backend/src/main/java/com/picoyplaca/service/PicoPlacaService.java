package com.picoyplaca.service;

import com.picoyplaca.dto.ConsultaRequest;
import com.picoyplaca.dto.ConsultaResponse;
import com.picoyplaca.dto.ReglasResponse;
import com.picoyplaca.exception.FechaAnteriorException;
import com.picoyplaca.exception.PlacaInvalidaException;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

@Service
public class PicoPlacaService {

    // Formato oficial ANT Ecuador: 3 letras + guion + 3 o 4 digitos (ej. ABC-1234, ABC-123)
    private static final Pattern PLACA_REGEX =
            Pattern.compile("^[A-Za-z]{3}-[0-9]{3,4}$");

    // Franjas horarias de restriccion segun Res. AQ-013-2023
    private static final LocalTime INICIO_MANANA = LocalTime.of(6, 0);
    private static final LocalTime FIN_MANANA    = LocalTime.of(9, 30);
    private static final LocalTime INICIO_TARDE  = LocalTime.of(16, 0);
    private static final LocalTime FIN_TARDE     = LocalTime.of(20, 0);

    // Digitos del ultimo numero de la placa restringidos por dia de semana (solo dias habiles)
    private static final Map<DayOfWeek, Set<Integer>> RESTRICCIONES = Map.of(
            DayOfWeek.MONDAY,    Set.of(1, 2),
            DayOfWeek.TUESDAY,   Set.of(3, 4),
            DayOfWeek.WEDNESDAY, Set.of(5, 6),
            DayOfWeek.THURSDAY,  Set.of(7, 8),
            DayOfWeek.FRIDAY,    Set.of(9, 0)
    );

    // Traduccion de dias al espanol para incluir en los mensajes de respuesta
    private static final Map<DayOfWeek, String> DIAS_ES = Map.of(
            DayOfWeek.MONDAY,    "LUNES",
            DayOfWeek.TUESDAY,   "MARTES",
            DayOfWeek.WEDNESDAY, "MIERCOLES",
            DayOfWeek.THURSDAY,  "JUEVES",
            DayOfWeek.FRIDAY,    "VIERNES",
            DayOfWeek.SATURDAY,  "SABADO",
            DayOfWeek.SUNDAY,    "DOMINGO"
    );

    public ConsultaResponse verificar(ConsultaRequest request) {
        validarFechaNoAnterior(request.getFechaHora());
        validarFormatoPlaca(request.getPlaca());

        String placa = request.getPlaca().toUpperCase();
        LocalDateTime fechaHora = request.getFechaHora();
        DayOfWeek diaSemana = fechaHora.getDayOfWeek();
        String diaNombre = DIAS_ES.getOrDefault(diaSemana, diaSemana.name());

        // Fin de semana: sin restriccion
        if (diaSemana == DayOfWeek.SATURDAY || diaSemana == DayOfWeek.SUNDAY) {
            return buildResponse(placa, fechaHora, diaNombre, true,
                    "El vehiculo con placa " + placa + " puede circular libremente. No hay restriccion de Pico y Placa los fines de semana.",
                    null, null);
        }

        int ultimoDigito = extraerUltimoDigito(placa);
        Set<Integer> digitosRestringidos = RESTRICCIONES.get(diaSemana);
        String digitosStr = formatearDigitos(digitosRestringidos);

        if (!digitosRestringidos.contains(ultimoDigito)) {
            return buildResponse(placa, fechaHora, diaNombre, true,
                    "El vehiculo con placa " + placa + " puede circular. Su digito (" + ultimoDigito +
                    ") no esta restringido el dia " + capitalize(diaNombre) + ".",
                    digitosStr, null);
        }

        // Placa restringida ese dia: verificar si la hora cae dentro de alguna franja
        LocalTime hora = fechaHora.toLocalTime();
        String franja = null;
        boolean enRestriccion = false;

        // !isBefore(inicio) && !isAfter(fin) equivale a: inicio <= hora <= fin (inclusive en ambos extremos)
        if (!hora.isBefore(INICIO_MANANA) && !hora.isAfter(FIN_MANANA)) {
            enRestriccion = true;
            franja = "06:00 - 09:30";
        } else if (!hora.isBefore(INICIO_TARDE) && !hora.isAfter(FIN_TARDE)) {
            enRestriccion = true;
            franja = "16:00 - 20:00";
        }

        String fechaStr = fechaHora.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        String horaStr  = fechaHora.format(DateTimeFormatter.ofPattern("HH:mm"));

        if (enRestriccion) {
            return buildResponse(placa, fechaHora, diaNombre, false,
                    "El vehiculo con placa " + placa + " NO puede circular el " +
                    capitalize(diaNombre) + " " + fechaStr + " a las " + horaStr +
                    ". Aplica restriccion de Pico y Placa (digitos " + digitosStr +
                    ", horario " + franja + ").",
                    digitosStr, franja);
        } else {
            return buildResponse(placa, fechaHora, diaNombre, true,
                    "El vehiculo con placa " + placa + " puede circular el " +
                    capitalize(diaNombre) + " " + fechaStr + " a las " + horaStr +
                    ". Aunque su digito (" + ultimoDigito + ") aplica restriccion hoy, " +
                    "la hora esta fuera del horario restringido (06:00-09:30 y 16:00-20:00).",
                    digitosStr, null);
        }
    }

    public ReglasResponse obtenerReglas() {
        return ReglasResponse.builder()
                .horarios(List.of(
                        ReglasResponse.FranjaHoraria.builder().franja("Manana").inicio("06:00").fin("09:30").build(),
                        ReglasResponse.FranjaHoraria.builder().franja("Tarde").inicio("16:00").fin("20:00").build()
                ))
                .restriccionesPorDia(List.of(
                        ReglasResponse.RestriccionDia.builder().dia("LUNES").digitos(List.of(1, 2)).build(),
                        ReglasResponse.RestriccionDia.builder().dia("MARTES").digitos(List.of(3, 4)).build(),
                        ReglasResponse.RestriccionDia.builder().dia("MIERCOLES").digitos(List.of(5, 6)).build(),
                        ReglasResponse.RestriccionDia.builder().dia("JUEVES").digitos(List.of(7, 8)).build(),
                        ReglasResponse.RestriccionDia.builder().dia("VIERNES").digitos(List.of(9, 0)).build()
                ))
                .fuenteNormativa("AMT Quito - Resolucion AQ-013-2023")
                .build();
    }

    private void validarFechaNoAnterior(LocalDateTime fechaHora) {
        // Se trunca a minutos porque el cliente datetime-local envia HH:mm:00.
        // Comparar con segundos causaria falsos positivos para la hora actual.
        LocalDateTime ahoraTruncado = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES);
        if (fechaHora.isBefore(ahoraTruncado)) {
            throw new FechaAnteriorException(fechaHora);
        }
    }

    private void validarFormatoPlaca(String placa) {
        if (!PLACA_REGEX.matcher(placa.trim()).matches()) {
            throw new PlacaInvalidaException(placa);
        }
    }

    int extraerUltimoDigito(String placa) {
        // Se recorre de derecha a izquierda para ignorar letras finales (ej. placas antiguas)
        String soloNumerosYLetras = placa.replaceAll("[^A-Za-z0-9]", "");
        for (int i = soloNumerosYLetras.length() - 1; i >= 0; i--) {
            char c = soloNumerosYLetras.charAt(i);
            if (Character.isDigit(c)) {
                return Character.getNumericValue(c);
            }
        }
        throw new PlacaInvalidaException(placa);
    }

    private String formatearDigitos(Set<Integer> digitos) {
        // Ordena para garantizar salida consistente: "0 y 9" en vez de "9 y 0"
        List<Integer> sorted = digitos.stream().sorted().toList();
        return sorted.get(0) + " y " + sorted.get(1);
    }

    private String capitalize(String text) {
        if (text == null || text.isEmpty()) return text;
        return text.charAt(0) + text.substring(1).toLowerCase();
    }

    private ConsultaResponse buildResponse(String placa, LocalDateTime fechaHora, String diaSemana,
                                            boolean puedeCircular, String mensaje,
                                            String digitosRestringidos, String franja) {
        return ConsultaResponse.builder()
                .placa(placa)
                .fechaHora(fechaHora)
                .diaSemana(diaSemana)
                .puedeCircular(puedeCircular)
                .mensaje(mensaje)
                .digitosRestringidosHoy(digitosRestringidos)
                .franjaHorariaRestriccion(franja)
                .build();
    }
}
