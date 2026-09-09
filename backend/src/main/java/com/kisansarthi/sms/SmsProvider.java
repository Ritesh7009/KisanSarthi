package com.kisansarthi.sms;

public interface SmsProvider {
    String sendSms(String recipientPhone, String message, String senderHeader);
}
