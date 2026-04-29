package com.petgym.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// DTO-ответ на успешный вход или регистрацию
// Отправляется клиенту (браузеру/Postman) и содержит JWT-токен для дальнейших запросов
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
    private String token;     // JWT-токен — строка для авторизации в заголовке Authorization: Bearer <token>
    private String role;      // роль пользователя ("CLIENT", "TRAINER" и т.д.) — фронтенд показывает нужный интерфейс
    private Long userId;      // id пользователя в БД
    private String firstName; // имя (чтобы показать на странице "Добро пожаловать, Иван!")
    private String lastName;  // фамилия
    private String email;     // email
}
