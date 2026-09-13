package com.kisansarthi.report;

import java.math.BigDecimal;

public class MandiPerformanceDto {
    private String mandiId;
    private String mandiName;
    private String hindiName;
    private String district;
    private long totalBookings;
    private long completedProcurements;
    private BigDecimal certifiedQuantityQuintals;
    private int remainingSlotCapacityQuintals;
    private int averageProcessingMins;
    private int estimatedWaitTimeMins;
    private int currentQueueLength;
    private double throughputPerHour; // completed transactions / operational hours
    private BigDecimal totalDbtAmountRs;
    private String status; // AVAILABLE, BUSY, HIGH_LOAD, FULL, CLOSED
    private String bottleneckReason; // Optional flag explanation

    public MandiPerformanceDto() {}

    // Getters and Setters
    public String getMandiId() { return mandiId; }
    public void setMandiId(String mandiId) { this.mandiId = mandiId; }

    public String getMandiName() { return mandiName; }
    public void setMandiName(String mandiName) { this.mandiName = mandiName; }

    public String getHindiName() { return hindiName; }
    public void setHindiName(String hindiName) { this.hindiName = hindiName; }

    public String getDistrict() { return district; }
    public void setDistrict(String district) { this.district = district; }

    public long getTotalBookings() { return totalBookings; }
    public void setTotalBookings(long totalBookings) { this.totalBookings = totalBookings; }

    public long getCompletedProcurements() { return completedProcurements; }
    public void setCompletedProcurements(long completedProcurements) { this.completedProcurements = completedProcurements; }

    public BigDecimal getCertifiedQuantityQuintals() { return certifiedQuantityQuintals; }
    public void setCertifiedQuantityQuintals(BigDecimal certifiedQuantityQuintals) { this.certifiedQuantityQuintals = certifiedQuantityQuintals; }

    public int getRemainingSlotCapacityQuintals() { return remainingSlotCapacityQuintals; }
    public void setRemainingSlotCapacityQuintals(int remainingSlotCapacityQuintals) { this.remainingSlotCapacityQuintals = remainingSlotCapacityQuintals; }

    public int getAverageProcessingMins() { return averageProcessingMins; }
    public void setAverageProcessingMins(int averageProcessingMins) { this.averageProcessingMins = averageProcessingMins; }

    public int getEstimatedWaitTimeMins() { return estimatedWaitTimeMins; }
    public void setEstimatedWaitTimeMins(int estimatedWaitTimeMins) { this.estimatedWaitTimeMins = estimatedWaitTimeMins; }

    public int getCurrentQueueLength() { return currentQueueLength; }
    public void setCurrentQueueLength(int currentQueueLength) { this.currentQueueLength = currentQueueLength; }

    public double getThroughputPerHour() { return throughputPerHour; }
    public void setThroughputPerHour(double throughputPerHour) { this.throughputPerHour = throughputPerHour; }

    public BigDecimal getTotalDbtAmountRs() { return totalDbtAmountRs; }
    public void setTotalDbtAmountRs(BigDecimal totalDbtAmountRs) { this.totalDbtAmountRs = totalDbtAmountRs; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getBottleneckReason() { return bottleneckReason; }
    public void setBottleneckReason(String bottleneckReason) { this.bottleneckReason = bottleneckReason; }
}
