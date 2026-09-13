package com.kisansarthi.report;

import java.math.BigDecimal;
import java.util.List;

public class QualityAndWeighmentReportDto {
    // Weighment Metrics
    private long totalVehiclesWeighed;
    private BigDecimal totalGrossQuintals;
    private BigDecimal totalTareQuintals;
    private BigDecimal totalCertifiedNetQuintals;
    private BigDecimal averageNetQuintalsPerVehicle;

    // Moisture & QC Metrics
    private BigDecimal averageMoisturePct;
    private BigDecimal minMoisturePct;
    private BigDecimal maxMoisturePct;
    private long samplesWithinFaqThreshold;
    private long samplesAboveFaqThreshold;
    private double percentAboveFaqThreshold;

    // Quality / Dockage Metrics
    private BigDecimal averageForeignMatterPct;
    private BigDecimal totalDockageQuintals;
    private double dockagePercentage;

    public QualityAndWeighmentReportDto() {}

    // Getters and Setters
    public long getTotalVehiclesWeighed() { return totalVehiclesWeighed; }
    public void setTotalVehiclesWeighed(long totalVehiclesWeighed) { this.totalVehiclesWeighed = totalVehiclesWeighed; }

    public BigDecimal getTotalGrossQuintals() { return totalGrossQuintals; }
    public void setTotalGrossQuintals(BigDecimal totalGrossQuintals) { this.totalGrossQuintals = totalGrossQuintals; }

    public BigDecimal getTotalTareQuintals() { return totalTareQuintals; }
    public void setTotalTareQuintals(BigDecimal totalTareQuintals) { this.totalTareQuintals = totalTareQuintals; }

    public BigDecimal getTotalCertifiedNetQuintals() { return totalCertifiedNetQuintals; }
    public void setTotalCertifiedNetQuintals(BigDecimal totalCertifiedNetQuintals) { this.totalCertifiedNetQuintals = totalCertifiedNetQuintals; }

    public BigDecimal getAverageNetQuintalsPerVehicle() { return averageNetQuintalsPerVehicle; }
    public void setAverageNetQuintalsPerVehicle(BigDecimal averageNetQuintalsPerVehicle) { this.averageNetQuintalsPerVehicle = averageNetQuintalsPerVehicle; }

    public BigDecimal getAverageMoisturePct() { return averageMoisturePct; }
    public void setAverageMoisturePct(BigDecimal averageMoisturePct) { this.averageMoisturePct = averageMoisturePct; }

    public BigDecimal getMinMoisturePct() { return minMoisturePct; }
    public void setMinMoisturePct(BigDecimal minMoisturePct) { this.minMoisturePct = minMoisturePct; }

    public BigDecimal getMaxMoisturePct() { return maxMoisturePct; }
    public void setMaxMoisturePct(BigDecimal maxMoisturePct) { this.maxMoisturePct = maxMoisturePct; }

    public long getSamplesWithinFaqThreshold() { return samplesWithinFaqThreshold; }
    public void setSamplesWithinFaqThreshold(long samplesWithinFaqThreshold) { this.samplesWithinFaqThreshold = samplesWithinFaqThreshold; }

    public long getSamplesAboveFaqThreshold() { return samplesAboveFaqThreshold; }
    public void setSamplesAboveFaqThreshold(long samplesAboveFaqThreshold) { this.samplesAboveFaqThreshold = samplesAboveFaqThreshold; }

    public double getPercentAboveFaqThreshold() { return percentAboveFaqThreshold; }
    public void setPercentAboveFaqThreshold(double percentAboveFaqThreshold) { this.percentAboveFaqThreshold = percentAboveFaqThreshold; }

    public BigDecimal getAverageForeignMatterPct() { return averageForeignMatterPct; }
    public void setAverageForeignMatterPct(BigDecimal averageForeignMatterPct) { this.averageForeignMatterPct = averageForeignMatterPct; }

    public BigDecimal getTotalDockageQuintals() { return totalDockageQuintals; }
    public void setTotalDockageQuintals(BigDecimal totalDockageQuintals) { this.totalDockageQuintals = totalDockageQuintals; }

    public double getDockagePercentage() { return dockagePercentage; }
    public void setDockagePercentage(double dockagePercentage) { this.dockagePercentage = dockagePercentage; }
}
