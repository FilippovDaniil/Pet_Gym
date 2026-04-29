package com.petgym.config;

import com.petgym.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;            // @Bean: метод возвращает объект, управляемый Spring
import org.springframework.context.annotation.Configuration;  // @Configuration: этот класс содержит настройки Spring
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider; // провайдер аутентификации через БД
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity; // включает @PreAuthorize на методах
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity; // включает Spring Security
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy; // политика сессий
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder; // алгоритм хэширования паролей
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;              // настройка CORS (Cross-Origin Resource Sharing)
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity  // включаем Spring Security
@EnableMethodSecurity // разрешаем использование @PreAuthorize на методах контроллеров
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter; // наш JWT-фильтр
    private final UserDetailsService userDetailsService;

    // Бин шифровщика паролей: BCrypt с cost factor = 10 (баланс скорость/надёжность)
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10);
    }

    // Провайдер аутентификации: проверяет email+пароль через БД
    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService); // откуда загружать пользователя
        provider.setPasswordEncoder(passwordEncoder());     // как сравнивать пароли
        return provider;
    }

    // AuthenticationManager используется в AuthService для проверки логина/пароля
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    // Главная настройка цепочки фильтров безопасности
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable) // отключаем CSRF (не нужен для stateless REST API с JWT)
            .cors(cors -> cors.configurationSource(corsConfigurationSource())) // включаем CORS с нашей конфигурацией
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)) // не создаём HTTP-сессии (JWT сам хранит состояние)
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**").permitAll()  // регистрация и вход доступны всем
                .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/api-docs/**", "/v3/api-docs/**").permitAll() // Swagger UI доступен всем
                .requestMatchers("/h2-console/**").permitAll() // H2 консоль (для разработки)
                .requestMatchers("/", "/index.html", "/client.html", "/reception.html", "/admin.html", "/trainer.html").permitAll() // статические HTML-страницы
                .requestMatchers("/css/**", "/js/**", "/favicon.ico").permitAll() // статические ресурсы
                .requestMatchers("/api/client/**").hasRole("CLIENT")     // только клиенты
                .requestMatchers("/api/reception/**").hasRole("RECEPTION") // только сотрудники ресепшен
                .requestMatchers("/api/admin/**").hasRole("ADMIN")       // только администраторы
                .requestMatchers("/api/trainer/**").hasRole("TRAINER")   // только тренеры
                .anyRequest().authenticated()                            // любой другой запрос — нужна аутентификация
            )
            .headers(headers -> headers.frameOptions(fo -> fo.sameOrigin())) // разрешаем iframe с того же домена (для H2 консоли)
            .authenticationProvider(authenticationProvider()) // регистрируем провайдер аутентификации
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class); // добавляем JWT-фильтр перед стандартным фильтром логина
        return http.build();
    }

    // Настройка CORS: разрешаем запросы с любых источников (для разработки)
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of("*")); // разрешаем любые Origin (в продакшене нужно указать конкретные домены)
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")); // разрешённые HTTP-методы
        config.setAllowedHeaders(List.of("*")); // разрешаем любые заголовки
        config.setAllowCredentials(true); // разрешаем передачу cookies/заголовков авторизации
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config); // применяем ко всем URL
        return source;
    }
}
