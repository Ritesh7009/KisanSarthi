package com.kisansarthi.sms;

import com.kisansarthi.common.ApiError;
import com.kisansarthi.common.ApiResponse;
import com.kisansarthi.common.BusinessException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/sms")
@Tag(name = "SMS Service", description = "e-Uparjan procurement SMS notifications and audit logging")
public class SmsController {

    private final SmsService smsService;
    private final SmsLogRepository smsLogRepository;

    public SmsController(SmsService smsService, SmsLogRepository smsLogRepository) {
        this.smsService = smsService;
        this.smsLogRepository = smsLogRepository;
    }

    @GetMapping("/config")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANDI_OPERATOR', 'MANDI_MANAGER', 'DISTRICT_OFFICER')")
    @Operation(summary = "Get SMS gateway configuration, active provider, and trial status")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSmsConfig() {
        Map<String, Object> config = new HashMap<>();
        boolean isTrial = smsService.isTrialMode();
        config.put("provider", smsService.getProviderName());
        config.put("trialMode", isTrial);
        config.put("accountType", isTrial ? "Trial" : "Full");
        config.put("trialTestPath", "/api/v1/sms/trial-test");
        config.put("predefinedTemplateName", "Order Confirmations");
        config.put("predefinedTemplateMessage", "Your 1234 order of 1 items has shipped and should be delivered on tomorrow. Details: https://twilio.com");
        config.put("productionDltAvailable", !isTrial);
        return ResponseEntity.ok(ApiResponse.ok(config));
    }

    @GetMapping({"", "/logs"})
    @PreAuthorize("hasAnyRole('ADMIN', 'MANDI_OPERATOR', 'MANDI_MANAGER', 'DISTRICT_OFFICER')")
    @Operation(summary = "Get recent SMS audit logs, optionally filtered by recipient phone number")
    public ResponseEntity<ApiResponse<List<SmsLogDto>>> getLogs(
            @Parameter(description = "Optional recipient 10-digit mobile number")
            @RequestParam(value = "phone", required = false) String phone
    ) {
        List<SmsLog> logs;
        if (phone != null && !phone.isBlank()) {
            String cleanPhone = phone.replaceAll("\\D", "");
            if (cleanPhone.length() >= 10) {
                cleanPhone = cleanPhone.substring(cleanPhone.length() - 10);
            }
            logs = smsLogRepository.findByRecipientPhoneOrderByCreatedAtDesc(cleanPhone);
            if (logs.isEmpty() && !cleanPhone.equals(phone.trim())) {
                logs = smsLogRepository.findByRecipientPhoneOrderByCreatedAtDesc(phone.trim());
            }
        } else {
            logs = smsService.getRecentLogs();
        }

        List<SmsLogDto> dtos = logs.stream().map(SmsLogDto::fromEntity).toList();
        return ResponseEntity.ok(ApiResponse.ok(dtos));
    }

    @PostMapping("/trial-test")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANDI_OPERATOR', 'MANDI_MANAGER', 'DISTRICT_OFFICER')")
    @Operation(summary = "Dispatch Twilio Trial Test SMS using pre-approved trial template to verified number")
    public ResponseEntity<ApiResponse<SendSmsResponse>> sendTrialTest(
            @RequestBody SendSmsRequest request
    ) {
        String phone = request.getRecipientPhone();
        if (phone == null || phone.isBlank()) {
            throw new BusinessException("PHONE_REQUIRED", "Recipient phone number is required");
        }

        String farmerName = request.getFarmerName() != null && !request.getFarmerName().isBlank()
                ? request.getFarmerName()
                : "Verified Test Recipient";

        String customTrialMsg = request.getCustomTrialMessage() != null && !request.getCustomTrialMessage().isBlank()
                ? request.getCustomTrialMessage()
                : request.getMessage();

        SmsResult result = smsService.sendTrialTestSms(phone, farmerName, customTrialMsg);

        if (result.isSuccess()) {
            SendSmsResponse response = new SendSmsResponse(
                    result.getSid(),
                    result.getStatus(),
                    phone,
                    farmerName,
                    customTrialMsg != null ? customTrialMsg : "Twilio Trial Order Confirmation Predefined Template"
            );
            return ResponseEntity.ok(ApiResponse.ok(
                    response,
                    "Twilio accepted Trial Test SMS (status: " + result.getStatus() + ")"
            ));
        } else {
            SendSmsResponse response = new SendSmsResponse(
                    null,
                    "FAILED",
                    phone,
                    farmerName,
                    customTrialMsg
            );
            response.setErrorCode(result.getErrorCode());
            response.setErrorMessage(result.getErrorMessage());

            ApiResponse<SendSmsResponse> errorResponse = new ApiResponse<>(
                    false,
                    response,
                    "Twilio Trial Test failed: " + (result.getErrorMessage() != null ? result.getErrorMessage() : "Twilio rejected request")
            );
            errorResponse.setError(new ApiError(
                    result.getErrorCode() != null ? "TWILIO_ERROR_" + result.getErrorCode() : "TWILIO_TRIAL_FAILED",
                    result.getErrorMessage() != null ? result.getErrorMessage() : "Twilio rejected request"
            ));

            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
    }

    @PostMapping("/send")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANDI_OPERATOR', 'MANDI_MANAGER', 'DISTRICT_OFFICER')")
    @Operation(summary = "Dispatch SMS message to farmer via configured provider (Twilio / Mock)")
    public ResponseEntity<ApiResponse<SendSmsResponse>> sendSms(
            @Valid @RequestBody SendSmsRequest request
    ) {
        String phone = request.getRecipientPhone();
        if (phone == null || phone.isBlank()) {
            throw new BusinessException("PHONE_REQUIRED", "Recipient phone number is required");
        }

        // If explicitly flagged as a trial test, route to trial test dispatch
        if (request.isTrialTest()) {
            return sendTrialTest(request);
        }

        // Check if account is in Trial mode: Trial mode prohibits custom DLT messages
        if (smsService.isTrialMode()) {
            String errorMsg = "Twilio Trial accounts cannot send this custom message. Use Twilio Trial Test mode or upgrade/configure the Twilio account for production SMS.";
            SendSmsResponse response = new SendSmsResponse(
                    null,
                    "FAILED",
                    phone,
                    request.getFarmerName(),
                    request.getMessage()
            );
            response.setErrorCode(400);
            response.setErrorMessage(errorMsg);

            ApiResponse<SendSmsResponse> errorResponse = new ApiResponse<>(false, response, errorMsg);
            errorResponse.setError(new ApiError("TWILIO_TRIAL_CUSTOM_RESTRICTION", errorMsg));
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }

        String message = request.getMessage();
        if (message == null || message.isBlank()) {
            throw new BusinessException("MESSAGE_REQUIRED", "Message content is required");
        }

        String farmerName = request.getFarmerName() != null && !request.getFarmerName().isBlank()
                ? request.getFarmerName()
                : "Farmer";

        // Synchronous dispatch to immediately capture real provider result
        SmsResult result = smsService.sendSms(phone, farmerName, message);

        if (result.isSuccess()) {
            SendSmsResponse response = new SendSmsResponse(
                    result.getSid(),
                    result.getStatus(),
                    phone,
                    farmerName,
                    message
            );
            return ResponseEntity.ok(ApiResponse.ok(
                    response,
                    "Twilio accepted SMS (status: " + result.getStatus() + ")"
            ));
        } else {
            SendSmsResponse response = new SendSmsResponse(
                    null,
                    "FAILED",
                    phone,
                    farmerName,
                    message
            );
            response.setErrorCode(result.getErrorCode());
            response.setErrorMessage(result.getErrorMessage());

            ApiResponse<SendSmsResponse> errorResponse = new ApiResponse<>(
                    false,
                    response,
                    "Twilio SMS failed: " + (result.getErrorMessage() != null ? result.getErrorMessage() : "Provider rejected request")
            );
            errorResponse.setError(new ApiError(
                    result.getErrorCode() != null ? "TWILIO_ERROR_" + result.getErrorCode() : "TWILIO_DISPATCH_FAILED",
                    result.getErrorMessage() != null ? result.getErrorMessage() : "Provider rejected request"
            ));

            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
    }
}
