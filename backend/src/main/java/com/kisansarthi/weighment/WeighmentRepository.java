package com.kisansarthi.weighment;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WeighmentRepository extends JpaRepository<Weighment, UUID> {
    Optional<Weighment> findByBookingId(UUID bookingId);

    @Query("SELECT COUNT(w), " +
           "COALESCE(SUM(w.grossWeightQuintals), 0), " +
           "COALESCE(SUM(w.tareWeightQuintals), 0), " +
           "COALESCE(SUM(w.netWeightQuintals), 0), " +
           "COALESCE(AVG(w.moisturePct), 0), " +
           "COALESCE(MIN(w.moisturePct), 0), " +
           "COALESCE(MAX(w.moisturePct), 0), " +
           "COALESCE(AVG(w.foreignMatterPct), 0) " +
           "FROM Weighment w")
    List<Object[]> getWeighmentSummaryMetrics();

    @Query("SELECT w.moisturePct, w.booking.crop.id FROM Weighment w WHERE w.moisturePct IS NOT NULL")
    List<Object[]> findMoistureAndCropPairs();
}

