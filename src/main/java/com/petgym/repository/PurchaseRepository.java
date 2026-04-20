package com.petgym.repository;

import com.petgym.domain.Purchase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PurchaseRepository extends JpaRepository<Purchase, Long> {

    List<Purchase> findByClientId(Long clientId);

    @Query("SELECT p FROM Purchase p WHERE p.client.id = :clientId AND p.startDate <= :date AND p.endDate >= :date")
    List<Purchase> findActivePurchases(@Param("clientId") Long clientId, @Param("date") LocalDate date);

    @Query("SELECT p FROM Purchase p WHERE p.endDate >= :from AND p.endDate <= :to ORDER BY p.endDate")
    List<Purchase> findExpiringSoon(@Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("SELECT COALESCE(SUM(p.paidAmount), 0) FROM Purchase p WHERE p.paymentDate >= :from AND p.paymentDate <= :to")
    BigDecimal sumRevenueByPeriod(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("SELECT p.membershipType.name, COUNT(p) FROM Purchase p WHERE p.paymentDate >= :from AND p.paymentDate <= :to GROUP BY p.membershipType.name")
    List<Object[]> countSoldByType(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("SELECT p FROM Purchase p JOIN FETCH p.membershipType WHERE p.client.id = :clientId ORDER BY p.paymentDate DESC")
    List<Purchase> findByClientIdWithType(@Param("clientId") Long clientId);
}
