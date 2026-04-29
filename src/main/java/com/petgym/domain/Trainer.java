package com.petgym.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "trainers") // отдельная таблица для дополнительных данных тренеров
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Trainer {

    @Id
    @Column(name = "user_id") // PK совпадает с PK таблицы users
    private Long userId;

    @OneToOne(fetch = FetchType.LAZY) // связь 1-к-1: каждый тренер — это пользователь
    @MapsId                           // userId берётся из user.id
    @JoinColumn(name = "user_id")
    private User user;

    private String specialization; // специализация тренера (например: "Йога", "Силовые тренировки")

    @Column(columnDefinition = "TEXT") // TEXT в PostgreSQL — строка без ограничения длины (для длинного bio)
    private String bio; // биография / описание тренера
}
