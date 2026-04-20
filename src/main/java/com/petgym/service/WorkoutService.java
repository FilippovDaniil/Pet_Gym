package com.petgym.service;

import com.petgym.domain.User;
import com.petgym.domain.WorkoutExercise;
import com.petgym.domain.WorkoutProgram;
import com.petgym.dto.WorkoutExerciseDto;
import com.petgym.dto.WorkoutProgramDto;
import com.petgym.exception.BusinessException;
import com.petgym.exception.ResourceNotFoundException;
import com.petgym.repository.UserRepository;
import com.petgym.repository.WorkoutExerciseRepository;
import com.petgym.repository.WorkoutProgramRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkoutService {

    private final WorkoutProgramRepository programRepository;
    private final WorkoutExerciseRepository exerciseRepository;
    private final UserRepository userRepository;
    private final MembershipService membershipService;

    @Transactional
    public WorkoutProgramDto createProgram(Long trainerId, Long clientId, WorkoutProgramDto dto) {
        if (!membershipService.hasActiveMembership(clientId, java.time.LocalDate.now())) {
            throw new BusinessException("У клиента нет активного абонемента");
        }
        User trainer = userRepository.findById(trainerId)
                .orElseThrow(() -> new ResourceNotFoundException("Trainer", trainerId));
        User client = userRepository.findById(clientId)
                .orElseThrow(() -> new ResourceNotFoundException("Client", clientId));

        WorkoutProgram program = WorkoutProgram.builder()
                .trainer(trainer)
                .client(client)
                .name(dto.getName())
                .build();

        if (dto.getExercises() != null) {
            List<WorkoutExercise> exercises = dto.getExercises().stream()
                    .map(e -> toExerciseEntity(e, program))
                    .collect(Collectors.toList());
            program.getExercises().addAll(exercises);
        }
        WorkoutProgram saved = programRepository.save(program);
        log.info("Workout program created by trainer {} for client {}: {}", trainerId, clientId, dto.getName());
        return toDto(saved);
    }

    @Transactional
    public WorkoutProgramDto updateProgram(Long programId, Long trainerId, WorkoutProgramDto dto) {
        WorkoutProgram program = programRepository.findById(programId)
                .orElseThrow(() -> new ResourceNotFoundException("WorkoutProgram", programId));
        if (!program.getTrainer().getId().equals(trainerId)) {
            throw new BusinessException("Вы не являетесь автором этой программы");
        }
        program.setName(dto.getName());
        program.getExercises().clear();
        if (dto.getExercises() != null) {
            dto.getExercises().forEach(e -> program.getExercises().add(toExerciseEntity(e, program)));
        }
        WorkoutProgram saved = programRepository.save(program);
        log.info("Workout program {} updated by trainer {}", programId, trainerId);
        return toDto(saved);
    }

    @Transactional(readOnly = true)
    public WorkoutProgramDto getClientProgram(Long clientId) {
        return programRepository.findByClientId(clientId)
                .map(this::toDto)
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public List<WorkoutProgramDto> getTrainerPrograms(Long trainerId) {
        return programRepository.findByTrainerId(trainerId)
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    private WorkoutExercise toExerciseEntity(WorkoutExerciseDto dto, WorkoutProgram program) {
        return WorkoutExercise.builder()
                .program(program)
                .exerciseName(dto.getExerciseName())
                .sets(dto.getSets())
                .reps(dto.getReps())
                .weight(dto.getWeight())
                .dayNumber(dto.getDayNumber() > 0 ? dto.getDayNumber() : 1)
                .orderIndex(dto.getOrderIndex())
                .build();
    }

    private WorkoutProgramDto toDto(WorkoutProgram p) {
        List<WorkoutExerciseDto> exercises = p.getExercises().stream()
                .map(e -> WorkoutExerciseDto.builder()
                        .id(e.getId())
                        .exerciseName(e.getExerciseName())
                        .sets(e.getSets())
                        .reps(e.getReps())
                        .weight(e.getWeight())
                        .dayNumber(e.getDayNumber())
                        .orderIndex(e.getOrderIndex())
                        .build())
                .collect(Collectors.toList());
        return WorkoutProgramDto.builder()
                .id(p.getId())
                .clientId(p.getClient().getId())
                .clientName(p.getClient().getFirstName() + " " + p.getClient().getLastName())
                .trainerId(p.getTrainer().getId())
                .trainerName(p.getTrainer().getFirstName() + " " + p.getTrainer().getLastName())
                .name(p.getName())
                .createdAt(p.getCreatedAt())
                .updatedAt(p.getUpdatedAt())
                .exercises(exercises)
                .build();
    }
}
