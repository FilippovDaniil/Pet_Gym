package com.petgym.domain;

import jakarta.persistence.*; // аннотации JPA для маппинга класса на таблицу БД
import lombok.*;              // аннотации Lombok для автогенерации boilerplate-кода

import java.time.LocalDateTime; // тип «дата + время» без часового пояса

@Entity                   // говорим JPA: этот класс — сущность, то есть строка в таблице БД
@Table(name = "users")    // указываем, в какую именно таблицу маппится класс
@Getter                   // Lombok: генерирует методы getXxx() для всех полей
@Setter                   // Lombok: генерирует методы setXxx() для всех полей
@NoArgsConstructor        // Lombok: генерирует конструктор без аргументов (нужен JPA)
@AllArgsConstructor       // Lombok: генерирует конструктор со всеми полями
@Builder                  // Lombok: генерирует паттерн «строитель» — User.builder().email("...").build()
public class User {

    @Id                                                      // это поле — первичный ключ таблицы
    @GeneratedValue(strategy = GenerationType.IDENTITY)      // значение генерирует БД (AUTO_INCREMENT / SERIAL)
    private Long id;

    @Column(nullable = false, unique = true) // столбец обязателен и уникален (два пользователя не могут иметь один email)
    private String email;

    @Column(nullable = false) // пароль обязателен (хранится в зашифрованном виде через BCrypt)
    private String password;

    @Column(name = "first_name", nullable = false) // name задаёт название столбца в БД (snake_case вместо camelCase)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    private String phone; // телефон необязателен — нет @Column(nullable=false)

    @Enumerated(EnumType.STRING) // хранить роль как строку ("CLIENT"), а не число (0) — читабельнее в БД
    @Column(nullable = false)
    private Role role;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt; // дата и время регистрации

    @Column(nullable = false)
    private boolean enabled; // флаг: пользователь активен (true) или заблокирован (false)

    @PrePersist // этот метод автоматически вызывается перед первым сохранением объекта в БД
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now(); // ставим текущее время, если не задано вручную
        enabled = true; // по умолчанию все новые пользователи активны
    }
}
