package com.petgym.security;

import io.jsonwebtoken.*;                       // библиотека jjwt для работы с JWT
import io.jsonwebtoken.io.Decoders;             // декодирование Base64-строки в байты
import io.jsonwebtoken.security.Keys;           // генерация ключа из байтов
import lombok.extern.slf4j.Slf4j;              // Lombok: создаёт поле log = LoggerFactory.getLogger(...)
import org.springframework.beans.factory.annotation.Value; // внедрение значения из application.properties
import org.springframework.stereotype.Component; // Spring-бин, доступный для внедрения

import javax.crypto.SecretKey; // тип криптографического ключа
import java.util.Date;

@Slf4j
@Component
public class JwtTokenProvider {

    @Value("${jwt.secret}") // берём секрет из application.properties: jwt.secret=...
    private String jwtSecret;

    @Value("${jwt.expiration}") // время жизни токена в миллисекундах: jwt.expiration=86400000 (24 часа)
    private long jwtExpiration;

    // преобразуем строку секрета (Base64) в объект ключа для подписи
    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(jwtSecret); // декодируем строку из Base64 в байты
        return Keys.hmacShaKeyFor(keyBytes); // создаём HMAC-SHA ключ из байтов
    }

    // создаём JWT-токен: встраиваем userId и role в полезную нагрузку (payload)
    public String generateToken(Long userId, String role) {
        Date now = new Date();                                    // текущее время
        Date expiryDate = new Date(now.getTime() + jwtExpiration); // время истечения = сейчас + expiration

        return Jwts.builder()
                .subject(String.valueOf(userId))  // subject = id пользователя (стандартное поле JWT)
                .claim("role", role)              // добавляем собственное поле role в payload
                .issuedAt(now)                    // когда выдан токен
                .expiration(expiryDate)           // когда истекает
                .signWith(getSigningKey())         // подписываем секретным ключом (HMAC-SHA256)
                .compact();                        // собираем итоговую строку вида "xxxxx.yyyyy.zzzzz"
    }

    // извлекаем id пользователя из токена
    public Long getUserIdFromToken(String token) {
        Claims claims = parseClaims(token); // разбираем и верифицируем токен
        return Long.parseLong(claims.getSubject()); // subject → Long
    }

    // извлекаем роль из токена
    public String getRoleFromToken(String token) {
        Claims claims = parseClaims(token);
        return claims.get("role", String.class); // достаём кастомное поле "role"
    }

    // проверяем, является ли токен валидным (подпись корректна, не просрочен)
    public boolean validateToken(String token) {
        try {
            parseClaims(token); // если разбор прошёл без исключений — токен валиден
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("Invalid JWT token: {}", e.getMessage()); // логируем невалидный токен
            return false;
        }
    }

    // вспомогательный метод: разбираем токен, проверяем подпись и возвращаем payload
    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey()) // указываем ключ для проверки подписи
                .build()
                .parseSignedClaims(token)   // парсим и проверяем, бросит исключение если невалидный
                .getPayload();              // возвращаем полезную нагрузку (Claims)
    }
}
