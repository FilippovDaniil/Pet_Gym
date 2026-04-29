package com.petgym.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "notifications") // таблица уведомлений (для клиентов и тренеров)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user; // кому адресовано уведомление

    @Column(nullable = false)
    private String message; // текст уведомления

    @Column(name = "is_read", nullable = false)
    private boolean read; // прочитано ли уведомление (false = новое)

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt; // когда создано

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
