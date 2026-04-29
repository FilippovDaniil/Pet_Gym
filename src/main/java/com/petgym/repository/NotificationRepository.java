package com.petgym.repository;

import com.petgym.domain.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;   // аннотация для запросов, изменяющих данные (UPDATE/DELETE)
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional; // гарантия атомарности операции

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    // все уведомления пользователя, отсортированные от новых к старым
    List<Notification> findByUserIdOrderByCreatedAtDesc(Long userId);

    // только непрочитанные уведомления пользователя
    List<Notification> findByUserIdAndReadFalseOrderByCreatedAtDesc(Long userId);

    @Modifying       // говорим Spring Data: этот запрос изменяет данные (не SELECT)
    @Transactional   // нужен для @Modifying — выполняем в рамках транзакции
    @Query("DELETE FROM Notification n WHERE n.createdAt < :before") // удалить старые уведомления
    void deleteOlderThan(@Param("before") LocalDateTime before);

    @Modifying
    @Transactional
    @Query("UPDATE Notification n SET n.read = true WHERE n.user.id = :userId") // пометить все как прочитанные
    void markAllAsReadByUserId(@Param("userId") Long userId);
}
