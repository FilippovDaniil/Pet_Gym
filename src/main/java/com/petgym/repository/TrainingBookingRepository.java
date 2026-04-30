package com.petgym.repository;

import com.petgym.domain.BookingStatus;
import com.petgym.domain.TrainingBooking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;  // аннотация для блокировки строк в БД
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType; // тип блокировки
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TrainingBookingRepository extends JpaRepository<TrainingBooking, Long> {

    // PESSIMISTIC_WRITE — пессимистичная блокировка: SELECT ... FOR UPDATE
    // Блокирует найденные строки до конца транзакции, чтобы два клиента не записались к одному тренеру одновременно
    // Ищем конфликты: тренер занят, если его тренировка пересекается по времени [startTime, endTime]
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM TrainingBooking b WHERE b.trainer.id = :trainerId AND b.status NOT IN ('CANCELLED_BY_CLIENT','CANCELLED_BY_TRAINER') AND b.startDateTime < :endTime AND b.endDateTime > :startTime")
    List<TrainingBooking> findConflictingBookings(@Param("trainerId") Long trainerId,
                                                  @Param("startTime") LocalDateTime startTime,
                                                  @Param("endTime") LocalDateTime endTime);

    // JOIN FETCH — загружаем client и trainer сразу (без N+1 проблемы)
    // предстоящие тренировки конкретного тренера, начиная с указанного момента
    @Query("SELECT b FROM TrainingBooking b JOIN FETCH b.client JOIN FETCH b.trainer WHERE b.trainer.id = :trainerId AND b.startDateTime >= :from ORDER BY b.startDateTime")
    List<TrainingBooking> findUpcomingByTrainerId(@Param("trainerId") Long trainerId, @Param("from") LocalDateTime from);

    // все бронирования клиента в порядке убывания даты (новые первыми)
    @Query("SELECT b FROM TrainingBooking b JOIN FETCH b.client JOIN FETCH b.trainer WHERE b.client.id = :clientId ORDER BY b.startDateTime DESC")
    List<TrainingBooking> findByClientIdOrderByStartDesc(@Param("clientId") Long clientId);

    // считаем, сколько у клиента будущих активных записей к одному тренеру (лимит 2)
    @Query("SELECT COUNT(b) FROM TrainingBooking b WHERE b.client.id = :clientId AND b.trainer.id = :trainerId AND b.status IN ('PENDING','CONFIRMED') AND b.startDateTime > :now")
    long countFutureBookingsByClientAndTrainer(@Param("clientId") Long clientId, @Param("trainerId") Long trainerId, @Param("now") LocalDateTime now);

    // общее число тренировок со статусом CONFIRMED или COMPLETED — для финансового отчёта
    @Query("SELECT COUNT(b) FROM TrainingBooking b WHERE b.status IN ('CONFIRMED','COMPLETED')")
    long countCompletedAndConfirmed();

    // найти бронирования по статусу, созданные раньше указанного момента — используется планировщиком
    List<TrainingBooking> findByStatusAndCreatedAtBefore(BookingStatus status, LocalDateTime before);

    // Без блокировки — только для чтения занятых слотов (используется в getFreeSlots)
    @Query("SELECT b FROM TrainingBooking b WHERE b.trainer.id = :trainerId AND b.status NOT IN ('CANCELLED_BY_CLIENT','CANCELLED_BY_TRAINER') AND b.startDateTime < :endTime AND b.endDateTime > :startTime")
    List<TrainingBooking> findOccupiedBookings(@Param("trainerId") Long trainerId,
                                               @Param("startTime") LocalDateTime startTime,
                                               @Param("endTime") LocalDateTime endTime);

    // DISTINCT — уникальные клиенты (чтобы не дублировать, если бронировали несколько раз)
    // возвращает объекты User, а не TrainingBooking
    @Query("SELECT DISTINCT b.client FROM TrainingBooking b WHERE b.trainer.id = :trainerId")
    List<com.petgym.domain.User> findClientsByTrainerId(@Param("trainerId") Long trainerId);
}
