package com.petgym.dto.report;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

// DTO финансового отчёта за период — возвращается администратору
@Data
@Builder
public class RevenueReportDto {
    private BigDecimal totalRevenue;         // суммарная выручка от продажи абонементов
    private long totalMembershipsSold;       // сколько абонементов продано за период
    private long totalTrainingsCount;        // сколько тренировок было проведено (CONFIRMED + COMPLETED)
    private Map<String, Long> membershipsByType; // разбивка по видам абонементов: {"1 месяц": 10, "3 месяца": 5}
}
