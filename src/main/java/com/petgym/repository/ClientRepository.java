package com.petgym.repository;

import com.petgym.domain.Client;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ClientRepository extends JpaRepository<Client, Long> {
    // Client хранится с PK = userId, поэтому ищем именно по userId
    Optional<Client> findByUserId(Long userId); // найти запись клиента по id пользователя
}
