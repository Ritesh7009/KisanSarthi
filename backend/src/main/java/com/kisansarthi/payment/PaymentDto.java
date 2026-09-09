package com.kisansarthi.payment;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public class PaymentDto {
    private UUID id;
    private UUID bookingId;
    private UUID farmerId;
    private String mandiId;
    private BigDecimal grossAmount;
    private BigDecimal deductions;
    private BigDecimal netPayableAmount;
    private String bankAccountLast4;
    private String ifscCode;
    private String paymentStatus;
    private String dbtReferenceNo;
    private OffsetDateTime initiatedAt;
    private OffsetDateTime creditedAt;
    private OffsetDateTime createdAt;

    public PaymentDto() {}

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getBookingId() {
        return bookingId;
    }

    public void setBookingId(UUID bookingId) {
        this.bookingId = bookingId;
    }

    public UUID getFarmerId() {
        return farmerId;
    }

    public void setFarmerId(UUID farmerId) {
        this.farmerId = farmerId;
    }

    public String getMandiId() {
        return mandiId;
    }

    public void setMandiId(String mandiId) {
        this.mandiId = mandiId;
    }

    public BigDecimal getGrossAmount() {
        return grossAmount;
    }

    public void setGrossAmount(BigDecimal grossAmount) {
        this.grossAmount = grossAmount;
    }

    public BigDecimal getDeductions() {
        return deductions;
    }

    public void setDeductions(BigDecimal deductions) {
        this.deductions = deductions;
    }

    public BigDecimal getNetPayableAmount() {
        return netPayableAmount;
    }

    public void setNetPayableAmount(BigDecimal netPayableAmount) {
        this.netPayableAmount = netPayableAmount;
    }

    public String getBankAccountLast4() {
        return bankAccountLast4;
    }

    public void setBankAccountLast4(String bankAccountLast4) {
        this.bankAccountLast4 = bankAccountLast4;
    }

    public String getIfscCode() {
        return ifscCode;
    }

    public void setIfscCode(String ifscCode) {
        this.ifscCode = ifscCode;
    }

    public String getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(String paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public String getDbtReferenceNo() {
        return dbtReferenceNo;
    }

    public void setDbtReferenceNo(String dbtReferenceNo) {
        this.dbtReferenceNo = dbtReferenceNo;
    }

    public OffsetDateTime getInitiatedAt() {
        return initiatedAt;
    }

    public void setInitiatedAt(OffsetDateTime initiatedAt) {
        this.initiatedAt = initiatedAt;
    }

    public OffsetDateTime getCreditedAt() {
        return creditedAt;
    }

    public void setCreditedAt(OffsetDateTime creditedAt) {
        this.creditedAt = creditedAt;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
