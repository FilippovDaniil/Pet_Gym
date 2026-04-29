package com.petgym.dto.auth;

import jakarta.validation.constraints.Email;     // валидация: строка должна быть корректным email
import jakarta.validation.constraints.NotBlank;  // валидация: строка не должна быть null, пустой или состоять из пробелов
import jakarta.validation.constraints.Size;      // валидация: ограничение длины строки
import lombok.Data; // Lombok: генерирует getters, setters, equals, hashCode, toString

import java.time.LocalDate;

// DTO (Data Transfer Object) — объект для передачи данных из HTTP-запроса в сервис
// Используется при регистрации нового клиента
@Data
public class RegisterRequest {

    @NotBlank // обязательное поле
    @Email    // должен быть валидный формат email (contains @)
    private String email;

    @NotBlank
    @Size(min = 6, max = 100) // пароль от 6 до 100 символов
    private String password;

    @NotBlank
    private String firstName; // имя

    @NotBlank
    private String lastName; // фамилия

    private String phone; // телефон необязателен

    private LocalDate birthDate; // дата рождения необязательна
}
