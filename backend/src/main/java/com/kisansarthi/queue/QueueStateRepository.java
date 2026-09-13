package com.kisansarthi.queue;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
public interface QueueStateRepository extends JpaRepository<QueueState, String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT q FROM QueueState q WHERE q.mandiId = :mandiId")
    Optional<QueueState> findByIdForUpdate(@Param("mandiId") String mandiId);

    @Transactional
    @Modifying
    @Query("UPDATE QueueState q SET q.currentServingToken = :serving, q.totalTokensGenerated = :total, q.waitingCount = :waiting WHERE q.mandiId = :id")
    void updateCounters(@Param("id") String id, @Param("serving") int serving, @Param("total") int total, @Param("waiting") int waiting);
}
