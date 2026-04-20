package com.petgym.dto;

import com.petgym.domain.Role;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class UserDto {
    private Long id;
    private String email;
    private String firstName;
    private String lastName;
    private String phone;
    private Role role;
    private LocalDateTime createdAt;
    private boolean enabled;
}
