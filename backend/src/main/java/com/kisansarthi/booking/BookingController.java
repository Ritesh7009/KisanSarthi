package com.kisansarthi.booking;

import com.kisansarthi.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/bookings")
@Tag(name = "Bookings & Tokens", description = "Procurement slot booking and token queue lifecycle")
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @PostMapping
    @Operation(summary = "Create a procurement booking and generate concurrency-safe APMC token")
    public ResponseEntity<ApiResponse<BookingResponse>> createBooking(
            @Valid @RequestBody CreateBookingRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            Authentication authentication
    ) {
        String username = authentication != null ? authentication.getName() : null;
        BookingResponse response = bookingService.createBooking(request, idempotencyKey, username);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(response, "Token " + response.getTokenNumber() + " successfully generated"));
    }

    @GetMapping("/my")
    @Operation(summary = "Get bookings for the currently authenticated farmer")
    public ResponseEntity<ApiResponse<List<BookingResponse>>> getMyBookings(Authentication authentication) {
        List<BookingResponse> bookings = bookingService.getBookingsByFarmer(authentication.getName());
        return ResponseEntity.ok(ApiResponse.ok(bookings));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get booking details by UUID")
    public ResponseEntity<ApiResponse<BookingResponse>> getBookingById(
            @Parameter(description = "Booking UUID") @PathVariable UUID id
    ) {
        BookingResponse booking = bookingService.getBookingById(id);
        return ResponseEntity.ok(ApiResponse.ok(booking));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANDI_OPERATOR', 'MANDI_MANAGER', 'DISTRICT_OFFICER')")
    @Operation(summary = "List all bookings (Administrative / Mandi operator view)")
    public ResponseEntity<ApiResponse<List<BookingResponse>>> getAllBookings(
            @RequestParam(value = "mandiId", required = false) String mandiId
    ) {
        List<BookingResponse> bookings = bookingService.getAllBookings(mandiId);
        return ResponseEntity.ok(ApiResponse.ok(bookings));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANDI_OPERATOR', 'MANDI_MANAGER')")
    @Operation(summary = "Transition booking token status along the procurement state machine")
    public ResponseEntity<ApiResponse<BookingResponse>> updateStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateBookingStatusRequest request
    ) {
        BookingResponse updated = bookingService.transitionStatus(id, request.getStatus());
        return ResponseEntity.ok(ApiResponse.ok(updated, "Status transitioned to " + request.getStatus()));
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancel an upcoming booking")
    public ResponseEntity<ApiResponse<BookingResponse>> cancelBooking(
            @PathVariable UUID id,
            Authentication authentication
    ) {
        BookingResponse cancelled = bookingService.cancelBooking(id, authentication.getName());
        return ResponseEntity.ok(ApiResponse.ok(cancelled, "Booking cancelled successfully"));
    }
}
