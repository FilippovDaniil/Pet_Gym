package com.petgym.controller;

import com.petgym.dto.*;
import com.petgym.service.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement; // в Swagger UI показывает, что нужен JWT
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat; // парсинг даты из строки запроса
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize; // проверка роли перед выполнением метода
import org.springframework.security.core.annotation.AuthenticationPrincipal; // извлекаем текущего пользователя из контекста безопасности
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/client") // все эндпоинты клиента начинаются с /api/client
@RequiredArgsConstructor
@PreAuthorize("hasRole('CLIENT')") // все методы этого контроллера доступны только пользователям с ролью CLIENT
@Tag(name = "Client", description = "API для клиентов")
@SecurityRequirement(name = "bearerAuth") // Swagger: нужен Bearer JWT токен
public class ClientController {

    private final MembershipService membershipService;
    private final BookingService bookingService;
    private final WorkoutService workoutService;
    private final UserService userService;
    private final NotificationService notificationService;
    private final com.petgym.repository.UserRepository userRepository;

    // Вспомогательный метод: извлекаем id текущего пользователя из JWT-контекста
    // UserDetails содержит email (username), по которому ищем id в БД
    private Long getCurrentUserId(UserDetails userDetails) {
        return userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow().getId();
    }

    // GET /api/client/memberships/types — список активных типов абонементов
    @GetMapping("/memberships/types")
    @Operation(summary = "Все активные типы абонементов")
    public ResponseEntity<List<MembershipTypeDto>> getMembershipTypes() {
        return ResponseEntity.ok(membershipService.getAllActiveTypes());
    }

    // GET /api/client/memberships/active — мои абонементы
    @GetMapping("/memberships/active")
    @Operation(summary = "Мои абонементы")
    public ResponseEntity<List<PurchaseDto>> getMyMemberships(@AuthenticationPrincipal UserDetails user) {
        // @AuthenticationPrincipal — Spring сам подставляет текущего авторизованного пользователя
        return ResponseEntity.ok(membershipService.getClientPurchases(getCurrentUserId(user)));
    }

    // POST /api/client/memberships/buy/{typeId} — купить абонемент
    @PostMapping("/memberships/buy/{typeId}")
    @Operation(summary = "Купить абонемент")
    public ResponseEntity<PurchaseDto> buyMembership(@PathVariable Long typeId, // {typeId} из URL
                                                     @AuthenticationPrincipal UserDetails user) {
        Long clientId = getCurrentUserId(user);
        return ResponseEntity.ok(membershipService.buyMembership(clientId, typeId, LocalDate.now()));
    }

    // GET /api/client/trainers — список всех тренеров
    @GetMapping("/trainers")
    @Operation(summary = "Список всех тренеров")
    public ResponseEntity<List<TrainerDto>> getTrainers() {
        return ResponseEntity.ok(userService.getAllTrainers());
    }

    // GET /api/client/trainers/{trainerId}/slots?date=2025-05-01 — свободные слоты тренера на дату
    @GetMapping("/trainers/{trainerId}/slots")
    @Operation(summary = "Свободные слоты тренера на дату")
    public ResponseEntity<List<LocalDateTime>> getSlots(
            @PathVariable Long trainerId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) { // @RequestParam — параметр из URL (?date=...)
        return ResponseEntity.ok(bookingService.getFreeSlots(trainerId, date));
    }

    // POST /api/client/bookings — создать бронирование
    @PostMapping("/bookings")
    @Operation(summary = "Создать бронирование")
    public ResponseEntity<BookingDto> createBooking(@Valid @RequestBody CreateBookingRequest request,
                                                    @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(bookingService.createBooking(getCurrentUserId(user), request));
    }

    // GET /api/client/bookings — мои бронирования
    @GetMapping("/bookings")
    @Operation(summary = "Мои бронирования")
    public ResponseEntity<List<BookingDto>> getMyBookings(@AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(bookingService.getClientBookings(getCurrentUserId(user)));
    }

    // DELETE /api/client/bookings/{bookingId} — отменить бронирование
    @DeleteMapping("/bookings/{bookingId}")
    @Operation(summary = "Отменить бронирование")
    public ResponseEntity<Void> cancelBooking(@PathVariable Long bookingId,
                                              @AuthenticationPrincipal UserDetails user) {
        bookingService.cancelByClient(bookingId, getCurrentUserId(user));
        return ResponseEntity.noContent().build(); // 204 No Content (успех, тела ответа нет)
    }

    // GET /api/client/workout-program — моя программа тренировок
    @GetMapping("/workout-program")
    @Operation(summary = "Моя программа тренировок")
    public ResponseEntity<WorkoutProgramDto> getMyProgram(@AuthenticationPrincipal UserDetails user) {
        WorkoutProgramDto program = workoutService.getClientProgram(getCurrentUserId(user));
        if (program == null) return ResponseEntity.noContent().build(); // 204 если программы нет
        return ResponseEntity.ok(program);
    }

    // GET /api/client/notifications — мои уведомления
    @GetMapping("/notifications")
    @Operation(summary = "Мои уведомления")
    public ResponseEntity<List<NotificationDto>> getNotifications(@AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(notificationService.getMyNotifications(getCurrentUserId(user)));
    }

    // POST /api/client/notifications/read — пометить все уведомления прочитанными
    @PostMapping("/notifications/read")
    @Operation(summary = "Пометить все уведомления как прочитанные")
    public ResponseEntity<Void> markRead(@AuthenticationPrincipal UserDetails user) {
        notificationService.markAllRead(getCurrentUserId(user));
        return ResponseEntity.ok().build(); // 200 OK без тела
    }
}
