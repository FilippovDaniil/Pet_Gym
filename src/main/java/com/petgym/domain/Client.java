package com.petgym.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate; // тип «только дата» (без времени) — подходит для дня рождения

@Entity
@Table(name = "clients") // отдельная таблица clients, расширяющая данные пользователя
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Client {

    @Id
    @Column(name = "user_id") // первичный ключ clients совпадает с id в таблице users (разделение 1-к-1)
    private Long userId;

    @OneToOne(fetch = FetchType.LAZY) // связь один-к-одному с User; LAZY — не загружать User до обращения
    @MapsId                           // говорит JPA: userId = это и есть PK, он берётся из связанного User
    @JoinColumn(name = "user_id")     // столбец внешнего ключа в таблице clients
    private User user;

    @Column(name = "birth_date") // дата рождения клиента (необязательна)
    private LocalDate birthDate;
}
