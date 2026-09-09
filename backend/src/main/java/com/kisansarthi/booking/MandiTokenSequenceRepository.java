package com.kisansarthi.booking;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface MandiTokenSequenceRepository extends JpaRepository<MandiTokenSequence, MandiTokenSequence.TokenSequenceId> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM MandiTokenSequence s WHERE s.mandiId = :mandiId AND s.procurementDate = :date")
    Optional<MandiTokenSequence> findByMandiIdAndProcurementDateForUpdate(
            @Param("mandiId") String mandiId,
            @Param("date") LocalDate date);
}
