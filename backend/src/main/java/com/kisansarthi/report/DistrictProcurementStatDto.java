package com.kisansarthi.report;

public class DistrictProcurementStatDto {
    private String district;
    private String hindiDistrict;
    private int registeredFarmers;
    private int activeMandis;
    private int totalSlotsToday;
    private int bookedSlotsToday;
    private double procuredQuintals;
    private double targetQuintals;
    private double totalPayoutLakhs;
    private String status; // NORMAL, HIGH_VOLUME, CONGESTED

    public DistrictProcurementStatDto() {}

    public DistrictProcurementStatDto(
            String district,
            String hindiDistrict,
            int registeredFarmers,
            int activeMandis,
            int totalSlotsToday,
            int bookedSlotsToday,
            double procuredQuintals,
            double targetQuintals,
            double totalPayoutLakhs,
            String status
    ) {
        this.district = district;
        this.hindiDistrict = hindiDistrict;
        this.registeredFarmers = registeredFarmers;
        this.activeMandis = activeMandis;
        this.totalSlotsToday = totalSlotsToday;
        this.bookedSlotsToday = bookedSlotsToday;
        this.procuredQuintals = procuredQuintals;
        this.targetQuintals = targetQuintals;
        this.totalPayoutLakhs = totalPayoutLakhs;
        this.status = status;
    }

    public String getDistrict() {
        return district;
    }

    public void setDistrict(String district) {
        this.district = district;
    }

    public String getHindiDistrict() {
        return hindiDistrict;
    }

    public void setHindiDistrict(String hindiDistrict) {
        this.hindiDistrict = hindiDistrict;
    }

    public int getRegisteredFarmers() {
        return registeredFarmers;
    }

    public void setRegisteredFarmers(int registeredFarmers) {
        this.registeredFarmers = registeredFarmers;
    }

    public int getActiveMandis() {
        return activeMandis;
    }

    public void setActiveMandis(int activeMandis) {
        this.activeMandis = activeMandis;
    }

    public int getTotalSlotsToday() {
        return totalSlotsToday;
    }

    public void setTotalSlotsToday(int totalSlotsToday) {
        this.totalSlotsToday = totalSlotsToday;
    }

    public int getBookedSlotsToday() {
        return bookedSlotsToday;
    }

    public void setBookedSlotsToday(int bookedSlotsToday) {
        this.bookedSlotsToday = bookedSlotsToday;
    }

    public double getProcuredQuintals() {
        return procuredQuintals;
    }

    public void setProcuredQuintals(double procuredQuintals) {
        this.procuredQuintals = procuredQuintals;
    }

    public double getTargetQuintals() {
        return targetQuintals;
    }

    public void setTargetQuintals(double targetQuintals) {
        this.targetQuintals = targetQuintals;
    }

    public double getTotalPayoutLakhs() {
        return totalPayoutLakhs;
    }

    public void setTotalPayoutLakhs(double totalPayoutLakhs) {
        this.totalPayoutLakhs = totalPayoutLakhs;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
