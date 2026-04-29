package com.petgym.repository;

import com.petgym.domain.WorkoutExercise;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WorkoutExerciseRepository extends JpaRepository<WorkoutExercise, Long> {
    // получить упражнения программы, отсортированные сначала по дню, потом по порядку внутри дня
    List<WorkoutExercise> findByProgramIdOrderByDayNumberAscOrderIndexAsc(Long programId);

    // удалить все упражнения программы (используется при полной замене списка)
    void deleteByProgramId(Long programId);
}
