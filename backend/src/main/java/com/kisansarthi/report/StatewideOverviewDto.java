package com.kisansarthi.report;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public class StatewideOverviewDto {
    private long totalBookings;
    private long totalCompletedProcurements;
    private BigDecimal totalCertifiedQuantityQuintals;
    private BigDecimal totalProcurementValueRs;
    private BigDecimal totalDbtDisbursedRs;
    private long totalFarmersServed;
    private long totalActiveMandis;
    private long totalWaitingFarmers;
    private Map<String, Long> statusBreakdown;

    public StatewideOverviewDto() {}

    public StatewideOverviewDto(
            long totalBookings,
            long totalCompletedProcurements,
            BigDecimal totalCertifiedQuantityQuintals,
            BigDecimal totalProcurementValueRs,
            BigDecimal totalDbtDisbursedRs,
            long totalFarmersServed,
            long totalActiveMandis,
            long totalWaitingFarmers,
            Map<String, Long> statusBreakdown
    ) {
        this.totalBookings = totalBookings;
        this.totalCompletedProcurements = totalCompletedProcurements;
        this.totalCertifiedQuantityQuintals = totalCertifiedQuantityQuintals != null ? totalCertifiedQuantityQuintals : BigDecimal.ZERO;
        this.totalProcurementValueRs = totalProcurementValueRs != null ? totalProcurementValueRs : BigDecimal.ZERO;
        this.totalDbtDisbursedRs = totalDbtDisbursedRs != null ? totalDbtDisbursedRs : BigDecimal.ZERO;
        this.totalFarmersServed = totalFarmersServed;
        this.totalActiveMandis = totalActiveMandis;
        this.totalWaitingFarmers = totalWaitingFarmers;
        this.statusBreakdown = statusBreakdown;
    }

    public long getTotalBookings() { return totalBookings; }
    public void setTotalBookings(long totalBookings) { this.totalBookings = totalBookings; }

    public long getTotalCompletedProcurements() { return totalCompletedProcurements; }
    public void setTotalCompletedProcurements(long totalCompletedProcurements) { this.totalCompletedProcurements = totalCompletedProcurements; }

    public BigDecimal getTotalCertifiedQuantityQuintals() { return totalCertifiedQuantityQuintals; }
    public void setTotalCertifiedQuantityQuintals(BigDecimal totalCertifiedQuantityQuintals) { this.totalCertifiedQuantityQuintals = totalCertifiedQuantityQuintals; }

    public BigDecimal getTotalProcurementValueRs() { return totalProcurementValueRs; }
    public void setTotalProcurementValueRs(BigDecimal totalProcurementValueRs) { this.totalProcurementValueRs = totalProcurementValueRs; }

    public BigDecimal getTotalDbtDisbursedRs() { return totalDbtDisbursedRs; }
    public void setTotalDbtDisbursedRs(BigDecimal totalDbtDisbursedRs) { this.totalDbtDisbursedRs = totalDbtDisbursedRs; }

    public long getTotalFarmersServed() { return totalFarmersServed; }
    public void setTotalFarmersServed(long totalFarmersServed) { this.totalFarmersServed = totalFarmersServed; }

    public long getTotalActiveMandis() { return totalActiveMandis; }
    public void setTotalActiveMandis(long totalActiveMandis) { this.totalActiveMandis = totalActiveMandis; }

    public long getTotalWaitingFarmers() { return totalWaitingFarmers; }
    public void setTotalWaitingFarmers(long totalWaitingFarmers) { this.totalWaitingFarmers = totalWaitingFarmers; }

    public Map<String, Long> getStatusBreakdown() { return statusBreakdown; }
    public void setStatusBreakdown(Map<String, Long> statusBreakdown) { this.statusBreakdown = statusBreakdown; }
}
