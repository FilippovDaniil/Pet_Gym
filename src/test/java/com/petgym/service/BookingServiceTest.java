package com.petgym.service;

import com.petgym.domain.*;
import com.petgym.dto.BookingDto;
import com.petgym.dto.CreateBookingRequest;
import com.petgym.exception.BookingConflictException;
import com.petgym.exception.InvalidCancellationException;
import com.petgym.exception.MembershipExpiredException;
import com.petgym.repository.TrainingBookingRepository;
import com.petgym.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock private TrainingBookingRepository bookingRepository;
    @Mock private UserRepository userRepository;
    @Mock private MembershipService membershipService;
    @Mock private NotificationService notificationService;

    @InjectMocks private BookingService bookingService;

    private User client;
    private User trainer;

    @BeforeEach
    void setUp() {
        client = User.builder().id(1L).email("client@test.com").firstName("Иван").lastName("Иванов")
                .role(Role.CLIENT).enabled(true).build();
        trainer = User.builder().id(2L).email("trainer@test.com").firstName("Алексей").lastName("Тренеров")
                .role(Role.TRAINER).enabled(true).build();
    }

    @Test
    void createBooking_success() {
        LocalDateTime start = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0).withSecond(0).withNano(0);
        CreateBookingRequest req = new CreateBookingRequest();
        req.setTrainerId(2L);
        req.setStartDateTime(start);

        when(membershipService.hasActiveMembership(eq(1L), any())).thenReturn(true);
        when(bookingRepository.countFutureBookingsByClientAndTrainer(anyLong(), anyLong(), any())).thenReturn(0L);
        when(bookingRepository.findConflictingBookings(anyLong(), any(), any())).thenReturn(List.of());
        when(userRepository.findById(1L)).thenReturn(Optional.of(client));
        when(userRepository.findById(2L)).thenReturn(Optional.of(trainer));
        when(bookingRepository.save(any())).thenAnswer(inv -> {
            TrainingBooking b = inv.getArgument(0);
            b = TrainingBooking.builder().id(1L).client(client).trainer(trainer)
                    .startDateTime(b.getStartDateTime()).endDateTime(b.getEndDateTime())
                    .status(BookingStatus.CONFIRMED).build();
            return b;
        });

        BookingDto result = bookingService.createBooking(1L, req);

        assertNotNull(result);
        assertEquals(BookingStatus.CONFIRMED, result.getStatus());
        assertEquals(start, result.getStartDateTime());
    }

    @Test
    void createBooking_noMembership_throwsException() {
        LocalDateTime start = LocalDateTime.now().plusDays(1);
        CreateBookingRequest req = new CreateBookingRequest();
        req.setTrainerId(2L);
        req.setStartDateTime(start);

        when(membershipService.hasActiveMembership(anyLong(), any())).thenReturn(false);

        assertThrows(MembershipExpiredException.class, () -> bookingService.createBooking(1L, req));
    }

    @Test
    void createBooking_conflict_throwsException() {
        LocalDateTime start = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0).withSecond(0).withNano(0);
        CreateBookingRequest req = new CreateBookingRequest();
        req.setTrainerId(2L);
        req.setStartDateTime(start);

        when(membershipService.hasActiveMembership(anyLong(), any())).thenReturn(true);
        when(bookingRepository.countFutureBookingsByClientAndTrainer(anyLong(), anyLong(), any())).thenReturn(0L);
        when(bookingRepository.findConflictingBookings(anyLong(), any(), any()))
                .thenReturn(List.of(new TrainingBooking()));

        assertThrows(BookingConflictException.class, () -> bookingService.createBooking(1L, req));
    }

    @Test
    void cancelByClient_tooLate_throwsException() {
        TrainingBooking booking = TrainingBooking.builder()
                .id(1L).client(client).trainer(trainer)
                .startDateTime(LocalDateTime.now().plusMinutes(30))
                .status(BookingStatus.CONFIRMED).build();

        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));

        assertThrows(InvalidCancellationException.class, () -> bookingService.cancelByClient(1L, 1L));
    }
}
