package com.petgym.repository;

import com.petgym.domain.MembershipType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MembershipTypeRepository extends JpaRepository<MembershipType, Long> {
    List<MembershipType> findByIsActiveTrue();
}
