package com.petgym.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class PurchaseDto {
    private Long id;
    private Long clientId;
    private String clientName;
    private Long typeId;
    private String typeName;
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal paidAmount;
    private LocalDateTime paymentDate;
    private boolean active;
}
