package com.kisansarthi.sms;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "kisansarthi.sms.provider", havingValue = "twilio")
public class TwilioSmsProvider implements SmsProvider {

    private final String accountSid;
    private final String authToken;
    private final String fromNumber;

    public TwilioSmsProvider(
            @Value("${TWILIO_ACCOUNT_SID:}") String accountSid,
            @Value("${TWILIO_AUTH_TOKEN:}") String authToken,
            @Value("${TWILIO_FROM_NUMBER:${TWILIO_PHONE_NUMBER:}}") String fromNumber) {
        this.accountSid = accountSid;
        this.authToken = authToken;
        this.fromNumber = fromNumber;

        if (accountSid.isBlank() || authToken.isBlank() || fromNumber.isBlank()) {
            throw new IllegalStateException(
                    "Twilio SMS is enabled but TWILIO_ACCOUNT_SID, TWILIO_AUTH_TOKEN, or TWILIO_FROM_NUMBER is missing");
        }

        Twilio.init(accountSid, authToken);
    }

    @Override
    public String sendSms(String recipientPhone, String message, String senderHeader) {
        Message sent = Message.creator(
                new PhoneNumber(recipientPhone),
                new PhoneNumber(fromNumber),
                message
        ).create();

        return sent.getSid();
    }
}
