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

    @GetMapping("/export/csv")
    @Operation(summary = "Export complete procurement register as CSV for state audits and e-Uparjan reporting")
    public ResponseEntity<byte[]> exportProcurementCsv(
            @RequestParam(required = false) String district,
            @RequestParam(required = false) String mandiId,
            @RequestParam(required = false) String cropId
    ) {
        List<ProcurementRegisterRowDto> rows = reportService.getProcurementRegister(district, mandiId, cropId);

        StringBuilder sb = new StringBuilder();
        sb.append("Token Number,Scheduled Date,Farmer Name,Phone,Masked Aadhaar,District,Mandi,Crop,Booked Qtl,Net Weight Qtl,Moisture %,Foreign Matter %,Payout Rs,Status,DBT Status,DBT Ref,Bank Account,IFSC,Completed At\n");

        for (ProcurementRegisterRowDto r : rows) {
            sb.append(escapeCsv(r.getTokenNumber())).append(",")
              .append(escapeCsv(r.getScheduledDate())).append(",")
              .append(escapeCsv(r.getFarmerName())).append(",")
              .append(escapeCsv(r.getFarmerPhone())).append(",")
              .append(escapeCsv(r.getMaskedAadhar())).append(",")
              .append(escapeCsv(r.getDistrict())).append(",")
              .append(escapeCsv(r.getMandiName())).append(",")
              .append(escapeCsv(r.getCropName())).append(",")
              .append(r.getEstimatedYieldQuintals() != null ? r.getEstimatedYieldQuintals().toString() : "").append(",")
              .append(r.getNetWeightQuintals() != null ? r.getNetWeightQuintals().toString() : "").append(",")
              .append(r.getMoisturePercentage() != null ? r.getMoisturePercentage().toString() : "").append(",")
              .append(r.getForeignMatterPercentage() != null ? r.getForeignMatterPercentage().toString() : "").append(",")
              .append(r.getTotalPayoutRs() != null ? r.getTotalPayoutRs().toString() : "").append(",")
              .append(escapeCsv(r.getStatus())).append(",")
              .append(escapeCsv(r.getPaymentStatus())).append(",")
              .append(escapeCsv(r.getDbtReferenceNo() != null ? r.getDbtReferenceNo() : "N/A")).append(",")
              .append(escapeCsv(r.getBankAccountLast4())).append(",")
              .append(escapeCsv(r.getIfscCode())).append(",")
              .append(escapeCsv(r.getCompletedAt() != null ? r.getCompletedAt() : "N/A"))
              .append("\n");
        }

        byte[] csvBytes = sb.toString().getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"kisansarthi_procurement_register.csv\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(csvBytes);
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
