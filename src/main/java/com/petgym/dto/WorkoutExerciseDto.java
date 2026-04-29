package com.petgym.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

// DTO одного упражнения внутри программы тренировок
@Data
@Builder
public class WorkoutExerciseDto {
    private Long id; // null при создании нового упражнения

    @NotBlank
    private String exerciseName; // название упражнения

    @NotNull
    @Min(1) // минимум 1 подход
    private Integer sets; // подходы

    @NotBlank
    private String reps; // повторения (строка: "10", "8-12", "до отказа")

    private String weight; // вес (необязательно: "60 кг", "собственный вес")

    @Min(1) // номер дня тренировочной недели (минимум 1)
    private int dayNumber;

    private int orderIndex; // порядковый номер упражнения в рамках дня
}
