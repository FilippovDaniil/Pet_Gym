package com.petgym.dto;

import com.petgym.domain.Role;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

// DTO для передачи данных о пользователе (без пароля!)
// Используется в API, чтобы никогда не отдавать хэш пароля наружу
@Data
@Builder
public class UserDto {
    private Long id;
    private String email;
    private String firstName;
    private String lastName;
    private String phone;
    private Role role;            // роль пользователя
    private LocalDateTime createdAt; // дата регистрации
    private boolean enabled;      // активен ли аккаунт
}
