package com.petgym.repository;

import com.petgym.domain.Purchase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;       // аннотация для написания JPQL (Java-аналог SQL)
import org.springframework.data.repository.query.Param;    // привязка именованного параметра к аргументу метода
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PurchaseRepository extends JpaRepository<Purchase, Long> {

    List<Purchase> findByClientId(Long clientId); // все покупки одного клиента (простой метод по имени)

    // JPQL-запрос: найти покупки, активные на заданную дату
    // условие: startDate <= date AND endDate >= date  → абонемент перекрывает указанную дату
    @Query("SELECT p FROM Purchase p WHERE p.client.id = :clientId AND p.startDate <= :date AND p.endDate >= :date")
    List<Purchase> findActivePurchases(@Param("clientId") Long clientId, @Param("date") LocalDate date);

    // найти абонементы, истекающие в диапазоне дат [from, to] — для рассылки напоминаний
    @Query("SELECT p FROM Purchase p WHERE p.endDate >= :from AND p.endDate <= :to ORDER BY p.endDate")
    List<Purchase> findExpiringSoon(@Param("from") LocalDate from, @Param("to") LocalDate to);

    // COALESCE(..., 0) — если нет ни одной покупки, вернуть 0 вместо null
    // суммируем оплаченные суммы за период → для финансового отчёта
    @Query("SELECT COALESCE(SUM(p.paidAmount), 0) FROM Purchase p WHERE p.paymentDate >= :from AND p.paymentDate <= :to")
    BigDecimal sumRevenueByPeriod(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    // GROUP BY — группируем по названию типа абонемента и считаем количество продаж каждого
    // возвращает список массивов: [0] = название типа (String), [1] = количество (Long)
    @Query("SELECT p.membershipType.name, COUNT(p) FROM Purchase p WHERE p.paymentDate >= :from AND p.paymentDate <= :to GROUP BY p.membershipType.name")
    List<Object[]> countSoldByType(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    // JOIN FETCH — сразу подгружаем связанный membershipType, чтобы не делать N+1 запросов
    // ORDER BY paymentDate DESC — сначала самые свежие покупки
    @Query("SELECT p FROM Purchase p JOIN FETCH p.membershipType WHERE p.client.id = :clientId ORDER BY p.paymentDate DESC")
    List<Purchase> findByClientIdWithType(@Param("clientId") Long clientId);
}
