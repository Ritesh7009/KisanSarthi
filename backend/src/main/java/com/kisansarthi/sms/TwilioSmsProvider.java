package com.kisansarthi.sms;

import com.twilio.Twilio;
import com.twilio.exception.ApiException;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "kisansarthi.sms.provider", havingValue = "twilio")
public class TwilioSmsProvider implements SmsProvider {

    private static final Logger log = LoggerFactory.getLogger(TwilioSmsProvider.class);

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

    /**
     * Converts Indian and standard phone numbers into strict E.164 international format (+91XXXXXXXXXX).
     */
    public static String toE164(String phone) {
        if (phone == null || phone.isBlank()) {
            return "";
        }
        String cleaned = phone.replaceAll("[\\s\\-\\(\\)]", "").trim();
        if (cleaned.startsWith("+")) {
            return cleaned;
        }
        String digits = cleaned.replaceAll("\\D", "");
        if (digits.length() == 10) {
            return "+91" + digits;
        } else if (digits.length() == 11 && digits.startsWith("0")) {
            return "+91" + digits.substring(1);
        } else if (digits.length() == 12 && digits.startsWith("91")) {
            return "+" + digits;
        } else if (!digits.isEmpty()) {
            return "+91" + digits;
        }
        return cleaned;
    }

    private String sanitizeMessage(String message) {
        if (message == null) {
            return "Unknown Twilio error";
        }
        String safe = message;
        if (authToken != null && !authToken.isBlank()) {
            safe = safe.replace(authToken, "[REDACTED]");
        }
        return safe;
    }

    @Override
    public SmsResult sendSms(String recipientPhone, String message, String senderHeader) {
        String e164Phone = toE164(recipientPhone);
        log.info("Attempting Twilio SMS to {}", e164Phone);

        try {
            Message sent = Message.creator(
                    new PhoneNumber(e164Phone),
                    new PhoneNumber(fromNumber),
                    message
            ).create();

            String rawStatus = sent.getStatus() != null ? sent.getStatus().toString().toUpperCase() : "ACCEPTED";
            String resolvedStatus;
            if ("QUEUED".equalsIgnoreCase(rawStatus)) {
                resolvedStatus = "QUEUED";
            } else if ("ACCEPTED".equalsIgnoreCase(rawStatus)) {
                resolvedStatus = "ACCEPTED";
            } else if ("FAILED".equalsIgnoreCase(rawStatus) || "UNDELIVERED".equalsIgnoreCase(rawStatus)) {
                resolvedStatus = "FAILED";
            } else {
                resolvedStatus = rawStatus;
            }

            log.info("Twilio accepted SMS. SID={}, status={}", sent.getSid(), resolvedStatus);
            return SmsResult.accepted(sent.getSid(), resolvedStatus);

        } catch (ApiException e) {
            Integer errorCode = e.getCode();
            String safeMessage = sanitizeMessage(e.getMessage());
            log.error("Twilio SMS failed. errorCode={}, message={}", errorCode, safeMessage);
            return SmsResult.failed(errorCode, safeMessage);

        } catch (Exception e) {
            String safeMessage = sanitizeMessage(e.getMessage());
            log.error("Twilio SMS failed. errorCode=null, message={}", safeMessage);
            return SmsResult.failed(null, safeMessage);
        }
    }
}
