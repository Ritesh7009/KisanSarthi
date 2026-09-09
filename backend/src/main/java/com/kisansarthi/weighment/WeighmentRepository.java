package com.kisansarthi.weighment;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface WeighmentRepository extends JpaRepository<Weighment, UUID> {
    Optional<Weighment> findByBookingId(UUID bookingId);
}
