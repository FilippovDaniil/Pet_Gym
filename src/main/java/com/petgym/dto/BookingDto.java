package com.petgym.dto;

import com.petgym.domain.BookingStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class BookingDto {
    private Long id;
    private Long clientId;
    private String clientName;
    private String clientEmail;
    private Long trainerId;
    private String trainerName;
    private LocalDateTime startDateTime;
    private LocalDateTime endDateTime;
    private BookingStatus status;
    private String cancellationReason;
    private LocalDateTime createdAt;
}
