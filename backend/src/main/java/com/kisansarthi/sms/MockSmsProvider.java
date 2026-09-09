package com.kisansarthi.sms;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@ConditionalOnProperty(name = "kisansarthi.sms.provider", havingValue = "mock", matchIfMissing = true)
public class MockSmsProvider implements SmsProvider {

    private static final Logger log = LoggerFactory.getLogger(MockSmsProvider.class);

    @Override
    public String sendSms(String recipientPhone, String message, String senderHeader) {
        String msgId = "MOCK-SMS-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        log.info("[MOCK SMS GATEWAY] Header: {}, To: ****{}, Message: [{}], Ref: {}",
                senderHeader,
                recipientPhone.substring(Math.max(0, recipientPhone.length() - 4)),
                message,
                msgId);
        return msgId;
    }
}
