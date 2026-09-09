package com.kisansarthi.mandi;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "mandis")
public class Mandi {

    @Id
    @Column(length = 50)
    private String id;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(name = "hindi_name", nullable = false, length = 150)
    private String hindiName;

    @Column(nullable = false, length = 100)
    private String district;

    @Column(name = "hindi_district", nullable = false, length = 100)
    private String hindiDistrict;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String address;

    @Column(name = "pin_code", nullable = false, length = 10)
    private String pinCode;

    @Column(name = "open_time", nullable = false, length = 20)
    private String openTime = "08:00 AM";

    @Column(name = "close_time", nullable = false, length = 20)
    private String closeTime = "06:00 PM";

    @Column(name = "weighbridges_count", nullable = false)
    private int weighbridgesCount = 4;

    @Column(name = "daily_capacity_quintals", nullable = false)
    private int dailyCapacityQuintals = 2000;

    @Column(name = "current_token_serving", nullable = false)
    private int currentTokenServing = 0;

    @Column(name = "total_tokens_today", nullable = false)
    private int totalTokensToday = 0;

    @Column(name = "active_tokens_waiting", nullable = false)
    private int activeTokensWaiting = 0;

    @Column(name = "average_processing_mins", nullable = false)
    private int averageProcessingMins = 15;

    @Column(name = "gate_status", nullable = false, length = 30)
    private String gateStatus = "OPEN";

    @Column(length = 20)
    private String phone;

    @Column(nullable = false)
    private double lat;

    @Column(nullable = false)
    private double lng;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public Mandi() {}

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getHindiName() { return hindiName; }
    public void setHindiName(String hindiName) { this.hindiName = hindiName; }
    public String getDistrict() { return district; }
    public void setDistrict(String district) { this.district = district; }
    public String getHindiDistrict() { return hindiDistrict; }
    public void setHindiDistrict(String hindiDistrict) { this.hindiDistrict = hindiDistrict; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getPinCode() { return pinCode; }
    public void setPinCode(String pinCode) { this.pinCode = pinCode; }
    public String getOpenTime() { return openTime; }
    public void setOpenTime(String openTime) { this.openTime = openTime; }
    public String getCloseTime() { return closeTime; }
    public void setCloseTime(String closeTime) { this.closeTime = closeTime; }
    public int getWeighbridgesCount() { return weighbridgesCount; }
    public void setWeighbridgesCount(int weighbridgesCount) { this.weighbridgesCount = weighbridgesCount; }
    public int getDailyCapacityQuintals() { return dailyCapacityQuintals; }
    public void setDailyCapacityQuintals(int dailyCapacityQuintals) { this.dailyCapacityQuintals = dailyCapacityQuintals; }
    public int getCurrentTokenServing() { return currentTokenServing; }
    public void setCurrentTokenServing(int currentTokenServing) { this.currentTokenServing = currentTokenServing; }
    public int getTotalTokensToday() { return totalTokensToday; }
    public void setTotalTokensToday(int totalTokensToday) { this.totalTokensToday = totalTokensToday; }
    public int getActiveTokensWaiting() { return activeTokensWaiting; }
    public void setActiveTokensWaiting(int activeTokensWaiting) { this.activeTokensWaiting = activeTokensWaiting; }
    public int getAverageProcessingMins() { return averageProcessingMins; }
    public void setAverageProcessingMins(int averageProcessingMins) { this.averageProcessingMins = averageProcessingMins; }
    public String getGateStatus() { return gateStatus; }
    public void setGateStatus(String gateStatus) { this.gateStatus = gateStatus; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public double getLat() { return lat; }
    public void setLat(double lat) { this.lat = lat; }
    public double getLng() { return lng; }
    public void setLng(double lng) { this.lng = lng; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
