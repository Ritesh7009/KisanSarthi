package com.kisansarthi.sms;

public interface SmsProvider {

    SmsResult sendSms(String recipientPhone, String message, String senderHeader);

    default boolean isTrialMode() {
        return false;
    }

    default SmsResult sendTrialTestSms(String recipientPhone, String customTrialMessage) {
        return sendSms(recipientPhone, customTrialMessage, "TWILIO-TRIAL");
    }

    default String getProviderName() {
        return "mock";
    }
}
