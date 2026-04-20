package com.petgym.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class VisitDto {
    private Long id;
    private Long clientId;
    private String clientName;
    private String clientEmail;
    private LocalDate visitDate;
    private String markedByName;
    private LocalDateTime markedAt;
}
