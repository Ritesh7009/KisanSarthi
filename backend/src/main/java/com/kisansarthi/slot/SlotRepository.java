package com.kisansarthi.slot;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SlotRepository extends JpaRepository<MandiSlot, String> {

    List<MandiSlot> findByMandiIdOrderByStartTimeAsc(String mandiId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM MandiSlot s WHERE s.id = :id")
    Optional<MandiSlot> findByIdForUpdate(@Param("id") String id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM MandiSlot s WHERE s.mandiId = :mandiId AND (s.id = :label OR s.slotLabel = :label OR CONCAT(s.startTime, ' - ', s.endTime) = :label OR s.slotLabel LIKE CONCAT('%', :label, '%'))")
    List<MandiSlot> findByMandiIdAndSlotIdentifierForUpdate(@Param("mandiId") String mandiId, @Param("label") String label);

    Optional<MandiSlot> findFirstByMandiIdAndSlotLabel(String mandiId, String slotLabel);

    Optional<MandiSlot> findFirstByMandiIdAndStartTimeAndEndTime(String mandiId, String startTime, String endTime);

    @Query("SELECT s.mandiId, " +
           "COALESCE(SUM(s.maxCapacityQuintals), 0), " +
           "COALESCE(SUM(s.bookedQuintals), 0), " +
           "COALESCE(SUM(CASE WHEN s.maxCapacityQuintals > s.bookedQuintals THEN (s.maxCapacityQuintals - s.bookedQuintals) ELSE 0 END), 0) " +
           "FROM MandiSlot s " +
           "GROUP BY s.mandiId")
    List<Object[]> aggregateSlotCapacitiesByMandi();
}

