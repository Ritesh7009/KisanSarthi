package com.kisansarthi.mandi;

import com.kisansarthi.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/mandis")
@Tag(name = "Mandis", description = "APMC Mandi directory and live operational status")
public class MandiController {

    private final MandiService mandiService;

    public MandiController(MandiService mandiService) {
        this.mandiService = mandiService;
    }

    @GetMapping
    @Operation(summary = "List all APMC Mandis, optionally filtered by district")
    public ResponseEntity<ApiResponse<List<MandiDto>>> getAllMandis(@RequestParam(required = false) String district) {
        List<MandiDto> mandis = mandiService.getAllMandis(district);
        return ResponseEntity.ok(ApiResponse.ok(mandis));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get Mandi details by ID")
    public ResponseEntity<ApiResponse<MandiDto>> getMandiById(@PathVariable String id) {
        MandiDto mandi = mandiService.getMandiById(id);
        return ResponseEntity.ok(ApiResponse.ok(mandi));
    }

    @GetMapping("/{id}/status")
    @Operation(summary = "Get live Mandi queue, capacity, and gate status")
    public ResponseEntity<ApiResponse<MandiStatusDto>> getMandiStatus(@PathVariable String id) {
        MandiStatusDto status = mandiService.getMandiStatus(id);
        return ResponseEntity.ok(ApiResponse.ok(status));
    }
}
