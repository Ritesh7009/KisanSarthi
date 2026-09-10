package com.kisansarthi.sms;

import jakarta.validation.constraints.NotBlank;

public class SendSmsRequest {

    private String recipientPhone;
    private String phone;

    @NotBlank(message = "Message content is required")
    private String message;

    private String farmerName;
    private String aadharMasked;
    private String senderHeader;
    private String dltTemplateId;
    private String dispatchedBy;
    private String channel;
    private Boolean trialTest;
    private String templateType;
    private String customTrialMessage;

    public SendSmsRequest() {}

    public SendSmsRequest(String recipientPhone, String message, String farmerName) {
        this.recipientPhone = recipientPhone;
        this.message = message;
        this.farmerName = farmerName;
    }

    public boolean isTrialTest() {
        return Boolean.TRUE.equals(trialTest)
                || "TWILIO_TRIAL_TEST".equalsIgnoreCase(templateType)
                || "TWILIO_TRIAL_TEST".equalsIgnoreCase(channel);
    }

    public Boolean getTrialTest() {
        return trialTest;
    }

    public void setTrialTest(Boolean trialTest) {
        this.trialTest = trialTest;
    }

    public String getTemplateType() {
        return templateType;
    }

    public void setTemplateType(String templateType) {
        this.templateType = templateType;
    }

    public String getCustomTrialMessage() {
        return customTrialMessage;
    }

    public void setCustomTrialMessage(String customTrialMessage) {
        this.customTrialMessage = customTrialMessage;
    }

    public String getRecipientPhone() {
        if (recipientPhone != null && !recipientPhone.isBlank()) {
            return recipientPhone;
        }
        return phone;
    }

    public void setRecipientPhone(String recipientPhone) {
        this.recipientPhone = recipientPhone;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
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
}
