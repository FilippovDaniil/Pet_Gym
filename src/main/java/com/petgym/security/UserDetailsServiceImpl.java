package com.petgym.security;

import com.petgym.domain.User;
import com.petgym.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority; // одно право/роль пользователя
import org.springframework.security.core.userdetails.UserDetails;          // интерфейс Spring Security: данные пользователя
import org.springframework.security.core.userdetails.UserDetailsService;   // интерфейс: загрузка пользователя по логину
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
// Реализуем UserDetailsService — Spring Security вызывает его для проверки логина/пароля
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    // Метод интерфейса: Spring Security вызывает его при аутентификации по логину (email)
    @Override
    @Transactional(readOnly = true) // читаем из БД, не меняем данные
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));
        return buildUserDetails(user); // преобразуем нашу сущность User в объект Spring Security
    }

    // Дополнительный метод: загрузка пользователя по id (используется в JWT-фильтре)
    @Transactional(readOnly = true)
    public UserDetails loadUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UsernameNotFoundException("User not found by id: " + id));
        return buildUserDetails(user);
    }

    // Строим объект UserDetails из нашей сущности User
    private UserDetails buildUserDetails(User user) {
        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getEmail())       // логин = email
                .password(user.getPassword())    // хэшированный пароль
                // роль превращается в право "ROLE_CLIENT", "ROLE_TRAINER" и т.д. (Spring требует ROLE_ префикс)
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())))
                .accountExpired(false)           // аккаунт не истёк
                .accountLocked(false)            // аккаунт не заблокирован
                .credentialsExpired(false)       // пароль не просрочен
                .disabled(!user.isEnabled())     // disabled = true если пользователь заблокирован
                .build();
    }

    // вспомогательный метод для получения id по email (используется в контроллерах)
    public Long getUserIdByEmail(String email) {
        return userRepository.findByEmail(email)
                .map(User::getId) // извлекаем id из User
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));
    }
}
