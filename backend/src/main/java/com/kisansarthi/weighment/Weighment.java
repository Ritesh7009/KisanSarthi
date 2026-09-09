package com.kisansarthi.weighment;

import com.kisansarthi.auth.User;
import com.kisansarthi.booking.Booking;
import com.kisansarthi.mandi.Mandi;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "weighments")
public class Weighment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "booking_id", nullable = false, unique = true)
    private Booking booking;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "mandi_id", nullable = false)
    private Mandi mandi;

    @Column(name = "weighbridge_bay", nullable = false, length = 20)
    private String weighbridgeBay = "Kanta Bay 1";

    @Column(name = "gross_weight_quintals", nullable = false, precision = 8, scale = 2)
    private BigDecimal grossWeightQuintals;

    @Column(name = "gross_weighed_at", nullable = false)
    private OffsetDateTime grossWeighedAt = OffsetDateTime.now();

    @Column(name = "tare_weight_quintals", precision = 8, scale = 2)
    private BigDecimal tareWeightQuintals;

    @Column(name = "tare_weighed_at")
    private OffsetDateTime tareWeighedAt;

    @Column(name = "net_weight_quintals", precision = 8, scale = 2)
    private BigDecimal netWeightQuintals;

    @Column(name = "moisture_pct", precision = 5, scale = 2)
    private BigDecimal moisturePct;

    @Column(name = "foreign_matter_pct", precision = 5, scale = 2)
    private BigDecimal foreignMatterPct;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "weighbridge_operator_id")
    private User weighbridgeOperator;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt = OffsetDateTime.now();

    public Weighment() {}

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Booking getBooking() {
        return booking;
    }

    public void setBooking(Booking booking) {
        this.booking = booking;
    }

    public Mandi getMandi() {
        return mandi;
    }

    public void setMandi(Mandi mandi) {
        this.mandi = mandi;
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

    public User getWeighbridgeOperator() {
        return weighbridgeOperator;
    }

    public void setWeighbridgeOperator(User weighbridgeOperator) {
        this.weighbridgeOperator = weighbridgeOperator;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
