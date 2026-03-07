package com.picoyplaca.controller;

import com.picoyplaca.dto.ConsultaRequest;
import com.picoyplaca.dto.ConsultaResponse;
import com.picoyplaca.dto.ReglasResponse;
import com.picoyplaca.service.PicoPlacaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/pico-placa")
@RequiredArgsConstructor
@Tag(name = "Pico y Placa", description = "API para verificacion de restriccion vehicular Pico y Placa - Quito, Ecuador")
public class PicoPlacaController {

    private final PicoPlacaService picoPlacaService;

    @PostMapping("/verificar")
    @Operation(
        summary = "Verificar circulacion",
        description = "Verifica si un vehiculo puede circular en la fecha y hora indicadas segun la normativa Pico y Placa de Quito."
    )
    public ResponseEntity<ConsultaResponse> verificar(@Valid @RequestBody ConsultaRequest request) {
        return ResponseEntity.ok(picoPlacaService.verificar(request));
    }

    @GetMapping("/reglas")
    @Operation(
        summary = "Obtener reglas",
        description = "Retorna las reglas de Pico y Placa configuradas: horarios de restriccion y digitos por dia."
    )
    public ResponseEntity<ReglasResponse> obtenerReglas() {
        return ResponseEntity.ok(picoPlacaService.obtenerReglas());
    }
}
