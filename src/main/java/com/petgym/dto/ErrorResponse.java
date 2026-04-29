package com.petgym.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

// DTO ответа при ошибке — унифицированный формат всех ошибок API
// Клиент всегда получает JSON с одинаковой структурой, независимо от типа ошибки
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponse {
    private int status;          // HTTP-код ошибки (404, 409, 500 и т.д.)
    private String error;        // текстовое название статуса ("Not Found", "Conflict")
    private String message;      // понятное описание ошибки на русском
    private String path;         // URL, на который пришёл запрос (для отладки)
    private LocalDateTime timestamp; // когда произошла ошибка
}
