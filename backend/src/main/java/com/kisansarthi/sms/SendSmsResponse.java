package com.kisansarthi.sms;

import java.time.Instant;

public class SendSmsResponse {

    private String sid;
    private String deliveryReceiptId;
    private String status;
    private String recipientPhone;
    private String farmerName;
    private String message;
    private Integer errorCode;
    private String errorMessage;
    private String dispatchedAt;

    public SendSmsResponse() {
        this.dispatchedAt = Instant.now().toString();
    }

    public SendSmsResponse(String sid, String status, String recipientPhone, String farmerName, String message) {
        this.sid = sid;
        this.deliveryReceiptId = sid;
        this.status = status;
        this.recipientPhone = recipientPhone;
        this.farmerName = farmerName;
        this.message = message;
        this.dispatchedAt = Instant.now().toString();
    }

    public String getSid() {
        return sid != null ? sid : deliveryReceiptId;
    }

    public void setSid(String sid) {
        this.sid = sid;
        if (this.deliveryReceiptId == null) {
            this.deliveryReceiptId = sid;
        }
    }

    public String getDeliveryReceiptId() {
        return deliveryReceiptId != null ? deliveryReceiptId : sid;
    }

    public void setDeliveryReceiptId(String deliveryReceiptId) {
        this.deliveryReceiptId = deliveryReceiptId;
        if (this.sid == null) {
            this.sid = deliveryReceiptId;
        }
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

    public Integer getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(Integer errorCode) {
        this.errorCode = errorCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getDispatchedAt() {
        return dispatchedAt;
    }

    public void setDispatchedAt(String dispatchedAt) {
        this.dispatchedAt = dispatchedAt;
    }
}
