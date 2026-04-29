package com.petgym.dto;

import jakarta.validation.constraints.Future;   // валидация: дата должна быть в будущем
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

// DTO запроса на создание бронирования — то, что клиент отправляет в теле POST-запроса
@Data
public class CreateBookingRequest {

    @NotNull
    private Long trainerId; // к какому тренеру записываемся

    @NotNull
    @Future // Spring проверит, что дата > сейчас (дополнительно к проверке в сервисе)
    private LocalDateTime startDateTime; // желаемое время начала тренировки
    // время окончания не передаём — сервис сам добавит 1 час
}
