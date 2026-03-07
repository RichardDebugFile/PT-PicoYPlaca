package com.picoyplaca.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.picoyplaca.dto.ConsultaRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests de integracion: levanta el contexto Spring completo (sin mocks).
 * Verifica el flujo end-to-end: HTTP -> Controller -> Service -> Response.
 * Usa @AutoConfigureMockMvc para no levantar el servidor TCP real.
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Integracion - Flujo completo Spring Boot")
class PicoPlacaIntegrationTest {

    private static final String BASE_URL = "/api/v1/pico-placa";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    // =========================================================================
    // Flujo exitoso end-to-end
    // =========================================================================

    @Nested
    @DisplayName("Flujo exitoso - Logica de negocio real")
    class FlujoExitoso {

        @Test
        @DisplayName("ABC-1234 el Martes 08:00 -> puedeCircular=false (logica real)")
        void flujoCompleto_VehiculoRestringido() throws Exception {
            // 2030-01-01 es Martes; digito 4 restringido en manana
            mockMvc.perform(post(BASE_URL + "/verificar")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json("ABC-1234", "2030-01-01T08:00:00")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.puedeCircular").value(false))
                    .andExpect(jsonPath("$.diaSemana").value("MARTES"))
                    .andExpect(jsonPath("$.franjaHorariaRestriccion").value("06:00 - 09:30"))
                    .andExpect(jsonPath("$.digitosRestringidosHoy").value("3 y 4"))
                    .andExpect(jsonPath("$.mensaje").value(containsString("NO puede circular")));
        }

        @Test
        @DisplayName("ABC-1230 el Sabado 08:00 -> puedeCircular=true (logica real)")
        void flujoCompleto_VehiculoLibreSabado() throws Exception {
            // 2030-01-05 es Sabado
            mockMvc.perform(post(BASE_URL + "/verificar")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json("ABC-1230", "2030-01-05T08:00:00")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.puedeCircular").value(true))
                    .andExpect(jsonPath("$.diaSemana").value("SABADO"))
                    .andExpect(jsonPath("$.digitosRestringidosHoy").doesNotExist())
                    .andExpect(jsonPath("$.franjaHorariaRestriccion").doesNotExist());
        }

        @Test
        @DisplayName("ABC-1234 el Martes 10:00 -> puedeCircular=true (fuera de horario)")
        void flujoCompleto_VehiculoRestringidoPeroFueraHorario() throws Exception {
            mockMvc.perform(post(BASE_URL + "/verificar")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json("ABC-1234", "2030-01-01T10:00:00")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.puedeCircular").value(true))
                    .andExpect(jsonPath("$.franjaHorariaRestriccion").doesNotExist());
        }

        @Test
        @DisplayName("ABC-123 (moto) el Martes 08:00 -> puedeCircular=false (digito 3)")
        void flujoCompleto_Motocicleta_Restringida() throws Exception {
            // ABC-123 -> ultimo digito 3, restringido el Martes
            mockMvc.perform(post(BASE_URL + "/verificar")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json("ABC-123", "2030-01-01T08:00:00")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.puedeCircular").value(false));
        }

        @Test
        @DisplayName("AB-123A (letra final) -> 400 PLACA_INVALIDA (formato no reconocido por ANT)")
        void flujoCompleto_PlacaConLetraFinal_Invalida() throws Exception {
            mockMvc.perform(post(BASE_URL + "/verificar")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json("AB-123A", "2030-01-01T08:00:00")))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("PLACA_INVALIDA"));
        }
    }

    // =========================================================================
    // Flujo de errores end-to-end
    // =========================================================================

    @Nested
    @DisplayName("Flujo de errores - Validaciones reales")
    class FlujoErrores {

        @Test
        @DisplayName("Fecha en el pasado -> 400 FECHA_ANTERIOR (sin mock)")
        void flujoError_FechaAnterior() throws Exception {
            mockMvc.perform(post(BASE_URL + "/verificar")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json("ABC-1234", "2020-01-01T08:00:00")))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("FECHA_ANTERIOR"))
                    .andExpect(jsonPath("$.mensaje").value(containsString("anterior")))
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.timestamp").isNotEmpty());
        }

        @Test
        @DisplayName("Formato de placa invalido -> 400 PLACA_INVALIDA (sin mock)")
        void flujoError_PlacaInvalida() throws Exception {
            mockMvc.perform(post(BASE_URL + "/verificar")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json("1234-ABC", "2030-01-01T08:00:00")))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("PLACA_INVALIDA"))
                    .andExpect(jsonPath("$.mensaje").value(containsString("1234-ABC")));
        }

        @Test
        @DisplayName("Placa vacia -> 400 VALIDACION_FALLIDA con detalle del campo")
        void flujoError_PlacaVacia() throws Exception {
            mockMvc.perform(post(BASE_URL + "/verificar")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json("", "2030-01-01T08:00:00")))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("VALIDACION_FALLIDA"))
                    .andExpect(jsonPath("$.errores.placa").isNotEmpty());
        }

        @Test
        @DisplayName("FechaHora nula -> 400 VALIDACION_FALLIDA con detalle del campo")
        void flujoError_FechaNula() throws Exception {
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
    }

    // =========================================================================
    // GET /reglas - Integracion
    // =========================================================================

    @Nested
    @DisplayName("GET /reglas - Estructura completa")
    class ReglasFlujo {

        @Test
        @DisplayName("Retorna las 5 restricciones diarias correctas")
        void reglas_Contiene5Dias() throws Exception {
            mockMvc.perform(get(BASE_URL + "/reglas"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.restriccionesPorDia", hasSize(5)))
                    .andExpect(jsonPath("$.restriccionesPorDia[*].dia",
                            containsInAnyOrder("LUNES", "MARTES", "MIERCOLES", "JUEVES", "VIERNES")));
        }

        @Test
        @DisplayName("Retorna exactamente 2 franjas horarias")
        void reglas_Contiene2Franjas() throws Exception {
            mockMvc.perform(get(BASE_URL + "/reglas"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.horarios", hasSize(2)));
        }

        @Test
        @DisplayName("Franja manana tiene horario 06:00 - 09:30")
        void reglas_FranjaMananaCorrecta() throws Exception {
            mockMvc.perform(get(BASE_URL + "/reglas"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.horarios[?(@.franja=='Manana')].inicio").value("06:00"))
                    .andExpect(jsonPath("$.horarios[?(@.franja=='Manana')].fin").value("09:30"));
        }

        @Test
        @DisplayName("Franja tarde tiene horario 16:00 - 20:00")
        void reglas_FranjaTardeCorrecta() throws Exception {
            mockMvc.perform(get(BASE_URL + "/reglas"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.horarios[?(@.franja=='Tarde')].inicio").value("16:00"))
                    .andExpect(jsonPath("$.horarios[?(@.franja=='Tarde')].fin").value("20:00"));
        }

        @Test
        @DisplayName("Fuente normativa referencia la resolucion AMT")
        void reglas_FuenteNormativaCorrecta() throws Exception {
            mockMvc.perform(get(BASE_URL + "/reglas"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.fuenteNormativa").value(containsString("AQ-013-2023")));
        }
    }

    // =========================================================================
    // CORS y cabeceras
    // =========================================================================

    @Nested
    @DisplayName("Cabeceras HTTP y CORS")
    class Cabeceras {

        @Test
        @DisplayName("Preflight CORS OPTIONS /verificar -> responde 200")
        void cors_PreflightVerificar_Responde200() throws Exception {
            mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                            .options(BASE_URL + "/verificar")
                            .header("Origin", "http://localhost:4200")
                            .header("Access-Control-Request-Method", "POST")
                            .header("Access-Control-Request-Headers", "Content-Type"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Content-Type de respuesta es application/json")
        void respuesta_ContentTypeJson() throws Exception {
            mockMvc.perform(get(BASE_URL + "/reglas"))
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
        }
    }

    // =========================================================================
    // Helper
    // =========================================================================

    private String json(String placa, String fechaHora) throws Exception {
        ConsultaRequest req = new ConsultaRequest();
        req.setPlaca(placa);
        if (fechaHora != null) {
            req.setFechaHora(LocalDateTime.parse(fechaHora));
        }
        return objectMapper.writeValueAsString(req);
    }
}
