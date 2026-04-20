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

    @Query("SELECT v FROM Visit v JOIN FETCH v.client WHERE v.visitDate = :date ORDER BY v.markedAt")
    List<Visit> findByVisitDate(@Param("date") LocalDate date);

    List<Visit> findByClientIdOrderByVisitDateDesc(Long clientId);

    boolean existsByClientIdAndVisitDate(Long clientId, LocalDate date);
}
