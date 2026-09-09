package com.kisansarthi.ai;

public class SlotRecommendationRequest {
    private String district;
    private String mandiName;
    private String cropName;
    private double estimatedYieldQuintals;
    private String harvestDate;
    private String vehicleType;

    public SlotRecommendationRequest() {}

    public String getDistrict() {
        return district;
    }

    public void setDistrict(String district) {
        this.district = district;
    }

    public String getMandiName() {
        return mandiName;
    }

    public void setMandiName(String mandiName) {
        this.mandiName = mandiName;
    }

    public String getCropName() {
        return cropName;
    }

    public void setCropName(String cropName) {
        this.cropName = cropName;
    }

    public double getEstimatedYieldQuintals() {
        return estimatedYieldQuintals;
    }

    public void setEstimatedYieldQuintals(double estimatedYieldQuintals) {
        this.estimatedYieldQuintals = estimatedYieldQuintals;
    }

    public String getHarvestDate() {
        return harvestDate;
    }

    public void setHarvestDate(String harvestDate) {
        this.harvestDate = harvestDate;
    }

    public String getVehicleType() {
        return vehicleType;
    }

    public void setVehicleType(String vehicleType) {
        this.vehicleType = vehicleType;
    }
}
