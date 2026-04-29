package com.petgym.domain;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "purchases") // таблица фактов покупки абонементов
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Purchase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)        // много покупок у одного клиента; LAZY — загружать по требованию
    @JoinColumn(name = "client_id", nullable = false) // внешний ключ client_id → users.id
    private User client; // кто купил абонемент

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "type_id", nullable = false) // внешний ключ на тип абонемента
    private MembershipType membershipType; // какой абонемент купили

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate; // с какого числа действует абонемент

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate; // по какое число включительно действует (startDate + durationDays)

    @Column(name = "paid_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal paidAmount; // сколько заплатили (фиксируем на момент покупки, цена могла измениться)

    @Column(name = "payment_date", nullable = false)
    private LocalDateTime paymentDate; // когда произошла оплата

    @PrePersist // вызывается перед первой вставкой в БД
    protected void onCreate() {
        if (paymentDate == null) paymentDate = LocalDateTime.now(); // автоматически ставим время оплаты
    }
}
