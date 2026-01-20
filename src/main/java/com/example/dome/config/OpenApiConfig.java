package com.example.dome.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI domeOpenAPI() {
        return new OpenAPI()
                .info(new Info().title("Distributed Order Matching Engine API")
                .description("High-performance Matching Engine REST API")
                .version("v0.0.1"));
    }
}
