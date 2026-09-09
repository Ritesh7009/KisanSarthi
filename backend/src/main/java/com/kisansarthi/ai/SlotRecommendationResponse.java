package com.kisansarthi.ai;

import java.util.List;

public class SlotRecommendationResponse {
    private String recommendedDate;
    private String recommendedSlot;
    private int estimatedWaitMins;
    private String congestionLevel; // LOW, MODERATE, HIGH
    private String reasoning;
    private String hindiReasoning;
    private List<String> alternativeSlots;
    private double confidenceScore;

    public SlotRecommendationResponse() {}

    public String getRecommendedDate() {
        return recommendedDate;
    }

    public void setRecommendedDate(String recommendedDate) {
        this.recommendedDate = recommendedDate;
    }

    public String getRecommendedSlot() {
        return recommendedSlot;
    }

    public void setRecommendedSlot(String recommendedSlot) {
        this.recommendedSlot = recommendedSlot;
    }

    public int getEstimatedWaitMins() {
        return estimatedWaitMins;
    }

    public void setEstimatedWaitMins(int estimatedWaitMins) {
        this.estimatedWaitMins = estimatedWaitMins;
    }

    public String getCongestionLevel() {
        return congestionLevel;
    }

    public void setCongestionLevel(String congestionLevel) {
        this.congestionLevel = congestionLevel;
    }

    public String getReasoning() {
        return reasoning;
    }

    public void setReasoning(String reasoning) {
        this.reasoning = reasoning;
    }

    public String getHindiReasoning() {
        return hindiReasoning;
    }

    public void setHindiReasoning(String hindiReasoning) {
        this.hindiReasoning = hindiReasoning;
    }

    public List<String> getAlternativeSlots() {
        return alternativeSlots;
    }

    public void setAlternativeSlots(List<String> alternativeSlots) {
        this.alternativeSlots = alternativeSlots;
    }

    public double getConfidenceScore() {
        return confidenceScore;
    }

    public void setConfidenceScore(double confidenceScore) {
        this.confidenceScore = confidenceScore;
    }
}
