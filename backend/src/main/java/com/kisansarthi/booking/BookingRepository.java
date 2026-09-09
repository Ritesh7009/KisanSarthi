package com.kisansarthi.booking;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BookingRepository extends JpaRepository<Booking, UUID> {
    Optional<Booking> findByIdempotencyKey(String idempotencyKey);
    Optional<Booking> findByTokenNumber(String tokenNumber);
    List<Booking> findByFarmerIdOrderByCreatedAtDesc(UUID farmerId);
    List<Booking> findByMandiIdAndScheduledDateOrderByTokenSequenceAsc(String mandiId, LocalDate date);
    List<Booking> findByMandiIdOrderByCreatedAtDesc(String mandiId);
    List<Booking> findByMandiIdAndStatus(String mandiId, BookingStatus status);
}
