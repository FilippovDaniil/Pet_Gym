package com.petgym.repository;

import com.petgym.domain.WorkoutExercise;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WorkoutExerciseRepository extends JpaRepository<WorkoutExercise, Long> {
    List<WorkoutExercise> findByProgramIdOrderByDayNumberAscOrderIndexAsc(Long programId);
    void deleteByProgramId(Long programId);
}
