package com.petgym.repository;

import com.petgym.domain.BookingStatus;
import com.petgym.domain.TrainingBooking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TrainingBookingRepository extends JpaRepository<TrainingBooking, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM TrainingBooking b WHERE b.trainer.id = :trainerId AND b.status NOT IN ('CANCELLED_BY_CLIENT','CANCELLED_BY_TRAINER') AND b.startDateTime < :endTime AND b.endDateTime > :startTime")
    List<TrainingBooking> findConflictingBookings(@Param("trainerId") Long trainerId,
                                                  @Param("startTime") LocalDateTime startTime,
                                                  @Param("endTime") LocalDateTime endTime);

    @Query("SELECT b FROM TrainingBooking b JOIN FETCH b.client JOIN FETCH b.trainer WHERE b.trainer.id = :trainerId AND b.startDateTime >= :from ORDER BY b.startDateTime")
    List<TrainingBooking> findUpcomingByTrainerId(@Param("trainerId") Long trainerId, @Param("from") LocalDateTime from);

    @Query("SELECT b FROM TrainingBooking b JOIN FETCH b.client JOIN FETCH b.trainer WHERE b.client.id = :clientId ORDER BY b.startDateTime DESC")
    List<TrainingBooking> findByClientIdOrderByStartDesc(@Param("clientId") Long clientId);

    @Query("SELECT COUNT(b) FROM TrainingBooking b WHERE b.client.id = :clientId AND b.trainer.id = :trainerId AND b.status IN ('PENDING','CONFIRMED') AND b.startDateTime > :now")
    long countFutureBookingsByClientAndTrainer(@Param("clientId") Long clientId, @Param("trainerId") Long trainerId, @Param("now") LocalDateTime now);

    @Query("SELECT COUNT(b) FROM TrainingBooking b WHERE b.status IN ('CONFIRMED','COMPLETED')")
    long countCompletedAndConfirmed();

    List<TrainingBooking> findByStatusAndCreatedAtBefore(BookingStatus status, LocalDateTime before);

    @Query("SELECT DISTINCT b.client FROM TrainingBooking b WHERE b.trainer.id = :trainerId")
    List<com.petgym.domain.User> findClientsByTrainerId(@Param("trainerId") Long trainerId);
}
