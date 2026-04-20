package com.petgym.scheduled;

import com.petgym.repository.NotificationRepository;
import com.petgym.repository.PurchaseRepository;
import com.petgym.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class ScheduledTasks {

    private final NotificationRepository notificationRepository;
    private final PurchaseRepository purchaseRepository;
    private final NotificationService notificationService;

    /** Ежедневно в 00:05 — чистим уведомления старше 30 дней */
    @Scheduled(cron = "0 5 0 * * *")
    @Transactional
    public void cleanOldNotifications() {
        LocalDateTime threshold = LocalDateTime.now().minusDays(30);
        notificationRepository.deleteOlderThan(threshold);
        log.info("Cleaned notifications older than {}", threshold);
    }

    /** Ежедневно в 08:00 — напоминаем клиентам об истекающем абонементе (через 3 дня) */
    @Scheduled(cron = "0 0 8 * * *")
    @Transactional
    public void notifyExpiringMemberships() {
        LocalDate from = LocalDate.now().plusDays(1);
        LocalDate to = LocalDate.now().plusDays(3);
        purchaseRepository.findExpiringSoon(from, to).forEach(purchase -> {
            String msg = String.format("Ваш абонемент «%s» истекает %s. Пора продлить!",
                    purchase.getMembershipType().getName(), purchase.getEndDate());
            notificationService.send(purchase.getClient().getId(), msg);
        });
        log.info("Sent expiring membership notifications for period {} - {}", from, to);
    }
}
