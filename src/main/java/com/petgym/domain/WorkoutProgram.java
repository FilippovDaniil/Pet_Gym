package com.petgym.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "workout_programs") // программа тренировок, составленная тренером для конкретного клиента
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkoutProgram {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    private User client; // для кого составлена программа

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trainer_id", nullable = false)
    private User trainer; // кто составил программу

    @Column(nullable = false)
    private String name; // название программы (например: "Программа на массу — апрель")

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // @OneToMany — один WorkoutProgram содержит много упражнений
    // mappedBy = "program" — обратная сторона: поле program в WorkoutExercise указывает на эту программу
    // cascade = ALL — при сохранении/удалении программы автоматически сохраняются/удаляются упражнения
    // orphanRemoval = true — если упражнение убрали из списка, удалить его из БД
    @OneToMany(mappedBy = "program", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("dayNumber ASC, orderIndex ASC") // при загрузке упражнения сортируются по дню и порядковому индексу
    @Builder.Default // Lombok: без этого поле списка будет null при использовании builder()
    private List<WorkoutExercise> exercises = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
    }

    @PreUpdate // вызывается перед каждым UPDATE (не только первой вставкой)
    protected void onUpdate() {
        updatedAt = LocalDateTime.now(); // обновляем время последнего изменения
    }
}
