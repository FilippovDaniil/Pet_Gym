package com.petgym.repository;

import com.petgym.domain.Visit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface VisitRepository extends JpaRepository<Visit, Long> {

    // JOIN FETCH v.client — загружаем данные клиента сразу, чтобы не делать N+1 запросов
    // нужно для отображения имени клиента в списке посещений за сегодня
    @Query("SELECT v FROM Visit v JOIN FETCH v.client WHERE v.visitDate = :date ORDER BY v.markedAt")
    List<Visit> findByVisitDate(@Param("date") LocalDate date); // все посещения за указанную дату

    List<Visit> findByClientIdOrderByVisitDateDesc(Long clientId); // история посещений клиента (новые первыми)

    // проверить: был ли клиент уже сегодня? Чтобы не дать отметить дважды за один день
    boolean existsByClientIdAndVisitDate(Long clientId, LocalDate date);
}
