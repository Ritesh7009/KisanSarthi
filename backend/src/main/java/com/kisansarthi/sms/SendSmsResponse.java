package com.kisansarthi.sms;

import java.time.Instant;

public class SendSmsResponse {

    private String deliveryReceiptId;
    private String status;
    private String recipientPhone;
    private String farmerName;
    private String message;
    private String dispatchedAt;

    public SendSmsResponse() {
        this.dispatchedAt = Instant.now().toString();
    }

    public SendSmsResponse(String deliveryReceiptId, String status, String recipientPhone, String farmerName, String message) {
        this.deliveryReceiptId = deliveryReceiptId;
        this.status = status;
        this.recipientPhone = recipientPhone;
        this.farmerName = farmerName;
        this.message = message;
        this.dispatchedAt = Instant.now().toString();
    }

    public String getDeliveryReceiptId() {
        return deliveryReceiptId;
    }

    public void setDeliveryReceiptId(String deliveryReceiptId) {
        this.deliveryReceiptId = deliveryReceiptId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
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

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getDispatchedAt() {
        return dispatchedAt;
    }

    public void setDispatchedAt(String dispatchedAt) {
        this.dispatchedAt = dispatchedAt;
    }
}
