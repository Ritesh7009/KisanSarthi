package com.kisansarthi.queue;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface QueueEventRepository extends JpaRepository<QueueEvent, UUID> {
    List<QueueEvent> findByMandiIdOrderByCreatedAtDesc(String mandiId);
}
