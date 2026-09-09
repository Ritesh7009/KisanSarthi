package com.kisansarthi.booking;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public class BookingResponse {
    private UUID id;
    private String tokenNumber;
    private int tokenSequence;
    private UUID farmerId;
    private String farmerName;
    private String farmerPhone;
    private String district;
    private String village;
    private String mandiId;
    private String mandiName;
    private String cropId;
    private String cropName;
    private String slotId;
    private LocalDate scheduledDate;
    private String timeSlot;
    private String vehicleType;
    private String vehicleNumber;
    private BigDecimal estimatedYieldQuintals;
    private BigDecimal acreage;
    private String status;
    private String qrCodeData;
    private BigDecimal netWeightQuintals;
    private BigDecimal moisturePercentage;
    private BigDecimal settlementAmount;
    private Instant createdAt;

    public BookingResponse() {}

    public static BookingResponse fromEntity(Booking b) {
        BookingResponse r = new BookingResponse();
        r.setId(b.getId());
        r.setTokenNumber(b.getTokenNumber());
        r.setTokenSequence(b.getTokenSequence());
        r.setFarmerId(b.getFarmer().getId());
        r.setFarmerName(b.getFarmer().getName());
        r.setFarmerPhone(b.getFarmer().getPhone());
        r.setDistrict(b.getFarmer().getDistrict());
        r.setVillage(b.getFarmer().getVillage());
        r.setMandiId(b.getMandi().getId());
        r.setMandiName(b.getMandi().getName());
        r.setCropId(b.getCrop().getId());
        r.setCropName(b.getCrop().getName());
        r.setSlotId(b.getSlotId());
        r.setScheduledDate(b.getScheduledDate());
        r.setTimeSlot(b.getTimeSlot());
        r.setVehicleType(b.getVehicleType());
        r.setVehicleNumber(b.getVehicleNumber());
        r.setEstimatedYieldQuintals(b.getEstimatedYieldQuintals());
        r.setAcreage(b.getAcreage());
        r.setStatus(b.getStatus().name());
        r.setQrCodeData(b.getQrCodeData());
        r.setNetWeightQuintals(b.getNetWeightQuintals());
        r.setMoisturePercentage(b.getMoisturePercentage());
        r.setSettlementAmount(b.getSettlementAmount());
        r.setCreatedAt(b.getCreatedAt());
        return r;
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getTokenNumber() { return tokenNumber; }
    public void setTokenNumber(String tokenNumber) { this.tokenNumber = tokenNumber; }
    public int getTokenSequence() { return tokenSequence; }
    public void setTokenSequence(int tokenSequence) { this.tokenSequence = tokenSequence; }
    public UUID getFarmerId() { return farmerId; }
    public void setFarmerId(UUID farmerId) { this.farmerId = farmerId; }
    public String getFarmerName() { return farmerName; }
    public void setFarmerName(String farmerName) { this.farmerName = farmerName; }
    public String getFarmerPhone() { return farmerPhone; }
    public void setFarmerPhone(String farmerPhone) { this.farmerPhone = farmerPhone; }
    public String getDistrict() { return district; }
    public void setDistrict(String district) { this.district = district; }
    public String getVillage() { return village; }
    public void setVillage(String village) { this.village = village; }
    public String getMandiId() { return mandiId; }
    public void setMandiId(String mandiId) { this.mandiId = mandiId; }
    public String getMandiName() { return mandiName; }
    public void setMandiName(String mandiName) { this.mandiName = mandiName; }
    public String getCropId() { return cropId; }
    public void setCropId(String cropId) { this.cropId = cropId; }
    public String getCropName() { return cropName; }
    public void setCropName(String cropName) { this.cropName = cropName; }
    public String getSlotId() { return slotId; }
    public void setSlotId(String slotId) { this.slotId = slotId; }
    public LocalDate getScheduledDate() { return scheduledDate; }
    public void setScheduledDate(LocalDate scheduledDate) { this.scheduledDate = scheduledDate; }
    public String getTimeSlot() { return timeSlot; }
    public void setTimeSlot(String timeSlot) { this.timeSlot = timeSlot; }
    public String getVehicleType() { return vehicleType; }
    public void setVehicleType(String vehicleType) { this.vehicleType = vehicleType; }
    public String getVehicleNumber() { return vehicleNumber; }
    public void setVehicleNumber(String vehicleNumber) { this.vehicleNumber = vehicleNumber; }
    public BigDecimal getEstimatedYieldQuintals() { return estimatedYieldQuintals; }
    public void setEstimatedYieldQuintals(BigDecimal estimatedYieldQuintals) { this.estimatedYieldQuintals = estimatedYieldQuintals; }
    public BigDecimal getAcreage() { return acreage; }
    public void setAcreage(BigDecimal acreage) { this.acreage = acreage; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getQrCodeData() { return qrCodeData; }
    public void setQrCodeData(String qrCodeData) { this.qrCodeData = qrCodeData; }
    public BigDecimal getNetWeightQuintals() { return netWeightQuintals; }
    public void setNetWeightQuintals(BigDecimal netWeightQuintals) { this.netWeightQuintals = netWeightQuintals; }
    public BigDecimal getMoisturePercentage() { return moisturePercentage; }
    public void setMoisturePercentage(BigDecimal moisturePercentage) { this.moisturePercentage = moisturePercentage; }
    public BigDecimal getSettlementAmount() { return settlementAmount; }
    public void setSettlementAmount(BigDecimal settlementAmount) { this.settlementAmount = settlementAmount; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
