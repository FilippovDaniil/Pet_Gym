package com.petgym.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

// DTO программы тренировок — содержит сведения о программе вместе со списком упражнений
@Data
@Builder
public class WorkoutProgramDto {
    private Long id;
    private Long clientId;
    private String clientName;   // имя клиента
    private Long trainerId;
    private String trainerName;  // имя тренера-автора

    @NotBlank
    private String name;         // название программы

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<WorkoutExerciseDto> exercises; // список упражнений (вложенные DTO)
}
