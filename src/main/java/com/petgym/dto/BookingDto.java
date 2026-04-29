package com.petgym.dto;

import com.petgym.domain.BookingStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

// DTO бронирования тренировки — ответ на запрос о брони
@Data
@Builder
public class BookingDto {
    private Long id;
    private Long clientId;
    private String clientName;   // имя клиента
    private String clientEmail;
    private Long trainerId;
    private String trainerName;  // имя тренера
    private LocalDateTime startDateTime; // начало тренировки
    private LocalDateTime endDateTime;   // конец тренировки
    private BookingStatus status;        // текущий статус
    private String cancellationReason;   // причина отмены (если отменено)
    private LocalDateTime createdAt;     // когда создано бронирование
}
