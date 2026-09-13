package com.kisansarthi.weighment;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WeighmentRepository extends JpaRepository<Weighment, UUID> {
    Optional<Weighment> findByBookingId(UUID bookingId);

    @Query("SELECT w FROM Weighment w WHERE w.booking.id IN :bookingIds")
    List<Weighment> findByBookingIdIn(@Param("bookingIds") Collection<UUID> bookingIds);

    @Query("SELECT COUNT(w), " +
           "COALESCE(SUM(w.grossWeightQuintals), 0), " +
           "COALESCE(SUM(w.tareWeightQuintals), 0), " +
           "COALESCE(SUM(w.netWeightQuintals), 0), " +
           "COALESCE(AVG(w.moisturePct), 0), " +
           "COALESCE(MIN(w.moisturePct), 0), " +
           "COALESCE(MAX(w.moisturePct), 0), " +
           "COALESCE(AVG(w.foreignMatterPct), 0), " +
           "COALESCE(SUM(w.netWeightQuintals * w.foreignMatterPct / 100.0), 0) " +
           "FROM Weighment w")
    List<Object[]> getWeighmentSummaryMetrics();

    @Query("SELECT " +
           "COUNT(w), " +
           "COUNT(CASE WHEN w.moisturePct > COALESCE(c.moistureLimitPct, 12.0) THEN 1 END), " +
           "COUNT(CASE WHEN w.moisturePct <= COALESCE(c.moistureLimitPct, 12.0) THEN 1 END) " +
           "FROM Weighment w " +
           "LEFT JOIN w.booking b " +
           "LEFT JOIN b.crop c " +
           "WHERE w.moisturePct IS NOT NULL")
    List<Object[]> getMoistureComplianceStats();
}



