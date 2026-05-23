package com.pokemon.tcg.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Pokemon TCG API")
                        .version("1.0.0")
                        .description("Documentación de la API de backend para el juego Pokemon TCG (TPI P3)"));
    }
}
