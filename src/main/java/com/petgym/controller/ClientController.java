package com.petgym.controller;

import com.petgym.dto.*;
import com.petgym.service.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/client")
@RequiredArgsConstructor
@PreAuthorize("hasRole('CLIENT')")
@Tag(name = "Client", description = "API для клиентов")
@SecurityRequirement(name = "bearerAuth")
public class ClientController {

    private final MembershipService membershipService;
    private final BookingService bookingService;
    private final WorkoutService workoutService;
    private final UserService userService;
    private final NotificationService notificationService;
    private final com.petgym.repository.UserRepository userRepository;

    private Long getCurrentUserId(UserDetails userDetails) {
        return userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow().getId();
    }

    @GetMapping("/memberships/types")
    @Operation(summary = "Все активные типы абонементов")
    public ResponseEntity<List<MembershipTypeDto>> getMembershipTypes() {
        return ResponseEntity.ok(membershipService.getAllActiveTypes());
    }

    @GetMapping("/memberships/active")
    @Operation(summary = "Мои абонементы")
    public ResponseEntity<List<PurchaseDto>> getMyMemberships(@AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(membershipService.getClientPurchases(getCurrentUserId(user)));
    }

    @PostMapping("/memberships/buy/{typeId}")
    @Operation(summary = "Купить абонемент")
    public ResponseEntity<PurchaseDto> buyMembership(@PathVariable Long typeId,
                                                     @AuthenticationPrincipal UserDetails user) {
        Long clientId = getCurrentUserId(user);
        return ResponseEntity.ok(membershipService.buyMembership(clientId, typeId, LocalDate.now()));
    }

    @GetMapping("/trainers")
    @Operation(summary = "Список всех тренеров")
    public ResponseEntity<List<TrainerDto>> getTrainers() {
        return ResponseEntity.ok(userService.getAllTrainers());
    }

    @GetMapping("/trainers/{trainerId}/slots")
    @Operation(summary = "Свободные слоты тренера на дату")
    public ResponseEntity<List<LocalDateTime>> getSlots(
            @PathVariable Long trainerId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(bookingService.getFreeSlots(trainerId, date));
    }

    @PostMapping("/bookings")
    @Operation(summary = "Создать бронирование")
    public ResponseEntity<BookingDto> createBooking(@Valid @RequestBody CreateBookingRequest request,
                                                    @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(bookingService.createBooking(getCurrentUserId(user), request));
    }

    @GetMapping("/bookings")
    @Operation(summary = "Мои бронирования")
    public ResponseEntity<List<BookingDto>> getMyBookings(@AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(bookingService.getClientBookings(getCurrentUserId(user)));
    }

    @DeleteMapping("/bookings/{bookingId}")
    @Operation(summary = "Отменить бронирование")
    public ResponseEntity<Void> cancelBooking(@PathVariable Long bookingId,
                                              @AuthenticationPrincipal UserDetails user) {
        bookingService.cancelByClient(bookingId, getCurrentUserId(user));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/workout-program")
    @Operation(summary = "Моя программа тренировок")
    public ResponseEntity<WorkoutProgramDto> getMyProgram(@AuthenticationPrincipal UserDetails user) {
        WorkoutProgramDto program = workoutService.getClientProgram(getCurrentUserId(user));
        if (program == null) return ResponseEntity.noContent().build();
        return ResponseEntity.ok(program);
    }

    @GetMapping("/notifications")
    @Operation(summary = "Мои уведомления")
    public ResponseEntity<List<NotificationDto>> getNotifications(@AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(notificationService.getMyNotifications(getCurrentUserId(user)));
    }

    @PostMapping("/notifications/read")
    @Operation(summary = "Пометить все уведомления как прочитанные")
    public ResponseEntity<Void> markRead(@AuthenticationPrincipal UserDetails user) {
        notificationService.markAllRead(getCurrentUserId(user));
        return ResponseEntity.ok().build();
    }
}
