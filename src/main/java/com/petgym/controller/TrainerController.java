package com.petgym.controller;

import com.petgym.dto.*;
import com.petgym.service.BookingService;
import com.petgym.service.NotificationService;
import com.petgym.service.UserService;
import com.petgym.service.WorkoutService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/trainer") // все эндпоинты тренера начинаются с /api/trainer
@RequiredArgsConstructor
@PreAuthorize("hasRole('TRAINER')") // только тренеры
@Tag(name = "Trainer", description = "API для тренеров")
@SecurityRequirement(name = "bearerAuth")
public class TrainerController {

    private final BookingService bookingService;
    private final WorkoutService workoutService;
    private final UserService userService;
    private final NotificationService notificationService;
    private final com.petgym.repository.UserRepository userRepository;
    private final com.petgym.repository.TrainingBookingRepository bookingRepository;

    private Long getCurrentUserId(UserDetails userDetails) {
        return userRepository.findByEmail(userDetails.getUsername()).orElseThrow().getId();
    }

    // GET /api/trainer/bookings — предстоящие тренировки тренера
    @GetMapping("/bookings")
    @Operation(summary = "Мои предстоящие тренировки")
    public ResponseEntity<List<BookingDto>> getMyBookings(@AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(bookingService.getTrainerUpcomingBookings(getCurrentUserId(user)));
    }

    // PUT /api/trainer/bookings/{bookingId}/confirm — подтвердить бронирование
    @PutMapping("/bookings/{bookingId}/confirm")
    @Operation(summary = "Подтвердить бронирование")
    public ResponseEntity<BookingDto> confirm(@PathVariable Long bookingId,
                                              @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(bookingService.confirmByTrainer(bookingId, getCurrentUserId(user)));
    }

    // PUT /api/trainer/bookings/{bookingId}/cancel — отменить бронирование с указанием причины
    @PutMapping("/bookings/{bookingId}/cancel")
    @Operation(summary = "Отменить бронирование")
    public ResponseEntity<Void> cancel(@PathVariable Long bookingId,
                                       @RequestBody CancelRequest request, // причина отмены в теле запроса
                                       @AuthenticationPrincipal UserDetails user) {
        bookingService.cancelByTrainer(bookingId, getCurrentUserId(user), request.getReason());
        return ResponseEntity.noContent().build(); // 204 No Content
    }

    // GET /api/trainer/clients — мои клиенты (все кто когда-либо бронировал у меня)
    @GetMapping("/clients")
    @Operation(summary = "Мои клиенты (кто бронировал тренировки)")
    public ResponseEntity<List<ClientDto>> getMyClients(@AuthenticationPrincipal UserDetails user) {
        Long trainerId = getCurrentUserId(user);
        // запрос возвращает объекты User, а не Client — конвертируем вручную
        List<com.petgym.domain.User> clients = bookingRepository.findClientsByTrainerId(trainerId);
        List<ClientDto> result = clients.stream()
                .map(c -> ClientDto.builder()
                        .id(c.getId())
                        .email(c.getEmail())
                        .firstName(c.getFirstName())
                        .lastName(c.getLastName())
                        .phone(c.getPhone())
                        .build())
                .toList(); // List.of из стрима (Java 16+)
        return ResponseEntity.ok(result);
    }

    // GET /api/trainer/clients/{clientId}/workout-program — посмотреть программу клиента
    @GetMapping("/clients/{clientId}/workout-program")
    @Operation(summary = "Программа тренировок клиента")
    public ResponseEntity<WorkoutProgramDto> getClientProgram(@PathVariable Long clientId) {
        WorkoutProgramDto program = workoutService.getClientProgram(clientId);
        if (program == null) return ResponseEntity.noContent().build(); // 204 если программы нет
        return ResponseEntity.ok(program);
    }

    // POST /api/trainer/clients/{clientId}/workout-program — создать программу для клиента
    @PostMapping("/clients/{clientId}/workout-program")
    @Operation(summary = "Создать программу для клиента")
    public ResponseEntity<WorkoutProgramDto> createProgram(@PathVariable Long clientId,
                                                           @Valid @RequestBody WorkoutProgramDto dto,
                                                           @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(workoutService.createProgram(getCurrentUserId(user), clientId, dto));
    }

    // PUT /api/trainer/clients/{clientId}/workout-program/{programId} — обновить программу
    @PutMapping("/clients/{clientId}/workout-program/{programId}")
    @Operation(summary = "Обновить программу клиента")
    public ResponseEntity<WorkoutProgramDto> updateProgram(@PathVariable Long clientId,
                                                           @PathVariable Long programId,
                                                           @Valid @RequestBody WorkoutProgramDto dto,
                                                           @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(workoutService.updateProgram(programId, getCurrentUserId(user), dto));
    }

    // GET /api/trainer/programs — все программы, созданные мной
    @GetMapping("/programs")
    @Operation(summary = "Все программы созданные мной")
    public ResponseEntity<List<WorkoutProgramDto>> getMyPrograms(@AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(workoutService.getTrainerPrograms(getCurrentUserId(user)));
    }

    // GET /api/trainer/notifications — уведомления тренера
    @GetMapping("/notifications")
    @Operation(summary = "Уведомления тренера")
    public ResponseEntity<List<NotificationDto>> getNotifications(@AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(notificationService.getMyNotifications(getCurrentUserId(user)));
    }

    // POST /api/trainer/notifications/read — пометить уведомления прочитанными
    @PostMapping("/notifications/read")
    public ResponseEntity<Void> markRead(@AuthenticationPrincipal UserDetails user) {
        notificationService.markAllRead(getCurrentUserId(user));
        return ResponseEntity.ok().build();
    }

    // Вложенный класс для тела запроса отмены тренировки
    @Data
    static class CancelRequest {
        private String reason; // причина отмены (обязательна при отмене тренером)
    }
}
