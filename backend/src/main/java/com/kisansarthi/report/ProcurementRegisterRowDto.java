package com.kisansarthi.report;

import java.math.BigDecimal;
import java.time.LocalDate;

public class ProcurementRegisterRowDto {
    private String bookingId;
    private String tokenNumber;
    private int tokenSequence;
    private String scheduledDate;
    private String farmerName;
    private String farmerPhone;
    private String maskedAadhar;
    private String district;
    private String mandiName;
    private String cropName;
    private BigDecimal estimatedYieldQuintals;
    private BigDecimal netWeightQuintals;
    private BigDecimal moisturePercentage;
    private BigDecimal foreignMatterPercentage;
    private BigDecimal totalPayoutRs;
    private String status;
    private String paymentStatus;
    private String dbtReferenceNo;
    private String bankAccountLast4;
    private String ifscCode;
    private String completedAt;

    public ProcurementRegisterRowDto() {}

    // Getters and Setters
    public String getBookingId() { return bookingId; }
    public void setBookingId(String bookingId) { this.bookingId = bookingId; }

    public String getTokenNumber() { return tokenNumber; }
    public void setTokenNumber(String tokenNumber) { this.tokenNumber = tokenNumber; }

    public int getTokenSequence() { return tokenSequence; }
    public void setTokenSequence(int tokenSequence) { this.tokenSequence = tokenSequence; }

    public String getScheduledDate() { return scheduledDate; }
    public void setScheduledDate(String scheduledDate) { this.scheduledDate = scheduledDate; }

    public String getFarmerName() { return farmerName; }
    public void setFarmerName(String farmerName) { this.farmerName = farmerName; }

    public String getFarmerPhone() { return farmerPhone; }
    public void setFarmerPhone(String farmerPhone) { this.farmerPhone = farmerPhone; }

    public String getMaskedAadhar() { return maskedAadhar; }
    public void setMaskedAadhar(String maskedAadhar) { this.maskedAadhar = maskedAadhar; }

    public String getDistrict() { return district; }
    public void setDistrict(String district) { this.district = district; }

    public String getMandiName() { return mandiName; }
    public void setMandiName(String mandiName) { this.mandiName = mandiName; }

    public String getCropName() { return cropName; }
    public void setCropName(String cropName) { this.cropName = cropName; }

    public BigDecimal getEstimatedYieldQuintals() { return estimatedYieldQuintals; }
    public void setEstimatedYieldQuintals(BigDecimal estimatedYieldQuintals) { this.estimatedYieldQuintals = estimatedYieldQuintals; }

    public BigDecimal getNetWeightQuintals() { return netWeightQuintals; }
    public void setNetWeightQuintals(BigDecimal netWeightQuintals) { this.netWeightQuintals = netWeightQuintals; }

    public BigDecimal getMoisturePercentage() { return moisturePercentage; }
    public void setMoisturePercentage(BigDecimal moisturePercentage) { this.moisturePercentage = moisturePercentage; }

    public BigDecimal getForeignMatterPercentage() { return foreignMatterPercentage; }
    public void setForeignMatterPercentage(BigDecimal foreignMatterPercentage) { this.foreignMatterPercentage = foreignMatterPercentage; }

    public BigDecimal getTotalPayoutRs() { return totalPayoutRs; }
    public void setTotalPayoutRs(BigDecimal totalPayoutRs) { this.totalPayoutRs = totalPayoutRs; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; }

    public String getDbtReferenceNo() { return dbtReferenceNo; }
    public void setDbtReferenceNo(String dbtReferenceNo) { this.dbtReferenceNo = dbtReferenceNo; }

    public String getBankAccountLast4() { return bankAccountLast4; }
    public void setBankAccountLast4(String bankAccountLast4) { this.bankAccountLast4 = bankAccountLast4; }

    public String getIfscCode() { return ifscCode; }
    public void setIfscCode(String ifscCode) { this.ifscCode = ifscCode; }

    public String getCompletedAt() { return completedAt; }
    public void setCompletedAt(String completedAt) { this.completedAt = completedAt; }
}
