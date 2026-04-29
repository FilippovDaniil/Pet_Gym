package com.petgym.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "training_bookings") // таблица записей на тренировку
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrainingBooking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    private User client; // кто записался

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trainer_id", nullable = false)
    private User trainer; // к какому тренеру

    @Column(name = "start_date_time", nullable = false)
    private LocalDateTime startDateTime; // начало тренировки

    @Column(name = "end_date_time", nullable = false)
    private LocalDateTime endDateTime; // конец тренировки (обычно startDateTime + 1 час)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BookingStatus status; // текущий статус записи (CONFIRMED, CANCELLED, и т.д.)

    @Column(name = "cancellation_reason")
    private String cancellationReason; // причина отмены (заполняется тренером при отмене)

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt; // когда создана запись

    @Version // поле для оптимистичной блокировки: JPA автоматически инкрементирует его при каждом UPDATE
             // Если два потока одновременно сохраняют один и тот же объект — один получит исключение
    private Long version;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (status == null) status = BookingStatus.CONFIRMED; // при создании статус сразу CONFIRMED
    }
}
