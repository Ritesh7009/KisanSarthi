package com.kisansarthi.slot;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SlotRepository extends JpaRepository<MandiSlot, String> {
    List<MandiSlot> findByMandiIdOrderByStartTimeAsc(String mandiId);
}
