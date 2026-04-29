package com.petgym.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

// DTO клиента — расширенная версия UserDto с дополнительными полями клиента
@Data
@Builder
public class ClientDto {
    private Long id;
    private String email;
    private String firstName;
    private String lastName;
    private String phone;
    private LocalDate birthDate;      // дата рождения из таблицы clients
    private LocalDateTime createdAt;  // дата регистрации из таблицы users
    private boolean enabled;
    private boolean hasActiveMembership; // есть ли у клиента действующий абонемент (вычисляется динамически)
}
