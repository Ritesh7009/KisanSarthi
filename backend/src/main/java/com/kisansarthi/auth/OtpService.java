package com.kisansarthi.auth;

import com.kisansarthi.common.BusinessException;
import com.kisansarthi.sms.SmsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.HexFormat;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Service
public class OtpService {

    private static final Logger log = LoggerFactory.getLogger(OtpService.class);
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int OTP_EXPIRY_MINUTES = 5;
    private static final int MAX_ATTEMPTS = 3;
    private static final int COOLDOWN_SECONDS = 60;

    private final RedisTemplate<String, Object> redisTemplate;
    private final SmsService smsService;

    // In-memory fallback if Redis is unavailable
    private final ConcurrentHashMap<String, OtpMetadata> localStore = new ConcurrentHashMap<>();

    private static class OtpMetadata {
        String hash;
        int attempts;
        Instant createdAt;
        Instant expiresAt;

        OtpMetadata(String hash, int attempts, Instant createdAt, Instant expiresAt) {
            this.hash = hash;
            this.attempts = attempts;
            this.createdAt = createdAt;
            this.expiresAt = expiresAt;
        }
    }

    @Autowired
    public OtpService(
            @Autowired(required = false) RedisTemplate<String, Object> redisTemplate,
            SmsService smsService) {
        this.redisTemplate = redisTemplate;
        this.smsService = smsService;
    }

    public void generateAndSendOtp(String phone) {
        // 1. Check cooldown
        if (isCooldownActive(phone)) {
            throw new BusinessException("OTP_COOLDOWN", "Please wait 60 seconds before requesting a new OTP.");
        }

        // 2. Generate 6-digit cryptographically secure OTP
        int otpInt = 100000 + RANDOM.nextInt(900000);
        String otp = String.valueOf(otpInt);
        String otpHash = hashOtp(otp);

        // 3. Store hash with TTL
        saveOtp(phone, otpHash);

        // 4. Send via SMS service (Never return OTP in API response)
        String message = "Your e-Uparjan login OTP is: " + otp + ". Valid for 5 mins. Do NOT share this with anyone.";
        smsService.sendOtp(phone, message);

        log.info("OTP generated and dispatched successfully for phone ending with ****{}", phone.substring(Math.max(0, phone.length() - 4)));
    }

    public boolean verifyOtp(String phone, String inputOtp) {
        String key = "otp:" + phone;
        String attemptKey = "otp_attempts:" + phone;

        // Try Redis first
        if (redisTemplate != null) {
            try {
                Object storedHashObj = redisTemplate.opsForValue().get(key);
                if (storedHashObj == null) {
                    throw new BusinessException("OTP_EXPIRED", "OTP has expired or does not exist. Please request a new one.");
                }

                Long attempts = redisTemplate.opsForValue().increment(attemptKey);
                if (attempts != null && attempts > MAX_ATTEMPTS) {
                    redisTemplate.delete(key);
                    redisTemplate.delete(attemptKey);
                    throw new BusinessException("MAX_ATTEMPTS_EXCEEDED", "Maximum verification attempts exceeded. Please request a new OTP.");
                }

                String inputHash = hashOtp(inputOtp);
                boolean matches = inputHash.equals(storedHashObj.toString());
                if (matches) {
                    redisTemplate.delete(key);
                    redisTemplate.delete(attemptKey);
                    return true;
                }
                return false;
            } catch (BusinessException be) {
                throw be;
            } catch (Exception e) {
                log.warn("Redis unavailable for OTP verification, falling back to in-memory store: {}", e.getMessage());
            }
        }

        // Fallback to local memory
        OtpMetadata meta = localStore.get(phone);
        if (meta == null || Instant.now().isAfter(meta.expiresAt)) {
            localStore.remove(phone);
            throw new BusinessException("OTP_EXPIRED", "OTP has expired or does not exist. Please request a new one.");
        }

        meta.attempts++;
        if (meta.attempts > MAX_ATTEMPTS) {
            localStore.remove(phone);
            throw new BusinessException("MAX_ATTEMPTS_EXCEEDED", "Maximum verification attempts exceeded. Please request a new OTP.");
        }

        String inputHash = hashOtp(inputOtp);
        boolean matches = inputHash.equals(meta.hash);
        if (matches) {
            localStore.remove(phone);
            return true;
        }
        return false;
    }

    private boolean isCooldownActive(String phone) {
        String cooldownKey = "otp_cooldown:" + phone;
        if (redisTemplate != null) {
            try {
                return Boolean.TRUE.equals(redisTemplate.hasKey(cooldownKey));
            } catch (Exception e) {
                log.debug("Redis error checking cooldown: {}", e.getMessage());
            }
        }
        OtpMetadata meta = localStore.get(phone);
        if (meta != null) {
            return Instant.now().isBefore(meta.createdAt.plusSeconds(COOLDOWN_SECONDS));
        }
        return false;
    }

    private void saveOtp(String phone, String otpHash) {
        String key = "otp:" + phone;
        String cooldownKey = "otp_cooldown:" + phone;
        String attemptKey = "otp_attempts:" + phone;

        if (redisTemplate != null) {
            try {
                redisTemplate.opsForValue().set(key, otpHash, OTP_EXPIRY_MINUTES, TimeUnit.MINUTES);
                redisTemplate.opsForValue().set(cooldownKey, "1", COOLDOWN_SECONDS, TimeUnit.SECONDS);
                redisTemplate.opsForValue().set(attemptKey, 0, OTP_EXPIRY_MINUTES, TimeUnit.MINUTES);
                return;
            } catch (Exception e) {
                log.warn("Redis error saving OTP, falling back: {}", e.getMessage());
            }
        }

        Instant now = Instant.now();
        localStore.put(phone, new OtpMetadata(otpHash, 0, now, now.plusSeconds(OTP_EXPIRY_MINUTES * 60L)));
    }

    private String hashOtp(String otp) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedhash = digest.digest(otp.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(encodedhash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }
}
