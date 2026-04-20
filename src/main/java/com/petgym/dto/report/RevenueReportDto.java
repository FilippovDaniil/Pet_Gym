package com.petgym.dto.report;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class RevenueReportDto {
    private BigDecimal totalRevenue;
    private long totalMembershipsSold;
    private long totalTrainingsCount;
    private Map<String, Long> membershipsByType;
}
