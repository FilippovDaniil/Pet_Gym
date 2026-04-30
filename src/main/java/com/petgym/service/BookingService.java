package com.petgym.service;

import com.petgym.domain.BookingStatus;
import com.petgym.domain.TrainingBooking;
import com.petgym.domain.User;
import com.petgym.dto.BookingDto;
import com.petgym.dto.CreateBookingRequest;
import com.petgym.exception.BookingConflictException;
import com.petgym.exception.BusinessException;
import com.petgym.exception.InvalidCancellationException;
import com.petgym.exception.MembershipExpiredException;
import com.petgym.exception.ResourceNotFoundException;
import com.petgym.repository.TrainingBookingRepository;
import com.petgym.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookingService {

    private final TrainingBookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final MembershipService membershipService;
    private final NotificationService notificationService; // для отправки уведомлений тренеру

    // Создать бронирование тренировки
    @Transactional
    public BookingDto createBooking(Long clientId, CreateBookingRequest request) {
        LocalDateTime start = request.getStartDateTime();
        LocalDateTime end = start.plusHours(1); // тренировка всегда 1 час
        LocalDateTime now = LocalDateTime.now();

        // Бизнес-правило 1: дата должна быть в будущем
        if (!start.isAfter(now)) {
            throw new BusinessException("Дата бронирования должна быть в будущем");
        }
        // Бизнес-правило 2: нельзя бронировать более чем за 30 дней
        if (start.isAfter(now.plusDays(30))) {
            throw new BusinessException("Бронирование возможно не более чем за 30 дней вперёд");
        }

        // Бизнес-правило 3: у клиента должен быть активный абонемент на день тренировки
        if (!membershipService.hasActiveMembership(clientId, start.toLocalDate())) {
            throw new MembershipExpiredException("У вас нет активного абонемента на дату тренировки");
        }

        // Бизнес-правило 4: не более 2 будущих бронирований у одного тренера
        long futureCount = bookingRepository.countFutureBookingsByClientAndTrainer(
                clientId, request.getTrainerId(), now);
        if (futureCount >= 2) {
            throw new BookingConflictException("Нельзя иметь более 2 будущих бронирований у одного тренера");
        }

        // Бизнес-правило 5: тренер не должен быть занят в это время (с пессимистичной блокировкой)
        List<TrainingBooking> conflicts = bookingRepository.findConflictingBookings(
                request.getTrainerId(), start, end);
        if (!conflicts.isEmpty()) {
            throw new BookingConflictException("Тренер уже занят в это время");
        }

        // Все проверки пройдены — загружаем сущности и создаём бронирование
        User client = userRepository.findById(clientId)
                .orElseThrow(() -> new ResourceNotFoundException("User", clientId));
        User trainer = userRepository.findById(request.getTrainerId())
                .orElseThrow(() -> new ResourceNotFoundException("Trainer", request.getTrainerId()));

        TrainingBooking booking = TrainingBooking.builder()
                .client(client)
                .trainer(trainer)
                .startDateTime(start)
                .endDateTime(end)
                .status(BookingStatus.CONFIRMED)
                .build();
        booking = bookingRepository.save(booking);

        // Уведомляем тренера о новой тренировке
        notificationService.send(trainer.getId(),
                String.format("Новая тренировка: %s %s в %s", client.getFirstName(), client.getLastName(), start));
        log.info("Booking created: clientId={}, trainerId={}, start={}", clientId, request.getTrainerId(), start);
        return toDto(booking);
    }

    // Отмена бронирования клиентом
    @Transactional
    public void cancelByClient(Long bookingId, Long clientId) {
        TrainingBooking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", bookingId));

        // проверяем, что это бронирование принадлежит текущему клиенту
        if (!booking.getClient().getId().equals(clientId)) {
            throw new BusinessException("Это не ваше бронирование");
        }
        // нельзя отменить уже отменённое
        if (booking.getStatus() == BookingStatus.CANCELLED_BY_CLIENT
                || booking.getStatus() == BookingStatus.CANCELLED_BY_TRAINER) {
            throw new InvalidCancellationException("Бронирование уже отменено");
        }
        // нельзя отменить менее чем за 2 часа до тренировки
        if (LocalDateTime.now().isAfter(booking.getStartDateTime().minusHours(2))) {
            throw new InvalidCancellationException("Отмена невозможна менее чем за 2 часа до тренировки");
        }

        booking.setStatus(BookingStatus.CANCELLED_BY_CLIENT);
        bookingRepository.save(booking);

        // уведомляем тренера об отмене
        notificationService.send(booking.getTrainer().getId(),
                String.format("Клиент %s %s отменил тренировку %s",
                        booking.getClient().getFirstName(), booking.getClient().getLastName(),
                        booking.getStartDateTime()));
        log.info("Booking {} cancelled by client {}", bookingId, clientId);
    }

    // Отмена бронирования тренером
    @Transactional
    public void cancelByTrainer(Long bookingId, Long trainerId, String reason) {
        TrainingBooking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", bookingId));

        // проверяем, что бронирование принадлежит этому тренеру
        if (!booking.getTrainer().getId().equals(trainerId)) {
            throw new BusinessException("Это не ваше бронирование");
        }
        if (booking.getStatus() == BookingStatus.CANCELLED_BY_CLIENT
                || booking.getStatus() == BookingStatus.CANCELLED_BY_TRAINER) {
            throw new InvalidCancellationException("Бронирование уже отменено");
        }

        booking.setStatus(BookingStatus.CANCELLED_BY_TRAINER);
        booking.setCancellationReason(reason); // сохраняем причину отмены
        bookingRepository.save(booking);

        // уведомляем клиента об отмене с указанием причины
        notificationService.send(booking.getClient().getId(),
                String.format("Тренер отменил вашу тренировку %s. Причина: %s",
                        booking.getStartDateTime(), reason));
        log.info("Booking {} cancelled by trainer {}. Reason: {}", bookingId, trainerId, reason);
    }

    // Получить все бронирования клиента (история)
    @Transactional(readOnly = true)
    public List<BookingDto> getClientBookings(Long clientId) {
        return bookingRepository.findByClientIdOrderByStartDesc(clientId)
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    // Получить предстоящие тренировки тренера
    @Transactional(readOnly = true)
    public List<BookingDto> getTrainerUpcomingBookings(Long trainerId) {
        return bookingRepository.findUpcomingByTrainerId(trainerId, LocalDateTime.now())
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    // Получить свободные слоты тренера на дату (часы с 9 до 18)
    @Transactional(readOnly = true)
    public List<LocalDateTime> getFreeSlots(Long trainerId, LocalDate date) {
        // получаем все занятые бронирования за рабочий день тренера (9:00–18:00)
        // используем метод без PESSIMISTIC_WRITE, т.к. read-only транзакция не допускает SELECT FOR UPDATE
        List<TrainingBooking> bookings = bookingRepository.findOccupiedBookings(
                trainerId,
                date.atTime(9, 0),
                date.atTime(18, 0));

        // собираем список занятых начал тренировок
        List<LocalDateTime> booked = bookings.stream()
                .map(TrainingBooking::getStartDateTime)
                .collect(Collectors.toList());

        // перебираем каждый час с 9 до 17 и добавляем незанятые слоты в будущем
        List<LocalDateTime> slots = new java.util.ArrayList<>();
        for (int hour = 9; hour < 18; hour++) {
            LocalDateTime slot = date.atTime(hour, 0);
            if (!booked.contains(slot) && slot.isAfter(LocalDateTime.now())) { // только свободные и будущие
                slots.add(slot);
            }
        }
        return slots;
    }

    // Тренер подтверждает бронирование (если было PENDING)
    @Transactional
    public BookingDto confirmByTrainer(Long bookingId, Long trainerId) {
        TrainingBooking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", bookingId));
        if (!booking.getTrainer().getId().equals(trainerId)) {
            throw new BusinessException("Это не ваше бронирование");
        }
        booking.setStatus(BookingStatus.CONFIRMED);
        bookingRepository.save(booking);
        log.info("Booking {} confirmed by trainer {}", bookingId, trainerId);
        return toDto(booking);
    }

    // Entity TrainingBooking → DTO BookingDto
    private BookingDto toDto(TrainingBooking b) {
        return BookingDto.builder()
                .id(b.getId())
                .clientId(b.getClient().getId())
                .clientName(b.getClient().getFirstName() + " " + b.getClient().getLastName())
                .clientEmail(b.getClient().getEmail())
                .trainerId(b.getTrainer().getId())
                .trainerName(b.getTrainer().getFirstName() + " " + b.getTrainer().getLastName())
                .startDateTime(b.getStartDateTime())
                .endDateTime(b.getEndDateTime())
                .status(b.getStatus())
                .cancellationReason(b.getCancellationReason())
                .createdAt(b.getCreatedAt())
                .build();
    }
}
