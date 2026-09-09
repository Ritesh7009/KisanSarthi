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
}
