package com.kisansarthi.report;

import java.util.List;

public class DistrictStatsReportDto {
    private List<DistrictProcurementStatDto> stats;
    private int mandisCount;
    private int totalBookings;
    private int registeredFarmersCount;

    public DistrictStatsReportDto() {}

    public DistrictStatsReportDto(
            List<DistrictProcurementStatDto> stats,
            int mandisCount,
            int totalBookings,
            int registeredFarmersCount
    ) {
        this.stats = stats;
        this.mandisCount = mandisCount;
        this.totalBookings = totalBookings;
        this.registeredFarmersCount = registeredFarmersCount;
    }

    public List<DistrictProcurementStatDto> getStats() {
        return stats;
    }

    public void setStats(List<DistrictProcurementStatDto> stats) {
        this.stats = stats;
    }

    public int getMandisCount() {
        return mandisCount;
    }

    public void setMandisCount(int mandisCount) {
        this.mandisCount = mandisCount;
    }

    public int getTotalBookings() {
        return totalBookings;
    }

    public void setTotalBookings(int totalBookings) {
        this.totalBookings = totalBookings;
    }

    public int getRegisteredFarmersCount() {
        return registeredFarmersCount;
    }

    public void setRegisteredFarmersCount(int registeredFarmersCount) {
        this.registeredFarmersCount = registeredFarmersCount;
    }
}
