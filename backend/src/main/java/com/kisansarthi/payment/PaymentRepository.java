package com.kisansarthi.payment;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {
    Optional<Payment> findByBookingId(UUID bookingId);
    List<Payment> findByFarmerId(UUID farmerId);

    @Query("SELECT p FROM Payment p WHERE p.booking.id IN :bookingIds")
    List<Payment> findByBookingIdIn(@Param("bookingIds") Collection<UUID> bookingIds);

    @Query("SELECT COALESCE(SUM(p.netPayableAmount), 0) FROM Payment p WHERE UPPER(p.paymentStatus) = 'COMPLETED'")
    BigDecimal sumSettledPayments();

    @Query("SELECT p.mandi.district, COALESCE(SUM(p.netPayableAmount), 0) FROM Payment p WHERE UPPER(p.paymentStatus) = 'COMPLETED' GROUP BY p.mandi.district")
    List<Object[]> sumSettledPaymentsByDistrict();

    @Query("SELECT p.mandi.id, COALESCE(SUM(p.netPayableAmount), 0) FROM Payment p WHERE UPPER(p.paymentStatus) = 'COMPLETED' GROUP BY p.mandi.id")
    List<Object[]> sumSettledPaymentsByMandi();

    @Query("SELECT p.booking.crop.id, COALESCE(SUM(p.netPayableAmount), 0) FROM Payment p WHERE UPPER(p.paymentStatus) = 'COMPLETED' GROUP BY p.booking.crop.id")
    List<Object[]> sumSettledPaymentsByCrop();

    @Query("SELECT p.paymentStatus, COUNT(p), COALESCE(SUM(p.netPayableAmount), 0) " +
           "FROM Payment p " +
           "GROUP BY p.paymentStatus")
    List<Object[]> aggregatePaymentCountsAndAmountsByStatus();

    @Query("SELECT p FROM Payment p " +
           "JOIN FETCH p.farmer f " +
           "JOIN FETCH p.mandi m " +
           "JOIN FETCH p.booking b " +
           "WHERE UPPER(p.paymentStatus) <> 'COMPLETED' " +
           "AND COALESCE(p.initiatedAt, p.createdAt) <= :cutoffTime")
    List<Payment> findDelayedPaymentsWithDetails(@Param("cutoffTime") OffsetDateTime cutoffTime);

    @Query("SELECT p.initiatedAt, p.creditedAt " +
           "FROM Payment p " +
           "WHERE UPPER(p.paymentStatus) = 'COMPLETED' " +
           "AND p.initiatedAt IS NOT NULL " +
           "AND p.creditedAt IS NOT NULL")
    List<Object[]> findCompletedPaymentSettlementTimestamps();
}



