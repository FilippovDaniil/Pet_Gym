package com.petgym.exception;

// Исключение: недопустимая попытка отмены бронирования
// Бросается когда клиент пытается отменить тренировку менее чем за 2 часа до её начала,
// или когда бронирование уже отменено
// GlobalExceptionHandler вернёт HTTP 400 (Bad Request)
public class InvalidCancellationException extends RuntimeException {
    public InvalidCancellationException(String message) {
        super(message);
    }
}
