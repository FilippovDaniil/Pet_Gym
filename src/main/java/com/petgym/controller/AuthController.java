package com.petgym.controller;

import com.petgym.dto.auth.AuthResponse;
import com.petgym.dto.auth.LoginRequest;
import com.petgym.dto.auth.RegisterRequest;
import com.petgym.service.AuthService;
import io.swagger.v3.oas.annotations.Operation; // аннотация для описания метода в Swagger UI
import io.swagger.v3.oas.annotations.tags.Tag;  // группировка эндпоинтов в Swagger UI
import jakarta.validation.Valid;                 // запускает валидацию аннотаций @NotBlank, @Email и т.д. на DTO
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity; // HTTP-ответ с телом и статус-кодом
import org.springframework.web.bind.annotation.*;

@RestController          // это REST-контроллер: методы возвращают JSON, а не HTML
@RequestMapping("/api/auth") // все эндпоинты этого контроллера начинаются с /api/auth
@RequiredArgsConstructor
@Tag(name = "Auth", description = "Регистрация и вход") // группа в Swagger UI
public class AuthController {

    private final AuthService authService;

    // POST /api/auth/register — регистрация нового клиента
    @PostMapping("/register")
    @Operation(summary = "Регистрация нового клиента") // описание в Swagger
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        // @RequestBody — десериализуем JSON из тела запроса в объект RegisterRequest
        // @Valid — запускаем валидацию (если ошибка — GlobalExceptionHandler вернёт 400)
        return ResponseEntity.ok(authService.register(request)); // 200 OK + тело JSON
    }

    // POST /api/auth/login — вход в систему
    @PostMapping("/login")
    @Operation(summary = "Вход в систему")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }
}
