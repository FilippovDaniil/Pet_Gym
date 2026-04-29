package com.petgym.exception;

// Исключение: конфликт при бронировании тренировки
// Бросается когда тренер уже занят в запрошенное время, или у клиента уже 2 активных брони
// GlobalExceptionHandler вернёт HTTP 409 (Conflict)
public class BookingConflictException extends RuntimeException {
    public BookingConflictException(String message) {
        super(message);
    }
}
