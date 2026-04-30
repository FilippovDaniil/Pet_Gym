package com.petgym.service;

import com.petgym.dto.report.RevenueReportDto;
import com.petgym.repository.PurchaseRepository;
import com.petgym.repository.TrainingBookingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportService {

    private final PurchaseRepository purchaseRepository;
    private final TrainingBookingRepository bookingRepository;

    // Получить финансовый отчёт за период [from, to] (для администратора)
    @Transactional(readOnly = true)
    public RevenueReportDto getRevenueReport(LocalDate from, LocalDate to) {
        // LocalDate → LocalDateTime: from = начало дня 00:00:00, to = конец дня 23:59:59.999
        LocalDateTime fromDt = from.atStartOfDay();
        LocalDateTime toDt = to.atTime(LocalTime.MAX); // LocalTime.MAX = 23:59:59.999999999

        // суммарная выручка за период
        BigDecimal total = purchaseRepository.sumRevenueByPeriod(fromDt, toDt);

        // разбивка продаж по типу абонемента → List<Object[]> = [{название, количество}, ...]
        List<Object[]> byType = purchaseRepository.countSoldByType(fromDt, toDt);

        // собираем Map: {"1 месяц" → 10, "3 месяца" → 5}
        // LinkedHashMap сохраняет порядок добавления (как в запросе)
        Map<String, Long> typeMap = new LinkedHashMap<>();
        long totalSold = 0;
        for (Object[] row : byType) {
            String name = (String) row[0];  // первый элемент массива — название типа
            Long count = (Long) row[1];     // второй — количество продаж
            typeMap.put(name, count);
            totalSold += count; // суммируем для итогового числа
        }

        // общее количество тренировок (CONFIRMED + COMPLETED) за всё время
        long trainingsCount = bookingRepository.countCompletedAndConfirmed();

        RevenueReportDto report = RevenueReportDto.builder()
                .totalRevenue(total != null ? total : BigDecimal.ZERO)
                .totalMembershipsSold(totalSold)
                .totalTrainingsCount(trainingsCount)
                .membershipsByType(typeMap)
                .build();
        log.info("[ADMIN] event=REVENUE_REPORT from={} to={} totalRevenue={} membershipsSold={} trainingsCount={}",
                from, to, report.getTotalRevenue(), totalSold, trainingsCount);
        return report;
    }
}
