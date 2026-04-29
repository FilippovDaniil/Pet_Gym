package com.petgym.repository;

import com.petgym.domain.MembershipType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MembershipTypeRepository extends JpaRepository<MembershipType, Long> {
    // Spring Data разбирает имя метода: findBy + IsActive + True
    // генерирует SQL: WHERE is_active = true
    List<MembershipType> findByIsActiveTrue(); // получить только активные (продаваемые) виды абонементов
}
