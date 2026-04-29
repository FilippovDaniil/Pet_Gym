package com.petgym.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// Настройка Swagger UI (OpenAPI 3.0) — автоматическая документация API
// После запуска доступна по адресу: http://localhost:8080/swagger-ui/index.html
@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Фитнес-клуб API")               // заголовок документации
                        .description("REST API для управления фитнес-клубом с бассейном") // описание
                        .version("1.0.0"))                      // версия API
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth")) // требуем JWT для защищённых эндпоинтов
                .components(new Components()
                        .addSecuritySchemes("bearerAuth", new SecurityScheme() // описываем схему авторизации
                                .name("bearerAuth")
                                .type(SecurityScheme.Type.HTTP) // HTTP-авторизация
                                .scheme("bearer")               // схема "Bearer"
                                .bearerFormat("JWT")));         // формат токена — JWT
        // Теперь в Swagger UI появится кнопка "Authorize", куда можно вставить токен
    }
}
