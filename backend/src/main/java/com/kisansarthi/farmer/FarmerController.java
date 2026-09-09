package com.kisansarthi.farmer;

import com.kisansarthi.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/farmers")
@Tag(name = "Farmers", description = "Farmer profile management and official directory")
public class FarmerController {

    private final FarmerService farmerService;

    public FarmerController(FarmerService farmerService) {
        this.farmerService = farmerService;
    }

    @GetMapping("/me")
    @Operation(summary = "Get currently authenticated farmer profile")
    public ResponseEntity<ApiResponse<FarmerDto>> getMe(Authentication auth) {
        FarmerDto farmer = farmerService.getFarmerByUsername(auth.getName());
        return ResponseEntity.ok(ApiResponse.ok(farmer));
    }

    @PutMapping("/me")
    @Operation(summary = "Update currently authenticated farmer profile")
    public ResponseEntity<ApiResponse<FarmerDto>> updateMe(Authentication auth, @RequestBody UpdateFarmerRequest request) {
        FarmerDto updated = farmerService.updateFarmer(auth.getName(), request);
        return ResponseEntity.ok(ApiResponse.ok(updated, "Farmer profile updated successfully"));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANDI_OPERATOR', 'MANDI_MANAGER', 'DISTRICT_OFFICER')")
    @Operation(summary = "List all farmers (Administrative)")
    public ResponseEntity<ApiResponse<List<FarmerDto>>> getAllFarmers() {
        return ResponseEntity.ok(ApiResponse.ok(farmerService.getAllFarmers()));
    }
}
