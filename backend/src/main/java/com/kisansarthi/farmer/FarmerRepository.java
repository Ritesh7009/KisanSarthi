package com.kisansarthi.farmer;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FarmerRepository extends JpaRepository<Farmer, UUID> {
    Optional<Farmer> findByPhone(String phone);
    Optional<Farmer> findByKisanId(String kisanId);
    Optional<Farmer> findByUserId(UUID userId);
    boolean existsByPhone(String phone);

    Page<Farmer> findByDistrictIgnoreCase(String district, Pageable pageable);

    @Query("SELECT f FROM Farmer f WHERE (:district IS NULL OR :district = '' OR LOWER(f.district) = LOWER(:district))")
    Page<Farmer> findAllWithOptionalDistrict(@Param("district") String district, Pageable pageable);

    @Query("SELECT f.district, COUNT(f) FROM Farmer f GROUP BY f.district")
    List<Object[]> countFarmersByDistrict();

    @Query("SELECT f.district, COUNT(f) FROM Farmer f WHERE LOWER(f.district) = LOWER(:district) GROUP BY f.district")
    List<Object[]> countFarmersForSingleDistrict(@Param("district") String district);

    long countByDistrictIgnoreCase(String district);
}


