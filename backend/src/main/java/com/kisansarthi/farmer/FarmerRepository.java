package com.kisansarthi.farmer;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface FarmerRepository extends JpaRepository<Farmer, UUID> {
    Optional<Farmer> findByPhone(String phone);
    Optional<Farmer> findByKisanId(String kisanId);
    Optional<Farmer> findByUserId(UUID userId);
    boolean existsByPhone(String phone);
}
