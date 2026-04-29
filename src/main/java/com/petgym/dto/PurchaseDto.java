package com.petgym.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

// DTO покупки абонемента (запись о факте продажи)
@Data
@Builder
public class PurchaseDto {
    private Long id;
    private Long clientId;
    private String clientName;     // имя + фамилия клиента (склеивается в сервисе)
    private Long typeId;
    private String typeName;       // название вида абонемента
    private LocalDate startDate;   // начало действия
    private LocalDate endDate;     // конец действия
    private BigDecimal paidAmount; // сколько заплатили
    private LocalDateTime paymentDate; // когда оплатили
    private boolean active;        // действует ли абонемент сегодня (вычисляется в сервисе)
}
