package com.kisansarthi.report;

import com.kisansarthi.booking.Booking;
import com.kisansarthi.booking.BookingRepository;
import com.kisansarthi.booking.BookingStatus;
import com.kisansarthi.crop.Crop;
import com.kisansarthi.crop.CropRepository;
import com.kisansarthi.farmer.Farmer;
import com.kisansarthi.farmer.FarmerRepository;
import com.kisansarthi.mandi.Mandi;
import com.kisansarthi.mandi.MandiRepository;
import com.kisansarthi.payment.Payment;
import com.kisansarthi.payment.PaymentRepository;
import com.kisansarthi.slot.MandiSlot;
import com.kisansarthi.slot.SlotRepository;
import com.kisansarthi.weighment.Weighment;
import com.kisansarthi.weighment.WeighmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ReportService {

    private final MandiRepository mandiRepository;
    private final BookingRepository bookingRepository;
    private final FarmerRepository farmerRepository;
    private final CropRepository cropRepository;
    private final WeighmentRepository weighmentRepository;
    private final PaymentRepository paymentRepository;
    private final SlotRepository slotRepository;
    private final ProcurementTargetRepository targetRepository;

    private static final ZoneId IST_ZONE = ZoneId.of("Asia/Kolkata");

    private static final List<BookingStatus> COMPLETED_STATUSES = List.of(
            BookingStatus.PROCUREMENT_COMPLETED,
            BookingStatus.PAYMENT_PENDING,
            BookingStatus.PAYMENT_INITIATED,
            BookingStatus.PAYMENT_PROCESSING,
            BookingStatus.PAYMENT_CREDITED,
            BookingStatus.COMPLETED
    );

    public ReportService(
            MandiRepository mandiRepository,
            BookingRepository bookingRepository,
            FarmerRepository farmerRepository,
            CropRepository cropRepository,
            WeighmentRepository weighmentRepository,
            PaymentRepository paymentRepository,
            SlotRepository slotRepository,
            ProcurementTargetRepository targetRepository
    ) {
        this.mandiRepository = mandiRepository;
        this.bookingRepository = bookingRepository;
        this.farmerRepository = farmerRepository;
        this.cropRepository = cropRepository;
        this.weighmentRepository = weighmentRepository;
        this.paymentRepository = paymentRepository;
        this.slotRepository = slotRepository;
        this.targetRepository = targetRepository;
    }

    // ==========================================
    // 1. STATEWIDE OVERVIEW (DATABASE AGGREGATED)
    // ==========================================
    @Transactional(readOnly = true)
    public StatewideOverviewDto getStatewideOverview() {
        long totalBookings = bookingRepository.count();
        long totalCompletedProcurements = bookingRepository.countByStatusIn(COMPLETED_STATUSES);
        BigDecimal totalCertifiedQuantity = bookingRepository.sumNetWeightByStatusIn(COMPLETED_STATUSES).setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalProcurementValue = bookingRepository.sumSettlementAmountByStatusIn(COMPLETED_STATUSES).setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalDbtDisbursed = paymentRepository.sumSettledPayments().setScale(2, RoundingMode.HALF_UP);
        long totalFarmersServed = bookingRepository.countDistinctFarmersByStatusIn(COMPLETED_STATUSES);

        long totalActiveMandis = mandiRepository.countByGateStatusIgnoreCase("OPEN");

        List<BookingStatus> queueStatuses = List.of(
                BookingStatus.GATE_CALLED,
                BookingStatus.GATE_ENTERED,
                BookingStatus.WEIGHING,
                BookingStatus.WEIGHMENT_STAGE_1,
                BookingStatus.QUALITY_CHECK,
                BookingStatus.WEIGHMENT_STAGE_2
        );
        long totalWaitingFarmers = bookingRepository.countByStatusIn(queueStatuses);

        Map<String, Long> statusBreakdown = new HashMap<>();
        List<Object[]> statusCounts = bookingRepository.countGroupedByStatus();
        for (Object[] row : statusCounts) {
            if (row[0] != null && row[1] != null) {
                statusBreakdown.put(row[0].toString(), ((Number) row[1]).longValue());
            }
        }

        return new StatewideOverviewDto(
                totalBookings,
                totalCompletedProcurements,
                totalCertifiedQuantity,
                totalProcurementValue,
                totalDbtDisbursed,
                totalFarmersServed,
                totalActiveMandis,
                totalWaitingFarmers,
                statusBreakdown
        );
    }

    // ==========================================
    // 2. DISTRICT-WISE ANALYTICS (DATABASE AGGREGATED)
    // ==========================================
    @Transactional(readOnly = true)
    public DistrictStatsReportDto getDistrictStats() {
        List<Mandi> allMandis = mandiRepository.findAll();
        List<ProcurementTarget> targets = targetRepository.findAll();

        Map<String, List<Mandi>> mandisByDistrict = allMandis.stream()
                .collect(Collectors.groupingBy(Mandi::getDistrict));

        Map<String, ProcurementTarget> targetByDistrict = targets.stream()
                .collect(Collectors.toMap(
                        t -> t.getDistrict().toLowerCase(),
                        t -> t,
                        (existing, replacement) -> existing
                ));

        Map<String, Long> farmersByDistrict = new HashMap<>();
        for (Object[] row : farmerRepository.countFarmersByDistrict()) {
            if (row[0] != null && row[1] != null) {
                farmersByDistrict.put((String) row[0], ((Number) row[1]).longValue());
            }
        }

        // Aggregate bookings grouped by district in SQL
        // row: [district, totalBookings, completedBookings, sumNetWeight, sumSettlement, distinctFarmers]
        Map<String, Object[]> bookingAggByDistrict = new HashMap<>();
        for (Object[] row : bookingRepository.aggregateProcurementByDistrict(COMPLETED_STATUSES)) {
            if (row[0] != null) {
                bookingAggByDistrict.put((String) row[0], row);
            }
        }

        // Aggregate today's booked yields by district in SQL
        Map<String, BigDecimal> bookedTodayByDistrict = new HashMap<>();
        for (Object[] row : bookingRepository.sumEstimatedYieldTodayByDistrict(LocalDate.now(IST_ZONE))) {
            if (row[0] != null && row[1] != null) {
                bookedTodayByDistrict.put((String) row[0], (BigDecimal) row[1]);
            }
        }

        // Aggregate settled payments by district in SQL
        Map<String, BigDecimal> settledPaymentsByDistrict = new HashMap<>();
        for (Object[] row : paymentRepository.sumSettledPaymentsByDistrict()) {
            if (row[0] != null && row[1] != null) {
                settledPaymentsByDistrict.put((String) row[0], (BigDecimal) row[1]);
            }
        }

        Set<String> districtNames = new LinkedHashSet<>();
        mandisByDistrict.keySet().forEach(districtNames::add);
        targets.forEach(t -> districtNames.add(t.getDistrict()));

        List<DistrictProcurementStatDto> dtoList = new ArrayList<>();

        for (String district : districtNames) {
            List<Mandi> districtMandis = mandisByDistrict.getOrDefault(district, Collections.emptyList());
            int activeMandis = (int) districtMandis.stream().filter(m -> "OPEN".equalsIgnoreCase(m.getGateStatus())).count();
            if (activeMandis == 0 && !districtMandis.isEmpty()) {
                activeMandis = districtMandis.size();
            }

            int registeredFarmers = farmersByDistrict.getOrDefault(district, 0L).intValue();
            String hindiDistrict = districtMandis.isEmpty() ? district : districtMandis.get(0).getHindiDistrict();

            ProcurementTarget target = targetByDistrict.get(district.toLowerCase());
            double targetQtl = target != null ? target.getTargetQuintals().doubleValue() : 500000.0;
            double warehouseCapQtl = target != null ? target.getWarehouseCapacityQuintals().doubleValue() : (targetQtl * 1.2);

            Object[] agg = bookingAggByDistrict.get(district);
            long totalBookings = agg != null ? ((Number) agg[1]).longValue() : 0L;
            int completedCount = agg != null ? ((Number) agg[2]).intValue() : 0;
            double actualProcuredQtl = agg != null ? ((BigDecimal) agg[3]).doubleValue() : 0.0;
            double totalPayoutRs = agg != null ? ((BigDecimal) agg[4]).doubleValue() : 0.0;
            int farmersServed = agg != null ? ((Number) agg[5]).intValue() : 0;
            int pendingCount = (int) (totalBookings - completedCount);

            double totalPayoutLakhs = totalPayoutRs / 100000.0;

            BigDecimal dbtSettled = settledPaymentsByDistrict.getOrDefault(district, BigDecimal.ZERO);
            double dbtDisbursedCrores = dbtSettled.doubleValue() / 10000000.0;

            int totalSlotsToday = districtMandis.stream().mapToInt(Mandi::getDailyCapacityQuintals).sum();
            int bookedSlotsToday = bookedTodayByDistrict.getOrDefault(district, BigDecimal.ZERO).intValue();

            double achievementPct = targetQtl > 0 ? (actualProcuredQtl / targetQtl) * 100.0 : 0.0;
            String status = achievementPct > 85.0 ? "HIGH_VOLUME" : (pendingCount > 20 ? "CONGESTED" : "NORMAL");

            DistrictProcurementStatDto dto = new DistrictProcurementStatDto(
                    district,
                    hindiDistrict,
                    registeredFarmers,
                    activeMandis,
                    totalSlotsToday,
                    bookedSlotsToday,
                    actualProcuredQtl,
                    targetQtl,
                    totalPayoutLakhs,
                    status
            );
            dto.setAchievementPercentage(achievementPct);
            dto.setFarmersServed(farmersServed);
            dto.setCompletedTransactions(completedCount);
            dto.setPendingTransactions(pendingCount);
            dto.setTotalProcurementValueLakhs(totalPayoutLakhs);
            dto.setTotalDbtDisbursedCrores(dbtDisbursedCrores);
            dto.setWarehouseCapacityQuintals(warehouseCapQtl);
            dto.setWarehouseOccupiedQuintals(Math.min(warehouseCapQtl, actualProcuredQtl));

            dtoList.add(dto);
        }

        return new DistrictStatsReportDto(
                dtoList,
                allMandis.size(),
                (int) bookingRepository.count(),
                (int) farmerRepository.count()
        );
    }

    // ==========================================
    // 3. MANDI-LEVEL PERFORMANCE & REAL-TIME STATUS
    // ==========================================
    @Transactional(readOnly = true)
    public List<MandiPerformanceDto> getMandiPerformance(String districtFilter) {
        List<Mandi> mandis = mandiRepository.findAll();
        if (districtFilter != null && !districtFilter.isBlank()) {
            mandis = mandis.stream()
                    .filter(m -> m.getDistrict().equalsIgnoreCase(districtFilter.trim()))
                    .toList();
        }

        // Aggregate slot capacities by mandi using SQL aggregation (removes full slot table load)
        Map<String, Object[]> slotAggByMandi = new HashMap<>();
        for (Object[] row : slotRepository.aggregateSlotCapacitiesByMandi()) {
            if (row[0] != null) {
                slotAggByMandi.put((String) row[0], row);
            }
        }

        // Aggregate bookings by mandi: [mandiId, totalBookings, completedCount, sumNetWeight]
        Map<String, Object[]> bookingAggByMandi = new HashMap<>();
        for (Object[] row : bookingRepository.aggregateProcurementByMandi(COMPLETED_STATUSES)) {
            if (row[0] != null) {
                bookingAggByMandi.put((String) row[0], row);
            }
        }

        // Aggregate settled payments by mandi
        Map<String, BigDecimal> dbtByMandi = new HashMap<>();
        for (Object[] row : paymentRepository.sumSettledPaymentsByMandi()) {
            if (row[0] != null && row[1] != null) {
                dbtByMandi.put((String) row[0], (BigDecimal) row[1]);
            }
        }

        List<MandiPerformanceDto> result = new ArrayList<>();

        for (Mandi mandi : mandis) {
            Object[] slotAgg = slotAggByMandi.get(mandi.getId());
            Object[] agg = bookingAggByMandi.get(mandi.getId());

            long totalBookings = agg != null ? ((Number) agg[1]).longValue() : 0L;
            long completedCount = agg != null ? ((Number) agg[2]).longValue() : 0L;
            BigDecimal certifiedQtl = agg != null ? ((BigDecimal) agg[3]).setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

            int remainingCapacity;
            if (slotAgg != null) {
                remainingCapacity = ((Number) slotAgg[3]).intValue();
            } else {
                remainingCapacity = mandi.getDailyCapacityQuintals();
            }

            int avgProcessingMins = mandi.getAverageProcessingMins() > 0 ? mandi.getAverageProcessingMins() : 15;
            int queueLen = mandi.getActiveTokensWaiting();
            int waitTime = queueLen * avgProcessingMins;

            // Throughput: completed transactions per operating hour (dynamically calculated from operating window)
            double operatingHours = calculateOperatingHours(mandi.getOpenTime(), mandi.getCloseTime());
            double throughputPerHour = operatingHours > 0.0
                    ? Math.round((completedCount / operatingHours) * 10.0) / 10.0
                    : 0.0;
            BigDecimal totalDbt = dbtByMandi.getOrDefault(mandi.getId(), BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);

            // Deterministic real-time status calculation
            String derivedStatus;
            String bottleneck = null;

            if (!"OPEN".equalsIgnoreCase(mandi.getGateStatus())) {
                derivedStatus = "CLOSED";
                bottleneck = "Mandi gate is officially closed";
            } else if (remainingCapacity <= 0) {
                derivedStatus = "FULL";
                bottleneck = "All slot capacities exhausted for today";
            } else if (queueLen >= 25 || waitTime >= 90) {
                derivedStatus = "HIGH_LOAD";
                bottleneck = "High queue length (" + queueLen + " vehicles waiting, " + waitTime + " mins est. wait)";
            } else if (queueLen >= 10 || waitTime >= 40) {
                derivedStatus = "BUSY";
            } else {
                derivedStatus = "AVAILABLE";
            }

            MandiPerformanceDto dto = new MandiPerformanceDto();
            dto.setMandiId(mandi.getId());
            dto.setMandiName(mandi.getName());
            dto.setHindiName(mandi.getHindiName());
            dto.setDistrict(mandi.getDistrict());
            dto.setTotalBookings(totalBookings);
            dto.setCompletedProcurements(completedCount);
            dto.setCertifiedQuantityQuintals(certifiedQtl);
            dto.setRemainingSlotCapacityQuintals(remainingCapacity);
            dto.setAverageProcessingMins(avgProcessingMins);
            dto.setEstimatedWaitTimeMins(waitTime);
            dto.setCurrentQueueLength(queueLen);
            dto.setThroughputPerHour(throughputPerHour);
            dto.setTotalDbtAmountRs(totalDbt);
            dto.setStatus(derivedStatus);
            dto.setBottleneckReason(bottleneck);

            result.add(dto);
        }

        return result;
    }

    // ==========================================
    // 4. BOTTLENECK DETECTION (DETERMINISTIC RULES)
    // ==========================================
    @Transactional(readOnly = true)
    public List<BottleneckAlertDto> detectBottlenecks() {
        List<MandiPerformanceDto> mandiStats = getMandiPerformance(null);
        List<BottleneckAlertDto> alerts = new ArrayList<>();

        for (MandiPerformanceDto m : mandiStats) {
            // Rule 1: High Queue Length
            if (m.getCurrentQueueLength() >= 20) {
                alerts.add(new BottleneckAlertDto(
                        m.getMandiId(),
                        m.getMandiName(),
                        m.getDistrict(),
                        m.getCurrentQueueLength() >= 30 ? "CRITICAL" : "MEDIUM",
                        "QUEUE_CONGESTION",
                        m.getCurrentQueueLength() + " vehicles in queue",
                        "Active vehicle queue exceeds operational safety threshold of 20 vehicles.",
                        m.getCurrentQueueLength(),
                        m.getEstimatedWaitTimeMins(),
                        m.getRemainingSlotCapacityQuintals()
                ));
            }

            // Rule 2: High Wait Time
            if (m.getEstimatedWaitTimeMins() >= 60) {
                alerts.add(new BottleneckAlertDto(
                        m.getMandiId(),
                        m.getMandiName(),
                        m.getDistrict(),
                        m.getEstimatedWaitTimeMins() >= 120 ? "CRITICAL" : "MEDIUM",
                        "HIGH_WAIT_TIME",
                        m.getEstimatedWaitTimeMins() + " mins wait time",
                        "Estimated farmer turnaround delay exceeds the 60-minute Citizen Charter SLA.",
                        m.getCurrentQueueLength(),
                        m.getEstimatedWaitTimeMins(),
                        m.getRemainingSlotCapacityQuintals()
                ));
            }

            // Rule 3: Capacity Pressure
            if (m.getRemainingSlotCapacityQuintals() <= 200 && !"CLOSED".equals(m.getStatus())) {
                alerts.add(new BottleneckAlertDto(
                        m.getMandiId(),
                        m.getMandiName(),
                        m.getDistrict(),
                        m.getRemainingSlotCapacityQuintals() <= 50 ? "CRITICAL" : "LOW",
                        "CAPACITY_PRESSURE",
                        m.getRemainingSlotCapacityQuintals() + " Qtl remaining",
                        "Remaining intake intake capacity is nearly saturated.",
                        m.getCurrentQueueLength(),
                        m.getEstimatedWaitTimeMins(),
                        m.getRemainingSlotCapacityQuintals()
                ));
            }
        }

        return alerts;
    }

    // ==========================================
    // 5. CROP-WISE ANALYTICS (DATABASE AGGREGATED)
    // ==========================================
    @Transactional(readOnly = true)
    public List<CropProcurementReportDto> getCropProcurementReports() {
        List<Crop> crops = cropRepository.findAll();

        // Aggregate bookings grouped by crop in SQL
        // row: [cropId, totalBookings, completedCount, sumNetWeight, sumSettlement, distinctFarmers]
        Map<String, Object[]> cropAggMap = new HashMap<>();
        for (Object[] row : bookingRepository.aggregateProcurementByCrop(COMPLETED_STATUSES)) {
            if (row[0] != null) {
                cropAggMap.put((String) row[0], row);
            }
        }

        // Aggregate settled payments by crop in SQL
        Map<String, BigDecimal> settledPaymentsByCrop = new HashMap<>();
        for (Object[] row : paymentRepository.sumSettledPaymentsByCrop()) {
            if (row[0] != null && row[1] != null) {
                settledPaymentsByCrop.put((String) row[0], (BigDecimal) row[1]);
            }
        }

        List<CropProcurementReportDto> reports = new ArrayList<>();

        for (Crop crop : crops) {
            Object[] agg = cropAggMap.get(crop.getId());

            long totalBookings = agg != null ? ((Number) agg[1]).longValue() : 0L;
            long completedCount = agg != null ? ((Number) agg[2]).longValue() : 0L;
            BigDecimal certifiedQtl = agg != null ? ((BigDecimal) agg[3]).setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            BigDecimal totalValue = agg != null ? ((BigDecimal) agg[4]).setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            long farmersServed = agg != null ? ((Number) agg[5]).longValue() : 0L;

            BigDecimal avgQtl = farmersServed > 0
                    ? certifiedQtl.divide(BigDecimal.valueOf(farmersServed), 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

            BigDecimal dbtDisbursed = settledPaymentsByCrop.getOrDefault(crop.getId(), BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);

            CropProcurementReportDto dto = new CropProcurementReportDto();
            dto.setCropId(crop.getId());
            dto.setCropName(crop.getName());
            dto.setHindiName(crop.getHindiName());
            dto.setSeason(crop.getSeason());
            dto.setTotalBookings(totalBookings);
            dto.setCompletedProcurements(completedCount);
            dto.setCertifiedQuantityQuintals(certifiedQtl);
            dto.setTotalProcurementValueRs(totalValue);
            dto.setFarmersServed(farmersServed);
            dto.setAverageQuantityPerFarmerQuintals(avgQtl);
            dto.setTotalDbtDisbursedRs(dbtDisbursed);

            reports.add(dto);
        }

        return reports;
    }

    // ==========================================
    // 6. WEIGHMENT & QUALITY / MOISTURE ANALYTICS (DATABASE AGGREGATED)
    // ==========================================
    @Transactional(readOnly = true)
    public QualityAndWeighmentReportDto getQualityAndWeighmentReport() {
        List<Object[]> summary = weighmentRepository.getWeighmentSummaryMetrics();
        QualityAndWeighmentReportDto dto = new QualityAndWeighmentReportDto();

        if (summary.isEmpty() || summary.get(0) == null) {
            setEmptyQcDto(dto);
            return dto;
        }

        Object[] s = summary.get(0);
        long count = ((Number) s[0]).longValue();
        dto.setTotalVehiclesWeighed(count);

        if (count == 0) {
            setEmptyQcDto(dto);
            return dto;
        }

        BigDecimal totalGross = toBigDecimal(s[1]).setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalTare = toBigDecimal(s[2]).setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalNet = toBigDecimal(s[3]).setScale(2, RoundingMode.HALF_UP);
        BigDecimal avgNet = totalNet.divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP);

        dto.setTotalGrossQuintals(totalGross);
        dto.setTotalTareQuintals(totalTare);
        dto.setTotalCertifiedNetQuintals(totalNet);
        dto.setAverageNetQuintalsPerVehicle(avgNet);

        BigDecimal avgMoist = toBigDecimal(s[4]).setScale(2, RoundingMode.HALF_UP);
        BigDecimal minMoist = toBigDecimal(s[5]).setScale(2, RoundingMode.HALF_UP);
        BigDecimal maxMoist = toBigDecimal(s[6]).setScale(2, RoundingMode.HALF_UP);
        BigDecimal avgFm = toBigDecimal(s[7]).setScale(2, RoundingMode.HALF_UP);

        dto.setAverageMoisturePct(avgMoist);
        dto.setMinMoisturePct(minMoist);
        dto.setMaxMoisturePct(maxMoist);
        dto.setAverageForeignMatterPct(avgFm);

        // True mathematical dockage: SUM(net_weight * foreign_matter_pct / 100)
        BigDecimal totalDockage = s.length > 8 && s[8] != null
                ? toBigDecimal(s[8]).setScale(2, RoundingMode.HALF_UP)
                : totalNet.multiply(avgFm).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        dto.setTotalDockageQuintals(totalDockage);

        // Effective dockage percentage = (totalDockage / totalNet) * 100
        double effectiveDockagePct = totalNet.compareTo(BigDecimal.ZERO) > 0
                ? totalDockage.multiply(new BigDecimal("100")).divide(totalNet, 2, RoundingMode.HALF_UP).doubleValue()
                : avgFm.doubleValue();
        dto.setDockagePercentage(effectiveDockagePct);

        // Moisture FAQ threshold check directly aggregated in SQL (no moisture row loops or full table scans)
        List<Object[]> moistureStats = weighmentRepository.getMoistureComplianceStats();
        long totalSampled = 0;
        long aboveLimitCount = 0;
        long withinLimitCount = 0;

        if (!moistureStats.isEmpty() && moistureStats.get(0) != null) {
            Object[] mRow = moistureStats.get(0);
            totalSampled = mRow[0] != null ? ((Number) mRow[0]).longValue() : 0L;
            aboveLimitCount = mRow[1] != null ? ((Number) mRow[1]).longValue() : 0L;
            withinLimitCount = mRow[2] != null ? ((Number) mRow[2]).longValue() : 0L;
        }

        double pctAbove = totalSampled > 0 ? (aboveLimitCount * 100.0) / totalSampled : 0.0;

        dto.setSamplesWithinFaqThreshold(withinLimitCount);
        dto.setSamplesAboveFaqThreshold(aboveLimitCount);
        dto.setPercentAboveFaqThreshold(Math.round(pctAbove * 10.0) / 10.0);

        return dto;
    }

    private void setEmptyQcDto(QualityAndWeighmentReportDto dto) {
        dto.setTotalVehiclesWeighed(0);
        dto.setTotalGrossQuintals(BigDecimal.ZERO);
        dto.setTotalTareQuintals(BigDecimal.ZERO);
        dto.setTotalCertifiedNetQuintals(BigDecimal.ZERO);
        dto.setAverageNetQuintalsPerVehicle(BigDecimal.ZERO);
        dto.setAverageMoisturePct(BigDecimal.ZERO);
        dto.setMinMoisturePct(BigDecimal.ZERO);
        dto.setMaxMoisturePct(BigDecimal.ZERO);
        dto.setSamplesWithinFaqThreshold(0);
        dto.setSamplesAboveFaqThreshold(0);
        dto.setPercentAboveFaqThreshold(0.0);
        dto.setAverageForeignMatterPct(BigDecimal.ZERO);
        dto.setTotalDockageQuintals(BigDecimal.ZERO);
        dto.setDockagePercentage(0.0);
    }

    // ==========================================
    // 7. DBT & PAYMENT ANALYTICS WITH REAL SETTLEMENT TIME CALCULATION
    // ==========================================
    @Transactional(readOnly = true)
    public PaymentAnalyticsReportDto getPaymentAnalytics(long delaySlaHours) {
        BigDecimal totalSettledRs = paymentRepository.sumSettledPayments();

        // 1. Aggregate status counts and amounts directly in SQL
        long completedCount = 0;
        long initiatedCount = 0;
        long pendingCount = 0;
        long failedCount = 0;
        BigDecimal totalPendingRs = BigDecimal.ZERO;

        for (Object[] row : paymentRepository.aggregatePaymentCountsAndAmountsByStatus()) {
            if (row[0] != null) {
                String status = row[0].toString().toUpperCase();
                long count = ((Number) row[1]).longValue();
                BigDecimal amount = (BigDecimal) row[2];

                if ("COMPLETED".equals(status)) {
                    completedCount += count;
                } else if ("FAILED".equals(status)) {
                    failedCount += count;
                    // FAILED payments require remediation and re-initiation, included in pending pool
                    totalPendingRs = totalPendingRs.add(amount);
                } else if ("INITIATED".equals(status) || "PROCESSING".equals(status)) {
                    initiatedCount += count;
                    totalPendingRs = totalPendingRs.add(amount);
                } else {
                    pendingCount += count;
                    totalPendingRs = totalPendingRs.add(amount);
                }
            }
        }

        // 2. Fetch aggregate delayed payment metrics directly from SQL
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime cutoffTime = now.minusHours(delaySlaHours);

        long totalDelayedCount = 0;
        BigDecimal totalDelayedAmount = BigDecimal.ZERO;
        List<Object[]> delayedMetrics = paymentRepository.aggregateDelayedPaymentMetrics(cutoffTime);
        if (!delayedMetrics.isEmpty() && delayedMetrics.get(0) != null) {
            Object[] dm = delayedMetrics.get(0);
            totalDelayedCount = dm[0] != null ? ((Number) dm[0]).longValue() : 0L;
            totalDelayedAmount = dm[1] != null ? (BigDecimal) dm[1] : BigDecimal.ZERO;
        }

        // 3. Fetch strictly bounded top-N delayed payments (oldest delay first)
        org.springframework.data.domain.PageRequest delayPageRequest =
                org.springframework.data.domain.PageRequest.of(0, ReportConstants.MAX_DELAYED_PAYMENT_ALERTS);
        List<Payment> delayedPayments = paymentRepository.findDelayedPaymentsWithDetailsBounded(cutoffTime, delayPageRequest);

        List<PaymentAnalyticsReportDto.PaymentDelayAlertDto> delayed = new ArrayList<>();
        for (Payment p : delayedPayments) {
            String status = p.getPaymentStatus();
            BigDecimal netAmount = p.getNetPayableAmount() != null ? p.getNetPayableAmount() : BigDecimal.ZERO;
            OffsetDateTime referenceTime = p.getInitiatedAt() != null ? p.getInitiatedAt() : p.getCreatedAt();
            long hours = Duration.between(referenceTime, now).toHours();

            PaymentAnalyticsReportDto.PaymentDelayAlertDto alert = new PaymentAnalyticsReportDto.PaymentDelayAlertDto();
            if (p.getBooking() != null) {
                alert.setBookingId(p.getBooking().getId().toString());
                alert.setTokenNumber(p.getBooking().getTokenNumber());
            }
            if (p.getFarmer() != null) {
                alert.setFarmerReference(p.getFarmer().getName() + " (" + p.getFarmer().getKisanId() + ")");
                alert.setMaskedAadhar(p.getFarmer().getMaskedAadhar());
            }
            if (p.getMandi() != null) {
                alert.setMandiId(p.getMandi().getId());
                alert.setMandiName(p.getMandi().getName());
            }
            alert.setNetPayableAmount(netAmount);
            alert.setPaymentStatus(status);
            alert.setInitiatedAt(referenceTime != null ? referenceTime.toString() : null);
            alert.setCompletedAt(p.getCreditedAt() != null ? p.getCreditedAt().toString() : null);
            alert.setDelayHours(hours);
            alert.setMaskedAccount("XXXX-XXXX-" + p.getBankAccountLast4());
            delayed.add(alert);
        }

        // 4. Pure PostgreSQL calculation of average settlement hours
        Double dbAvgHours = paymentRepository.calculateAverageSettlementHoursNative();
        double avgSettlementHours = dbAvgHours != null ? Math.round(dbAvgHours * 10.0) / 10.0 : 0.0;

        PaymentAnalyticsReportDto dto = new PaymentAnalyticsReportDto();
        dto.setTotalDbtInitiated(initiatedCount + completedCount);
        dto.setTotalDbtCompleted(completedCount);
        dto.setTotalDbtPending(pendingCount + initiatedCount);
        dto.setTotalDbtFailed(failedCount);
        dto.setTotalAmountSettledRs(totalSettledRs.setScale(2, RoundingMode.HALF_UP));
        dto.setTotalAmountPendingRs(totalPendingRs.setScale(2, RoundingMode.HALF_UP));
        dto.setAverageSettlementHours(avgSettlementHours);
        dto.setTotalDelayedPaymentsCount(totalDelayedCount);
        dto.setTotalDelayedAmountRs(totalDelayedAmount.setScale(2, RoundingMode.HALF_UP));
        dto.setDelayedPayments(delayed);

        return dto;
    }

    // ==========================================
    // 8. TIME-SERIES ANALYTICS (DATABASE AGGREGATED)
    // ==========================================
    @Transactional(readOnly = true)
    public List<TimeSeriesPointDto> getTimeSeries(int days) {
        LocalDate today = LocalDate.now(IST_ZONE);
        LocalDate start = today.minusDays(days - 1);

        // row: [scheduledDate, totalCount, completedCount, sumNetWeight, sumSettlement, distinctFarmers]
        List<Object[]> dailyAggs = bookingRepository.aggregateDailyTimeSeries(start, today, COMPLETED_STATUSES);
        Map<LocalDate, Object[]> aggByDate = new HashMap<>();
        for (Object[] row : dailyAggs) {
            if (row[0] != null) {
                aggByDate.put((LocalDate) row[0], row);
            }
        }

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd MMM");
        List<TimeSeriesPointDto> points = new ArrayList<>();

        for (int i = 0; i < days; i++) {
            LocalDate d = start.plusDays(i);
            Object[] agg = aggByDate.get(d);

            long bCount = agg != null ? ((Number) agg[1]).longValue() : 0L;
            long compCount = agg != null ? ((Number) agg[2]).longValue() : 0L;
            BigDecimal netQtl = agg != null ? ((BigDecimal) agg[3]).setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            BigDecimal payout = agg != null ? ((BigDecimal) agg[4]).setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            long farmersServed = agg != null ? ((Number) agg[5]).longValue() : 0L;

            points.add(new TimeSeriesPointDto(d.format(fmt), bCount, compCount, netQtl, payout, farmersServed));
        }

        return points;
    }

    // ==========================================
    // 9. PROCUREMENT REGISTER (OPTIMIZED JOIN QUERIES & PAGINATION)
    // ==========================================
    @Deprecated
    @Transactional(readOnly = true)
    public List<ProcurementRegisterRowDto> getProcurementRegister(String district, String mandiId, String cropId) {
        // Enforce strict server-side bounding (max 200) for legacy non-paginated compatibility endpoint
        org.springframework.data.domain.PageRequest pageRequest =
                org.springframework.data.domain.PageRequest.of(0, ReportConstants.MAX_REGISTER_PAGE_SIZE);
        org.springframework.data.domain.Page<Booking> bookingPage =
                bookingRepository.findFilteredForRegisterPageable(district, mandiId, cropId, null, pageRequest);
        return mapBookingsToRegisterRows(bookingPage.getContent());
    }

    @Transactional(readOnly = true)
    public PaginatedProcurementRegisterDto getPaginatedProcurementRegister(
            String district, String mandiId, String cropId, String search, int page, int size
    ) {
        int boundedSize = Math.max(1, Math.min(size, ReportConstants.MAX_REGISTER_PAGE_SIZE));
        int boundedPage = Math.max(0, page);
        org.springframework.data.domain.PageRequest pageRequest = org.springframework.data.domain.PageRequest.of(boundedPage, boundedSize);

        String trimmedSearch = (search != null && !search.isBlank()) ? search.trim() : null;

        org.springframework.data.domain.Page<Booking> bookingPage = bookingRepository.findFilteredForRegisterPageable(
                district, mandiId, cropId, trimmedSearch, pageRequest
        );

        List<ProcurementRegisterRowDto> content = mapBookingsToRegisterRows(bookingPage.getContent());

        return new PaginatedProcurementRegisterDto(
                content,
                bookingPage.getNumber(),
                bookingPage.getSize(),
                bookingPage.getTotalElements(),
                bookingPage.getTotalPages(),
                bookingPage.isLast()
        );
    }

    /**
     * Memory-bounded progressive streaming of procurement register directly to an output stream in batches.
     */
    @Transactional(readOnly = true)
    public void streamProcurementRegisterCsv(
            String district,
            String mandiId,
            String cropId,
            String search,
            java.io.OutputStream outputStream
    ) throws java.io.IOException {
        CsvStreamWriter csvWriter = new CsvStreamWriter(outputStream);
        csvWriter.writeHeader(
                "Token Number", "Scheduled Date", "Farmer Name", "Phone", "Masked Aadhaar",
                "District", "Mandi", "Crop", "Booked Qtl", "Net Weight Qtl", "Moisture %",
                "Foreign Matter %", "Payout Rs", "Status", "DBT Status", "DBT Ref",
                "Bank Account", "IFSC", "Completed At"
        );

        String trimmedSearch = (search != null && !search.isBlank()) ? search.trim() : null;

        int pageIndex = 0;
        int batchSize = ReportConstants.CSV_EXPORT_BATCH_SIZE;
        boolean hasMore = true;

        while (hasMore) {
            org.springframework.data.domain.PageRequest pageRequest =
                    org.springframework.data.domain.PageRequest.of(pageIndex, batchSize);

            org.springframework.data.domain.Page<Booking> bookingPage =
                    bookingRepository.findFilteredForRegisterPageable(district, mandiId, cropId, trimmedSearch, pageRequest);

            List<Booking> batchBookings = bookingPage.getContent();
            if (batchBookings.isEmpty()) {
                break;
            }

            List<ProcurementRegisterRowDto> rows = mapBookingsToRegisterRows(batchBookings);
            for (ProcurementRegisterRowDto row : rows) {
                csvWriter.writeProcurementRow(row);
            }

            if (bookingPage.isLast() || batchBookings.size() < batchSize) {
                hasMore = false;
            } else {
                pageIndex++;
            }
        }

        csvWriter.flush();
    }

    private List<ProcurementRegisterRowDto> mapBookingsToRegisterRows(List<Booking> bookings) {
        if (bookings.isEmpty()) {
            return Collections.emptyList();
        }

        List<UUID> bookingIds = bookings.stream().map(Booking::getId).toList();

        // Fetch weighments and payments ONLY for the filtered booking IDs of this batch (bounded memory)
        Map<UUID, Weighment> weighmentByBooking = weighmentRepository.findByBookingIdIn(bookingIds).stream()
                .collect(Collectors.toMap(w -> w.getBooking().getId(), w -> w, (a, b) -> a));

        Map<UUID, Payment> paymentByBooking = paymentRepository.findByBookingIdIn(bookingIds).stream()
                .collect(Collectors.toMap(p -> p.getBooking().getId(), p -> p, (a, b) -> a));

        return bookings.stream()
                .map(b -> {
                    ProcurementRegisterRowDto row = new ProcurementRegisterRowDto();
                    row.setBookingId(b.getId().toString());
                    row.setTokenNumber(b.getTokenNumber());
                    row.setTokenSequence(b.getTokenSequence());
                    row.setScheduledDate(b.getScheduledDate().toString());
                    row.setFarmerName(b.getFarmer().getName());
                    row.setFarmerPhone(b.getFarmer().getPhone());
                    row.setMaskedAadhar(b.getFarmer().getMaskedAadhar());
                    row.setDistrict(b.getMandi().getDistrict());
                    row.setMandiName(b.getMandi().getName());
                    row.setCropName(b.getCrop().getName());
                    row.setEstimatedYieldQuintals(b.getEstimatedYieldQuintals());
                    row.setNetWeightQuintals(b.getNetWeightQuintals());
                    row.setMoisturePercentage(b.getMoisturePercentage());
                    row.setTotalPayoutRs(b.getSettlementAmount());
                    row.setStatus(b.getStatus().name());

                    Weighment w = weighmentByBooking.get(b.getId());
                    if (w != null) {
                        row.setForeignMatterPercentage(w.getForeignMatterPct());
                    }

                    Payment p = paymentByBooking.get(b.getId());
                    if (p != null) {
                        row.setPaymentStatus(p.getPaymentStatus());
                        row.setDbtReferenceNo(p.getDbtReferenceNo());
                        row.setBankAccountLast4("XXXX" + p.getBankAccountLast4());
                        row.setIfscCode(p.getIfscCode());
                        row.setCompletedAt(p.getCreditedAt() != null ? p.getCreditedAt().toString() : null);
                    } else {
                        row.setPaymentStatus("UNINITIATED");
                    }

                    return row;
                })
                .toList();
    }

    private double calculateOperatingHours(String openTimeStr, String closeTimeStr) {
        if (openTimeStr == null || closeTimeStr == null || openTimeStr.isBlank() || closeTimeStr.isBlank()) {
            return 8.0;
        }
        try {
            LocalTime open = parseTimeString(openTimeStr.trim());
            LocalTime close = parseTimeString(closeTimeStr.trim());
            if (open != null && close != null) {
                long minutes = java.time.Duration.between(open, close).toMinutes();
                if (minutes <= 0) {
                    minutes += 24 * 60; // handle overnight wrap-around safely
                }
                double hours = minutes / 60.0;
                return hours > 0.0 ? hours : 8.0;
            }
        } catch (Exception ignored) {
            // Fall back to standard 8-hour operating shift
        }
        return 8.0;
    }

    private BigDecimal toBigDecimal(Object val) {
        if (val == null) return BigDecimal.ZERO;
        if (val instanceof BigDecimal) return (BigDecimal) val;
        if (val instanceof Number) return BigDecimal.valueOf(((Number) val).doubleValue());
        try {
            return new BigDecimal(val.toString().trim());
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    private LocalTime parseTimeString(String timeStr) {
        DateTimeFormatter[] formatters = new DateTimeFormatter[]{
                DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH),
                DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH),
                DateTimeFormatter.ofPattern("HH:mm"),
                DateTimeFormatter.ofPattern("H:mm")
        };
        for (DateTimeFormatter dtf : formatters) {
            try {
                return LocalTime.parse(timeStr.toUpperCase(), dtf);
            } catch (Exception ignored) {}
        }
        return null;
    }
}

