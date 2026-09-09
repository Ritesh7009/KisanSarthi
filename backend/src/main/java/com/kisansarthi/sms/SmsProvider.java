package com.kisansarthi.sms;

public interface SmsProvider {
    SmsResult sendSms(String recipientPhone, String message, String senderHeader);
}
