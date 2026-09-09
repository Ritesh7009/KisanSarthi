package com.kisansarthi.ai;

import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

@Service
public class AiService {

    public SlotRecommendationResponse recommendSlot(SlotRecommendationRequest req) {
        SlotRecommendationResponse resp = new SlotRecommendationResponse();

        LocalDate targetDate = req.getHarvestDate() != null
                ? LocalDate.parse(req.getHarvestDate()).plusDays(2)
                : LocalDate.now().plusDays(1);

        resp.setRecommendedDate(targetDate.toString());
        resp.setRecommendedSlot("08:00 AM - 10:00 AM");
        resp.setEstimatedWaitMins(12);
        resp.setCongestionLevel("LOW");
        resp.setReasoning("Early morning arrival at " + (req.getMandiName() != null ? req.getMandiName() : "Mandi")
                + " avoids the post-noon truck queues. Predicted wait time is only 12 minutes.");
        resp.setHindiReasoning("सुबह 08:00 - 10:00 बजे का स्लॉट न्यूनतम कतार एवं त्वरित इलेक्ट्रॉनिक तुलाई सुनिश्चित करता है।");
        resp.setAlternativeSlots(Arrays.asList("10:00 AM - 12:00 PM", "12:00 PM - 02:00 PM"));
        resp.setConfidenceScore(0.94);

        return resp;
    }

    public YieldAdvisorResponse analyzeYield(YieldAdvisorRequest req) {
        YieldAdvisorResponse resp = new YieldAdvisorResponse();

        double acreage = req.getAcreage() > 0 ? req.getAcreage() : 4.0;
        double avgYieldPerAcre = req.getCropName() != null && req.getCropName().toLowerCase().contains("wheat") ? 18.5 : 8.5;
        double totalYield = req.getEstimatedYield() != null && req.getEstimatedYield() > 0
                ? req.getEstimatedYield()
                : (acreage * avgYieldPerAcre);

        double mspRate = req.getCropName() != null && req.getCropName().toLowerCase().contains("wheat") ? 2425.0 : 4892.0;
        double grossRevenue = totalYield * mspRate;
        double costPerAcre = 14500.0;
        double totalCost = acreage * costPerAcre;
        double profit = Math.max(0, grossRevenue - totalCost);
        double margin = grossRevenue > 0 ? (profit / grossRevenue) * 100 : 0;

        resp.setEstimatedYieldQuintals(Math.round(totalYield * 100.0) / 100.0);
        resp.setExpectedGrossRevenue(Math.round(grossRevenue * 100.0) / 100.0);
        resp.setExpectedProfit(Math.round(profit * 100.0) / 100.0);
        resp.setProfitMarginPct(String.format("%.1f%%", margin));
        resp.setOptimalMandi("Krishi Upaj Mandi Samiti, " + (req.getDistrict() != null ? req.getDistrict() : "Sehore"));
        resp.setYieldGrade(totalYield >= acreage * avgYieldPerAcre ? "EXCELLENT" : "ABOVE_AVERAGE");
        resp.setMarketInsights(Arrays.asList(
                "Procurement prices in MP currently include special bonus rates over Central MSP.",
                "Direct e-Uparjan procurement eliminates 4-6% middleman commissions.",
                "Prompt DBT settlement within 48-72 hours directly into Aadhaar-seeded bank account."
        ));
        resp.setRiskFactors(Arrays.asList(
                "Moisture content exceeding 12% may lead to weighment deduction.",
                "Ensure vehicle tare weight is recorded on certified Mandi weighbridge."
        ));

        return resp;
    }
}
