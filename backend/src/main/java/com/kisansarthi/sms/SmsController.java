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

import java.util.List;

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
