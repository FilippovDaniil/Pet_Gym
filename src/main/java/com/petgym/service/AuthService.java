package com.petgym.service;

import com.petgym.domain.Client;
import com.petgym.domain.Role;
import com.petgym.domain.User;
import com.petgym.dto.auth.AuthResponse;
import com.petgym.dto.auth.LoginRequest;
import com.petgym.dto.auth.RegisterRequest;
import com.petgym.exception.BusinessException;
import com.petgym.repository.ClientRepository;
import com.petgym.repository.UserRepository;
import com.petgym.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final ClientRepository clientRepository;
    private final PasswordEncoder passwordEncoder;  // для шифрования пароля
    private final JwtTokenProvider tokenProvider;   // для генерации JWT
    private final AuthenticationManager authenticationManager; // для проверки логин/пароль

    // Регистрация нового клиента
    @Transactional // если что-то упадёт — откатим и User, и Client
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            log.warn("[WARN] event=REGISTER_FAILED email={} reason=\"email уже занят\"", request.getEmail());
            throw new BusinessException("Email уже используется: " + request.getEmail());
        }

        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .phone(request.getPhone())
                .role(Role.CLIENT)
                .enabled(true)
                .build();
        user = userRepository.save(user);

        Client client = Client.builder()
                .user(user)
                .birthDate(request.getBirthDate())
                .build();
        clientRepository.save(client);

        log.info("[AUTH] event=REGISTER userId={} email={} name=\"{} {}\"",
                user.getId(), user.getEmail(), user.getFirstName(), user.getLastName());
        String token = tokenProvider.generateToken(user.getId(), user.getRole().name()); // генерируем JWT
        return buildAuthResponse(user, token); // возвращаем токен + данные пользователя
    }

    // Вход в систему
    public AuthResponse login(LoginRequest request) {
        // authenticationManager проверяет email + пароль через UserDetailsService
        // если пароль неверный — бросит BadCredentialsException (перехватит GlobalExceptionHandler → 401)
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BusinessException("Пользователь не найден"));
        log.info("[AUTH] event=LOGIN userId={} email={} role={}", user.getId(), user.getEmail(), user.getRole());
        String token = tokenProvider.generateToken(user.getId(), user.getRole().name());
        return buildAuthResponse(user, token);
    }

    // вспомогательный метод: собираем объект ответа
    private AuthResponse buildAuthResponse(User user, String token) {
        return AuthResponse.builder()
                .token(token)                   // JWT-токен
                .role(user.getRole().name())    // "CLIENT", "TRAINER" и т.д.
                .userId(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .build();
    }
}
