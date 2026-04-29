package com.petgym.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

// DTO записи о посещении клуба
@Data
@Builder
public class VisitDto {
    private Long id;
    private Long clientId;
    private String clientName;    // ФИО клиента
    private String clientEmail;
    private LocalDate visitDate;  // дата посещения
    private String markedByName;  // кто отметил (имя сотрудника ресепшен)
    private LocalDateTime markedAt; // точное время отметки
}
