package com.petgym.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "workout_exercises")
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
    @JoinColumn(name = "program_id", nullable = false)
    private WorkoutProgram program;

    @Column(name = "exercise_name", nullable = false)
    private String exerciseName;

    @Column(nullable = false)
    private int sets;

    @Column(nullable = false)
    private String reps;

    private String weight;

    @Column(name = "day_number", nullable = false)
    private int dayNumber;

    @Column(name = "order_index", nullable = false)
    private int orderIndex;
}
