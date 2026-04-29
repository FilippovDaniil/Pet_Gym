package com.petgym.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

// DTO вида абонемента — используется и для чтения, и для создания/обновления
@Data
@Builder
public class MembershipTypeDto {
    private Long id; // null при создании нового вида

    @NotBlank // название обязательно
    private String name;

    @NotNull
    @Min(1) // срок минимум 1 день
    private Integer durationDays;

    @NotNull
    private BigDecimal price; // цена

    private boolean active; // активен ли вид (можно ли купить)
}
