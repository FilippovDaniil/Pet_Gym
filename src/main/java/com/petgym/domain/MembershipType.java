package com.petgym.domain;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal; // тип для точных денежных значений (не теряет копейки, в отличие от double)

@Entity
@Table(name = "membership_types") // справочник видов абонементов
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MembershipType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name; // название абонемента, например: "1 месяц", "3 месяца + бассейн"

    @Column(name = "duration_days", nullable = false)
    private int durationDays; // срок действия в днях

    @Column(nullable = false, precision = 10, scale = 2) // precision — всего цифр, scale — после запятой
    private BigDecimal price; // стоимость абонемента в рублях

    @Column(name = "is_active", nullable = false)
    private boolean isActive; // false — абонемент снят с продажи, но старые записи сохраняются
}
