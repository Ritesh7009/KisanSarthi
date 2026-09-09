package com.kisansarthi.crop;

import com.kisansarthi.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/crops")
@Tag(name = "Crops & MSP", description = "Commodity specifications, MSP, and MP State Bonus management")
public class CropController {

    private final CropService cropService;

    public CropController(CropService cropService) {
        this.cropService = cropService;
    }

    @GetMapping
    @Operation(summary = "List all registered crops and current MSP rates")
    public ResponseEntity<ApiResponse<List<CropDto>>> getAllCrops() {
        return ResponseEntity.ok(ApiResponse.ok(cropService.getAllCrops()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get crop specifications by ID")
    public ResponseEntity<ApiResponse<CropDto>> getCropById(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.ok(cropService.getCropById(id)));
    }

    @PostMapping("/{id}/msp")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update standard MSP or MP State Bonus rate (Admin only)")
    public ResponseEntity<ApiResponse<CropDto>> updateMsp(@PathVariable String id, @Valid @RequestBody UpdateMspRequest request) {
        CropDto updated = cropService.updateCropMsp(id, request);
        return ResponseEntity.ok(ApiResponse.ok(updated, "MSP rate updated successfully"));
    }
}
