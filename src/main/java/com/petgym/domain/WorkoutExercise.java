package com.petgym.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "workout_exercises") // одно упражнение внутри программы тренировок
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkoutExercise {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "program_id", nullable = false) // внешний ключ: к какой программе относится упражнение
    private WorkoutProgram program;

    @Column(name = "exercise_name", nullable = false)
    private String exerciseName; // название упражнения (например: "Жим лёжа")

    @Column(nullable = false)
    private int sets; // количество подходов

    @Column(nullable = false)
    private String reps; // количество повторений (строка, т.к. может быть "8-12" или "до отказа")

    private String weight; // рабочий вес (строка: "60 кг" или "собственный вес")

    @Column(name = "day_number", nullable = false)
    private int dayNumber; // номер дня тренировочной недели (1 = понедельник и т.д.)

    @Column(name = "order_index", nullable = false)
    private int orderIndex; // порядок упражнения внутри дня (1-е, 2-е, 3-е и т.д.)
}
