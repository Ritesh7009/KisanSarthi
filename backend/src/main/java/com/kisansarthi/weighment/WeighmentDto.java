package com.kisansarthi.weighment;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public class WeighmentDto {
    private UUID id;
    private UUID bookingId;
    private String mandiId;
    private String weighbridgeBay;
    private BigDecimal grossWeightQuintals;
    private OffsetDateTime grossWeighedAt;
    private BigDecimal tareWeightQuintals;
    private OffsetDateTime tareWeighedAt;
    private BigDecimal netWeightQuintals;
    private BigDecimal moisturePct;
    private BigDecimal foreignMatterPct;
    private OffsetDateTime createdAt;

    // Additional fields for kg input/output, quality specs, and settlement preview
    private BigDecimal grossWeightKg;
    private BigDecimal tareWeightKg;
    private BigDecimal netWeightKg;
    private BigDecimal moistureLimitPct;
    private String qualityGrade;
    private BigDecimal varianceQuintals;
    private BigDecimal mspRatePerQuintal;
    private BigDecimal grossPayableAmount;
    private BigDecimal deductions;
    private BigDecimal netPayableAmount;
    private String bookingStatus;
    private String tokenNumber;
    private String farmerName;
    private String cropName;
    private String vehicleNumber;

    public WeighmentDto() {}

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getBookingId() {
        return bookingId;
    }

    public void setBookingId(UUID bookingId) {
        this.bookingId = bookingId;
    }

    public String getMandiId() {
        return mandiId;
    }

    public void setMandiId(String mandiId) {
        this.mandiId = mandiId;
    }

    public String getWeighbridgeBay() {
        return weighbridgeBay;
    }

    public void setWeighbridgeBay(String weighbridgeBay) {
        this.weighbridgeBay = weighbridgeBay;
    }

    public BigDecimal getGrossWeightQuintals() {
        return grossWeightQuintals;
    }

    public void setGrossWeightQuintals(BigDecimal grossWeightQuintals) {
        this.grossWeightQuintals = grossWeightQuintals;
    }

    public OffsetDateTime getGrossWeighedAt() {
        return grossWeighedAt;
    }

    public void setGrossWeighedAt(OffsetDateTime grossWeighedAt) {
        this.grossWeighedAt = grossWeighedAt;
    }

    public BigDecimal getTareWeightQuintals() {
        return tareWeightQuintals;
    }

    public void setTareWeightQuintals(BigDecimal tareWeightQuintals) {
        this.tareWeightQuintals = tareWeightQuintals;
    }

    public OffsetDateTime getTareWeighedAt() {
        return tareWeighedAt;
    }

    public void setTareWeighedAt(OffsetDateTime tareWeighedAt) {
        this.tareWeighedAt = tareWeighedAt;
    }

    public BigDecimal getNetWeightQuintals() {
        return netWeightQuintals;
    }

    public void setNetWeightQuintals(BigDecimal netWeightQuintals) {
        this.netWeightQuintals = netWeightQuintals;
    }

    public BigDecimal getMoisturePct() {
        return moisturePct;
    }

    public void setMoisturePct(BigDecimal moisturePct) {
        this.moisturePct = moisturePct;
    }

    public BigDecimal getForeignMatterPct() {
        return foreignMatterPct;
    }

    public void setForeignMatterPct(BigDecimal foreignMatterPct) {
        this.foreignMatterPct = foreignMatterPct;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public BigDecimal getGrossWeightKg() {
        return grossWeightKg;
    }

    public void setGrossWeightKg(BigDecimal grossWeightKg) {
        this.grossWeightKg = grossWeightKg;
    }

    public BigDecimal getTareWeightKg() {
        return tareWeightKg;
    }

    public void setTareWeightKg(BigDecimal tareWeightKg) {
        this.tareWeightKg = tareWeightKg;
    }

    public BigDecimal getNetWeightKg() {
        return netWeightKg;
    }

    public void setNetWeightKg(BigDecimal netWeightKg) {
        this.netWeightKg = netWeightKg;
    }

    public BigDecimal getMoistureLimitPct() {
        return moistureLimitPct;
    }

    public void setMoistureLimitPct(BigDecimal moistureLimitPct) {
        this.moistureLimitPct = moistureLimitPct;
    }

    public String getQualityGrade() {
        return qualityGrade;
    }

    public void setQualityGrade(String qualityGrade) {
        this.qualityGrade = qualityGrade;
    }

    public BigDecimal getVarianceQuintals() {
        return varianceQuintals;
    }

    public void setVarianceQuintals(BigDecimal varianceQuintals) {
        this.varianceQuintals = varianceQuintals;
    }

    public BigDecimal getMspRatePerQuintal() {
        return mspRatePerQuintal;
    }

    public void setMspRatePerQuintal(BigDecimal mspRatePerQuintal) {
        this.mspRatePerQuintal = mspRatePerQuintal;
    }

    public BigDecimal getGrossPayableAmount() {
        return grossPayableAmount;
    }

    public void setGrossPayableAmount(BigDecimal grossPayableAmount) {
        this.grossPayableAmount = grossPayableAmount;
    }

    public BigDecimal getDeductions() {
        return deductions;
    }

    public void setDeductions(BigDecimal deductions) {
        this.deductions = deductions;
    }

    public BigDecimal getNetPayableAmount() {
        return netPayableAmount;
    }

    public void setNetPayableAmount(BigDecimal netPayableAmount) {
        this.netPayableAmount = netPayableAmount;
    }

    public String getBookingStatus() {
        return bookingStatus;
    }

    public void setBookingStatus(String bookingStatus) {
        this.bookingStatus = bookingStatus;
    }

    public String getTokenNumber() {
        return tokenNumber;
    }

    public void setTokenNumber(String tokenNumber) {
        this.tokenNumber = tokenNumber;
    }

    public String getFarmerName() {
        return farmerName;
    }

    public void setFarmerName(String farmerName) {
        this.farmerName = farmerName;
    }

    public String getCropName() {
        return cropName;
    }

    public void setCropName(String cropName) {
        this.cropName = cropName;
    }

    public String getVehicleNumber() {
        return vehicleNumber;
    }

    public void setVehicleNumber(String vehicleNumber) {
        this.vehicleNumber = vehicleNumber;
    }
}
