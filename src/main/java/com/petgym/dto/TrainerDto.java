package com.petgym.dto;

import lombok.Builder;
import lombok.Data;

// DTO тренера — публичные данные для списка тренеров (видят клиенты)
@Data
@Builder
public class TrainerDto {
    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private String specialization; // специализация, например "Йога и растяжка"
    private String bio;            // биография / описание тренера
}
