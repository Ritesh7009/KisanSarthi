package com.kisansarthi.payment;

import com.kisansarthi.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/bookings/{bookingId}/payment")
@Tag(name = "DBT Payment", description = "Direct Benefit Transfer settlement and payment status")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @GetMapping
    @Operation(summary = "Get DBT payment status for a booking")
    public ResponseEntity<ApiResponse<PaymentDto>> getPayment(@PathVariable UUID bookingId) {
        PaymentDto dto = paymentService.getPayment(bookingId);
        return ResponseEntity.ok(ApiResponse.ok(dto));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANDI_MANAGER', 'DISTRICT_OFFICER', 'MANDI_OPERATOR')")
    @Operation(summary = "Initiate DBT settlement payment to farmer bank account")
    public ResponseEntity<ApiResponse<PaymentDto>> initiatePayment(
            @PathVariable UUID bookingId,
            @RequestBody(required = false) PaymentDto req
    ) {
        PaymentDto dto = paymentService.initiatePayment(bookingId, req != null ? req : new PaymentDto());
        return ResponseEntity.ok(ApiResponse.ok(dto, "DBT Payment initiated successfully"));
    }

    @PostMapping("/confirm")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANDI_MANAGER', 'DISTRICT_OFFICER')")
    @Operation(summary = "Confirm DBT bank credit acknowledgement")
    public ResponseEntity<ApiResponse<PaymentDto>> confirmPayment(
            @PathVariable UUID bookingId,
            @RequestBody(required = false) Map<String, String> body
    ) {
        String dbtRef = body != null ? body.get("dbtReferenceNo") : null;
        PaymentDto dto = paymentService.confirmPaymentCredit(bookingId, dbtRef);
        return ResponseEntity.ok(ApiResponse.ok(dto, "DBT Payment credit confirmed"));
    }
}
