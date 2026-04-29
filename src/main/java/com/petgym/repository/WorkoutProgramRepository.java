package com.petgym.repository;

import com.petgym.domain.WorkoutProgram;
import org.springframework.data.jpa.repository.EntityGraph; // аннотация для явной загрузки связанных сущностей
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WorkoutProgramRepository extends JpaRepository<WorkoutProgram, Long> {

    // @EntityGraph загружает указанные связи жадно (EAGER) прямо в одном запросе
    // "exercises" — поле из WorkoutProgram, содержащее список упражнений
    // Без этого Hibernate загружал бы exercises отдельным запросом при первом обращении (N+1 проблема)
    @EntityGraph(attributePaths = {"exercises"})
    Optional<WorkoutProgram> findByClientId(Long clientId); // найти программу клиента (у клиента одна программа)

    @EntityGraph(attributePaths = {"exercises", "client"}) // грузим упражнения И данные клиента сразу
    List<WorkoutProgram> findByTrainerId(Long trainerId); // все программы, созданные конкретным тренером
}
