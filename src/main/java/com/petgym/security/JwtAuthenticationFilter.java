package com.petgym.security;

import jakarta.servlet.FilterChain;          // цепочка фильтров: позволяет передать запрос следующему фильтру
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken; // объект аутентификации Spring Security
import org.springframework.security.core.context.SecurityContextHolder;                // хранилище текущей аутентификации (per-request)
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils; // утилиты для работы со строками
import org.springframework.web.filter.OncePerRequestFilter; // базовый класс: фильтр выполняется ровно один раз на запрос

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor // Lombok: конструктор со всеми final-полями (внедрение зависимостей)
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider tokenProvider;
    private final UserDetailsServiceImpl userDetailsService;

    // Этот метод вызывается Spring Security для каждого HTTP-запроса
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String token = extractTokenFromRequest(request); // извлекаем токен из заголовка Authorization

        if (StringUtils.hasText(token) && tokenProvider.validateToken(token)) {
            // токен есть и он валидный → аутентифицируем пользователя
            Long userId = tokenProvider.getUserIdFromToken(token); // достаём userId из токена
            UserDetails userDetails = userDetailsService.loadUserById(userId); // загружаем пользователя из БД

            // создаём объект аутентификации с правами пользователя
            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
            auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request)); // прикрепляем детали запроса (IP и т.д.)

            SecurityContextHolder.getContext().setAuthentication(auth); // сохраняем аутентификацию в контекст запроса
            // теперь @AuthenticationPrincipal и hasRole() работают корректно для этого запроса
        }

        filterChain.doFilter(request, response); // передаём запрос дальше по цепочке фильтров
    }

    // извлекаем JWT из заголовка Authorization: Bearer <token>
    private String extractTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization"); // читаем заголовок
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7); // убираем префикс "Bearer " (7 символов) и возвращаем сам токен
        }
        return null; // заголовка нет или он не в формате Bearer — возвращаем null
    }
}
