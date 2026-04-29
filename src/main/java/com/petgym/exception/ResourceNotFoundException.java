package com.petgym.exception;

// Исключение: запрашиваемый ресурс не найден в БД
// Бросается когда findById() возвращает пустой Optional
// GlobalExceptionHandler перехватит его и вернёт HTTP 404
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) { // конструктор с произвольным сообщением
        super(message);
    }
    public ResourceNotFoundException(String resource, Long id) { // конструктор: "MembershipType not found with id: 5"
        super(resource + " not found with id: " + id);
    }
}
