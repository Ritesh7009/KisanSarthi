package com.kisansarthi.farmer;

import java.math.BigDecimal;
import java.util.UUID;

public class FarmerDto {
    private UUID id;
    private String kisanId;
    private String name;
    private String hindiName;
    private String phone;
    private String maskedAadhar;
    private String district;
    private String village;
    private BigDecimal landSizeAcres;
    private String bankAccountLast4;
    private String ifscCode;

    public FarmerDto() {}

    public static FarmerDto fromEntity(Farmer f) {
        FarmerDto dto = new FarmerDto();
        dto.setId(f.getId());
        dto.setKisanId(f.getKisanId());
        dto.setName(f.getName());
        dto.setHindiName(f.getHindiName());
        dto.setPhone(f.getPhone());
        dto.setMaskedAadhar(f.getMaskedAadhar());
        dto.setDistrict(f.getDistrict());
        dto.setVillage(f.getVillage());
        dto.setLandSizeAcres(f.getLandSizeAcres());
        dto.setBankAccountLast4(f.getBankAccountLast4());
        dto.setIfscCode(f.getIfscCode());
        return dto;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getKisanId() { return kisanId; }
    public void setKisanId(String kisanId) { this.kisanId = kisanId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getHindiName() { return hindiName; }
    public void setHindiName(String hindiName) { this.hindiName = hindiName; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getMaskedAadhar() { return maskedAadhar; }
    public void setMaskedAadhar(String maskedAadhar) { this.maskedAadhar = maskedAadhar; }
    public String getDistrict() { return district; }
    public void setDistrict(String district) { this.district = district; }
    public String getVillage() { return village; }
    public void setVillage(String village) { this.village = village; }
    public BigDecimal getLandSizeAcres() { return landSizeAcres; }
    public void setLandSizeAcres(BigDecimal landSizeAcres) { this.landSizeAcres = landSizeAcres; }
    public String getBankAccountLast4() { return bankAccountLast4; }
    public void setBankAccountLast4(String bankAccountLast4) { this.bankAccountLast4 = bankAccountLast4; }
    public String getIfscCode() { return ifscCode; }
    public void setIfscCode(String ifscCode) { this.ifscCode = ifscCode; }
}
