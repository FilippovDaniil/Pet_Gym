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
@RequestMapping("/api/reception")
@RequiredArgsConstructor
@PreAuthorize("hasRole('RECEPTION')")
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

    @GetMapping("/clients")
    @Operation(summary = "Поиск клиентов по email/телефону")
    public ResponseEntity<List<ClientDto>> searchClients(@RequestParam(required = false, defaultValue = "") String query) {
        if (query.isBlank()) return ResponseEntity.ok(userService.getAllClients());
        return ResponseEntity.ok(userService.searchClients(query));
    }

    @PostMapping("/clients")
    @Operation(summary = "Создать нового клиента")
    public ResponseEntity<ClientDto> createClient(@RequestBody CreateClientRequest request) {
        UserDto dto = UserDto.builder()
                .email(request.getEmail())
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .phone(request.getPhone())
                .role(Role.CLIENT)
                .build();
        return ResponseEntity.ok(userService.createClient(dto, request.getPassword(), request.getBirthDate()));
    }

    @PostMapping("/memberships")
    @Operation(summary = "Оформить абонемент клиенту")
    public ResponseEntity<PurchaseDto> createMembership(@RequestBody CreateMembershipRequest request) {
        LocalDate startDate = request.getStartDate() != null ? request.getStartDate() : LocalDate.now();
        return ResponseEntity.ok(membershipService.buyMembership(request.getClientId(), request.getTypeId(), startDate));
    }

    @GetMapping("/memberships/active")
    @Operation(summary = "Все активные абонементы")
    public ResponseEntity<List<PurchaseDto>> getActiveMemberships() {
        return ResponseEntity.ok(membershipService.getActiveMemberships());
    }

    @PostMapping("/visits")
    @Operation(summary = "Отметить посещение клиента")
    public ResponseEntity<VisitDto> markVisit(@RequestBody MarkVisitRequest request,
                                              @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(visitService.markVisit(request.getClientId(), getCurrentUserId(user)));
    }

    @GetMapping("/visits/today")
    @Operation(summary = "Посещения сегодня")
    public ResponseEntity<List<VisitDto>> getTodayVisits() {
        return ResponseEntity.ok(visitService.getTodayVisits());
    }

    @Data
    static class CreateClientRequest {
        private String email;
        private String firstName;
        private String lastName;
        private String phone;
        private String password;
        private LocalDate birthDate;
    }

    @Data
    static class CreateMembershipRequest {
        private Long clientId;
        private Long typeId;
        private LocalDate startDate;
    }

    @Data
    static class MarkVisitRequest {
        private Long clientId;
    }
}
