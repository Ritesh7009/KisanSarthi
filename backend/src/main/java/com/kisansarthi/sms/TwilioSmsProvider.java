package com.kisansarthi.sms;

import com.twilio.Twilio;
import com.twilio.exception.ApiException;
import com.twilio.rest.api.v2010.Account;
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

    @Value("${kisansarthi.sms.twilio.trial-mode:${TWILIO_TRIAL_MODE:${TWILIO_TRIAL:}}}")
    private String trialModeConfig;

    @Value("${kisansarthi.sms.twilio.trial-message:${TWILIO_TRIAL_MESSAGE:Your 1234 order of 1 items has shipped and should be delivered on tomorrow. Details: https://twilio.com}}")
    private String defaultTrialMessage;

    private volatile Boolean cachedIsTrial = null;

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
    public String getProviderName() {
        return "twilio";
    }

    @Override
    public boolean isTrialMode() {
        if (cachedIsTrial != null) {
            return cachedIsTrial;
        }
        synchronized (this) {
            if (cachedIsTrial != null) {
                return cachedIsTrial;
            }
            if (trialModeConfig != null && !trialModeConfig.isBlank()) {
                cachedIsTrial = Boolean.parseBoolean(trialModeConfig.trim());
                log.info("[Twilio] Trial mode explicitly configured via property: {}", cachedIsTrial);
                return cachedIsTrial;
            }
            try {
                Account account = Account.fetcher(accountSid).fetch();
                if (account != null && account.getType() != null) {
                    cachedIsTrial = account.getType().toString().equalsIgnoreCase("Trial");
                    log.info("[Twilio] Auto-detected account type from Twilio API: {} (isTrial: {})", account.getType(), cachedIsTrial);
                    return cachedIsTrial;
                }
            } catch (Exception e) {
                log.warn("[Twilio] Could not fetch account info from API: {}. Defaulting to true for trial safety.", e.getMessage());
            }
            // Default to true for safety in trial environment
            cachedIsTrial = true;
            return cachedIsTrial;
        }
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
        if (isTrialMode()) {
            log.warn("[Twilio] Blocked custom DLT message to recipient in Trial mode. Twilio Trial accounts cannot send custom DLT messages.");
            return SmsResult.failed(
                    400,
                    "Twilio Trial accounts cannot send this custom message. Use Twilio Trial Test mode or upgrade/configure the Twilio account for production SMS."
            );
        }

        String e164Phone = toE164(recipientPhone);
        log.info("Attempting production Twilio SMS to {}", e164Phone);

        try {
            Message sent = Message.creator(
                    new PhoneNumber(e164Phone),
                    new PhoneNumber(fromNumber),
                    message
            ).create();

            String rawStatus = sent.getStatus() != null ? sent.getStatus().toString().toUpperCase() : "ACCEPTED";
            String resolvedStatus = resolveTwilioStatus(rawStatus);

            log.info("Twilio accepted production SMS. SID={}, status={}", sent.getSid(), resolvedStatus);
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

    @Override
    public SmsResult sendTrialTestSms(String recipientPhone, String customTrialMessage) {
        String e164Phone = toE164(recipientPhone);
        String messageToSend = (customTrialMessage != null && !customTrialMessage.isBlank())
                ? customTrialMessage.trim()
                : defaultTrialMessage;

        log.info("Attempting Twilio Trial Test SMS to {} with pre-approved trial template", e164Phone);

        try {
            Message sent = Message.creator(
                    new PhoneNumber(e164Phone),
                    new PhoneNumber(fromNumber),
                    messageToSend
            ).create();

            String rawStatus = sent.getStatus() != null ? sent.getStatus().toString().toUpperCase() : "ACCEPTED";
            String resolvedStatus = resolveTwilioStatus(rawStatus);

            log.info("Twilio accepted Trial Test SMS. SID={}, status={}", sent.getSid(), resolvedStatus);
            return SmsResult.accepted(sent.getSid(), resolvedStatus);

        } catch (ApiException e) {
            Integer errorCode = e.getCode();
            String safeMessage = sanitizeMessage(e.getMessage());
            log.error("Twilio Trial Test SMS failed. errorCode={}, message={}", errorCode, safeMessage);
            return SmsResult.failed(errorCode, safeMessage);

        } catch (Exception e) {
            String safeMessage = sanitizeMessage(e.getMessage());
            log.error("Twilio Trial Test SMS failed. errorCode=null, message={}", safeMessage);
            return SmsResult.failed(null, safeMessage);
        }
    }

    private String resolveTwilioStatus(String rawStatus) {
        if ("QUEUED".equalsIgnoreCase(rawStatus)) {
            return "QUEUED";
        } else if ("ACCEPTED".equalsIgnoreCase(rawStatus)) {
            return "ACCEPTED";
        } else if ("SENT".equalsIgnoreCase(rawStatus)) {
            return "SENT";
        } else if ("DELIVERED".equalsIgnoreCase(rawStatus)) {
            return "DELIVERED";
        } else if ("FAILED".equalsIgnoreCase(rawStatus) || "UNDELIVERED".equalsIgnoreCase(rawStatus)) {
            return "FAILED";
        }
        return rawStatus;
    }
}
