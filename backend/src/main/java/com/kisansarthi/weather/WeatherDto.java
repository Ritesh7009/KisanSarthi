package com.kisansarthi.weather;

public class WeatherDto {
    private String district;
    private String condition;
    private String description;
    private String hindiDescription;
    private double tempCelsius;
    private int rainProbability;
    private int humidity;
    private double windSpeedKmH;
    private String alertSeverity; // LOW, MEDIUM, HIGH, NONE
    private String advisory;
    private String hindiAdvisory;

    public WeatherDto() {}

    public String getDistrict() {
        return district;
    }

    public void setDistrict(String district) {
        this.district = district;
    }

    public String getCondition() {
        return condition;
    }

    public void setCondition(String condition) {
        this.condition = condition;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getHindiDescription() {
        return hindiDescription;
    }

    public void setHindiDescription(String hindiDescription) {
        this.hindiDescription = hindiDescription;
    }

    public double getTempCelsius() {
        return tempCelsius;
    }

    public void setTempCelsius(double tempCelsius) {
        this.tempCelsius = tempCelsius;
    }

    public int getRainProbability() {
        return rainProbability;
    }

    public void setRainProbability(int rainProbability) {
        this.rainProbability = rainProbability;
    }

    public int getHumidity() {
        return humidity;
    }

    public void setHumidity(int humidity) {
        this.humidity = humidity;
    }

    public double getWindSpeedKmH() {
        return windSpeedKmH;
    }

    public void setWindSpeedKmH(double windSpeedKmH) {
        this.windSpeedKmH = windSpeedKmH;
    }

    public String getAlertSeverity() {
        return alertSeverity;
    }

    public void setAlertSeverity(String alertSeverity) {
        this.alertSeverity = alertSeverity;
    }

    public String getAdvisory() {
        return advisory;
    }

    public void setAdvisory(String advisory) {
        this.advisory = advisory;
    }

    public String getHindiAdvisory() {
        return hindiAdvisory;
    }

    public void setHindiAdvisory(String hindiAdvisory) {
        this.hindiAdvisory = hindiAdvisory;
    }
}
