package com.petgym.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class WorkoutExerciseDto {
    private Long id;

    @NotBlank
    private String exerciseName;

    @NotNull
    @Min(1)
    private Integer sets;

    @NotBlank
    private String reps;

    private String weight;

    @Min(1)
    private int dayNumber;

    private int orderIndex;
}
