package com.kisansarthi.report;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProcurementTargetRepository extends JpaRepository<ProcurementTarget, UUID> {
    List<ProcurementTarget> findByDistrictIgnoreCase(String district);
    List<ProcurementTarget> findByProcurementYearAndSeason(String procurementYear, String season);
    Optional<ProcurementTarget> findFirstByDistrictIgnoreCaseOrderByEffectiveEndDesc(String district);
}
