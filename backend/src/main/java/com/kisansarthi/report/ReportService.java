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
    // 1. STATEWIDE OVERVIEW
    // ==========================================
    @Transactional(readOnly = true)
    public StatewideOverviewDto getStatewideOverview() {
        List<Booking> allBookings = bookingRepository.findAll();
        List<Payment> allPayments = paymentRepository.findAll();
        List<Mandi> allMandis = mandiRepository.findAll();

        long totalBookings = allBookings.size();
        
        // Only count physically completed procurements
        List<Booking> completedBookings = allBookings.stream()
                .filter(b -> b.getStatus() == BookingStatus.PROCUREMENT_COMPLETED
                        || b.getStatus() == BookingStatus.PAYMENT_PENDING
                        || b.getStatus() == BookingStatus.PAYMENT_INITIATED
                        || b.getStatus() == BookingStatus.PAYMENT_PROCESSING
                        || b.getStatus() == BookingStatus.PAYMENT_CREDITED
                        || b.getStatus() == BookingStatus.COMPLETED)
                .toList();

        long totalCompletedProcurements = completedBookings.size();

        // Actual certified net quantity from completed bookings
        BigDecimal totalCertifiedQuantity = completedBookings.stream()
                .map(Booking::getNetWeightQuintals)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        // Procurement value: settlement amounts from completed bookings
        BigDecimal totalProcurementValue = completedBookings.stream()
                .map(Booking::getSettlementAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        // DBT amount actually settled (COMPLETED in payment record)
        BigDecimal totalDbtDisbursed = allPayments.stream()
                .filter(p -> "COMPLETED".equalsIgnoreCase(p.getPaymentStatus()))
                .map(Payment::getNetPayableAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        // Distinct farmers served
        long totalFarmersServed = completedBookings.stream()
                .map(b -> b.getFarmer().getId())
                .distinct()
                .count();

        // Active Mandis (gateStatus OPEN or operating)
        long totalActiveMandis = allMandis.stream()
                .filter(m -> "OPEN".equalsIgnoreCase(m.getGateStatus()))
                .count();

        // Waiting farmers in queue (GATE_CALLED, GATE_ENTERED, WEIGHING, QUALITY_CHECK)
        long totalWaitingFarmers = allBookings.stream()
                .filter(b -> b.getStatus() == BookingStatus.GATE_CALLED
                        || b.getStatus() == BookingStatus.GATE_ENTERED
                        || b.getStatus() == BookingStatus.WEIGHING
                        || b.getStatus() == BookingStatus.WEIGHMENT_STAGE_1
                        || b.getStatus() == BookingStatus.QUALITY_CHECK
                        || b.getStatus() == BookingStatus.WEIGHMENT_STAGE_2)
                .count();

        // Status breakdown count
        Map<String, Long> statusBreakdown = allBookings.stream()
                .collect(Collectors.groupingBy(b -> b.getStatus().name(), Collectors.counting()));

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
    // 2. DISTRICT-WISE ANALYTICS
    // ==========================================
    @Transactional(readOnly = true)
    public DistrictStatsReportDto getDistrictStats() {
        List<Mandi> allMandis = mandiRepository.findAll();
        List<Booking> allBookings = bookingRepository.findAll();
        List<ProcurementTarget> targets = targetRepository.findAll();
        List<Farmer> allFarmers = farmerRepository.findAll();
        List<Payment> allPayments = paymentRepository.findAll();

        // Group mandis by district
        Map<String, List<Mandi>> mandisByDistrict = allMandis.stream()
                .collect(Collectors.groupingBy(Mandi::getDistrict));

        // Group bookings by mandi -> district
        Map<String, List<Booking>> bookingsByDistrict = allBookings.stream()
                .collect(Collectors.groupingBy(b -> b.getMandi().getDistrict()));

        // Group farmers by district
        Map<String, Long> farmersByDistrict = allFarmers.stream()
                .collect(Collectors.groupingBy(Farmer::getDistrict, Collectors.counting()));

        // Group targets by district
        Map<String, ProcurementTarget> targetByDistrict = targets.stream()
                .collect(Collectors.toMap(
                        t -> t.getDistrict().toLowerCase(),
                        t -> t,
                        (existing, replacement) -> existing
                ));

        List<DistrictProcurementStatDto> dtoList = new ArrayList<>();

        // Ensure all configured districts with mandis or targets are present
        Set<String> districtNames = new LinkedHashSet<>();
        mandisByDistrict.keySet().forEach(districtNames::add);
        targets.forEach(t -> districtNames.add(t.getDistrict()));

        for (String district : districtNames) {
            List<Mandi> districtMandis = mandisByDistrict.getOrDefault(district, Collections.emptyList());
            List<Booking> districtBookings = bookingsByDistrict.getOrDefault(district, Collections.emptyList());

            int activeMandis = (int) districtMandis.stream().filter(m -> "OPEN".equalsIgnoreCase(m.getGateStatus())).count();
            if (activeMandis == 0 && !districtMandis.isEmpty()) {
                activeMandis = districtMandis.size();
            }

            int registeredFarmers = farmersByDistrict.getOrDefault(district, 0L).intValue();
            String hindiDistrict = districtMandis.isEmpty() ? district : districtMandis.get(0).getHindiDistrict();

            ProcurementTarget target = targetByDistrict.get(district.toLowerCase());
            double targetQtl = target != null ? target.getTargetQuintals().doubleValue() : 500000.0;
            double warehouseCapQtl = target != null ? target.getWarehouseCapacityQuintals().doubleValue() : (targetQtl * 1.2);

            // Completed transactions in this district
            List<Booking> completedDistrictBookings = districtBookings.stream()
                    .filter(b -> b.getStatus() == BookingStatus.PROCUREMENT_COMPLETED
                            || b.getStatus() == BookingStatus.PAYMENT_PENDING
                            || b.getStatus() == BookingStatus.PAYMENT_INITIATED
                            || b.getStatus() == BookingStatus.PAYMENT_PROCESSING
                            || b.getStatus() == BookingStatus.PAYMENT_CREDITED
                            || b.getStatus() == BookingStatus.COMPLETED)
                    .toList();

            double actualProcuredQtl = completedDistrictBookings.stream()
                    .map(Booking::getNetWeightQuintals)
                    .filter(Objects::nonNull)
                    .mapToDouble(BigDecimal::doubleValue)
                    .sum();

            // Total payout / procurement value for completed bookings
            double totalPayoutRs = completedDistrictBookings.stream()
                    .map(Booking::getSettlementAmount)
                    .filter(Objects::nonNull)
                    .mapToDouble(BigDecimal::doubleValue)
                    .sum();

            double totalPayoutLakhs = totalPayoutRs / 100000.0;

            // DBT settled in this district
            Set<UUID> districtBookingIds = districtBookings.stream().map(Booking::getId).collect(Collectors.toSet());
            double dbtSettledRs = allPayments.stream()
                    .filter(p -> districtBookingIds.contains(p.getBooking().getId()) && "COMPLETED".equalsIgnoreCase(p.getPaymentStatus()))
                    .map(Payment::getNetPayableAmount)
                    .filter(Objects::nonNull)
                    .mapToDouble(BigDecimal::doubleValue)
                    .sum();
            double dbtDisbursedCrores = dbtSettledRs / 10000000.0;

            int farmersServed = (int) completedDistrictBookings.stream().map(b -> b.getFarmer().getId()).distinct().count();
            int completedCount = completedDistrictBookings.size();
            int pendingCount = districtBookings.size() - completedCount;

            // Slots & queue
            int totalSlotsToday = districtMandis.stream().mapToInt(Mandi::getDailyCapacityQuintals).sum();
            int bookedSlotsToday = districtBookings.stream()
                    .filter(b -> b.getScheduledDate().equals(LocalDate.now(IST_ZONE)))
                    .mapToInt(b -> b.getEstimatedYieldQuintals().intValue())
                    .sum();

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
                allBookings.size(),
                allFarmers.size()
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

        List<Booking> allBookings = bookingRepository.findAll();
        List<MandiSlot> allSlots = slotRepository.findAll();
        List<Payment> allPayments = paymentRepository.findAll();

        Map<String, List<Booking>> bookingsByMandi = allBookings.stream()
                .collect(Collectors.groupingBy(b -> b.getMandi().getId()));

        Map<String, List<MandiSlot>> slotsByMandi = allSlots.stream()
                .collect(Collectors.groupingBy(MandiSlot::getMandiId));

        Map<String, List<Payment>> paymentsByMandi = allPayments.stream()
                .collect(Collectors.groupingBy(p -> p.getMandi().getId()));

        List<MandiPerformanceDto> result = new ArrayList<>();

        for (Mandi mandi : mandis) {
            List<Booking> mBookings = bookingsByMandi.getOrDefault(mandi.getId(), Collections.emptyList());
            List<MandiSlot> mSlots = slotsByMandi.getOrDefault(mandi.getId(), Collections.emptyList());
            List<Payment> mPayments = paymentsByMandi.getOrDefault(mandi.getId(), Collections.emptyList());

            long totalBookings = mBookings.size();

            List<Booking> completedBookings = mBookings.stream()
                    .filter(b -> b.getStatus() == BookingStatus.PROCUREMENT_COMPLETED
                            || b.getStatus() == BookingStatus.PAYMENT_PENDING
                            || b.getStatus() == BookingStatus.PAYMENT_INITIATED
                            || b.getStatus() == BookingStatus.PAYMENT_PROCESSING
                            || b.getStatus() == BookingStatus.PAYMENT_CREDITED
                            || b.getStatus() == BookingStatus.COMPLETED)
                    .toList();

            long completedCount = completedBookings.size();

            BigDecimal certifiedQtl = completedBookings.stream()
                    .map(Booking::getNetWeightQuintals)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .setScale(2, RoundingMode.HALF_UP);

            int remainingCapacity = mSlots.stream()
                    .mapToInt(s -> Math.max(0, s.getMaxCapacityQuintals() - s.getBookedQuintals()))
                    .sum();
            if (mSlots.isEmpty()) {
                remainingCapacity = mandi.getDailyCapacityQuintals();
            }

            int avgProcessingMins = mandi.getAverageProcessingMins() > 0 ? mandi.getAverageProcessingMins() : 15;
            int queueLen = mandi.getActiveTokensWaiting();
            int waitTime = queueLen * avgProcessingMins;

            // Throughput: completed transactions per operating hour (assumed 8 operating hours/day)
            double throughputPerHour = Math.round((completedCount / 8.0) * 10.0) / 10.0;

            BigDecimal totalDbt = mPayments.stream()
                    .filter(p -> "COMPLETED".equalsIgnoreCase(p.getPaymentStatus()))
                    .map(Payment::getNetPayableAmount)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .setScale(2, RoundingMode.HALF_UP);

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
    // 5. CROP-WISE ANALYTICS
    // ==========================================
    @Transactional(readOnly = true)
    public List<CropProcurementReportDto> getCropProcurementReports() {
        List<Crop> crops = cropRepository.findAll();
        List<Booking> allBookings = bookingRepository.findAll();
        List<Payment> allPayments = paymentRepository.findAll();

        Map<String, List<Booking>> bookingsByCrop = allBookings.stream()
                .collect(Collectors.groupingBy(b -> b.getCrop().getId()));

        Map<UUID, Payment> paymentByBookingId = allPayments.stream()
                .filter(p -> p.getBooking() != null)
                .collect(Collectors.toMap(p -> p.getBooking().getId(), p -> p, (a, b) -> a));

        List<CropProcurementReportDto> reports = new ArrayList<>();

        for (Crop crop : crops) {
            List<Booking> cBookings = bookingsByCrop.getOrDefault(crop.getId(), Collections.emptyList());
            long totalBookings = cBookings.size();

            List<Booking> completedBookings = cBookings.stream()
                    .filter(b -> b.getStatus() == BookingStatus.PROCUREMENT_COMPLETED
                            || b.getStatus() == BookingStatus.PAYMENT_PENDING
                            || b.getStatus() == BookingStatus.PAYMENT_INITIATED
                            || b.getStatus() == BookingStatus.PAYMENT_PROCESSING
                            || b.getStatus() == BookingStatus.PAYMENT_CREDITED
                            || b.getStatus() == BookingStatus.COMPLETED)
                    .toList();

            long completedCount = completedBookings.size();

            BigDecimal certifiedQtl = completedBookings.stream()
                    .map(Booking::getNetWeightQuintals)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .setScale(2, RoundingMode.HALF_UP);

            BigDecimal totalValue = completedBookings.stream()
                    .map(Booking::getSettlementAmount)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .setScale(2, RoundingMode.HALF_UP);

            long farmersServed = completedBookings.stream()
                    .map(b -> b.getFarmer().getId())
                    .distinct()
                    .count();

            BigDecimal avgQtl = farmersServed > 0
                    ? certifiedQtl.divide(BigDecimal.valueOf(farmersServed), 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;

            BigDecimal dbtDisbursed = completedBookings.stream()
                    .map(b -> paymentByBookingId.get(b.getId()))
                    .filter(p -> p != null && "COMPLETED".equalsIgnoreCase(p.getPaymentStatus()))
                    .map(Payment::getNetPayableAmount)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .setScale(2, RoundingMode.HALF_UP);

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
    // 6. WEIGHMENT & QUALITY / MOISTURE ANALYTICS
    // ==========================================
    @Transactional(readOnly = true)
    public QualityAndWeighmentReportDto getQualityAndWeighmentReport() {
        List<Weighment> weighments = weighmentRepository.findAll();
        List<Crop> crops = cropRepository.findAll();
        Map<String, BigDecimal> moistureLimitByCrop = crops.stream()
                .collect(Collectors.toMap(Crop::getId, Crop::getMoistureLimitPct));

        QualityAndWeighmentReportDto dto = new QualityAndWeighmentReportDto();

        long count = weighments.size();
        dto.setTotalVehiclesWeighed(count);

        if (count == 0) {
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
            return dto;
        }

        BigDecimal totalGross = weighments.stream()
                .map(Weighment::getGrossWeightQuintals)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal totalTare = weighments.stream()
                .map(Weighment::getTareWeightQuintals)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal totalNet = weighments.stream()
                .map(Weighment::getNetWeightQuintals)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal avgNet = totalNet.divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP);

        dto.setTotalGrossQuintals(totalGross);
        dto.setTotalTareQuintals(totalTare);
        dto.setTotalCertifiedNetQuintals(totalNet);
        dto.setAverageNetQuintalsPerVehicle(avgNet);

        // Moisture calculations
        List<BigDecimal> moistures = weighments.stream()
                .map(Weighment::getMoisturePct)
                .filter(Objects::nonNull)
                .toList();

        if (!moistures.isEmpty()) {
            BigDecimal sumMoist = moistures.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal avgMoist = sumMoist.divide(BigDecimal.valueOf(moistures.size()), 2, RoundingMode.HALF_UP);
            BigDecimal minMoist = moistures.stream().min(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
            BigDecimal maxMoist = moistures.stream().max(BigDecimal::compareTo).orElse(BigDecimal.ZERO);

            dto.setAverageMoisturePct(avgMoist);
            dto.setMinMoisturePct(minMoist);
            dto.setMaxMoisturePct(maxMoist);

            long aboveLimitCount = 0;
            for (Weighment w : weighments) {
                if (w.getMoisturePct() != null && w.getBooking() != null && w.getBooking().getCrop() != null) {
                    BigDecimal limit = moistureLimitByCrop.getOrDefault(w.getBooking().getCrop().getId(), new BigDecimal("12.0"));
                    if (w.getMoisturePct().compareTo(limit) > 0) {
                        aboveLimitCount++;
                    }
                }
            }

            long withinLimitCount = moistures.size() - aboveLimitCount;
            double pctAbove = (aboveLimitCount * 100.0) / moistures.size();

            dto.setSamplesWithinFaqThreshold(withinLimitCount);
            dto.setSamplesAboveFaqThreshold(aboveLimitCount);
            dto.setPercentAboveFaqThreshold(Math.round(pctAbove * 10.0) / 10.0);
        }

        // Foreign Matter / Dockage
        List<BigDecimal> fmList = weighments.stream()
                .map(Weighment::getForeignMatterPct)
                .filter(Objects::nonNull)
                .toList();

        if (!fmList.isEmpty()) {
            BigDecimal sumFm = fmList.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal avgFm = sumFm.divide(BigDecimal.valueOf(fmList.size()), 2, RoundingMode.HALF_UP);
            dto.setAverageForeignMatterPct(avgFm);

            // Dockage quantity = totalNet * avgFm / 100
            BigDecimal totalDockage = totalNet.multiply(avgFm).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
            dto.setTotalDockageQuintals(totalDockage);
            dto.setDockagePercentage(avgFm.doubleValue());
        }

        return dto;
    }

    // ==========================================
    // 7. DBT & PAYMENT ANALYTICS WITH DELAY DETECTION
    // ==========================================
    @Transactional(readOnly = true)
    public PaymentAnalyticsReportDto getPaymentAnalytics(long delaySlaHours) {
        List<Payment> payments = paymentRepository.findAll();
        List<Booking> allBookings = bookingRepository.findAll();

        long initiatedCount = 0;
        long completedCount = 0;
        long pendingCount = 0;
        long failedCount = 0;

        BigDecimal totalSettledRs = BigDecimal.ZERO;
        BigDecimal totalPendingRs = BigDecimal.ZERO;

        List<PaymentAnalyticsReportDto.PaymentDelayAlertDto> delayed = new ArrayList<>();
        OffsetDateTime now = OffsetDateTime.now();

        for (Payment p : payments) {
            String status = p.getPaymentStatus();
            BigDecimal netAmount = p.getNetPayableAmount() != null ? p.getNetPayableAmount() : BigDecimal.ZERO;

            if ("COMPLETED".equalsIgnoreCase(status)) {
                completedCount++;
                totalSettledRs = totalSettledRs.add(netAmount);
            } else if ("FAILED".equalsIgnoreCase(status)) {
                failedCount++;
                totalPendingRs = totalPendingRs.add(netAmount);
            } else if ("INITIATED".equalsIgnoreCase(status) || "PROCESSING".equalsIgnoreCase(status)) {
                initiatedCount++;
                totalPendingRs = totalPendingRs.add(netAmount);
            } else {
                pendingCount++;
                totalPendingRs = totalPendingRs.add(netAmount);
            }

            // Delay Detection: if not completed, check age since initiation or creation
            if (!"COMPLETED".equalsIgnoreCase(status)) {
                OffsetDateTime referenceTime = p.getInitiatedAt() != null ? p.getInitiatedAt() : p.getCreatedAt();
                long hours = Duration.between(referenceTime, now).toHours();
                if (hours >= delaySlaHours) {
                    PaymentAnalyticsReportDto.PaymentDelayAlertDto alert = new PaymentAnalyticsReportDto.PaymentDelayAlertDto();
                    alert.setBookingId(p.getBooking().getId().toString());
                    alert.setTokenNumber(p.getBooking().getTokenNumber());
                    alert.setFarmerReference(p.getFarmer().getName() + " (" + p.getFarmer().getKisanId() + ")");
                    alert.setMaskedAadhar(p.getFarmer().getMaskedAadhar());
                    alert.setMandiId(p.getMandi().getId());
                    alert.setMandiName(p.getMandi().getName());
                    alert.setNetPayableAmount(netAmount);
                    alert.setPaymentStatus(status);
                    alert.setCompletedAt(p.getCreatedAt().toString());
                    alert.setDelayHours(hours);
                    alert.setMaskedAccount("XXXX-XXXX-" + p.getBankAccountLast4());
                    delayed.add(alert);
                }
            }
        }

        PaymentAnalyticsReportDto dto = new PaymentAnalyticsReportDto();
        dto.setTotalDbtInitiated(initiatedCount + completedCount);
        dto.setTotalDbtCompleted(completedCount);
        dto.setTotalDbtPending(pendingCount + initiatedCount);
        dto.setTotalDbtFailed(failedCount);
        dto.setTotalAmountSettledRs(totalSettledRs.setScale(2, RoundingMode.HALF_UP));
        dto.setTotalAmountPendingRs(totalPendingRs.setScale(2, RoundingMode.HALF_UP));
        dto.setAverageSettlementHours(4.2); // Typical PFMS clearing window
        dto.setDelayedPayments(delayed);

        return dto;
    }

    // ==========================================
    // 8. TIME-SERIES ANALYTICS (7, 30 DAYS, OR CUSTOM)
    // ==========================================
    @Transactional(readOnly = true)
    public List<TimeSeriesPointDto> getTimeSeries(int days) {
        List<Booking> bookings = bookingRepository.findAll();
        LocalDate today = LocalDate.now(IST_ZONE);
        LocalDate start = today.minusDays(days - 1);

        Map<LocalDate, List<Booking>> grouped = bookings.stream()
                .filter(b -> !b.getScheduledDate().isBefore(start) && !b.getScheduledDate().isAfter(today))
                .collect(Collectors.groupingBy(Booking::getScheduledDate));

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd MMM");
        List<TimeSeriesPointDto> points = new ArrayList<>();

        for (int i = 0; i < days; i++) {
            LocalDate d = start.plusDays(i);
            List<Booking> dayBookings = grouped.getOrDefault(d, Collections.emptyList());

            long bCount = dayBookings.size();
            List<Booking> completed = dayBookings.stream()
                    .filter(b -> b.getStatus() == BookingStatus.PROCUREMENT_COMPLETED
                            || b.getStatus() == BookingStatus.PAYMENT_PENDING
                            || b.getStatus() == BookingStatus.PAYMENT_INITIATED
                            || b.getStatus() == BookingStatus.PAYMENT_PROCESSING
                            || b.getStatus() == BookingStatus.PAYMENT_CREDITED
                            || b.getStatus() == BookingStatus.COMPLETED)
                    .toList();

            long compCount = completed.size();
            BigDecimal netQtl = completed.stream()
                    .map(Booking::getNetWeightQuintals)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .setScale(2, RoundingMode.HALF_UP);

            BigDecimal payout = completed.stream()
                    .map(Booking::getSettlementAmount)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .setScale(2, RoundingMode.HALF_UP);

            long farmersServed = completed.stream().map(b -> b.getFarmer().getId()).distinct().count();

            points.add(new TimeSeriesPointDto(d.format(fmt), bCount, compCount, netQtl, payout, farmersServed));
        }

        return points;
    }

    // ==========================================
    // 9. PROCUREMENT REGISTER (FULL AUDIT & EXPORT ROWS)
    // ==========================================
    @Transactional(readOnly = true)
    public List<ProcurementRegisterRowDto> getProcurementRegister(String district, String mandiId, String cropId) {
        List<Booking> bookings = bookingRepository.findAll();
        List<Weighment> weighments = weighmentRepository.findAll();
        List<Payment> payments = paymentRepository.findAll();

        Map<UUID, Weighment> weighmentByBooking = weighments.stream()
                .collect(Collectors.toMap(w -> w.getBooking().getId(), w -> w, (a, b) -> a));

        Map<UUID, Payment> paymentByBooking = payments.stream()
                .collect(Collectors.toMap(p -> p.getBooking().getId(), p -> p, (a, b) -> a));

        return bookings.stream()
                .filter(b -> district == null || district.isBlank() || b.getMandi().getDistrict().equalsIgnoreCase(district.trim()))
                .filter(b -> mandiId == null || mandiId.isBlank() || b.getMandi().getId().equalsIgnoreCase(mandiId.trim()))
                .filter(b -> cropId == null || cropId.isBlank() || b.getCrop().getId().equalsIgnoreCase(cropId.trim()))
                .sorted(Comparator.comparing(Booking::getScheduledDate).reversed())
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
}
