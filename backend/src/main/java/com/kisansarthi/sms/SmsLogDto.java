package com.kisansarthi.sms;

import java.time.Instant;
import java.util.UUID;

public class SmsLogDto {

    private String id;
    private String recipientPhone;
    private String farmerName;
    private String aadharMasked;
    private String message;
    private String senderHeader;
    private String dltTemplateId;
    private String status;
    private String dispatchedAt;
    private String dispatchedBy;
    private String channel;
    private String deliveryReceiptId;
    private String deliveryReport;
    private Instant createdAt;
    private Instant updatedAt;

    public SmsLogDto() {}

    public static SmsLogDto fromEntity(SmsLog log) {
        if (log == null) {
            return null;
        }

        SmsLogDto dto = new SmsLogDto();
        dto.setId(log.getId() != null ? log.getId().toString() : UUID.randomUUID().toString());
        dto.setRecipientPhone(log.getRecipientPhone());
        dto.setFarmerName(log.getFarmerName() != null && !log.getFarmerName().isBlank() ? log.getFarmerName() : "Farmer");
        dto.setAadharMasked(log.getMaskedAadhar() != null ? log.getMaskedAadhar() : "XXXX-XXXX-4589");
        dto.setMessage(log.getMessage());
        dto.setSenderHeader(log.getSenderHeader() != null ? log.getSenderHeader() : "VK-EUPARJAN");
        dto.setDltTemplateId("DLT-TE-1107161");
        dto.setStatus(log.getStatus() != null ? log.getStatus() : "SENT");
        dto.setDispatchedAt(log.getCreatedAt() != null ? log.getCreatedAt().toString() : Instant.now().toString());
        dto.setDispatchedBy("Admin (Mandi Secretary)");
        dto.setChannel("SMS_GATEWAY");
        dto.setDeliveryReport(log.getDeliveryReport());

        String report = log.getDeliveryReport();
        if (report != null && report.contains("Ref: ")) {
            dto.setDeliveryReceiptId(report.substring(report.indexOf("Ref: ") + 5).trim());
        } else if (report != null && !report.isBlank()) {
            dto.setDeliveryReceiptId(report);
        } else {
            dto.setDeliveryReceiptId(null);
        }

        dto.setCreatedAt(log.getCreatedAt());
        dto.setUpdatedAt(log.getUpdatedAt());
        return dto;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getRecipientPhone() {
        return recipientPhone;
    }

    public void setRecipientPhone(String recipientPhone) {
        this.recipientPhone = recipientPhone;
    }

    public String getFarmerName() {
        return farmerName;
    }

    public void setFarmerName(String farmerName) {
        this.farmerName = farmerName;
    }

    public String getAadharMasked() {
        return aadharMasked;
    }

    public void setAadharMasked(String aadharMasked) {
        this.aadharMasked = aadharMasked;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getSenderHeader() {
        return senderHeader;
    }

    public void setSenderHeader(String senderHeader) {
        this.senderHeader = senderHeader;
    }

    public String getDltTemplateId() {
        return dltTemplateId;
    }

    public void setDltTemplateId(String dltTemplateId) {
        this.dltTemplateId = dltTemplateId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getDispatchedAt() {
        return dispatchedAt;
    }

    public void setDispatchedAt(String dispatchedAt) {
        this.dispatchedAt = dispatchedAt;
    }

    public String getDispatchedBy() {
        return dispatchedBy;
    }

    public void setDispatchedBy(String dispatchedBy) {
        this.dispatchedBy = dispatchedBy;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public String getDeliveryReceiptId() {
        return deliveryReceiptId;
    }

    public void setDeliveryReceiptId(String deliveryReceiptId) {
        this.deliveryReceiptId = deliveryReceiptId;
    }

    public String getDeliveryReport() {
        return deliveryReport;
    }

    public void setDeliveryReport(String deliveryReport) {
        this.deliveryReport = deliveryReport;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
