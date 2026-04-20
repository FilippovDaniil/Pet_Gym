package com.petgym.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CreateBookingRequest {

    @NotNull
    private Long trainerId;

    @NotNull
    @Future
    private LocalDateTime startDateTime;
}
