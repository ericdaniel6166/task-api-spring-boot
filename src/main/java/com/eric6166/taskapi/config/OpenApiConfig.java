package com.eric6166.taskapi.config;

import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@SecurityScheme(
        name = "BearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT"
)
public class OpenApiConfig {

    @Bean
    public OpenAPI taskApiOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Task API")
                        .description("REST API for task management with JWT authentication")
                        .version("v1.0.0"));
    }
}
