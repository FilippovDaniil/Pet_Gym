package com.petgym.exception;

import com.petgym.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;      // нет прав на эндпоинт
import org.springframework.security.authentication.BadCredentialsException; // неверный логин/пароль
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException; // ошибка валидации @Valid
import org.springframework.web.bind.annotation.ExceptionHandler;     // перехват конкретного исключения
import org.springframework.web.bind.annotation.RestControllerAdvice; // применяется ко всем контроллерам

import java.time.LocalDateTime;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice // "советник" для всех @RestController: перехватывает исключения и возвращает JSON
public class GlobalExceptionHandler {

    // Ресурс не найден → 404 Not Found
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
        log.warn("[WARN] event=NOT_FOUND url={} message=\"{}\"", request.getRequestURI(), ex.getMessage());
        return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage(), request.getRequestURI());
    }

    // Нет активного абонемента → 403 Forbidden
    @ExceptionHandler(MembershipExpiredException.class)
    public ResponseEntity<ErrorResponse> handleMembershipExpired(MembershipExpiredException ex, HttpServletRequest request) {
        log.warn("[WARN] event=MEMBERSHIP_EXPIRED url={} message=\"{}\"", request.getRequestURI(), ex.getMessage());
        return buildResponse(HttpStatus.FORBIDDEN, ex.getMessage(), request.getRequestURI());
    }

    // Конфликт при бронировании → 409 Conflict
    @ExceptionHandler(BookingConflictException.class)
    public ResponseEntity<ErrorResponse> handleBookingConflict(BookingConflictException ex, HttpServletRequest request) {
        log.warn("[WARN] event=BOOKING_CONFLICT url={} message=\"{}\"", request.getRequestURI(), ex.getMessage());
        return buildResponse(HttpStatus.CONFLICT, ex.getMessage(), request.getRequestURI());
    }

    // Недопустимая отмена → 400 Bad Request
    @ExceptionHandler(InvalidCancellationException.class)
    public ResponseEntity<ErrorResponse> handleInvalidCancellation(InvalidCancellationException ex, HttpServletRequest request) {
        log.warn("[WARN] event=INVALID_CANCELLATION url={} message=\"{}\"", request.getRequestURI(), ex.getMessage());
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), request.getRequestURI());
    }

    // Общее бизнес-нарушение → 400 Bad Request
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusiness(BusinessException ex, HttpServletRequest request) {
        log.warn("[WARN] event=BUSINESS_RULE_VIOLATION url={} message=\"{}\"", request.getRequestURI(), ex.getMessage());
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), request.getRequestURI());
    }

    // Неверный логин/пароль → 401 Unauthorized
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials(BadCredentialsException ex, HttpServletRequest request) {
        log.warn("[WARN] event=LOGIN_FAILED url={}", request.getRequestURI());
        return buildResponse(HttpStatus.UNAUTHORIZED, "Неверный email или пароль", request.getRequestURI());
    }

    // Попытка зайти на эндпоинт без нужной роли → 403 Forbidden
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        log.warn("[WARN] event=ACCESS_DENIED url={}", request.getRequestURI());
        return buildResponse(HttpStatus.FORBIDDEN, "Доступ запрещён", request.getRequestURI());
    }

    // Ошибка валидации @Valid (поля DTO не прошли проверку) → 400 Bad Request
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        log.warn("[WARN] event=VALIDATION_FAILED url={} fields=\"{}\"", request.getRequestURI(), message);
        return buildResponse(HttpStatus.BAD_REQUEST, message, request.getRequestURI());
    }

    // Все остальные непредвиденные исключения → 500 Internal Server Error
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneral(Exception ex, HttpServletRequest request) {
        log.error("[ERROR] event=UNEXPECTED_ERROR url={} exception={} message=\"{}\"",
                request.getRequestURI(), ex.getClass().getSimpleName(), ex.getMessage(), ex);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Внутренняя ошибка сервера", request.getRequestURI());
    }

    // вспомогательный метод: строим тело ответа ErrorResponse и оборачиваем в ResponseEntity
    private ResponseEntity<ErrorResponse> buildResponse(HttpStatus status, String message, String path) {
        return ResponseEntity.status(status).body(ErrorResponse.builder()
                .status(status.value())        // числовой HTTP-код
                .error(status.getReasonPhrase()) // текстовое название: "Not Found", "Conflict" и т.д.
                .message(message)              // наше сообщение
                .path(path)                    // URL запроса
                .timestamp(LocalDateTime.now()) // время ошибки
                .build());
    }
}
