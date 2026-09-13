package com.kisansarthi.payment;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {
    Optional<Payment> findByBookingId(UUID bookingId);
    List<Payment> findByFarmerId(UUID farmerId);

    @Query("SELECT COALESCE(SUM(p.netPayableAmount), 0) FROM Payment p WHERE UPPER(p.paymentStatus) = 'COMPLETED'")
    BigDecimal sumSettledPayments();

    @Query("SELECT p.mandi.district, COALESCE(SUM(p.netPayableAmount), 0) FROM Payment p WHERE UPPER(p.paymentStatus) = 'COMPLETED' GROUP BY p.mandi.district")
    List<Object[]> sumSettledPaymentsByDistrict();

    @Query("SELECT p.mandi.id, COALESCE(SUM(p.netPayableAmount), 0) FROM Payment p WHERE UPPER(p.paymentStatus) = 'COMPLETED' GROUP BY p.mandi.id")
    List<Object[]> sumSettledPaymentsByMandi();

    @Query("SELECT p.booking.crop.id, COALESCE(SUM(p.netPayableAmount), 0) FROM Payment p WHERE UPPER(p.paymentStatus) = 'COMPLETED' GROUP BY p.booking.crop.id")
    List<Object[]> sumSettledPaymentsByCrop();

    @Query("SELECT p FROM Payment p " +
           "JOIN FETCH p.farmer f " +
           "JOIN FETCH p.mandi m " +
           "JOIN FETCH p.booking b " +
           "WHERE UPPER(p.paymentStatus) <> 'COMPLETED'")
    List<Payment> findNonCompletedPaymentsWithDetails();
}

