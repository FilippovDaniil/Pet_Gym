package com.petgym.repository;

import com.petgym.domain.WorkoutProgram;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WorkoutProgramRepository extends JpaRepository<WorkoutProgram, Long> {

    @EntityGraph(attributePaths = {"exercises"})
    Optional<WorkoutProgram> findByClientId(Long clientId);

    @EntityGraph(attributePaths = {"exercises", "client"})
    List<WorkoutProgram> findByTrainerId(Long trainerId);
}
