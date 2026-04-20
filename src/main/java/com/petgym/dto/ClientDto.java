package com.petgym.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class ClientDto {
    private Long id;
    private String email;
    private String firstName;
    private String lastName;
    private String phone;
    private LocalDate birthDate;
    private LocalDateTime createdAt;
    private boolean enabled;
    private boolean hasActiveMembership;
}
