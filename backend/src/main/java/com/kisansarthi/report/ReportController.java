package com.kisansarthi.report;

import com.kisansarthi.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/reports")
@Tag(name = "Reports & Analytics", description = "District procurement progress, targets, and treasury payouts")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/district-stats")
    @Operation(summary = "Get Madhya Pradesh district-wise procurement targets and statistics")
    public ResponseEntity<ApiResponse<DistrictStatsReportDto>> getDistrictStats() {
        DistrictStatsReportDto report = reportService.getDistrictStats();
        return ResponseEntity.ok(ApiResponse.ok(report));
    }
}
