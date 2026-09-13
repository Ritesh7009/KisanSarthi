package com.kisansarthi.report;

import java.math.BigDecimal;
import java.util.List;

public class PaymentAnalyticsReportDto {
    private long totalDbtInitiated;
    private long totalDbtCompleted;
    private long totalDbtPending;
    private long totalDbtFailed;
    private BigDecimal totalAmountSettledRs;
    private BigDecimal totalAmountPendingRs;
    private double averageSettlementHours;
    private long totalDelayedPaymentsCount;
    private BigDecimal totalDelayedAmountRs;
    private List<PaymentDelayAlertDto> delayedPayments;

    public PaymentAnalyticsReportDto() {}

    public static class PaymentDelayAlertDto {
        private String bookingId;
        private String tokenNumber;
        private String farmerReference;
        private String maskedAadhar;
        private String mandiId;
        private String mandiName;
        private BigDecimal netPayableAmount;
        private String paymentStatus;
        private String initiatedAt;
        private String completedAt; // Actual completion/credit timestamp; null if incomplete/pending
        private long delayHours;
        private String maskedAccount;

        public PaymentDelayAlertDto() {}

        public String getBookingId() { return bookingId; }
        public void setBookingId(String bookingId) { this.bookingId = bookingId; }

        public String getTokenNumber() { return tokenNumber; }
        public void setTokenNumber(String tokenNumber) { this.tokenNumber = tokenNumber; }

        public String getFarmerReference() { return farmerReference; }
        public void setFarmerReference(String farmerReference) { this.farmerReference = farmerReference; }

        public String getMaskedAadhar() { return maskedAadhar; }
        public void setMaskedAadhar(String maskedAadhar) { this.maskedAadhar = maskedAadhar; }

        public String getMandiId() { return mandiId; }
        public void setMandiId(String mandiId) { this.mandiId = mandiId; }

        public String getMandiName() { return mandiName; }
        public void setMandiName(String mandiName) { this.mandiName = mandiName; }

        public BigDecimal getNetPayableAmount() { return netPayableAmount; }
        public void setNetPayableAmount(BigDecimal netPayableAmount) { this.netPayableAmount = netPayableAmount; }

        public String getPaymentStatus() { return paymentStatus; }
        public void setPaymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; }

        public String getInitiatedAt() { return initiatedAt; }
        public void setInitiatedAt(String initiatedAt) { this.initiatedAt = initiatedAt; }

        public String getCompletedAt() { return completedAt; }
        public void setCompletedAt(String completedAt) { this.completedAt = completedAt; }

        public long getDelayHours() { return delayHours; }
        public void setDelayHours(long delayHours) { this.delayHours = delayHours; }

        public String getMaskedAccount() { return maskedAccount; }
        public void setMaskedAccount(String maskedAccount) { this.maskedAccount = maskedAccount; }
    }

    // Getters and Setters
    public long getTotalDbtInitiated() { return totalDbtInitiated; }
    public void setTotalDbtInitiated(long totalDbtInitiated) { this.totalDbtInitiated = totalDbtInitiated; }

    public long getTotalDbtCompleted() { return totalDbtCompleted; }
    public void setTotalDbtCompleted(long totalDbtCompleted) { this.totalDbtCompleted = totalDbtCompleted; }

    public long getTotalDbtPending() { return totalDbtPending; }
    public void setTotalDbtPending(long totalDbtPending) { this.totalDbtPending = totalDbtPending; }

    public long getTotalDbtFailed() { return totalDbtFailed; }
    public void setTotalDbtFailed(long totalDbtFailed) { this.totalDbtFailed = totalDbtFailed; }

    public BigDecimal getTotalAmountSettledRs() { return totalAmountSettledRs; }
    public void setTotalAmountSettledRs(BigDecimal totalAmountSettledRs) { this.totalAmountSettledRs = totalAmountSettledRs; }

    public BigDecimal getTotalAmountPendingRs() { return totalAmountPendingRs; }
    public void setTotalAmountPendingRs(BigDecimal totalAmountPendingRs) { this.totalAmountPendingRs = totalAmountPendingRs; }

    public double getAverageSettlementHours() { return averageSettlementHours; }
    public void setAverageSettlementHours(double averageSettlementHours) { this.averageSettlementHours = averageSettlementHours; }

    public long getTotalDelayedPaymentsCount() { return totalDelayedPaymentsCount; }
    public void setTotalDelayedPaymentsCount(long totalDelayedPaymentsCount) { this.totalDelayedPaymentsCount = totalDelayedPaymentsCount; }

    public BigDecimal getTotalDelayedAmountRs() { return totalDelayedAmountRs; }
    public void setTotalDelayedAmountRs(BigDecimal totalDelayedAmountRs) { this.totalDelayedAmountRs = totalDelayedAmountRs; }

    public List<PaymentDelayAlertDto> getDelayedPayments() { return delayedPayments; }
    public void setDelayedPayments(List<PaymentDelayAlertDto> delayedPayments) { this.delayedPayments = delayedPayments; }
}

