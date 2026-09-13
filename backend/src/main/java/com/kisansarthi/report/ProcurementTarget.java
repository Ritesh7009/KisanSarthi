package com.kisansarthi.report;

import com.kisansarthi.crop.Crop;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "procurement_targets")
public class ProcurementTarget {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 100)
    private String district;

    @Column(name = "hindi_district", length = 100)
    private String hindiDistrict;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "crop_id")
    private Crop crop;

    @Column(nullable = false, length = 20)
    private String season;

    @Column(name = "procurement_year", nullable = false, length = 20)
    private String procurementYear;

    @Column(name = "target_quintals", nullable = false, precision = 12, scale = 2)
    private BigDecimal targetQuintals = BigDecimal.ZERO;

    @Column(name = "warehouse_capacity_quintals", nullable = false, precision = 12, scale = 2)
    private BigDecimal warehouseCapacityQuintals = BigDecimal.ZERO;

    @Column(name = "effective_start", nullable = false)
    private LocalDate effectiveStart;

    @Column(name = "effective_end", nullable = false)
    private LocalDate effectiveEnd;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt = OffsetDateTime.now();

    public ProcurementTarget() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getDistrict() { return district; }
    public void setDistrict(String district) { this.district = district; }

    public String getHindiDistrict() { return hindiDistrict; }
    public void setHindiDistrict(String hindiDistrict) { this.hindiDistrict = hindiDistrict; }

    public Crop getCrop() { return crop; }
    public void setCrop(Crop crop) { this.crop = crop; }

    public String getSeason() { return season; }
    public void setSeason(String season) { this.season = season; }

    public String getProcurementYear() { return procurementYear; }
    public void setProcurementYear(String procurementYear) { this.procurementYear = procurementYear; }

    public BigDecimal getTargetQuintals() { return targetQuintals; }
    public void setTargetQuintals(BigDecimal targetQuintals) { this.targetQuintals = targetQuintals; }

    public BigDecimal getWarehouseCapacityQuintals() { return warehouseCapacityQuintals; }
    public void setWarehouseCapacityQuintals(BigDecimal warehouseCapacityQuintals) { this.warehouseCapacityQuintals = warehouseCapacityQuintals; }

    public LocalDate getEffectiveStart() { return effectiveStart; }
    public void setEffectiveStart(LocalDate effectiveStart) { this.effectiveStart = effectiveStart; }

    public LocalDate getEffectiveEnd() { return effectiveEnd; }
    public void setEffectiveEnd(LocalDate effectiveEnd) { this.effectiveEnd = effectiveEnd; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }

    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
}
