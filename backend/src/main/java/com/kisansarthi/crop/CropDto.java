package com.kisansarthi.crop;

import java.math.BigDecimal;

public class CropDto {
    private String id;
    private String name;
    private String hindiName;
    private String malwiName;
    private String season;
    private BigDecimal standardMspPerQuintal;
    private BigDecimal mpBonusPerQuintal;
    private BigDecimal totalMsp;
    private BigDecimal marketPricePerQuintal;
    private BigDecimal typicalCostPerAcre;
    private BigDecimal averageYieldPerAcreQuintal;
    private BigDecimal moistureLimitPct;
    private String gradeSpecs;
    private String icon;

    public CropDto() {}

    public static CropDto fromEntity(Crop c) {
        CropDto dto = new CropDto();
        dto.setId(c.getId());
        dto.setName(c.getName());
        dto.setHindiName(c.getHindiName());
        dto.setMalwiName(c.getMalwiName());
        dto.setSeason(c.getSeason());
        dto.setStandardMspPerQuintal(c.getStandardMspPerQuintal());
        dto.setMpBonusPerQuintal(c.getMpBonusPerQuintal());
        dto.setTotalMsp(c.getTotalMsp());
        dto.setMarketPricePerQuintal(c.getMarketPricePerQuintal());
        dto.setTypicalCostPerAcre(c.getTypicalCostPerAcre());
        dto.setAverageYieldPerAcreQuintal(c.getAverageYieldPerAcreQuintal());
        dto.setMoistureLimitPct(c.getMoistureLimitPct());
        dto.setGradeSpecs(c.getGradeSpecs());
        dto.setIcon(c.getIcon());
        return dto;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getHindiName() { return hindiName; }
    public void setHindiName(String hindiName) { this.hindiName = hindiName; }
    public String getMalwiName() { return malwiName; }
    public void setMalwiName(String malwiName) { this.malwiName = malwiName; }
    public String getSeason() { return season; }
    public void setSeason(String season) { this.season = season; }
    public BigDecimal getStandardMspPerQuintal() { return standardMspPerQuintal; }
    public void setStandardMspPerQuintal(BigDecimal standardMspPerQuintal) { this.standardMspPerQuintal = standardMspPerQuintal; }
    public BigDecimal getMpBonusPerQuintal() { return mpBonusPerQuintal; }
    public void setMpBonusPerQuintal(BigDecimal mpBonusPerQuintal) { this.mpBonusPerQuintal = mpBonusPerQuintal; }
    public BigDecimal getTotalMsp() { return totalMsp; }
    public void setTotalMsp(BigDecimal totalMsp) { this.totalMsp = totalMsp; }
    public BigDecimal getMarketPricePerQuintal() { return marketPricePerQuintal; }
    public void setMarketPricePerQuintal(BigDecimal marketPricePerQuintal) { this.marketPricePerQuintal = marketPricePerQuintal; }
    public BigDecimal getTypicalCostPerAcre() { return typicalCostPerAcre; }
    public void setTypicalCostPerAcre(BigDecimal typicalCostPerAcre) { this.typicalCostPerAcre = typicalCostPerAcre; }
    public BigDecimal getAverageYieldPerAcreQuintal() { return averageYieldPerAcreQuintal; }
    public void setAverageYieldPerAcreQuintal(BigDecimal averageYieldPerAcreQuintal) { this.averageYieldPerAcreQuintal = averageYieldPerAcreQuintal; }
    public BigDecimal getMoistureLimitPct() { return moistureLimitPct; }
    public void setMoistureLimitPct(BigDecimal moistureLimitPct) { this.moistureLimitPct = moistureLimitPct; }
    public String getGradeSpecs() { return gradeSpecs; }
    public void setGradeSpecs(String gradeSpecs) { this.gradeSpecs = gradeSpecs; }
    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }
}
