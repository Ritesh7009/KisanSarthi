package com.kisansarthi.booking;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
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

    // ==========================================
    // HIGH PERFORMANCE NATIVE / JPQL REPORT AGGREGATIONS
    // ==========================================

    @Query("SELECT COUNT(b) FROM Booking b WHERE b.status IN :statuses")
    long countByStatusIn(@Param("statuses") Collection<BookingStatus> statuses);

    @Query("SELECT COALESCE(SUM(b.netWeightQuintals), 0) FROM Booking b WHERE b.status IN :statuses")
    BigDecimal sumNetWeightByStatusIn(@Param("statuses") Collection<BookingStatus> statuses);

    @Query("SELECT COALESCE(SUM(b.settlementAmount), 0) FROM Booking b WHERE b.status IN :statuses")
    BigDecimal sumSettlementAmountByStatusIn(@Param("statuses") Collection<BookingStatus> statuses);

    @Query("SELECT COUNT(DISTINCT b.farmer.id) FROM Booking b WHERE b.status IN :statuses")
    long countDistinctFarmersByStatusIn(@Param("statuses") Collection<BookingStatus> statuses);

    @Query("SELECT b.status, COUNT(b) FROM Booking b GROUP BY b.status")
    List<Object[]> countGroupedByStatus();

    @Query("SELECT b.mandi.id, COUNT(b), " +
           "COUNT(CASE WHEN b.status IN :completedStatuses THEN 1 END), " +
           "COALESCE(SUM(CASE WHEN b.status IN :completedStatuses THEN b.netWeightQuintals ELSE 0 END), 0) " +
           "FROM Booking b GROUP BY b.mandi.id")
    List<Object[]> aggregateProcurementByMandi(@Param("completedStatuses") Collection<BookingStatus> completedStatuses);

    @Query("SELECT b.crop.id, COUNT(b), " +
           "COUNT(CASE WHEN b.status IN :completedStatuses THEN 1 END), " +
           "COALESCE(SUM(CASE WHEN b.status IN :completedStatuses THEN b.netWeightQuintals ELSE 0 END), 0), " +
           "COALESCE(SUM(CASE WHEN b.status IN :completedStatuses THEN b.settlementAmount ELSE 0 END), 0), " +
           "COUNT(DISTINCT CASE WHEN b.status IN :completedStatuses THEN b.farmer.id END) " +
           "FROM Booking b GROUP BY b.crop.id")
    List<Object[]> aggregateProcurementByCrop(@Param("completedStatuses") Collection<BookingStatus> completedStatuses);

    @Query("SELECT b.mandi.district, COUNT(b), " +
           "COUNT(CASE WHEN b.status IN :completedStatuses THEN 1 END), " +
           "COALESCE(SUM(CASE WHEN b.status IN :completedStatuses THEN b.netWeightQuintals ELSE 0 END), 0), " +
           "COALESCE(SUM(CASE WHEN b.status IN :completedStatuses THEN b.settlementAmount ELSE 0 END), 0), " +
           "COUNT(DISTINCT CASE WHEN b.status IN :completedStatuses THEN b.farmer.id END) " +
           "FROM Booking b GROUP BY b.mandi.district")
    List<Object[]> aggregateProcurementByDistrict(@Param("completedStatuses") Collection<BookingStatus> completedStatuses);

    @Query("SELECT b.mandi.district, COALESCE(SUM(b.estimatedYieldQuintals), 0) " +
           "FROM Booking b WHERE b.scheduledDate = :today GROUP BY b.mandi.district")
    List<Object[]> sumEstimatedYieldTodayByDistrict(@Param("today") LocalDate today);

    @Query("SELECT b.scheduledDate, COUNT(b), " +
           "COUNT(CASE WHEN b.status IN :completedStatuses THEN 1 END), " +
           "COALESCE(SUM(CASE WHEN b.status IN :completedStatuses THEN b.netWeightQuintals ELSE 0 END), 0), " +
           "COALESCE(SUM(CASE WHEN b.status IN :completedStatuses THEN b.settlementAmount ELSE 0 END), 0), " +
           "COUNT(DISTINCT CASE WHEN b.status IN :completedStatuses THEN b.farmer.id END) " +
           "FROM Booking b WHERE b.scheduledDate >= :startDate AND b.scheduledDate <= :endDate " +
           "GROUP BY b.scheduledDate ORDER BY b.scheduledDate ASC")
    List<Object[]> aggregateDailyTimeSeries(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("completedStatuses") Collection<BookingStatus> completedStatuses);

    @Query(value = "SELECT b FROM Booking b " +
           "JOIN FETCH b.farmer f " +
           "JOIN FETCH b.mandi m " +
           "JOIN FETCH b.crop c " +
           "WHERE (:district IS NULL OR :district = '' OR LOWER(m.district) = LOWER(:district)) " +
           "AND (:mandiId IS NULL OR :mandiId = '' OR m.id = :mandiId) " +
           "AND (:cropId IS NULL OR :cropId = '' OR c.id = :cropId) " +
           "AND (:search IS NULL OR :search = '' OR (" +
           "   LOWER(b.tokenNumber) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "   LOWER(f.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "   LOWER(f.phone) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "   LOWER(m.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "   LOWER(c.name) LIKE LOWER(CONCAT('%', :search, '%'))" +
           ")) " +
           "ORDER BY b.scheduledDate DESC, b.createdAt DESC, b.id DESC",
           countQuery = "SELECT COUNT(b) FROM Booking b " +
           "WHERE (:district IS NULL OR :district = '' OR LOWER(b.mandi.district) = LOWER(:district)) " +
           "AND (:mandiId IS NULL OR :mandiId = '' OR b.mandi.id = :mandiId) " +
           "AND (:cropId IS NULL OR :cropId = '' OR b.crop.id = :cropId) " +
           "AND (:search IS NULL OR :search = '' OR (" +
           "   LOWER(b.tokenNumber) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "   LOWER(b.farmer.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "   LOWER(b.farmer.phone) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "   LOWER(b.mandi.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "   LOWER(b.crop.name) LIKE LOWER(CONCAT('%', :search, '%'))" +
           "))")
    Page<Booking> findFilteredForRegisterPageable(
            @Param("district") String district,
            @Param("mandiId") String mandiId,
            @Param("cropId") String cropId,
            @Param("search") String search,
            Pageable pageable);

    @Query("SELECT b FROM Booking b " +
           "JOIN FETCH b.farmer f " +
           "JOIN FETCH b.mandi m " +
           "JOIN FETCH b.crop c " +
           "WHERE (:district IS NULL OR :district = '' OR LOWER(m.district) = LOWER(:district)) " +
           "AND (:mandiId IS NULL OR :mandiId = '' OR m.id = :mandiId) " +
           "AND (:cropId IS NULL OR :cropId = '' OR c.id = :cropId) " +
           "AND (:search IS NULL OR :search = '' OR (" +
           "   LOWER(b.tokenNumber) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "   LOWER(f.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "   LOWER(f.phone) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "   LOWER(m.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "   LOWER(c.name) LIKE LOWER(CONCAT('%', :search, '%'))" +
           ")) " +
           "ORDER BY b.scheduledDate DESC, b.createdAt DESC, b.id DESC")
    List<Booking> findFilteredForRegister(
            @Param("district") String district,
            @Param("mandiId") String mandiId,
            @Param("cropId") String cropId,
            @Param("search") String search);
}


