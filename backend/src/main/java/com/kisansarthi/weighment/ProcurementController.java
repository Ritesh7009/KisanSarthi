package com.kisansarthi.weighment;

import com.kisansarthi.booking.BookingResponse;
import com.kisansarthi.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/bookings/{bookingId}/procurement")
@Tag(name = "Procurement Certification", description = "Finalize procurement, issue J-Form, and queue DBT settlement")
public class ProcurementController {

    private final WeighmentService weighmentService;

    public ProcurementController(WeighmentService weighmentService) {
        this.weighmentService = weighmentService;
    }

    @PostMapping("/complete")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANDI_MANAGER', 'DISTRICT_OFFICER', 'MANDI_OPERATOR')")
    @Operation(summary = "Finalize procurement certification, issue official J-Form and queue DBT payment")
    public ResponseEntity<ApiResponse<BookingResponse>> completeProcurement(
            @PathVariable UUID bookingId,
            Authentication authentication
    ) {
        String username = authentication != null ? authentication.getName() : "APMC Officer";
        BookingResponse response = weighmentService.completeProcurement(bookingId, username);
        return ResponseEntity.ok(ApiResponse.ok(response, "Procurement finalized and certified successfully"));
    }
}
