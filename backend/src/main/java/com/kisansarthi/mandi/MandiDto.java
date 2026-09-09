package com.kisansarthi.mandi;

public class MandiDto {
    private String id;
    private String name;
    private String hindiName;
    private String district;
    private String hindiDistrict;
    private String address;
    private String pinCode;
    private String openTime;
    private String closeTime;
    private int weighbridgesCount;
    private int dailyCapacityQuintals;
    private int currentTokenServing;
    private int totalTokensToday;
    private int activeTokensWaiting;
    private int averageProcessingMins;
    private String gateStatus;
    private String phone;
    private double lat;
    private double lng;

    public MandiDto() {}

    public static MandiDto fromEntity(Mandi m) {
        MandiDto dto = new MandiDto();
        dto.setId(m.getId());
        dto.setName(m.getName());
        dto.setHindiName(m.getHindiName());
        dto.setDistrict(m.getDistrict());
        dto.setHindiDistrict(m.getHindiDistrict());
        dto.setAddress(m.getAddress());
        dto.setPinCode(m.getPinCode());
        dto.setOpenTime(m.getOpenTime());
        dto.setCloseTime(m.getCloseTime());
        dto.setWeighbridgesCount(m.getWeighbridgesCount());
        dto.setDailyCapacityQuintals(m.getDailyCapacityQuintals());
        dto.setCurrentTokenServing(m.getCurrentTokenServing());
        dto.setTotalTokensToday(m.getTotalTokensToday());
        dto.setActiveTokensWaiting(m.getActiveTokensWaiting());
        dto.setAverageProcessingMins(m.getAverageProcessingMins());
        dto.setGateStatus(m.getGateStatus());
        dto.setPhone(m.getPhone());
        dto.setLat(m.getLat());
        dto.setLng(m.getLng());
        return dto;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getHindiName() { return hindiName; }
    public void setHindiName(String hindiName) { this.hindiName = hindiName; }
    public String getDistrict() { return district; }
    public void setDistrict(String district) { this.district = district; }
    public String getHindiDistrict() { return hindiDistrict; }
    public void setHindiDistrict(String hindiDistrict) { this.hindiDistrict = hindiDistrict; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getPinCode() { return pinCode; }
    public void setPinCode(String pinCode) { this.pinCode = pinCode; }
    public String getOpenTime() { return openTime; }
    public void setOpenTime(String openTime) { this.openTime = openTime; }
    public String getCloseTime() { return closeTime; }
    public void setCloseTime(String closeTime) { this.closeTime = closeTime; }
    public int getWeighbridgesCount() { return weighbridgesCount; }
    public void setWeighbridgesCount(int weighbridgesCount) { this.weighbridgesCount = weighbridgesCount; }
    public int getDailyCapacityQuintals() { return dailyCapacityQuintals; }
    public void setDailyCapacityQuintals(int dailyCapacityQuintals) { this.dailyCapacityQuintals = dailyCapacityQuintals; }
    public int getCurrentTokenServing() { return currentTokenServing; }
    public void setCurrentTokenServing(int currentTokenServing) { this.currentTokenServing = currentTokenServing; }
    public int getTotalTokensToday() { return totalTokensToday; }
    public void setTotalTokensToday(int totalTokensToday) { this.totalTokensToday = totalTokensToday; }
    public int getActiveTokensWaiting() { return activeTokensWaiting; }
    public void setActiveTokensWaiting(int activeTokensWaiting) { this.activeTokensWaiting = activeTokensWaiting; }
    public int getAverageProcessingMins() { return averageProcessingMins; }
    public void setAverageProcessingMins(int averageProcessingMins) { this.averageProcessingMins = averageProcessingMins; }
    public String getGateStatus() { return gateStatus; }
    public void setGateStatus(String gateStatus) { this.gateStatus = gateStatus; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public double getLat() { return lat; }
    public void setLat(double lat) { this.lat = lat; }
    public double getLng() { return lng; }
    public void setLng(double lng) { this.lng = lng; }
}
