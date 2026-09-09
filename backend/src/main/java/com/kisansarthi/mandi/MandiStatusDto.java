package com.kisansarthi.mandi;

public class MandiStatusDto {
    private String mandiId;
    private String mandiName;
    private int currentTokenServing;
    private int totalTokensToday;
    private int activeTokensWaiting;
    private String gateStatus;
    private int estimatedWaitMinutes;
    private String currentBay;

    public MandiStatusDto() {}

    public MandiStatusDto(String mandiId, String mandiName, int currentTokenServing, int totalTokensToday, int activeTokensWaiting, String gateStatus, int estimatedWaitMinutes, String currentBay) {
        this.mandiId = mandiId;
        this.mandiName = mandiName;
        this.currentTokenServing = currentTokenServing;
        this.totalTokensToday = totalTokensToday;
        this.activeTokensWaiting = activeTokensWaiting;
        this.gateStatus = gateStatus;
        this.estimatedWaitMinutes = estimatedWaitMinutes;
        this.currentBay = currentBay;
    }

    public String getMandiId() { return mandiId; }
    public void setMandiId(String mandiId) { this.mandiId = mandiId; }
    public String getMandiName() { return mandiName; }
    public void setMandiName(String mandiName) { this.mandiName = mandiName; }
    public int getCurrentTokenServing() { return currentTokenServing; }
    public void setCurrentTokenServing(int currentTokenServing) { this.currentTokenServing = currentTokenServing; }
    public int getTotalTokensToday() { return totalTokensToday; }
    public void setTotalTokensToday(int totalTokensToday) { this.totalTokensToday = totalTokensToday; }
    public int getActiveTokensWaiting() { return activeTokensWaiting; }
    public void setActiveTokensWaiting(int activeTokensWaiting) { this.activeTokensWaiting = activeTokensWaiting; }
    public String getGateStatus() { return gateStatus; }
    public void setGateStatus(String gateStatus) { this.gateStatus = gateStatus; }
    public int getEstimatedWaitMinutes() { return estimatedWaitMinutes; }
    public void setEstimatedWaitMinutes(int estimatedWaitMinutes) { this.estimatedWaitMinutes = estimatedWaitMinutes; }
    public String getCurrentBay() { return currentBay; }
    public void setCurrentBay(String currentBay) { this.currentBay = currentBay; }
}
