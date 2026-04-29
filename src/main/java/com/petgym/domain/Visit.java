package com.petgym.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "visits") // журнал посещений клуба
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Visit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    private User client; // кто пришёл

    @Column(name = "visit_date", nullable = false)
    private LocalDate visitDate; // дата посещения (только дата, без времени)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "marked_by") // кто отметил посещение (сотрудник ресепшен), может быть null
    private User markedBy;

    @Column(name = "marked_at", nullable = false)
    private LocalDateTime markedAt; // точный момент отметки (дата + время)

    @PrePersist
    protected void onCreate() {
        if (markedAt == null) markedAt = LocalDateTime.now(); // фиксируем текущий момент при создании
    }
}
