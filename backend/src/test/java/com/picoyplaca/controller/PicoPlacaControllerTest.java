package com.picoyplaca.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.picoyplaca.dto.ConsultaRequest;
import com.picoyplaca.dto.ConsultaResponse;
import com.picoyplaca.dto.ReglasResponse;
import com.picoyplaca.exception.FechaAnteriorException;
import com.picoyplaca.exception.PlacaInvalidaException;
import com.picoyplaca.service.PicoPlacaService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests de la capa web (controller + exception handler).
 * Usa @WebMvcTest: carga solo el contexto web sin levantar el servidor completo.
 * El servicio se reemplaza con un mock para aislar la capa HTTP.
 */
@WebMvcTest(PicoPlacaController.class)
@DisplayName("PicoPlacaController - Capa Web")
class PicoPlacaControllerTest {

    private static final String BASE_URL = "/api/v1/pico-placa";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PicoPlacaService picoPlacaService;

    // =========================================================================
    // POST /verificar - Respuestas exitosas
    // =========================================================================

    @Nested
    @DisplayName("POST /verificar - Respuestas 200 OK")
    class VerificarExitoso {

        @Test
        @DisplayName("Vehiculo restringido: responde 200 con puedeCircular=false")
        void vehiculoRestringido_Responde200() throws Exception {
            ConsultaResponse respuesta = buildResponse("ABC-1234", false, "MARTES",
                    "El vehiculo NO puede circular.", "3 y 4", "06:00 - 09:30");
            when(picoPlacaService.verificar(any())).thenReturn(respuesta);

            mockMvc.perform(post(BASE_URL + "/verificar")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(buildJson("ABC-1234", "2030-01-01T08:00:00")))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.placa").value("ABC-1234"))
                    .andExpect(jsonPath("$.puedeCircular").value(false))
                    .andExpect(jsonPath("$.diaSemana").value("MARTES"))
                    .andExpect(jsonPath("$.digitosRestringidosHoy").value("3 y 4"))
                    .andExpect(jsonPath("$.franjaHorariaRestriccion").value("06:00 - 09:30"));
        }

        @Test
        @DisplayName("Vehiculo libre: responde 200 con puedeCircular=true")
        void vehiculoLibre_Responde200() throws Exception {
            ConsultaResponse respuesta = buildResponse("ABC-1230", true, "SABADO",
                    "Puede circular libremente.", null, null);
            when(picoPlacaService.verificar(any())).thenReturn(respuesta);

            mockMvc.perform(post(BASE_URL + "/verificar")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(buildJson("ABC-1230", "2030-01-05T08:00:00")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.puedeCircular").value(true))
                    .andExpect(jsonPath("$.diaSemana").value("SABADO"))
                    .andExpect(jsonPath("$.digitosRestringidosHoy").doesNotExist());
        }

        @Test
        @DisplayName("Respuesta incluye todos los campos del JSON esperado")
        void respuestaContieneEstructuraCompleta() throws Exception {
            ConsultaResponse respuesta = buildResponse("ABC-123", false, "LUNES",
                    "NO puede circular.", "1 y 2", "16:00 - 20:00");
            when(picoPlacaService.verificar(any())).thenReturn(respuesta);

            mockMvc.perform(post(BASE_URL + "/verificar")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(buildJson("ABC-123", "2030-01-07T17:00:00")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.placa").exists())
                    .andExpect(jsonPath("$.fechaHora").exists())
                    .andExpect(jsonPath("$.diaSemana").exists())
                    .andExpect(jsonPath("$.puedeCircular").exists())
                    .andExpect(jsonPath("$.mensaje").exists());
        }
    }

    // =========================================================================
    // POST /verificar - Validacion de campos (Bean Validation -> 400)
    // =========================================================================

    @Nested
    @DisplayName("POST /verificar - Validacion de campos (400 VALIDACION_FALLIDA)")
    class VerificarValidacion {

        @Test
        @DisplayName("Placa vacia -> 400 con error VALIDACION_FALLIDA")
        void placaVacia_Responde400() throws Exception {
            mockMvc.perform(post(BASE_URL + "/verificar")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(buildJson("", "2030-01-01T08:00:00")))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("VALIDACION_FALLIDA"))
                    .andExpect(jsonPath("$.errores.placa").isNotEmpty());
        }

        @Test
        @DisplayName("Placa nula -> 400 con error VALIDACION_FALLIDA")
        void placaNula_Responde400() throws Exception {
            String body = """
                    {"placa": null, "fechaHora": "2030-01-01T08:00:00"}
                    """;
            mockMvc.perform(post(BASE_URL + "/verificar")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("VALIDACION_FALLIDA"))
                    .andExpect(jsonPath("$.errores.placa").isNotEmpty());
        }

        @Test
        @DisplayName("FechaHora nula -> 400 con error VALIDACION_FALLIDA")
        void fechaNula_Responde400() throws Exception {
            String body = """
                    {"placa": "ABC-1234", "fechaHora": null}
                    """;
            mockMvc.perform(post(BASE_URL + "/verificar")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("VALIDACION_FALLIDA"))
                    .andExpect(jsonPath("$.errores.fechaHora").isNotEmpty());
        }

        @Test
        @DisplayName("Body vacio -> 400")
        void bodyVacio_Responde400() throws Exception {
            mockMvc.perform(post(BASE_URL + "/verificar")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("VALIDACION_FALLIDA"));
        }

        @Test
        @DisplayName("Content-Type incorrecto -> 415 Unsupported Media Type")
        void contentTypeIncorrecto_Responde415() throws Exception {
            mockMvc.perform(post(BASE_URL + "/verificar")
                            .contentType(MediaType.TEXT_PLAIN)
                            .content("ABC-1234"))
                    .andExpect(status().isUnsupportedMediaType());
        }
    }

    // =========================================================================
    // POST /verificar - Excepciones del servicio mapeadas por GlobalExceptionHandler
    // =========================================================================

    @Nested
    @DisplayName("POST /verificar - Manejo de excepciones (GlobalExceptionHandler)")
    class VerificarExcepciones {

        @Test
        @DisplayName("PlacaInvalidaException -> 400 con error PLACA_INVALIDA")
        void placaInvalida_Responde400ConErrorCorrecto() throws Exception {
            when(picoPlacaService.verificar(any()))
                    .thenThrow(new PlacaInvalidaException("1234-ABC"));

            mockMvc.perform(post(BASE_URL + "/verificar")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(buildJson("1234-ABC", "2030-01-01T08:00:00")))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("PLACA_INVALIDA"))
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.mensaje", containsString("1234-ABC")))
                    .andExpect(jsonPath("$.timestamp").isNotEmpty());
        }

        @Test
        @DisplayName("FechaAnteriorException -> 400 con error FECHA_ANTERIOR")
        void fechaAnterior_Responde400ConErrorCorrecto() throws Exception {
            LocalDateTime fechaPasada = LocalDateTime.of(2020, 1, 1, 8, 0);
            when(picoPlacaService.verificar(any()))
                    .thenThrow(new FechaAnteriorException(fechaPasada));

            mockMvc.perform(post(BASE_URL + "/verificar")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(buildJson("ABC-1234", "2020-01-01T08:00:00")))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("FECHA_ANTERIOR"))
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.mensaje", containsString("anterior")))
                    .andExpect(jsonPath("$.timestamp").isNotEmpty());
        }

        @Test
        @DisplayName("Error inesperado del servicio -> 500 ERROR_INTERNO")
        void errorInesperado_Responde500() throws Exception {
            when(picoPlacaService.verificar(any()))
                    .thenThrow(new RuntimeException("Fallo inesperado"));

            mockMvc.perform(post(BASE_URL + "/verificar")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(buildJson("ABC-1234", "2030-01-01T08:00:00")))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.error").value("ERROR_INTERNO"))
                    .andExpect(jsonPath("$.status").value(500));
        }

        @Test
        @DisplayName("Respuesta de error incluye campo timestamp")
        void errorResponse_IncludeTimestamp() throws Exception {
            when(picoPlacaService.verificar(any()))
                    .thenThrow(new PlacaInvalidaException("BAD"));

            mockMvc.perform(post(BASE_URL + "/verificar")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(buildJson("BAD", "2030-01-01T08:00:00")))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.timestamp").exists());
        }
    }

    // =========================================================================
    // GET /reglas
    // =========================================================================

    @Nested
    @DisplayName("GET /reglas")
    class ObtenerReglas {

        @Test
        @DisplayName("Retorna 200 con estructura de reglas completa")
        void obtenerReglas_Responde200() throws Exception {
            ReglasResponse reglas = ReglasResponse.builder()
                    .horarios(List.of(
                            ReglasResponse.FranjaHoraria.builder().franja("Manana").inicio("06:00").fin("09:30").build(),
                            ReglasResponse.FranjaHoraria.builder().franja("Tarde").inicio("16:00").fin("20:00").build()
                    ))
                    .restriccionesPorDia(List.of(
                            ReglasResponse.RestriccionDia.builder().dia("LUNES").digitos(List.of(1, 2)).build()
                    ))
                    .fuenteNormativa("AMT Quito - Resolucion AQ-013-2023")
                    .build();
            when(picoPlacaService.obtenerReglas()).thenReturn(reglas);

            mockMvc.perform(get(BASE_URL + "/reglas"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.horarios").isArray())
                    .andExpect(jsonPath("$.horarios", hasSize(2)))
                    .andExpect(jsonPath("$.horarios[0].franja").value("Manana"))
                    .andExpect(jsonPath("$.horarios[0].inicio").value("06:00"))
                    .andExpect(jsonPath("$.horarios[0].fin").value("09:30"))
                    .andExpect(jsonPath("$.restriccionesPorDia").isArray())
                    .andExpect(jsonPath("$.fuenteNormativa").value("AMT Quito - Resolucion AQ-013-2023"));
        }

        @Test
        @DisplayName("GET /reglas no acepta body (metodo correcto es GET)")
        void reglas_MetodoGETCorrecto() throws Exception {
            when(picoPlacaService.obtenerReglas()).thenReturn(
                    ReglasResponse.builder().horarios(List.of()).restriccionesPorDia(List.of())
                            .fuenteNormativa("test").build());

            mockMvc.perform(get(BASE_URL + "/reglas"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("POST /reglas -> 405 Method Not Allowed")
        void reglas_PostNoPermitido() throws Exception {
            mockMvc.perform(post(BASE_URL + "/reglas")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isMethodNotAllowed());
        }
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private String buildJson(String placa, String fechaHora) throws Exception {
        ConsultaRequest req = new ConsultaRequest();
        req.setPlaca(placa);
        if (fechaHora != null) {
            req.setFechaHora(LocalDateTime.parse(fechaHora));
        }
        return objectMapper.writeValueAsString(req);
    }

    private ConsultaResponse buildResponse(String placa, boolean puedeCircular, String diaSemana,
                                            String mensaje, String digitos, String franja) {
        return ConsultaResponse.builder()
                .placa(placa)
                .fechaHora(LocalDateTime.of(2030, 1, 1, 8, 0))
                .diaSemana(diaSemana)
                .puedeCircular(puedeCircular)
                .mensaje(mensaje)
                .digitosRestringidosHoy(digitos)
                .franjaHorariaRestriccion(franja)
                .build();
    }
}
