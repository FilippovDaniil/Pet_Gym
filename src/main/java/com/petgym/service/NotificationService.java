package com.petgym.service;

import com.petgym.domain.Notification;
import com.petgym.domain.User;
import com.petgym.dto.NotificationDto;
import com.petgym.repository.NotificationRepository;
import com.petgym.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    // Отправить уведомление пользователю (сохраняем в БД, пользователь увидит при следующем запросе)
    @Transactional
    public void send(Long userId, String message) {
        // getReferenceById — возвращает прокси без SELECT-запроса (нам нужен только id для FK)
        User user = userRepository.getReferenceById(userId);
        Notification notification = Notification.builder()
                .user(user)
                .message(message)
                .read(false) // новое уведомление всегда непрочитано
                .build();
        notificationRepository.save(notification);
        log.info("Notification sent to user {}: {}", userId, message);
    }

    // Получить непрочитанные уведомления пользователя
    @Transactional(readOnly = true)
    public List<NotificationDto> getMyNotifications(Long userId) {
        return notificationRepository.findByUserIdAndReadFalseOrderByCreatedAtDesc(userId)
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    // Пометить все уведомления пользователя как прочитанные (одним UPDATE-запросом)
    @Transactional
    public void markAllRead(Long userId) {
        notificationRepository.markAllAsReadByUserId(userId);
    }

    // Entity Notification → DTO NotificationDto
    private NotificationDto toDto(Notification n) {
        return NotificationDto.builder()
                .id(n.getId())
                .message(n.getMessage())
                .read(n.isRead())
                .createdAt(n.getCreatedAt())
                .build();
    }
}
