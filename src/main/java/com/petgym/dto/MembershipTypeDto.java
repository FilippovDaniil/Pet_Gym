package com.petgym.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class MembershipTypeDto {
    private Long id;

    @NotBlank
    private String name;

    @NotNull
    @Min(1)
    private Integer durationDays;

    @NotNull
    private BigDecimal price;

    private boolean active;
}
