package com.kisansarthi.ai;

import java.util.List;

public class YieldAdvisorResponse {
    private double estimatedYieldQuintals;
    private double expectedGrossRevenue;
    private double expectedProfit;
    private String profitMarginPct;
    private String optimalMandi;
    private String yieldGrade; // EXCELLENT, ABOVE_AVERAGE, AVERAGE, BELOW_AVERAGE
    private List<String> marketInsights;
    private List<String> riskFactors;

    public YieldAdvisorResponse() {}

    public double getEstimatedYieldQuintals() {
        return estimatedYieldQuintals;
    }

    public void setEstimatedYieldQuintals(double estimatedYieldQuintals) {
        this.estimatedYieldQuintals = estimatedYieldQuintals;
    }

    public double getExpectedGrossRevenue() {
        return expectedGrossRevenue;
    }

    public void setExpectedGrossRevenue(double expectedGrossRevenue) {
        this.expectedGrossRevenue = expectedGrossRevenue;
    }

    public double getExpectedProfit() {
        return expectedProfit;
    }

    public void setExpectedProfit(double expectedProfit) {
        this.expectedProfit = expectedProfit;
    }

    public String getProfitMarginPct() {
        return profitMarginPct;
    }

    public void setProfitMarginPct(String profitMarginPct) {
        this.profitMarginPct = profitMarginPct;
    }

    public String getOptimalMandi() {
        return optimalMandi;
    }

    public void setOptimalMandi(String optimalMandi) {
        this.optimalMandi = optimalMandi;
    }

    public String getYieldGrade() {
        return yieldGrade;
    }

    public void setYieldGrade(String yieldGrade) {
        this.yieldGrade = yieldGrade;
    }

    public List<String> getMarketInsights() {
        return marketInsights;
    }

    public void setMarketInsights(List<String> marketInsights) {
        this.marketInsights = marketInsights;
    }

    public List<String> getRiskFactors() {
        return riskFactors;
    }

    public void setRiskFactors(List<String> riskFactors) {
        this.riskFactors = riskFactors;
    }
}
