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

    /** Generic SMS entry point used by queue/payment flows. */
    @Async
    public void sendSms(String phone, String farmerName, String message) {
        dispatchSms(phone, farmerName, null, message, "GENERAL");
    }

    public List<SmsLog> getRecentLogs() {
        return smsLogRepository.findTop50ByOrderByCreatedAtDesc();
    }

    private void dispatchSms(String phone, String farmerName, String maskedAadhar, String message, String category) {
        SmsLog logEntry = new SmsLog(phone, farmerName, maskedAadhar, message, "SENDING");
        logEntry.setSenderHeader(defaultSenderHeader);
        logEntry = smsLogRepository.save(logEntry);

        try {
            String providerRef = smsProvider.sendSms(phone, message, defaultSenderHeader);
            logEntry.setStatus("SENT");
            logEntry.setDeliveryReport("Dispatched via provider. Ref: " + providerRef);
            logEntry.setUpdatedAt(Instant.now());
            smsLogRepository.save(logEntry);
        } catch (Exception e) {
            log.error("Failed to send SMS to ****{}: {}", phone.substring(Math.max(0, phone.length() - 4)), e.getMessage());
            logEntry.setStatus("FAILED");
            logEntry.setDeliveryReport("Error: " + e.getMessage());
            logEntry.setUpdatedAt(Instant.now());
            smsLogRepository.save(logEntry);
        }
    }
}
