package com.picoyplaca.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI picoYPlacaOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("API Pico y Placa - Quito, Ecuador")
                        .description("API REST para verificar si un vehiculo puede circular segun " +
                                     "la normativa de Pico y Placa vigente en el Distrito Metropolitano de Quito. " +
                                     "Base legal: Resolucion AQ-013-2023 (AMT Quito).")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("PT-PicoYPlaca")
                                .email("soporte@picoyplaca.ec"))
                        .license(new License()
                                .name("MIT")
                                .url("https://opensource.org/licenses/MIT")));
    }
}
