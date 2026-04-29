package com.petgym.controller;

import com.petgym.dto.MembershipTypeDto;
import com.petgym.dto.UserDto;
import com.petgym.dto.report.RevenueReportDto;
import com.petgym.service.MembershipService;
import com.petgym.service.ReportService;
import com.petgym.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/admin") // все эндпоинты администратора начинаются с /api/admin
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')") // только администраторы (бухгалтерия)
@Tag(name = "Admin", description = "API для бухгалтерии/администратора")
@SecurityRequirement(name = "bearerAuth")
public class AdminController {

    private final MembershipService membershipService;
    private final ReportService reportService;
    private final UserService userService;

    // GET /api/admin/membership-types — все виды абонементов (включая деактивированные)
    @GetMapping("/membership-types")
    @Operation(summary = "Все типы абонементов")
    public ResponseEntity<List<MembershipTypeDto>> getAllTypes() {
        return ResponseEntity.ok(membershipService.getAllTypes());
    }

    // POST /api/admin/membership-types — создать новый вид абонемента
    @PostMapping("/membership-types")
    @Operation(summary = "Создать тип абонемента")
    public ResponseEntity<MembershipTypeDto> createType(@Valid @RequestBody MembershipTypeDto dto) {
        return ResponseEntity.ok(membershipService.createType(dto));
    }

    // PUT /api/admin/membership-types/{id} — обновить вид абонемента
    @PutMapping("/membership-types/{id}")
    @Operation(summary = "Обновить тип абонемента")
    public ResponseEntity<MembershipTypeDto> updateType(@PathVariable Long id,
                                                        @Valid @RequestBody MembershipTypeDto dto) {
        return ResponseEntity.ok(membershipService.updateType(id, dto));
    }

    // DELETE /api/admin/membership-types/{id} — деактивировать вид (мягкое удаление)
    @DeleteMapping("/membership-types/{id}")
    @Operation(summary = "Деактивировать тип абонемента")
    public ResponseEntity<Void> deleteType(@PathVariable Long id) {
        membershipService.deleteType(id);
        return ResponseEntity.noContent().build(); // 204 No Content
    }

    // GET /api/admin/reports/revenue?from=2025-01-01&to=2025-12-31 — финансовый отчёт
    @GetMapping("/reports/revenue")
    @Operation(summary = "Финансовый отчёт за период")
    public ResponseEntity<RevenueReportDto> getRevenue(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from, // парсим YYYY-MM-DD из параметра
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(reportService.getRevenueReport(from, to));
    }

    // GET /api/admin/users — список всех сотрудников
    @GetMapping("/users")
    @Operation(summary = "Все сотрудники")
    public ResponseEntity<List<UserDto>> getAllStaff() {
        return ResponseEntity.ok(userService.getAllStaff());
    }

    // POST /api/admin/users — создать сотрудника (тренер или ресепшен)
    @PostMapping("/users")
    @Operation(summary = "Создать сотрудника (тренер/ресепшен)")
    public ResponseEntity<UserDto> createStaff(@RequestBody CreateStaffRequest request) {
        UserDto dto = UserDto.builder()
                .email(request.getEmail())
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .phone(request.getPhone())
                .role(request.getRole()) // роль задаёт администратор
                .build();
        return ResponseEntity.ok(userService.createStaff(dto, request.getPassword(),
                request.getSpecialization(), request.getBio()));
    }

    // Вложенный класс для тела запроса создания сотрудника
    @Data
    static class CreateStaffRequest {
        private String email;
        private String password;
        private String firstName;
        private String lastName;
        private String phone;
        private com.petgym.domain.Role role; // TRAINER или RECEPTION
        private String specialization; // только для тренеров
        private String bio;            // только для тренеров
    }
}
