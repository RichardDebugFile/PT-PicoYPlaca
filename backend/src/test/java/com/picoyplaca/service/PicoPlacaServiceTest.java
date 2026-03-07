package com.picoyplaca.service;

import com.picoyplaca.dto.ConsultaRequest;
import com.picoyplaca.dto.ConsultaResponse;
import com.picoyplaca.dto.ReglasResponse;
import com.picoyplaca.exception.FechaAnteriorException;
import com.picoyplaca.exception.PlacaInvalidaException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests unitarios de PicoPlacaService.
 * No se levanta contexto Spring: se instancia el servicio directamente.
 * Organizados por @Nested para cubrir cada dia, horario y caso borde.
 *
 * Calendario de referencia usado en los tests:
 *   2029-12-31 = Lunes
 *   2030-01-01 = Martes
 *   2030-01-02 = Miercoles
 *   2030-01-03 = Jueves
 *   2030-01-04 = Viernes
 *   2030-01-05 = Sabado
 *   2030-01-06 = Domingo
 */
@DisplayName("PicoPlacaService - Tests unitarios")
class PicoPlacaServiceTest {

    private PicoPlacaService service;

    @BeforeEach
    void setUp() {
        service = new PicoPlacaService();
    }

    // =========================================================================
    // LUNES - Digitos 1 y 2
    // =========================================================================

    @Nested
    @DisplayName("LUNES (digitos restringidos: 1 y 2)")
    class Lunes {

        // 2029-12-31 = Lunes
        private static final int ANIO = 2029, MES = 12, DIA = 31;

        @Test
        @DisplayName("Digito 1 en franja manana (08:00) -> NO circula")
        void digito1_MananaRestriccion_NoCircula() {
            var resp = verificar("ABC-1231", ANIO, MES, DIA, 8, 0);
            assertThat(resp.isPuedeCircular()).isFalse();
            assertThat(resp.getFranjaHorariaRestriccion()).isEqualTo("06:00 - 09:30");
            assertThat(resp.getDigitosRestringidosHoy()).isEqualTo("1 y 2");
        }

        @Test
        @DisplayName("Digito 2 en franja tarde (17:00) -> NO circula")
        void digito2_TardeRestriccion_NoCircula() {
            var resp = verificar("ABC-1232", ANIO, MES, DIA, 17, 0);
            assertThat(resp.isPuedeCircular()).isFalse();
            assertThat(resp.getFranjaHorariaRestriccion()).isEqualTo("16:00 - 20:00");
        }

        @Test
        @DisplayName("Digito 1 antes de las 06:00 (05:59) -> SI circula")
        void digito1_AntesDeFranja_SiCircula() {
            var resp = verificar("ABC-1231", ANIO, MES, DIA, 5, 59);
            assertThat(resp.isPuedeCircular()).isTrue();
        }

        @Test
        @DisplayName("Digito 1 entre 09:31 y 15:59 -> SI circula")
        void digito1_EntreHorarios_SiCircula() {
            var resp = verificar("ABC-1231", ANIO, MES, DIA, 12, 0);
            assertThat(resp.isPuedeCircular()).isTrue();
        }

        @Test
        @DisplayName("Digito 1 despues de 20:00 (21:00) -> SI circula")
        void digito1_DespuesDeFranjaTarde_SiCircula() {
            var resp = verificar("ABC-1231", ANIO, MES, DIA, 21, 0);
            assertThat(resp.isPuedeCircular()).isTrue();
        }

        @Test
        @DisplayName("Digito 3 en Lunes (no restringido ese dia) -> SI circula")
        void digito3_Lunes_SiCircula() {
            var resp = verificar("ABC-1233", ANIO, MES, DIA, 8, 0);
            assertThat(resp.isPuedeCircular()).isTrue();
        }

        @Test
        @DisplayName("Franja manana: limite inicio exacto 06:00 -> NO circula")
        void digito1_LimiteInicioManana_NoCircula() {
            var resp = verificar("ABC-1231", ANIO, MES, DIA, 6, 0);
            assertThat(resp.isPuedeCircular()).isFalse();
        }

        @Test
        @DisplayName("Franja manana: limite fin exacto 09:30 -> NO circula")
        void digito1_LimiteFinManana_NoCircula() {
            var resp = verificar("ABC-1231", ANIO, MES, DIA, 9, 30);
            assertThat(resp.isPuedeCircular()).isFalse();
        }

        @Test
        @DisplayName("Franja tarde: limite inicio exacto 16:00 -> NO circula")
        void digito1_LimiteInicioTarde_NoCircula() {
            var resp = verificar("ABC-1231", ANIO, MES, DIA, 16, 0);
            assertThat(resp.isPuedeCircular()).isFalse();
        }

        @Test
        @DisplayName("Franja tarde: limite fin exacto 20:00 -> NO circula")
        void digito1_LimiteFinTarde_NoCircula() {
            var resp = verificar("ABC-1231", ANIO, MES, DIA, 20, 0);
            assertThat(resp.isPuedeCircular()).isFalse();
        }
    }

    // =========================================================================
    // MARTES - Digitos 3 y 4
    // =========================================================================

    @Nested
    @DisplayName("MARTES (digitos restringidos: 3 y 4)")
    class Martes {

        // 2030-01-01 = Martes
        private static final int ANIO = 2030, MES = 1, DIA = 1;

        @Test
        @DisplayName("Digito 4 en franja manana -> NO circula")
        void digito4_MananaRestriccion_NoCircula() {
            var resp = verificar("ABC-1234", ANIO, MES, DIA, 8, 0);
            assertThat(resp.isPuedeCircular()).isFalse();
            assertThat(resp.getDiaSemana()).isEqualTo("MARTES");
        }

        @Test
        @DisplayName("Digito 4 fuera de horario (10:00) -> SI circula")
        void digito4_FueraHorario_SiCircula() {
            var resp = verificar("ABC-1234", ANIO, MES, DIA, 10, 0);
            assertThat(resp.isPuedeCircular()).isTrue();
            assertThat(resp.getFranjaHorariaRestriccion()).isNull();
        }

        @Test
        @DisplayName("Digito 4 en franja tarde (17:00) -> NO circula")
        void digito4_TardeRestriccion_NoCircula() {
            var resp = verificar("ABC-1234", ANIO, MES, DIA, 17, 0);
            assertThat(resp.isPuedeCircular()).isFalse();
        }

        @Test
        @DisplayName("Digito 4 despues de tarde (21:00) -> SI circula")
        void digito4_DespuesTarde_SiCircula() {
            var resp = verificar("ABC-1234", ANIO, MES, DIA, 21, 0);
            assertThat(resp.isPuedeCircular()).isTrue();
        }

        @Test
        @DisplayName("Digito 3 en franja manana -> NO circula")
        void digito3_MananaRestriccion_NoCircula() {
            var resp = verificar("ABC-123", ANIO, MES, DIA, 7, 0);
            assertThat(resp.isPuedeCircular()).isFalse();
        }
    }

    // =========================================================================
    // MIERCOLES - Digitos 5 y 6
    // =========================================================================

    @Nested
    @DisplayName("MIERCOLES (digitos restringidos: 5 y 6)")
    class Miercoles {

        // 2030-01-02 = Miercoles
        private static final int ANIO = 2030, MES = 1, DIA = 2;

        @Test
        @DisplayName("Digito 6 en franja manana -> NO circula")
        void digito6_MananaRestriccion_NoCircula() {
            var resp = verificar("ABC-1236", ANIO, MES, DIA, 8, 0);
            assertThat(resp.isPuedeCircular()).isFalse();
            assertThat(resp.getDigitosRestringidosHoy()).isEqualTo("5 y 6");
        }

        @Test
        @DisplayName("Digito 5 en franja tarde -> NO circula")
        void digito5_TardeRestriccion_NoCircula() {
            var resp = verificar("ABC-1235", ANIO, MES, DIA, 18, 0);
            assertThat(resp.isPuedeCircular()).isFalse();
        }

        @Test
        @DisplayName("Digito 4 en Miercoles (dia diferente) -> SI circula")
        void digito4_Miercoles_SiCircula() {
            var resp = verificar("ABC-1234", ANIO, MES, DIA, 8, 0);
            assertThat(resp.isPuedeCircular()).isTrue();
        }
    }

    // =========================================================================
    // JUEVES - Digitos 7 y 8
    // =========================================================================

    @Nested
    @DisplayName("JUEVES (digitos restringidos: 7 y 8)")
    class Jueves {

        // 2030-01-03 = Jueves
        private static final int ANIO = 2030, MES = 1, DIA = 3;

        @Test
        @DisplayName("Digito 8 en franja manana -> NO circula")
        void digito8_MananaRestriccion_NoCircula() {
            var resp = verificar("ABC-1238", ANIO, MES, DIA, 9, 0);
            assertThat(resp.isPuedeCircular()).isFalse();
            assertThat(resp.getDigitosRestringidosHoy()).isEqualTo("7 y 8");
        }

        @Test
        @DisplayName("Digito 7 en franja tarde -> NO circula")
        void digito7_TardeRestriccion_NoCircula() {
            var resp = verificar("ABC-1237", ANIO, MES, DIA, 19, 0);
            assertThat(resp.isPuedeCircular()).isFalse();
        }

        @Test
        @DisplayName("Digito 8 a las 09:31 (justo despues de franja manana) -> SI circula")
        void digito8_JustoDespuesFranjaManana_SiCircula() {
            var resp = verificar("ABC-1238", ANIO, MES, DIA, 9, 31);
            assertThat(resp.isPuedeCircular()).isTrue();
        }
    }

    // =========================================================================
    // VIERNES - Digitos 9 y 0
    // =========================================================================

    @Nested
    @DisplayName("VIERNES (digitos restringidos: 9 y 0)")
    class Viernes {

        // 2030-01-04 = Viernes
        private static final int ANIO = 2030, MES = 1, DIA = 4;

        @Test
        @DisplayName("Digito 9 en franja manana -> NO circula")
        void digito9_MananaRestriccion_NoCircula() {
            var resp = verificar("ABC-1239", ANIO, MES, DIA, 8, 0);
            assertThat(resp.isPuedeCircular()).isFalse();
        }

        @Test
        @DisplayName("Digito 0 en franja tarde -> NO circula")
        void digito0_TardeRestriccion_NoCircula() {
            var resp = verificar("PBC-5670", ANIO, MES, DIA, 17, 0);
            assertThat(resp.isPuedeCircular()).isFalse();
            assertThat(resp.getDigitosRestringidosHoy()).isEqualTo("0 y 9");
        }

        @Test
        @DisplayName("Digito 9 en fin exacto de franja manana (09:30) -> NO circula")
        void digito9_FinExactoFranjaManana_NoCircula() {
            var resp = verificar("ABC-1239", ANIO, MES, DIA, 9, 30);
            assertThat(resp.isPuedeCircular()).isFalse();
        }
    }

    // =========================================================================
    // FIN DE SEMANA - Sin restriccion
    // =========================================================================

    @Nested
    @DisplayName("FIN DE SEMANA (sin restriccion)")
    class FinDeSemana {

        @Test
        @DisplayName("Sabado con cualquier placa en horario pico -> SI circula")
        void sabado_HorarioPico_SiCircula() {
            // 2030-01-05 = Sabado
            var resp = verificar("ABC-1231", 2030, 1, 5, 8, 0);
            assertThat(resp.isPuedeCircular()).isTrue();
            assertThat(resp.getDiaSemana()).isEqualTo("SABADO");
            assertThat(resp.getDigitosRestringidosHoy()).isNull();
            assertThat(resp.getFranjaHorariaRestriccion()).isNull();
        }

        @Test
        @DisplayName("Domingo con cualquier placa en horario pico -> SI circula")
        void domingo_HorarioPico_SiCircula() {
            // 2030-01-06 = Domingo
            var resp = verificar("XYZ-9999", 2030, 1, 6, 17, 0);
            assertThat(resp.isPuedeCircular()).isTrue();
            assertThat(resp.getDiaSemana()).isEqualTo("DOMINGO");
        }

        @Test
        @DisplayName("Sabado: mensaje menciona fin de semana")
        void sabado_MensajeMencionaFinDeSemana() {
            var resp = verificar("ABC-1230", 2030, 1, 5, 8, 0);
            assertThat(resp.getMensaje()).containsIgnoringCase("fines de semana");
        }
    }

    // =========================================================================
    // Extraccion del ultimo digito numerico
    // =========================================================================

    @Nested
    @DisplayName("Extraccion del ultimo digito numerico de la placa")
    class ExtraerUltimoDigito {

        @ParameterizedTest(name = "{0} -> digito {1}")
        @CsvSource({
            "ABC-1234, 4",
            "ABC-123,  3",
            "PBX-1234, 4",
            "PBC-5670, 0",
            "PCB-9999, 9",
            "ABX-1111, 1",
            "XBC-2222, 2"
        })
        @DisplayName("Extrae correctamente el ultimo digito")
        void extraeUltimoDigito(String placa, int esperado) {
            assertThat(service.extraerUltimoDigito(placa)).isEqualTo(esperado);
        }
    }

    // =========================================================================
    // Formatos de placa validos
    // =========================================================================

    @Nested
    @DisplayName("Formatos de placa validos (no lanzan excepcion)")
    class FormatsPlacaValidos {

        @ParameterizedTest(name = "Placa valida: {0}")
        @ValueSource(strings = {"ABC-1234", "ABC-123", "PBX-1234", "PCB-9999", "ZZZ-0001"})
        @DisplayName("Formatos reconocidos como validos")
        void formatoValido_NoLanzaExcepcion(String placa) {
            ConsultaRequest req = buildRequest(placa, LocalDateTime.of(2030, 1, 5, 12, 0));
            assertThat(service.verificar(req)).isNotNull();
        }
    }

    // =========================================================================
    // Validaciones de entrada - Excepciones esperadas
    // =========================================================================

    @Nested
    @DisplayName("Validaciones de entrada - Excepciones")
    class ValidacionesEntrada {

        @Test
        @DisplayName("Fecha anterior a la actual -> FechaAnteriorException")
        void fechaAnterior_LanzaFechaAnteriorException() {
            ConsultaRequest req = buildRequest("ABC-1234", LocalDateTime.of(2020, 1, 1, 8, 0));
            assertThatThrownBy(() -> service.verificar(req))
                    .isInstanceOf(FechaAnteriorException.class)
                    .hasMessageContaining("anterior a la fecha y hora actual");
        }

        @ParameterizedTest(name = "Placa invalida: {0}")
        @ValueSource(strings = {"1234-ABC", "12345", "ABCD-1234", "A-1", "", "ABC-DEFG", "MA-123", "AB-123A", "PBX1234", "XX-5670"})
        @DisplayName("Formato de placa invalido -> PlacaInvalidaException")
        void placaInvalida_LanzaPlacaInvalidaException(String placa) {
            ConsultaRequest req = buildRequest(placa, LocalDateTime.of(2030, 1, 1, 8, 0));
            assertThatThrownBy(() -> service.verificar(req))
                    .isInstanceOf(PlacaInvalidaException.class);
        }

        @Test
        @DisplayName("Mensaje de FechaAnteriorException contiene la fecha ingresada")
        void fechaAnteriorException_MensajeConteneFecha() {
            LocalDateTime fecha = LocalDateTime.of(2020, 6, 15, 8, 0);
            ConsultaRequest req = buildRequest("ABC-1234", fecha);
            assertThatThrownBy(() -> service.verificar(req))
                    .isInstanceOf(FechaAnteriorException.class)
                    .hasMessageContaining("2020-06-15");
        }

        @Test
        @DisplayName("Mensaje de PlacaInvalidaException contiene la placa ingresada")
        void placaInvalidaException_MensajeContienePlaca() {
            String placaMala = "1234-ABC";
            ConsultaRequest req = buildRequest(placaMala, LocalDateTime.of(2030, 1, 1, 8, 0));
            assertThatThrownBy(() -> service.verificar(req))
                    .isInstanceOf(PlacaInvalidaException.class)
                    .hasMessageContaining(placaMala);
        }
    }

    // =========================================================================
    // obtenerReglas
    // =========================================================================

    @Nested
    @DisplayName("obtenerReglas - Estructura de reglas")
    class ObtenerReglas {

        @Test
        @DisplayName("Retorna exactamente 2 franjas horarias")
        void retorna2FranjasHorarias() {
            ReglasResponse reglas = service.obtenerReglas();
            assertThat(reglas.getHorarios()).hasSize(2);
        }

        @Test
        @DisplayName("Retorna exactamente 5 dias de restriccion (Lunes a Viernes)")
        void retorna5DiasRestriccion() {
            ReglasResponse reglas = service.obtenerReglas();
            assertThat(reglas.getRestriccionesPorDia()).hasSize(5);
        }

        @Test
        @DisplayName("Franja manana correcta: 06:00 - 09:30")
        void franjaMananaCorrecta() {
            ReglasResponse reglas = service.obtenerReglas();
            ReglasResponse.FranjaHoraria manana = reglas.getHorarios().get(0);
            assertThat(manana.getInicio()).isEqualTo("06:00");
            assertThat(manana.getFin()).isEqualTo("09:30");
        }

        @Test
        @DisplayName("Franja tarde correcta: 16:00 - 20:00")
        void franjaTardeCorrecta() {
            ReglasResponse reglas = service.obtenerReglas();
            ReglasResponse.FranjaHoraria tarde = reglas.getHorarios().get(1);
            assertThat(tarde.getInicio()).isEqualTo("16:00");
            assertThat(tarde.getFin()).isEqualTo("20:00");
        }

        @Test
        @DisplayName("Fuente normativa correcta")
        void fuenteNormativaCorrecta() {
            ReglasResponse reglas = service.obtenerReglas();
            assertThat(reglas.getFuenteNormativa()).contains("AQ-013-2023");
        }

        @Test
        @DisplayName("Lunes tiene digitos 1 y 2")
        void lunesTieneDigitos1y2() {
            ReglasResponse reglas = service.obtenerReglas();
            ReglasResponse.RestriccionDia lunes = reglas.getRestriccionesPorDia().stream()
                    .filter(r -> "LUNES".equals(r.getDia())).findFirst().orElseThrow();
            assertThat(lunes.getDigitos()).containsExactlyInAnyOrder(1, 2);
        }
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private ConsultaResponse verificar(String placa, int anio, int mes, int dia, int hora, int min) {
        return service.verificar(buildRequest(placa, LocalDateTime.of(anio, mes, dia, hora, min)));
    }

    private ConsultaRequest buildRequest(String placa, LocalDateTime fechaHora) {
        ConsultaRequest req = new ConsultaRequest();
        req.setPlaca(placa);
        req.setFechaHora(fechaHora);
        return req;
    }
}
