package com.petgym.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

// DTO уведомления — что видит пользователь в своём колокольчике
@Data
@Builder
public class NotificationDto {
    private Long id;
    private String message;          // текст уведомления
    private boolean read;            // прочитано ли
    private LocalDateTime createdAt; // когда создано
}
