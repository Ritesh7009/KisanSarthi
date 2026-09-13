package com.kisansarthi.weighment;

import com.kisansarthi.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/bookings/{bookingId}/weighment")
@Tag(name = "Weighment", description = "Dual-stage weighbridge gross, tare, and net yield certification")
public class WeighmentController {

    private final WeighmentService weighmentService;

    public WeighmentController(WeighmentService weighmentService) {
        this.weighmentService = weighmentService;
    }

    @GetMapping
    @Operation(summary = "Get certified weighment slip for a booking")
    public ResponseEntity<ApiResponse<WeighmentDto>> getWeighment(@PathVariable UUID bookingId) {
        WeighmentDto dto = weighmentService.getWeighment(bookingId);
        return ResponseEntity.ok(ApiResponse.ok(dto));
    }

    @PostMapping("/start")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANDI_OPERATOR', 'WEIGHBRIDGE_OPERATOR')")
    @Operation(summary = "Admit vehicle to weighbridge bay and start weighment process")
    public ResponseEntity<ApiResponse<WeighmentDto>> startWeighment(
            @PathVariable UUID bookingId,
            @RequestParam(value = "bay", required = false, defaultValue = "Kanta Bay 1") String bay,
            Authentication authentication
    ) {
        String username = authentication != null ? authentication.getName() : "Weighbridge Operator";
        WeighmentDto dto = weighmentService.startWeighment(bookingId, bay, username);
        return ResponseEntity.ok(ApiResponse.ok(dto, "Weighment initiated at " + bay));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANDI_OPERATOR', 'WEIGHBRIDGE_OPERATOR')")
    @Operation(summary = "Record weighbridge stage measurement (gross/tare/moisture)")
    public ResponseEntity<ApiResponse<WeighmentDto>> recordWeighment(
            @PathVariable UUID bookingId,
            @RequestBody WeighmentDto req,
            Authentication authentication
    ) {
        String username = authentication != null ? authentication.getName() : null;
        WeighmentDto dto = weighmentService.recordWeighment(bookingId, req, username);
        return ResponseEntity.ok(ApiResponse.ok(dto, "Weighment recorded successfully"));
    }

    @PostMapping("/complete")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANDI_OPERATOR', 'WEIGHBRIDGE_OPERATOR')")
    @Operation(summary = "Complete and certify net yield weighment")
    public ResponseEntity<ApiResponse<WeighmentDto>> completeWeighment(
            @PathVariable UUID bookingId,
            @RequestBody(required = false) WeighmentDto req,
            Authentication authentication
    ) {
        String username = authentication != null ? authentication.getName() : null;
        WeighmentDto payload = req != null ? req : new WeighmentDto();
        WeighmentDto dto = weighmentService.recordWeighment(bookingId, payload, username);
        return ResponseEntity.ok(ApiResponse.ok(dto, "Weighment certified successfully"));
    }
}
