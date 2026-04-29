package com.petgym.scheduled;

import com.petgym.repository.NotificationRepository;
import com.petgym.repository.PurchaseRepository;
import com.petgym.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled; // запускает метод по расписанию (cron или fixedRate)
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
// Класс с фоновыми задачами по расписанию
// @EnableScheduling в PetGymApplication.java активирует обработку @Scheduled
public class ScheduledTasks {

    private final NotificationRepository notificationRepository;
    private final PurchaseRepository purchaseRepository;
    private final NotificationService notificationService;

    // Cron выражение "0 5 0 * * *" читается как: секунда=0, минута=5, час=0, любой день, любой месяц, любой день недели
    // То есть: ежедневно в 00:05
    @Scheduled(cron = "0 5 0 * * *")
    @Transactional
    public void cleanOldNotifications() {
        LocalDateTime threshold = LocalDateTime.now().minusDays(30); // граница: 30 дней назад
        notificationRepository.deleteOlderThan(threshold); // удаляем старые уведомления одним DELETE-запросом
        log.info("Cleaned notifications older than {}", threshold);
    }

    // Ежедневно в 08:00 рассылаем напоминания клиентам, у которых абонемент истекает через 1–3 дня
    @Scheduled(cron = "0 0 8 * * *")
    @Transactional
    public void notifyExpiringMemberships() {
        LocalDate from = LocalDate.now().plusDays(1); // завтра
        LocalDate to = LocalDate.now().plusDays(3);   // через 3 дня

        // находим все абонементы, которые истекают в диапазоне [from, to]
        purchaseRepository.findExpiringSoon(from, to).forEach(purchase -> {
            String msg = String.format("Ваш абонемент «%s» истекает %s. Пора продлить!",
                    purchase.getMembershipType().getName(), purchase.getEndDate());
            notificationService.send(purchase.getClient().getId(), msg); // отправляем уведомление клиенту
        });
        log.info("Sent expiring membership notifications for period {} - {}", from, to);
    }
}
