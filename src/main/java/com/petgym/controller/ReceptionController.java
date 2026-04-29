package com.petgym.controller;

import com.petgym.domain.Role;
import com.petgym.dto.*;
import com.petgym.service.MembershipService;
import com.petgym.service.UserService;
import com.petgym.service.VisitService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/reception") // все эндпоинты ресепшена начинаются с /api/reception
@RequiredArgsConstructor
@PreAuthorize("hasRole('RECEPTION')") // только сотрудники ресепшена
@Tag(name = "Reception", description = "API для ресепшен")
@SecurityRequirement(name = "bearerAuth")
public class ReceptionController {

    private final UserService userService;
    private final MembershipService membershipService;
    private final VisitService visitService;
    private final com.petgym.repository.UserRepository userRepository;

    private Long getCurrentUserId(UserDetails userDetails) {
        return userRepository.findByEmail(userDetails.getUsername()).orElseThrow().getId();
    }

    // GET /api/reception/clients?query=... — поиск клиентов по email/телефону
    @GetMapping("/clients")
    @Operation(summary = "Поиск клиентов по email/телефону")
    public ResponseEntity<List<ClientDto>> searchClients(
            @RequestParam(required = false, defaultValue = "") String query) { // query необязателен, по умолчанию ""
        if (query.isBlank()) return ResponseEntity.ok(userService.getAllClients()); // если пусто — возвращаем всех
        return ResponseEntity.ok(userService.searchClients(query));
    }

    // POST /api/reception/clients — создать нового клиента (ресепшен регистрирует у стойки)
    @PostMapping("/clients")
    @Operation(summary = "Создать нового клиента")
    public ResponseEntity<ClientDto> createClient(@RequestBody CreateClientRequest request) {
        // собираем UserDto из данных запроса
        UserDto dto = UserDto.builder()
                .email(request.getEmail())
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .phone(request.getPhone())
                .role(Role.CLIENT)
                .build();
        return ResponseEntity.ok(userService.createClient(dto, request.getPassword(), request.getBirthDate()));
    }

    // POST /api/reception/memberships — оформить абонемент клиенту
    @PostMapping("/memberships")
    @Operation(summary = "Оформить абонемент клиенту")
    public ResponseEntity<PurchaseDto> createMembership(@RequestBody CreateMembershipRequest request) {
        // если дата начала не указана — берём сегодня
        LocalDate startDate = request.getStartDate() != null ? request.getStartDate() : LocalDate.now();
        return ResponseEntity.ok(membershipService.buyMembership(request.getClientId(), request.getTypeId(), startDate));
    }

    // GET /api/reception/memberships/active — все действующие абонементы
    @GetMapping("/memberships/active")
    @Operation(summary = "Все активные абонементы")
    public ResponseEntity<List<PurchaseDto>> getActiveMemberships() {
        return ResponseEntity.ok(membershipService.getActiveMemberships());
    }

    // POST /api/reception/visits — отметить посещение клиента
    @PostMapping("/visits")
    @Operation(summary = "Отметить посещение клиента")
    public ResponseEntity<VisitDto> markVisit(@RequestBody MarkVisitRequest request,
                                              @AuthenticationPrincipal UserDetails user) {
        // markedByUserId = id текущего сотрудника ресепшена (кто нажал кнопку)
        return ResponseEntity.ok(visitService.markVisit(request.getClientId(), getCurrentUserId(user)));
    }

    // GET /api/reception/visits/today — все посещения за сегодня
    @GetMapping("/visits/today")
    @Operation(summary = "Посещения сегодня")
    public ResponseEntity<List<VisitDto>> getTodayVisits() {
        return ResponseEntity.ok(visitService.getTodayVisits());
    }

    // Вложенные статические классы для тел входящих запросов
    // Используем @Data вместо отдельных DTO-файлов (удобно для простых локальных объектов)

    @Data
    static class CreateClientRequest { // тело запроса на создание клиента
        private String email;
        private String firstName;
        private String lastName;
        private String phone;
        private String password;
        private LocalDate birthDate;
    }

    @Data
    static class CreateMembershipRequest { // тело запроса на оформление абонемента
        private Long clientId;  // кому
        private Long typeId;    // какой вид
        private LocalDate startDate; // с какой даты (может быть null → сегодня)
    }

    @Data
    static class MarkVisitRequest { // тело запроса на отметку посещения
        private Long clientId; // кого отмечаем
    }
}
