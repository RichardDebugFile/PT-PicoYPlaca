package com.picoyplaca.controller;

import com.picoyplaca.dto.HealthResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1/health")
@Tag(name = "Health", description = "Endpoint de estado y diagnostico del servicio")
public class HealthController {

    @Value("${spring.application.name:pico-y-placa-api}")
    private String applicationName;

    @Value("${app.version:1.0.0}")
    private String appVersion;

    @GetMapping
    @Operation(
        summary = "Health check",
        description = "Retorna el estado del servicio, nombre de la aplicacion, version y timestamp actual."
    )
    public ResponseEntity<HealthResponse> health() {
        HealthResponse response = HealthResponse.builder()
                .status("UP")
                .application(applicationName)
                .version(appVersion)
                .timestamp(LocalDateTime.now())
                .build();
        return ResponseEntity.ok(response);
    }
}
