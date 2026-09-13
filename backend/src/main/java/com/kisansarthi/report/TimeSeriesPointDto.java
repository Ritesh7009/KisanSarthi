package com.kisansarthi.report;

import java.math.BigDecimal;

public class TimeSeriesPointDto {
    private String periodLabel; // date or season
    private long bookings;
    private long completedProcurements;
    private BigDecimal certifiedQuantityQuintals;
    private BigDecimal totalPayoutRs;
    private long farmersServed;

    public TimeSeriesPointDto() {}

    public TimeSeriesPointDto(String periodLabel, long bookings, long completedProcurements, BigDecimal certifiedQuantityQuintals, BigDecimal totalPayoutRs, long farmersServed) {
        this.periodLabel = periodLabel;
        this.bookings = bookings;
        this.completedProcurements = completedProcurements;
        this.certifiedQuantityQuintals = certifiedQuantityQuintals != null ? certifiedQuantityQuintals : BigDecimal.ZERO;
        this.totalPayoutRs = totalPayoutRs != null ? totalPayoutRs : BigDecimal.ZERO;
        this.farmersServed = farmersServed;
    }

    public String getPeriodLabel() { return periodLabel; }
    public void setPeriodLabel(String periodLabel) { this.periodLabel = periodLabel; }

    public long getBookings() { return bookings; }
    public void setBookings(long bookings) { this.bookings = bookings; }

    public long getCompletedProcurements() { return completedProcurements; }
    public void setCompletedProcurements(long completedProcurements) { this.completedProcurements = completedProcurements; }

    public BigDecimal getCertifiedQuantityQuintals() { return certifiedQuantityQuintals; }
    public void setCertifiedQuantityQuintals(BigDecimal certifiedQuantityQuintals) { this.certifiedQuantityQuintals = certifiedQuantityQuintals; }

    public BigDecimal getTotalPayoutRs() { return totalPayoutRs; }
    public void setTotalPayoutRs(BigDecimal totalPayoutRs) { this.totalPayoutRs = totalPayoutRs; }

    public long getFarmersServed() { return farmersServed; }
    public void setFarmersServed(long farmersServed) { this.farmersServed = farmersServed; }
}
