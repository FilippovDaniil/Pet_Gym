package com.petgym.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class WorkoutProgramDto {
    private Long id;
    private Long clientId;
    private String clientName;
    private Long trainerId;
    private String trainerName;

    @NotBlank
    private String name;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<WorkoutExerciseDto> exercises;
}
