package com.kisansarthi.crop;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "crops")
public class Crop {

    @Id
    @Column(length = 50)
    private String id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "hindi_name", nullable = false, length = 100)
    private String hindiName;

    @Column(name = "malwi_name", length = 100)
    private String malwiName;

    @Column(nullable = false, length = 20)
    private String season;

    @Column(name = "standard_msp_per_quintal", nullable = false)
    private BigDecimal standardMspPerQuintal;

    @Column(name = "mp_bonus_per_quintal", nullable = false)
    private BigDecimal mpBonusPerQuintal = BigDecimal.ZERO;

    @Column(name = "total_msp", nullable = false)
    private BigDecimal totalMsp;

    @Column(name = "market_price_per_quintal", nullable = false)
    private BigDecimal marketPricePerQuintal;

    @Column(name = "typical_cost_per_acre", nullable = false)
    private BigDecimal typicalCostPerAcre = BigDecimal.ZERO;

    @Column(name = "average_yield_per_acre_quintal", nullable = false)
    private BigDecimal averageYieldPerAcreQuintal = BigDecimal.ZERO;

    @Column(name = "moisture_limit_pct", nullable = false)
    private BigDecimal moistureLimitPct = new BigDecimal("12.00");

    @Column(name = "grade_specs", columnDefinition = "TEXT")
    private String gradeSpecs;

    @Column(length = 50)
    private String icon;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public Crop() {}

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getHindiName() { return hindiName; }
    public void setHindiName(String hindiName) { this.hindiName = hindiName; }
    public String getMalwiName() { return malwiName; }
    public void setMalwiName(String malwiName) { this.malwiName = malwiName; }
    public String getSeason() { return season; }
    public void setSeason(String season) { this.season = season; }
    public BigDecimal getStandardMspPerQuintal() { return standardMspPerQuintal; }
    public void setStandardMspPerQuintal(BigDecimal standardMspPerQuintal) { this.standardMspPerQuintal = standardMspPerQuintal; }
    public BigDecimal getMpBonusPerQuintal() { return mpBonusPerQuintal; }
    public void setMpBonusPerQuintal(BigDecimal mpBonusPerQuintal) { this.mpBonusPerQuintal = mpBonusPerQuintal; }
    public BigDecimal getTotalMsp() { return totalMsp; }
    public void setTotalMsp(BigDecimal totalMsp) { this.totalMsp = totalMsp; }
    public BigDecimal getMarketPricePerQuintal() { return marketPricePerQuintal; }
    public void setMarketPricePerQuintal(BigDecimal marketPricePerQuintal) { this.marketPricePerQuintal = marketPricePerQuintal; }
    public BigDecimal getTypicalCostPerAcre() { return typicalCostPerAcre; }
    public void setTypicalCostPerAcre(BigDecimal typicalCostPerAcre) { this.typicalCostPerAcre = typicalCostPerAcre; }
    public BigDecimal getAverageYieldPerAcreQuintal() { return averageYieldPerAcreQuintal; }
    public void setAverageYieldPerAcreQuintal(BigDecimal averageYieldPerAcreQuintal) { this.averageYieldPerAcreQuintal = averageYieldPerAcreQuintal; }
    public BigDecimal getMoistureLimitPct() { return moistureLimitPct; }
    public void setMoistureLimitPct(BigDecimal moistureLimitPct) { this.moistureLimitPct = moistureLimitPct; }
    public String getGradeSpecs() { return gradeSpecs; }
    public void setGradeSpecs(String gradeSpecs) { this.gradeSpecs = gradeSpecs; }
    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
