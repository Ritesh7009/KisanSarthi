package com.kisansarthi.sms;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "sms_logs")
public class SmsLog {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "recipient_phone", nullable = false, length = 20)
    private String recipientPhone;

    @Column(name = "farmer_name", length = 150)
    private String farmerName;

    @Column(name = "masked_aadhar", length = 20)
    private String maskedAadhar;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(name = "sender_header", nullable = false, length = 20)
    private String senderHeader = "VK-EUPARJAN";

    @Column(nullable = false, length = 30)
    private String status = "QUEUED";

    @Column(name = "delivery_report", columnDefinition = "TEXT")
    private String deliveryReport;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public SmsLog() {}

    public SmsLog(String recipientPhone, String farmerName, String maskedAadhar, String message, String status) {
        this.recipientPhone = recipientPhone;
        this.farmerName = farmerName;
        this.maskedAadhar = maskedAadhar;
        this.message = message;
        this.status = status;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getRecipientPhone() { return recipientPhone; }
    public void setRecipientPhone(String recipientPhone) { this.recipientPhone = recipientPhone; }
    public String getFarmerName() { return farmerName; }
    public void setFarmerName(String farmerName) { this.farmerName = farmerName; }
    public String getMaskedAadhar() { return maskedAadhar; }
    public void setMaskedAadhar(String maskedAadhar) { this.maskedAadhar = maskedAadhar; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getSenderHeader() { return senderHeader; }
    public void setSenderHeader(String senderHeader) { this.senderHeader = senderHeader; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getDeliveryReport() { return deliveryReport; }
    public void setDeliveryReport(String deliveryReport) { this.deliveryReport = deliveryReport; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public String getDispatchedAt() {
        return createdAt != null ? createdAt.toString() : Instant.now().toString();
    }

    public String getDeliveryReceiptId() {
        if (deliveryReport != null && deliveryReport.contains("Ref: ")) {
            return deliveryReport.substring(deliveryReport.indexOf("Ref: ") + 5).trim();
        }
        return deliveryReport;
    }
}
