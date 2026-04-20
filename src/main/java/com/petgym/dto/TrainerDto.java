package com.petgym.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TrainerDto {
    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private String specialization;
    private String bio;
}
