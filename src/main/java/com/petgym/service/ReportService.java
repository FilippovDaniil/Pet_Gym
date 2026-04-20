package com.petgym.service;

import com.petgym.dto.report.RevenueReportDto;
import com.petgym.repository.PurchaseRepository;
import com.petgym.repository.TrainingBookingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final PurchaseRepository purchaseRepository;
    private final TrainingBookingRepository bookingRepository;

    @Transactional(readOnly = true)
    public RevenueReportDto getRevenueReport(LocalDate from, LocalDate to) {
        LocalDateTime fromDt = from.atStartOfDay();
        LocalDateTime toDt = to.atTime(LocalTime.MAX);

        BigDecimal total = purchaseRepository.sumRevenueByPeriod(fromDt, toDt);
        List<Object[]> byType = purchaseRepository.countSoldByType(fromDt, toDt);

        Map<String, Long> typeMap = new LinkedHashMap<>();
        long totalSold = 0;
        for (Object[] row : byType) {
            String name = (String) row[0];
            Long count = (Long) row[1];
            typeMap.put(name, count);
            totalSold += count;
        }

        long trainingsCount = bookingRepository.countCompletedAndConfirmed();

        return RevenueReportDto.builder()
                .totalRevenue(total != null ? total : BigDecimal.ZERO)
                .totalMembershipsSold(totalSold)
                .totalTrainingsCount(trainingsCount)
                .membershipsByType(typeMap)
                .build();
    }
}
