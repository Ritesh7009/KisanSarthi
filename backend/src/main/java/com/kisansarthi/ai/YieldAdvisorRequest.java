package com.kisansarthi.ai;

import java.util.List;

public class YieldAdvisorRequest {
    private String cropName;
    private double acreage;
    private String district;
    private Double estimatedYield;

    public YieldAdvisorRequest() {}

    public String getCropName() {
        return cropName;
    }

    public void setCropName(String cropName) {
        this.cropName = cropName;
    }

    public double getAcreage() {
        return acreage;
    }

    public void setAcreage(double acreage) {
        this.acreage = acreage;
    }

    public String getDistrict() {
        return district;
    }

    public void setDistrict(String district) {
        this.district = district;
    }

    public Double getEstimatedYield() {
        return estimatedYield;
    }

    public void setEstimatedYield(Double estimatedYield) {
        this.estimatedYield = estimatedYield;
    }
}
