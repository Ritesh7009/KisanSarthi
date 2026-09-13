package com.kisansarthi.report;

import java.math.BigDecimal;

public class CropProcurementReportDto {
    private String cropId;
    private String cropName;
    private String hindiName;
    private String season;
    private long totalBookings;
    private long completedProcurements;
    private BigDecimal certifiedQuantityQuintals;
    private BigDecimal totalProcurementValueRs;
    private long farmersServed;
    private BigDecimal averageQuantityPerFarmerQuintals;
    private BigDecimal totalDbtDisbursedRs;

    public CropProcurementReportDto() {}

    public String getCropId() { return cropId; }
    public void setCropId(String cropId) { this.cropId = cropId; }

    public String getCropName() { return cropName; }
    public void setCropName(String cropName) { this.cropName = cropName; }

    public String getHindiName() { return hindiName; }
    public void setHindiName(String hindiName) { this.hindiName = hindiName; }

    public String getSeason() { return season; }
    public void setSeason(String season) { this.season = season; }

    public long getTotalBookings() { return totalBookings; }
    public void setTotalBookings(long totalBookings) { this.totalBookings = totalBookings; }

    public long getCompletedProcurements() { return completedProcurements; }
    public void setCompletedProcurements(long completedProcurements) { this.completedProcurements = completedProcurements; }

    public BigDecimal getCertifiedQuantityQuintals() { return certifiedQuantityQuintals; }
    public void setCertifiedQuantityQuintals(BigDecimal certifiedQuantityQuintals) { this.certifiedQuantityQuintals = certifiedQuantityQuintals; }

    public BigDecimal getTotalProcurementValueRs() { return totalProcurementValueRs; }
    public void setTotalProcurementValueRs(BigDecimal totalProcurementValueRs) { this.totalProcurementValueRs = totalProcurementValueRs; }

    public long getFarmersServed() { return farmersServed; }
    public void setFarmersServed(long farmersServed) { this.farmersServed = farmersServed; }

    public BigDecimal getAverageQuantityPerFarmerQuintals() { return averageQuantityPerFarmerQuintals; }
    public void setAverageQuantityPerFarmerQuintals(BigDecimal averageQuantityPerFarmerQuintals) { this.averageQuantityPerFarmerQuintals = averageQuantityPerFarmerQuintals; }

    public BigDecimal getTotalDbtDisbursedRs() { return totalDbtDisbursedRs; }
    public void setTotalDbtDisbursedRs(BigDecimal totalDbtDisbursedRs) { this.totalDbtDisbursedRs = totalDbtDisbursedRs; }
}
