package com.kisansarthi.report;

import com.kisansarthi.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/v1/reports")
@Tag(name = "Reports & Analytics", description = "District procurement progress, MSP targets, Mandi bottlenecks, QC, DBT, and audit exports")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/overview")
    @Operation(summary = "Get high-level statewide executive overview of procurement, value, DBT, and live queues")
    public ResponseEntity<ApiResponse<StatewideOverviewDto>> getStatewideOverview() {
        return ResponseEntity.ok(ApiResponse.ok(reportService.getStatewideOverview()));
    }

    @GetMapping("/district-stats")
    @Operation(summary = "Get Madhya Pradesh district-wise procurement targets, progress, and financial metrics")
    public ResponseEntity<ApiResponse<DistrictStatsReportDto>> getDistrictStats() {
        DistrictStatsReportDto report = reportService.getDistrictStats();
        return ResponseEntity.ok(ApiResponse.ok(report));
    }

    @GetMapping("/mandi-performance")
    @Operation(summary = "Get Mandi-level real-time throughput, wait times, capacity utilization, and load status")
    public ResponseEntity<ApiResponse<List<MandiPerformanceDto>>> getMandiPerformance(
            @RequestParam(required = false) String district
    ) {
        return ResponseEntity.ok(ApiResponse.ok(reportService.getMandiPerformance(district)));
    }

    @GetMapping("/bottlenecks")
    @Operation(summary = "Detect active bottlenecks across mandis (queue congestion, SLA breaches, capacity pressure)")
    public ResponseEntity<ApiResponse<List<BottleneckAlertDto>>> getBottlenecks() {
        return ResponseEntity.ok(ApiResponse.ok(reportService.detectBottlenecks()));
    }

    @GetMapping("/crops")
    @Operation(summary = "Get crop-wise procurement volumes, MSP settlement amounts, and farmer participation")
    public ResponseEntity<ApiResponse<List<CropProcurementReportDto>>> getCropProcurementReports() {
        return ResponseEntity.ok(ApiResponse.ok(reportService.getCropProcurementReports()));
    }

    @GetMapping("/quality-weighment")
    @Operation(summary = "Get weighbridge statistics, average moisture %, FAQ compliance, and dockage analysis")
    public ResponseEntity<ApiResponse<QualityAndWeighmentReportDto>> getQualityAndWeighmentReport() {
        return ResponseEntity.ok(ApiResponse.ok(reportService.getQualityAndWeighmentReport()));
    }

    @GetMapping("/payment-analytics")
    @Operation(summary = "Get DBT and PFMS payment reconciliation, status breakdown, and delay SLA alerts")
    public ResponseEntity<ApiResponse<PaymentAnalyticsReportDto>> getPaymentAnalytics(
            @RequestParam(required = false) Long delaySlaHours,
            @RequestParam(required = false) Long delayThresholdHours
    ) {
        long effectiveHours = delaySlaHours != null ? delaySlaHours : (delayThresholdHours != null ? delayThresholdHours : 24L);
        return ResponseEntity.ok(ApiResponse.ok(reportService.getPaymentAnalytics(effectiveHours)));
    }

    @GetMapping("/time-series")
    @Operation(summary = "Get daily time-series metrics for procurement quantity, bookings, and financial disbursements")
    public ResponseEntity<ApiResponse<List<TimeSeriesPointDto>>> getTimeSeries(
            @RequestParam(defaultValue = "7") int days
    ) {
        return ResponseEntity.ok(ApiResponse.ok(reportService.getTimeSeries(days)));
    }

    @GetMapping("/procurement-register")
    @Operation(summary = "Get granular procurement transactions register with filter criteria")
    public ResponseEntity<ApiResponse<List<ProcurementRegisterRowDto>>> getProcurementRegister(
            @RequestParam(required = false) String district,
            @RequestParam(required = false) String mandiId,
            @RequestParam(required = false) String cropId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(reportService.getProcurementRegister(district, mandiId, cropId)));
    }

    @GetMapping("/procurement-register/paginated")
    @Operation(summary = "Get paginated granular procurement transactions register with filter criteria")
    public ResponseEntity<ApiResponse<PaginatedProcurementRegisterDto>> getPaginatedProcurementRegister(
            @RequestParam(required = false) String district,
            @RequestParam(required = false) String mandiId,
            @RequestParam(required = false) String cropId,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(ApiResponse.ok(reportService.getPaginatedProcurementRegister(district, mandiId, cropId, search, page, size)));
    }

    @GetMapping("/export/csv")
    @Operation(summary = "Export complete procurement register as CSV for state audits and e-Uparjan reporting using memory-bounded streaming")
    public ResponseEntity<org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody> exportProcurementCsv(
            @RequestParam(required = false) String district,
            @RequestParam(required = false) String mandiId,
            @RequestParam(required = false) String cropId,
            @RequestParam(required = false) String search
    ) {
        // Enforce authorization synchronously on the HTTP request thread BEFORE committing response headers
        String effectiveDistrict = reportService.resolveAndAuthorizeDistrict(district);
        reportService.verifyMandiAccess(mandiId);

        org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody responseBody = outputStream -> {
            reportService.streamProcurementRegisterCsvWithAuthorizedDistrict(effectiveDistrict, mandiId, cropId, search, outputStream);
        };

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"kisansarthi_procurement_register.csv\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(responseBody);
    }
}
