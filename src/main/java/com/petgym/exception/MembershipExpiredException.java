package com.petgym.exception;

// Исключение: у клиента нет активного абонемента
// Бросается при попытке записаться на тренировку или отметить посещение
// GlobalExceptionHandler вернёт HTTP 403 (Forbidden)
public class MembershipExpiredException extends RuntimeException {
    public MembershipExpiredException(String message) {
        super(message);
    }
}
