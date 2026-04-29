package com.petgym.config;

import com.petgym.domain.Client;
import com.petgym.domain.Role;
import com.petgym.domain.Trainer;
import com.petgym.domain.User;
import com.petgym.repository.ClientRepository;
import com.petgym.repository.TrainerRepository;
import com.petgym.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner; // интерфейс: run() вызывается сразу после старта Spring
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Slf4j
@Component
@RequiredArgsConstructor
// ApplicationRunner — Spring вызовет метод run() один раз при старте приложения
// Используется для заполнения БД тестовыми данными
public class DataInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final ClientRepository clientRepository;
    private final TrainerRepository trainerRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional // все операции выполняются в одной транзакции (либо все, либо ни одна)
    public void run(ApplicationArguments args) {
        // создаём тестовых пользователей, если их ещё нет в БД
        // null в позиции birthDate/specialization/bio — эти поля не нужны для данной роли
        createUserIfAbsent("admin@fit.com", "admin", "Иван", "Администратов", "+79001234567", Role.ADMIN, null, null, null);
        createUserIfAbsent("reception@fit.com", "reception", "Мария", "Ресепшенова", "+79001234568", Role.RECEPTION, null, null, null);
        createUserIfAbsent("trainer1@fit.com", "trainer1", "Алексей", "Тренеров", "+79001234569", Role.TRAINER, null, "Силовые тренировки", "Опыт 7 лет. Специализация: пауэрлифтинг, бодибилдинг.");
        createUserIfAbsent("trainer2@fit.com", "trainer2", "Елена", "Йогина", "+79001234570", Role.TRAINER, null, "Йога и растяжка", "Сертифицированный инструктор по йоге, опыт 5 лет.");
        createUserIfAbsent("client1@fit.com", "client1", "Пётр", "Клиентов", "+79001234571", Role.CLIENT, LocalDate.of(1990, 5, 15), null, null);
        createUserIfAbsent("client2@fit.com", "client2", "Анна", "Спортсменова", "+79001234572", Role.CLIENT, LocalDate.of(1995, 8, 22), null, null);
        log.info("Test data initialized");
    }

    // вспомогательный метод: создаёт пользователя только если его ещё нет в БД
    private void createUserIfAbsent(String email, String password, String firstName, String lastName,
                                    String phone, Role role, LocalDate birthDate,
                                    String specialization, String bio) {
        if (userRepository.existsByEmail(email)) return; // уже существует — пропускаем

        User user = User.builder()
                .email(email)
                .password(passwordEncoder.encode(password)) // хэшируем пароль перед сохранением
                .firstName(firstName)
                .lastName(lastName)
                .phone(phone)
                .role(role)
                .enabled(true)
                .build();
        user = userRepository.save(user); // сохраняем пользователя, JPA вернёт объект с заполненным id

        // в зависимости от роли создаём дополнительные записи
        if (role == Role.CLIENT) {
            clientRepository.save(Client.builder().user(user).birthDate(birthDate).build());
        } else if (role == Role.TRAINER) {
            trainerRepository.save(Trainer.builder().user(user).specialization(specialization).bio(bio).build());
        }
        log.info("Created test user: {} [{}]", email, role);
    }
}
