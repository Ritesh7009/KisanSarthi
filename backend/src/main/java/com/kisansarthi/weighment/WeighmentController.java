package com.kisansarthi.weighment;

import com.kisansarthi.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANDI_OPERATOR', 'WEIGHBRIDGE_OPERATOR')")
    @Operation(summary = "Record weighbridge stage measurement (gross/tare/moisture)")
    public ResponseEntity<ApiResponse<WeighmentDto>> recordWeighment(
            @PathVariable UUID bookingId,
            @RequestBody WeighmentDto req
    ) {
        WeighmentDto dto = weighmentService.recordWeighment(bookingId, req);
        return ResponseEntity.ok(ApiResponse.ok(dto, "Weighment recorded successfully"));
    }
}
