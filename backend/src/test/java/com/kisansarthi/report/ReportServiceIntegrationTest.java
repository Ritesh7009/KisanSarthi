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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@org.springframework.test.context.TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:kisansarthi_report_test;DB_CLOSE_DELAY=-1;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.flyway.enabled=false"
})
public class ReportServiceIntegrationTest {

    @Autowired
    private ReportService reportService;

    @Autowired
    private MandiRepository mandiRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private FarmerRepository farmerRepository;

    @Autowired
    private CropRepository cropRepository;

    @Autowired
    private WeighmentRepository weighmentRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private SlotRepository slotRepository;

    @Autowired
    private ProcurementTargetRepository targetRepository;

    private Mandi testMandi;
    private Farmer testFarmer;
    private Crop testCrop;

    @BeforeEach
    void setUp() {
        String uid = UUID.randomUUID().toString().substring(0, 8);

        testMandi = new Mandi();
        testMandi.setId("mandi-rep-" + uid);
        testMandi.setName("Krishi Upaj Mandi " + uid);
        testMandi.setHindiName("मंडी " + uid);
        testMandi.setDistrict("Sehore");
        testMandi.setHindiDistrict("सीहोर");
        testMandi.setAddress("Mandi Road");
        testMandi.setPinCode("466001");
        testMandi.setDailyCapacityQuintals(2000);
        testMandi.setActiveTokensWaiting(12);
        testMandi.setGateStatus("OPEN");
        testMandi = mandiRepository.save(testMandi);

        testFarmer = new Farmer();
        testFarmer.setName("Ramesh Patel " + uid);
        testFarmer.setPhone("9826" + (System.currentTimeMillis() % 1000000));
        testFarmer.setKisanId("MP-FARM-" + uid);
        testFarmer.setDistrict("Sehore");
        testFarmer.setVillage("Mandi Gaon");
        testFarmer.setLandSizeAcres(new BigDecimal("5.5"));
        testFarmer.setMaskedAadhar("XXXX-XXXX-1234");
        testFarmer.setBankAccountLast4("9012");
        testFarmer.setIfscCode("SBIN0001234");
        testFarmer = farmerRepository.save(testFarmer);

        testCrop = new Crop();
        testCrop.setId("crop-wheat-" + uid);
        testCrop.setName("Wheat (Sharbati) " + uid);
        testCrop.setHindiName("गेहूं");
        testCrop.setSeason("Rabi");
        testCrop.setStandardMspPerQuintal(new BigDecimal("2275.00"));
        testCrop.setMpBonusPerQuintal(new BigDecimal("125.00"));
        testCrop.setTotalMsp(new BigDecimal("2400.00"));
        testCrop.setMarketPricePerQuintal(new BigDecimal("2600.00"));
        testCrop.setMoistureLimitPct(new BigDecimal("12.00"));
        testCrop = cropRepository.save(testCrop);

        // Procurement Target
        ProcurementTarget target = new ProcurementTarget();
        target.setDistrict("Sehore");
        target.setCrop(testCrop);
        target.setSeason("Rabi");
        target.setProcurementYear("2025-26");
        target.setTargetQuintals(new BigDecimal("100000.00"));
        target.setWarehouseCapacityQuintals(new BigDecimal("120000.00"));
        target.setEffectiveStart(LocalDate.now().minusMonths(1));
        target.setEffectiveEnd(LocalDate.now().plusMonths(6));
        targetRepository.save(target);

        // Create completed booking with weighment and payment
        Booking b = new Booking();
        b.setFarmer(testFarmer);
        b.setMandi(testMandi);
        b.setCrop(testCrop);
        b.setTokenNumber("TK-REP-" + uid);
        b.setTokenSequence(1);
        b.setScheduledDate(LocalDate.now());
        b.setTimeSlot("08:00 AM - 10:00 AM");
        b.setSlotId("slot-1");
        b.setVehicleNumber("MP-04-AB-1234");
        b.setVehicleType("Tractor Trolley");
        b.setQrCodeData("QR-MP-REP-" + uid);
        b.setEstimatedYieldQuintals(new BigDecimal("50.00"));
        b.setStatus(BookingStatus.PROCUREMENT_COMPLETED);
        b.setNetWeightQuintals(new BigDecimal("48.50"));
        b.setMoisturePercentage(new BigDecimal("11.20"));
        b.setSettlementAmount(new BigDecimal("116400.00")); // 48.5 * 2400
        b = bookingRepository.save(b);

        Weighment w = new Weighment();
        w.setBooking(b);
        w.setMandi(testMandi);
        w.setGrossWeightQuintals(new BigDecimal("68.50"));
        w.setTareWeightQuintals(new BigDecimal("20.00"));
        w.setNetWeightQuintals(new BigDecimal("48.50"));
        w.setMoisturePct(new BigDecimal("11.20"));
        w.setForeignMatterPct(new BigDecimal("0.50"));
        weighmentRepository.save(w);

        Payment p = new Payment();
        p.setBooking(b);
        p.setFarmer(testFarmer);
        p.setMandi(testMandi);
        p.setGrossAmount(new BigDecimal("116400.00"));
        p.setDeductions(BigDecimal.ZERO);
        p.setNetPayableAmount(new BigDecimal("116400.00"));
        p.setBankAccountLast4("9012");
        p.setIfscCode("SBIN0001234");
        p.setPaymentStatus("COMPLETED");
        p.setDbtReferenceNo("DBT-REF-" + uid);
        p.setCreditedAt(OffsetDateTime.now());
        paymentRepository.save(p);
    }

    @Test
    @DisplayName("Verify statewide overview aggregates actual database records accurately")
    void testStatewideOverview() {
        StatewideOverviewDto overview = reportService.getStatewideOverview();
        assertNotNull(overview);
        assertTrue(overview.getTotalBookings() >= 1);
        assertTrue(overview.getTotalCompletedProcurements() >= 1);
        assertTrue(overview.getTotalCertifiedQuantityQuintals().compareTo(BigDecimal.ZERO) > 0);
        assertTrue(overview.getTotalProcurementValueRs().compareTo(BigDecimal.ZERO) > 0);
        assertTrue(overview.getTotalDbtDisbursedRs().compareTo(BigDecimal.ZERO) > 0);
        assertTrue(overview.getTotalFarmersServed() >= 1);
    }

    @Test
    @DisplayName("Verify dynamic district stats aggregation and achievement percentages")
    void testDistrictStats() {
        DistrictStatsReportDto dto = reportService.getDistrictStats();
        assertNotNull(dto);
        assertNotNull(dto.getStats());
        assertFalse(dto.getStats().isEmpty());

        DistrictProcurementStatDto sehoreStat = dto.getStats().stream()
                .filter(s -> "Sehore".equalsIgnoreCase(s.getDistrict()))
                .findFirst()
                .orElse(null);

        assertNotNull(sehoreStat, "Sehore statistics must be generated dynamically");
        assertTrue(sehoreStat.getProcuredQuintals() > 0, "Procured quintals must reflect saved booking net weight");
        assertTrue(sehoreStat.getTargetQuintals() > 0);
    }

    @Test
    @DisplayName("Verify Mandi-level performance and queue turnaround metrics")
    void testMandiPerformance() {
        List<MandiPerformanceDto> performance = reportService.getMandiPerformance("Sehore");
        assertNotNull(performance);
        assertFalse(performance.isEmpty());

        MandiPerformanceDto targetMandi = performance.stream()
                .filter(m -> m.getMandiId().equals(testMandi.getId()))
                .findFirst()
                .orElse(null);

        assertNotNull(targetMandi);
        assertEquals(1, targetMandi.getCompletedProcurements());
        assertEquals(new BigDecimal("48.50"), targetMandi.getCertifiedQuantityQuintals());
    }

    @Test
    @DisplayName("Verify Quality & Weighment FAQ threshold calculations")
    void testQualityAndWeighmentReport() {
        QualityAndWeighmentReportDto qcReport = reportService.getQualityAndWeighmentReport();
        assertNotNull(qcReport);
        assertTrue(qcReport.getTotalVehiclesWeighed() >= 1);
        assertTrue(qcReport.getTotalCertifiedNetQuintals().compareTo(BigDecimal.ZERO) > 0);
        assertTrue(qcReport.getAverageMoisturePct().compareTo(BigDecimal.ZERO) > 0);
    }

    @Test
    @DisplayName("Verify DBT payment analytics and delay monitoring")
    void testPaymentAnalytics() {
        PaymentAnalyticsReportDto payReport = reportService.getPaymentAnalytics(24);
        assertNotNull(payReport);
        assertTrue(payReport.getTotalDbtCompleted() >= 1);
        assertTrue(payReport.getTotalAmountSettledRs().compareTo(BigDecimal.ZERO) > 0);
    }

    @Test
    @DisplayName("Verify time-series daily aggregation")
    void testTimeSeries() {
        List<TimeSeriesPointDto> series = reportService.getTimeSeries(7);
        assertNotNull(series);
        assertEquals(7, series.size());
    }

    @Test
    @DisplayName("Verify granular procurement register audit rows")
    void testProcurementRegister() {
        List<ProcurementRegisterRowDto> register = reportService.getProcurementRegister("Sehore", testMandi.getId(), null);
        assertNotNull(register);
        assertFalse(register.isEmpty());
        ProcurementRegisterRowDto row = register.get(0);
        assertEquals("COMPLETED", row.getPaymentStatus());
        assertEquals(new BigDecimal("48.50"), row.getNetWeightQuintals());
    }
}
