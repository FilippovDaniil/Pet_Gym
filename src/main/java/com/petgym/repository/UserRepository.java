package com.petgym.repository;

import com.petgym.domain.Role;
import com.petgym.domain.User;
import org.springframework.data.jpa.repository.JpaRepository; // базовый интерфейс Spring Data: даёт findAll, save, delete и т.д. бесплатно
import org.springframework.stereotype.Repository;             // пометка для Spring: это репозиторий (DAO)

import java.util.List;
import java.util.Optional; // обёртка над значением: может быть пустой (вместо null)

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    // Spring Data сам генерирует реализацию этих методов по их именам

    Optional<User> findByEmail(String email); // найти пользователя по email (вернёт пустой Optional, если не найден)

    boolean existsByEmail(String email); // проверить, есть ли уже пользователь с таким email

    List<User> findByRole(Role role); // найти всех пользователей с заданной ролью

    // Метод с длинным именем: ищет по email (без учёта регистра) ИЛИ по телефону
    // Spring Data разбирает имя метода: findBy + EmailContainingIgnoreCase + Or + PhoneContaining
    List<User> findByEmailContainingIgnoreCaseOrPhoneContaining(String email, String phone);
}
