package com.petgym.exception;

// Общее бизнес-исключение для нарушений бизнес-правил, не подходящих под другие типы
// Примеры: "Email уже используется", "Это не ваше бронирование", "Тип абонемента неактивен"
// GlobalExceptionHandler вернёт HTTP 400 (Bad Request)
public class BusinessException extends RuntimeException {
    public BusinessException(String message) {
        super(message);
    }
}
