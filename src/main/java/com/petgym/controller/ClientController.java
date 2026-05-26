package com.petgym.controller;

import com.petgym.dto.*;
import com.petgym.exception.ResourceNotFoundException;
import com.petgym.service.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
    private final PaymentService paymentService;
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

    // POST /api/client/memberships/{typeId}/purchases — купить абонемент без оплаты (офлайн/ресепшен) → 201 Created
    @PostMapping("/memberships/{typeId}/purchases")
    @Operation(summary = "Купить абонемент (без онлайн-оплаты)")
    public ResponseEntity<PurchaseDto> buyMembership(@PathVariable Long typeId,
                                                     @AuthenticationPrincipal UserDetails user) {
        Long clientId = getCurrentUserId(user);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(membershipService.buyMembership(clientId, typeId, LocalDate.now()));
    }

    // POST /api/client/memberships/{typeId}/pay — оплатить абонемент через Alfa Bank
    // Возвращает URL страницы оплаты банка, на который нужно перенаправить браузер
    @PostMapping("/memberships/{typeId}/pay")
    @Operation(summary = "Оплатить абонемент через Alfa Bank (возвращает URL страницы оплаты)")
    public ResponseEntity<java.util.Map<String, String>> payMembership(
            @PathVariable Long typeId,
            @AuthenticationPrincipal UserDetails user) {
        Long clientId = getCurrentUserId(user);
        String formUrl = paymentService.initiatePayment(clientId, typeId);
        return ResponseEntity.ok(java.util.Map.of("formUrl", formUrl));
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

    // POST /api/client/bookings — создать бронирование → 201 Created
    @PostMapping("/bookings")
    @Operation(summary = "Создать бронирование")
    public ResponseEntity<BookingDto> createBooking(@Valid @RequestBody CreateBookingRequest request,
                                                    @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(bookingService.createBooking(getCurrentUserId(user), request));
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
    // 404 если программа не назначена (а не 204 — ресурс не существует, а не "пустой ответ")
    @GetMapping("/workout-program")
    @Operation(summary = "Моя программа тренировок")
    public ResponseEntity<WorkoutProgramDto> getMyProgram(@AuthenticationPrincipal UserDetails user) {
        WorkoutProgramDto program = workoutService.getClientProgram(getCurrentUserId(user));
        if (program == null) throw new ResourceNotFoundException("Программа тренировок не назначена");
        return ResponseEntity.ok(program);
    }

    // GET /api/client/notifications — мои уведомления
    @GetMapping("/notifications")
    @Operation(summary = "Мои уведомления")
    public ResponseEntity<List<NotificationDto>> getNotifications(@AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(notificationService.getMyNotifications(getCurrentUserId(user)));
    }

    // PATCH /api/client/notifications — пометить все уведомления как прочитанные
    // PATCH вместо POST/read — частичное обновление ресурса, URL без глагола
    @PatchMapping("/notifications")
    @Operation(summary = "Пометить все уведомления как прочитанные")
    public ResponseEntity<Void> markRead(@AuthenticationPrincipal UserDetails user) {
        notificationService.markAllRead(getCurrentUserId(user));
        return ResponseEntity.ok().build();
    }
}
