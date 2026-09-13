package com.kisansarthi.mandi;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface MandiRepository extends JpaRepository<Mandi, String> {
    List<Mandi> findByDistrictIgnoreCase(String district);

    long countByGateStatusIgnoreCase(String gateStatus);

    @Transactional
    @Modifying(clearAutomatically = true)
    @Query("UPDATE Mandi m SET m.currentTokenServing = 0, m.totalTokensToday = 0, m.activeTokensWaiting = 0 WHERE m.id = :id")
    void resetCounters(@Param("id") String id);

    @Transactional
    @Modifying
    @Query("UPDATE Mandi m SET m.totalTokensToday = m.totalTokensToday + 1, m.activeTokensWaiting = m.activeTokensWaiting + 1 WHERE m.id = :id")
    void incrementTokenCounts(@Param("id") String id);

    @Transactional
    @Modifying
    @Query("UPDATE Mandi m SET m.activeTokensWaiting = CASE WHEN m.activeTokensWaiting > 0 THEN m.activeTokensWaiting - 1 ELSE 0 END WHERE m.id = :id")
    void decrementActiveWaiting(@Param("id") String id);
}

