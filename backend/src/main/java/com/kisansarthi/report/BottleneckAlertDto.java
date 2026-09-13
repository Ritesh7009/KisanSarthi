package com.kisansarthi.report;

import java.math.BigDecimal;
import java.util.List;

public class BottleneckAlertDto {
    private String mandiId;
    private String mandiName;
    private String district;
    private String severity; // LOW, MEDIUM, CRITICAL
    private String indicator; // QUEUE_CONGESTION, HIGH_WAIT_TIME, CAPACITY_PRESSURE, MOISTURE_SPIKE, PAYMENT_DELAY
    private String metricDescription;
    private String reason;
    private long waitingFarmers;
    private int estimatedWaitMinutes;
    private int remainingSlotCapacityQuintals;

    public BottleneckAlertDto() {}

    public BottleneckAlertDto(
            String mandiId,
            String mandiName,
            String district,
            String severity,
            String indicator,
            String metricDescription,
            String reason,
            long waitingFarmers,
            int estimatedWaitMinutes,
            int remainingSlotCapacityQuintals
    ) {
        this.mandiId = mandiId;
        this.mandiName = mandiName;
        this.district = district;
        this.severity = severity;
        this.indicator = indicator;
        this.metricDescription = metricDescription;
        this.reason = reason;
        this.waitingFarmers = waitingFarmers;
        this.estimatedWaitMinutes = estimatedWaitMinutes;
        this.remainingSlotCapacityQuintals = remainingSlotCapacityQuintals;
    }

    public String getMandiId() { return mandiId; }
    public void setMandiId(String mandiId) { this.mandiId = mandiId; }

    public String getMandiName() { return mandiName; }
    public void setMandiName(String mandiName) { this.mandiName = mandiName; }

    public String getDistrict() { return district; }
    public void setDistrict(String district) { this.district = district; }

    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }

    public String getIndicator() { return indicator; }
    public void setIndicator(String indicator) { this.indicator = indicator; }

    public String getMetricDescription() { return metricDescription; }
    public void setMetricDescription(String metricDescription) { this.metricDescription = metricDescription; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public long getWaitingFarmers() { return waitingFarmers; }
    public void setWaitingFarmers(long waitingFarmers) { this.waitingFarmers = waitingFarmers; }

    public int getEstimatedWaitMinutes() { return estimatedWaitMinutes; }
    public void setEstimatedWaitMinutes(int estimatedWaitMinutes) { this.estimatedWaitMinutes = estimatedWaitMinutes; }

    public int getRemainingSlotCapacityQuintals() { return remainingSlotCapacityQuintals; }
    public void setRemainingSlotCapacityQuintals(int remainingSlotCapacityQuintals) { this.remainingSlotCapacityQuintals = remainingSlotCapacityQuintals; }
}
