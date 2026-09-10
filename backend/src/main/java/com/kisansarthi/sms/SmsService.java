package com.kisansarthi.sms;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class SmsService {

    private static final Logger log = LoggerFactory.getLogger(SmsService.class);

    private final SmsProvider smsProvider;
    private final SmsLogRepository smsLogRepository;

    @Value("${kisansarthi.sms.sender-id:VK-EUPARJAN}")
    private String defaultSenderHeader;

    public SmsService(SmsProvider smsProvider, SmsLogRepository smsLogRepository) {
        this.smsProvider = smsProvider;
        this.smsLogRepository = smsLogRepository;
    }

    @Async
    public void sendOtp(String phone, String message) {
        dispatchSms(phone, "Farmer", null, message, "OTP");
    }

    @Async
    public void sendBookingConfirmation(String phone, String farmerName, String tokenNumber, String mandiName, String date, String slot) {
        String msg = String.format("e-Uparjan: Namaste %s ji. Aapka slot book ho gaya hai. Token: %s, Mandi: %s, Date: %s, Slot: %s. Kripya samay par pahuchein.",
                farmerName, tokenNumber, mandiName, date, slot);
        dispatchSms(phone, farmerName, null, msg, "BOOKING_CONFIRMATION");
    }

    @Async
    public void sendTokenCalledNotification(String phone, String farmerName, String tokenNumber, String bay) {
        String msg = String.format("e-Uparjan URGENT: %s ji, Token %s ko %s par pravesh hetu bulaya gaya hai. Turant Gate par sampark karein.",
                farmerName, tokenNumber, bay);
        dispatchSms(phone, farmerName, null, msg, "TOKEN_CALLED");
    }

    @Async
    public void sendStatusUpdate(String phone, String farmerName, String tokenNumber, String status) {
        String msg = String.format("e-Uparjan Update: Token %s ka status ab '%s' hai. - MP Mandi Board",
                tokenNumber, status);
        dispatchSms(phone, farmerName, null, msg, "STATUS_UPDATE");
    }

    @Async
    public void sendPaymentNotification(String phone, String farmerName, String tokenNumber, String amount, String dbtRef) {
        String msg = String.format("e-Uparjan DBT: %s ji, Token %s ke Rs %s ka bhugtan DBT Ref %s se aapke bank khate mein bhej diya gaya hai.",
                farmerName, tokenNumber, amount, dbtRef);
        dispatchSms(phone, farmerName, null, msg, "PAYMENT");
    }

    /**
     * Synchronous SMS entry point used by SMS controller and notification services
     * to immediately capture provider dispatch result (real SID and status).
     */
    public SmsResult sendSms(String phone, String farmerName, String message) {
        return dispatchSms(phone, farmerName, null, message, "GENERAL");
    }

    public boolean isTrialMode() {
        return smsProvider.isTrialMode();
    }

    public String getProviderName() {
        return smsProvider.getProviderName();
    }

    public SmsResult sendTrialTestSms(String phone, String farmerName, String customTrialMessage) {
        String msgContent = (customTrialMessage != null && !customTrialMessage.isBlank())
                ? customTrialMessage
                : "Your 1234 order of 1 items has shipped and should be delivered on tomorrow. Details: https://twilio.com";

        SmsLog logEntry = new SmsLog(phone, farmerName != null ? farmerName : "Test Recipient", null, msgContent, "SENDING");
        logEntry.setSenderHeader("TWILIO-TRIAL");
        logEntry.setDltTemplateId("TWILIO-PREDEFINED-ORDER-CONFIRMATION");
        logEntry.setDispatchedBy("Twilio Trial Dispatcher (Connectivity Test)");
        logEntry.setChannel("TWILIO_TRIAL_TEST");
        logEntry = smsLogRepository.save(logEntry);

        try {
            SmsResult result = smsProvider.sendTrialTestSms(phone, customTrialMessage);
            if (result.isSuccess()) {
                logEntry.setStatus(result.getStatus());
                logEntry.setDeliveryReceiptId(result.getSid());
                logEntry.setDeliveryReport("Twilio Trial Connectivity/Demo Test. Provider SID: " + result.getSid());
            } else {
                logEntry.setStatus("FAILED");
                logEntry.setDeliveryReport(String.format("Twilio Trial Error [code=%s]: %s",
                        result.getErrorCode() != null ? result.getErrorCode() : "N/A",
                        result.getErrorMessage()));
            }
            logEntry.setUpdatedAt(Instant.now());
            smsLogRepository.save(logEntry);
            return result;
        } catch (Exception e) {
            String safeMsg = e.getMessage() != null ? e.getMessage() : "Unknown exception during Twilio Trial Test";
            log.error("Unexpected error sending trial test SMS to ****{}: {}",
                    phone != null && phone.length() >= 4 ? phone.substring(phone.length() - 4) : phone,
                    safeMsg);
            logEntry.setStatus("FAILED");
            logEntry.setDeliveryReport("Error: " + safeMsg);
            logEntry.setUpdatedAt(Instant.now());
            smsLogRepository.save(logEntry);
            return SmsResult.failed(null, safeMsg);
        }
    }

    public List<SmsLog> getRecentLogs() {
        return smsLogRepository.findTop50ByOrderByCreatedAtDesc();
    }

    private SmsResult dispatchSms(String phone, String farmerName, String maskedAadhar, String message, String category) {
        if (smsProvider.isTrialMode()) {
            String trialErrMsg = "Twilio Trial accounts cannot send this custom message. Use Twilio Trial Test mode or upgrade/configure the Twilio account for production SMS.";
            log.warn("[SMS SERVICE] Blocked custom message dispatch in Trial mode to {}: {}", phone, trialErrMsg);
            SmsLog logEntry = new SmsLog(phone, farmerName, maskedAadhar, message, "FAILED");
            logEntry.setSenderHeader(defaultSenderHeader);
            logEntry.setDeliveryReport("Restricted: " + trialErrMsg);
            smsLogRepository.save(logEntry);
            return SmsResult.failed(400, trialErrMsg);
        }

        SmsLog logEntry = new SmsLog(phone, farmerName, maskedAadhar, message, "SENDING");
        logEntry.setSenderHeader(defaultSenderHeader);
        logEntry = smsLogRepository.save(logEntry);

        try {
            SmsResult result = smsProvider.sendSms(phone, message, defaultSenderHeader);
            if (result.isSuccess()) {
                logEntry.setStatus(result.getStatus());
                logEntry.setDeliveryReceiptId(result.getSid());
                logEntry.setDeliveryReport("Dispatched via provider. Ref: " + result.getSid());
            } else {
                logEntry.setStatus("FAILED");
                logEntry.setDeliveryReport(String.format("Error [code=%s]: %s",
                        result.getErrorCode() != null ? result.getErrorCode() : "N/A",
                        result.getErrorMessage()));
            }
            logEntry.setUpdatedAt(Instant.now());
            smsLogRepository.save(logEntry);
            return result;
        } catch (Exception e) {
            String safeMsg = e.getMessage() != null ? e.getMessage() : "Unknown exception during SMS dispatch";
            log.error("Unexpected error sending SMS to ****{}: {}",
                    phone != null && phone.length() >= 4 ? phone.substring(phone.length() - 4) : phone,
                    safeMsg);
            logEntry.setStatus("FAILED");
            logEntry.setDeliveryReport("Error: " + safeMsg);
            logEntry.setUpdatedAt(Instant.now());
            smsLogRepository.save(logEntry);
            return SmsResult.failed(null, safeMsg);
        }
    }
}
