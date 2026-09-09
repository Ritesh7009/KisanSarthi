package com.kisansarthi.slot;

import com.kisansarthi.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Tag(name = "Slots", description = "Mandi procurement time-slots and intake capacity")
public class SlotController {

    private final SlotService slotService;

    public SlotController(SlotService slotService) {
        this.slotService = slotService;
    }

    @GetMapping("/api/v1/mandis/{mandiId}/slots")
    @Operation(summary = "Get available intake time-slots for a Mandi")
    public ResponseEntity<ApiResponse<List<MandiSlot>>> getSlots(@PathVariable String mandiId) {
        List<MandiSlot> slots = slotService.getSlotsByMandiId(mandiId);
        return ResponseEntity.ok(ApiResponse.ok(slots));
    }

    @PostMapping("/api/v1/mandis/{mandiId}/slots")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANDI_MANAGER')")
    @Operation(summary = "Add a new procurement time-slot to Mandi")
    public ResponseEntity<ApiResponse<MandiSlot>> createSlot(@PathVariable String mandiId, @RequestBody MandiSlot slot) {
        MandiSlot created = slotService.createSlot(mandiId, slot);
        return ResponseEntity.ok(ApiResponse.ok(created, "Slot created successfully"));
    }

    @PutMapping("/api/v1/slots/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANDI_MANAGER')")
    @Operation(summary = "Update slot capacity or availability")
    public ResponseEntity<ApiResponse<MandiSlot>> updateSlot(@PathVariable String id, @RequestBody MandiSlot slot) {
        MandiSlot updated = slotService.updateSlot(id, slot);
        return ResponseEntity.ok(ApiResponse.ok(updated, "Slot updated successfully"));
    }
}
