package com.picoyplaca.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class HealthResponse {

    private String status;
    private String application;
    private String version;
    private LocalDateTime timestamp;
}
