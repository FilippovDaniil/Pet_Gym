package com.petgym.service;

import com.petgym.domain.Client;
import com.petgym.domain.Role;
import com.petgym.domain.Trainer;
import com.petgym.domain.User;
import com.petgym.dto.ClientDto;
import com.petgym.dto.TrainerDto;
import com.petgym.dto.UserDto;
import com.petgym.exception.BusinessException;
import com.petgym.exception.ResourceNotFoundException;
import com.petgym.repository.ClientRepository;
import com.petgym.repository.TrainerRepository;
import com.petgym.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final ClientRepository clientRepository;
    private final TrainerRepository trainerRepository;
    private final PasswordEncoder passwordEncoder;
    private final MembershipService membershipService;

    // Поиск клиентов по email или телефону (для ресепшена: находит клиента у стойки)
    @Transactional(readOnly = true)
    public List<ClientDto> searchClients(String query) {
        List<User> users = userRepository.findByEmailContainingIgnoreCaseOrPhoneContaining(query, query);
        return users.stream()
                .filter(u -> u.getRole() == Role.CLIENT) // оставляем только клиентов
                .map(u -> toClientDto(u, clientRepository.findByUserId(u.getId()).orElse(null)))
                .collect(Collectors.toList());
    }

    // Получить всех клиентов (для ресепшена)
    @Transactional(readOnly = true)
    public List<ClientDto> getAllClients() {
        return userRepository.findByRole(Role.CLIENT).stream()
                .map(u -> toClientDto(u, clientRepository.findByUserId(u.getId()).orElse(null)))
                .collect(Collectors.toList());
    }

    // Создать нового клиента вручную (сотрудник ресепшена регистрирует клиента у стойки)
    @Transactional
    public ClientDto createClient(UserDto dto, String password, LocalDate birthDate) {
        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new BusinessException("Email уже используется");
        }
        User user = User.builder()
                .email(dto.getEmail())
                // если пароль не передан — ставим "changeme" (клиент потом поменяет)
                .password(passwordEncoder.encode(password != null ? password : "changeme"))
                .firstName(dto.getFirstName())
                .lastName(dto.getLastName())
                .phone(dto.getPhone())
                .role(Role.CLIENT)
                .enabled(true)
                .build();
        user = userRepository.save(user);
        Client client = Client.builder().user(user).birthDate(birthDate).build();
        clientRepository.save(client);
        log.info("Client created by reception: {}", user.getEmail());
        return toClientDto(user, client);
    }

    // Создать сотрудника (администратор добавляет тренера или сотрудника ресепшена)
    @Transactional
    public UserDto createStaff(UserDto dto, String password, String specialization, String bio) {
        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new BusinessException("Email уже используется");
        }
        User user = User.builder()
                .email(dto.getEmail())
                .password(passwordEncoder.encode(password))
                .firstName(dto.getFirstName())
                .lastName(dto.getLastName())
                .phone(dto.getPhone())
                .role(dto.getRole()) // роль задаёт администратор (TRAINER или RECEPTION)
                .enabled(true)
                .build();
        user = userRepository.save(user);

        // если создаём тренера — дополнительно сохраняем данные специализации
        if (dto.getRole() == Role.TRAINER) {
            Trainer trainer = Trainer.builder()
                    .user(user)
                    .specialization(specialization)
                    .bio(bio)
                    .build();
            trainerRepository.save(trainer);
        }
        log.info("Staff created: {} [{}]", user.getEmail(), user.getRole());
        return toUserDto(user);
    }

    // Получить всех тренеров (для отображения клиентам и тренеру)
    @Transactional(readOnly = true)
    public List<TrainerDto> getAllTrainers() {
        return trainerRepository.findAll().stream() // берём из таблицы trainers, где есть специализация
                .map(t -> TrainerDto.builder()
                        .id(t.getUserId())
                        .firstName(t.getUser().getFirstName())   // данные из связанного User
                        .lastName(t.getUser().getLastName())
                        .email(t.getUser().getEmail())
                        .phone(t.getUser().getPhone())
                        .specialization(t.getSpecialization())
                        .bio(t.getBio())
                        .build())
                .collect(Collectors.toList());
    }

    // Получить всех сотрудников (не клиентов) — для администратора
    @Transactional(readOnly = true)
    public List<UserDto> getAllStaff() {
        return userRepository.findAll().stream()
                .filter(u -> u.getRole() != Role.CLIENT) // исключаем клиентов
                .map(this::toUserDto)
                .collect(Collectors.toList());
    }

    // Entity User + Client → ClientDto (объединяем данные из двух таблиц)
    private ClientDto toClientDto(User user, Client client) {
        return ClientDto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .phone(user.getPhone())
                .birthDate(client != null ? client.getBirthDate() : null) // client может быть null (запись ещё не создана)
                .createdAt(user.getCreatedAt())
                .enabled(user.isEnabled())
                .hasActiveMembership(membershipService.hasActiveMembership(user.getId(), LocalDate.now())) // проверяем сейчас
                .build();
    }

    // Entity User → UserDto (без пароля)
    private UserDto toUserDto(User user) {
        return UserDto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .phone(user.getPhone())
                .role(user.getRole())
                .createdAt(user.getCreatedAt())
                .enabled(user.isEnabled())
                .build();
    }
}
