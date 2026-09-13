package com.kisansarthi.weighment;

import com.kisansarthi.booking.Booking;
import com.kisansarthi.booking.BookingRepository;
import com.kisansarthi.booking.BookingResponse;
import com.kisansarthi.booking.BookingStatus;
import com.kisansarthi.common.BusinessException;
import com.kisansarthi.crop.Crop;
import com.kisansarthi.crop.CropRepository;
import com.kisansarthi.farmer.Farmer;
import com.kisansarthi.farmer.FarmerRepository;
import com.kisansarthi.mandi.Mandi;
import com.kisansarthi.mandi.MandiRepository;
import com.kisansarthi.payment.Payment;
import com.kisansarthi.payment.PaymentDto;
import com.kisansarthi.payment.PaymentRepository;
import com.kisansarthi.payment.PaymentService;
import com.kisansarthi.queue.QueueEventRepository;
import com.kisansarthi.queue.QueueStateRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:kisansarthi_weighment_test;DB_CLOSE_DELAY=-1;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.flyway.enabled=false"
})
public class WeighmentLifecycleTest {

    @Autowired
    private WeighmentService weighmentService;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private WeighmentRepository weighmentRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private QueueEventRepository queueEventRepository;

    @Autowired
    private QueueStateRepository queueStateRepository;

    @Autowired
    private MandiRepository mandiRepository;

    @Autowired
    private CropRepository cropRepository;

    @Autowired
    private FarmerRepository farmerRepository;

    private Mandi testMandi;
    private Crop testCrop;
    private Farmer testFarmer;
    private Booking testBooking;

    private void cleanData() {
        queueEventRepository.deleteAllInBatch();
        queueStateRepository.findAll().forEach(qs -> {
            if (qs.getActiveBooking() != null) {
                qs.setActiveBooking(null);
                queueStateRepository.save(qs);
            }
        });
        queueStateRepository.flush();
        queueStateRepository.deleteAllInBatch();
        weighmentRepository.deleteAllInBatch();
        paymentRepository.deleteAllInBatch();
        bookingRepository.deleteAllInBatch();
        farmerRepository.deleteAllInBatch();
        cropRepository.deleteAllInBatch();
        mandiRepository.deleteAllInBatch();
    }

    @BeforeEach
    void setUp() {
        cleanData();

        testMandi = new Mandi();
        testMandi.setId("mandi-weighment-test-" + UUID.randomUUID().toString().substring(0, 6));
        testMandi.setName("Hoshangabad Mandi Test");
        testMandi.setHindiName("होशंगाबाद टेस्ट मंडी");
        testMandi.setDistrict("Narmadapuram");
        testMandi.setHindiDistrict("नर्मदापुरम");
        testMandi.setAddress("Mandi Complex, Narmadapuram, MP");
        testMandi.setPinCode("461001");
        testMandi.setPhone("07574252111");
        testMandi.setCurrentTokenServing(0);
        testMandi.setTotalTokensToday(0);
        testMandi.setActiveTokensWaiting(0);
        testMandi = mandiRepository.save(testMandi);

        testCrop = new Crop();
        testCrop.setId("crop-wheat-test-" + UUID.randomUUID().toString().substring(0, 6));
        testCrop.setName("Sharbati Wheat");
        testCrop.setHindiName("गेहूँ शरबती");
        testCrop.setSeason("RABI");
        testCrop.setStandardMspPerQuintal(BigDecimal.valueOf(2275.00));
        testCrop.setMpBonusPerQuintal(BigDecimal.valueOf(125.00));
        testCrop.setTotalMsp(BigDecimal.valueOf(2400.00));
        testCrop.setMarketPricePerQuintal(BigDecimal.valueOf(2350.00));
        testCrop.setMoistureLimitPct(BigDecimal.valueOf(12.00));
        testCrop = cropRepository.save(testCrop);

        testFarmer = new Farmer();
        testFarmer.setKisanId("KISAN-9826012345");
        testFarmer.setName("Ramesh Patel");
        testFarmer.setPhone("9826012345");
        testFarmer.setMaskedAadhar("XXXX-XXXX-1234");
        testFarmer.setDistrict("Narmadapuram");
        testFarmer.setVillage("Babai");
        testFarmer.setBankAccountLast4("4321");
        testFarmer.setIfscCode("SBIN0001111");
        testFarmer = farmerRepository.save(testFarmer);

        testBooking = new Booking();
        testBooking.setMandi(testMandi);
        testBooking.setFarmer(testFarmer);
        testBooking.setCrop(testCrop);
        testBooking.setSlotId("slot-1");
        testBooking.setTimeSlot("10:00 AM - 12:00 PM");
        testBooking.setScheduledDate(LocalDate.now());
        testBooking.setTokenNumber("MP-HOS-001");
        testBooking.setTokenSequence(1);
        testBooking.setStatus(BookingStatus.GATE_ENTERED);
        testBooking.setEstimatedYieldQuintals(BigDecimal.valueOf(45.00));
        testBooking.setVehicleNumber("MP-04-AB-1234");
        testBooking.setVehicleType("Tractor Trolley");
        testBooking.setQrCodeData("QR-MP-HOS-001-2026");
        testBooking = bookingRepository.save(testBooking);
    }

    @AfterEach
    void tearDown() {
        cleanData();
    }

    @Test
    @DisplayName("Should advance booking to WEIGHING and log queue event on startWeighment")
    void testStartWeighment() {
        WeighmentDto result = weighmentService.startWeighment(testBooking.getId(), "Kanta Bay 2", "test-operator");

        assertNotNull(result);
        assertEquals("WEIGHING", result.getBookingStatus());
        assertEquals("Kanta Bay 2", result.getWeighbridgeBay());

        Booking updated = bookingRepository.findById(testBooking.getId()).orElseThrow();
        assertEquals(BookingStatus.WEIGHING, updated.getStatus());
    }

    @Test
    @DisplayName("Should reject invalid gross weight <= 0")
    void testRejectInvalidGrossWeight() {
        WeighmentDto req = new WeighmentDto();
        req.setGrossWeightQuintals(BigDecimal.valueOf(-5.00));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                weighmentService.recordWeighment(testBooking.getId(), req, "operator1"));
        assertEquals("INVALID_WEIGHT", ex.getCode());

        req.setGrossWeightQuintals(BigDecimal.ZERO);
        assertThrows(BusinessException.class, () ->
                weighmentService.recordWeighment(testBooking.getId(), req, "operator1"));
    }

    @Test
    @DisplayName("Should reject tare weight before gross weight is recorded")
    void testRejectTareBeforeGross() {
        WeighmentDto req = new WeighmentDto();
        req.setTareWeightQuintals(BigDecimal.valueOf(15.00));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                weighmentService.recordWeighment(testBooking.getId(), req, "operator1"));
        assertEquals("INVALID_WEIGHT", ex.getCode());
    }

    @Test
    @DisplayName("Should reject tare weight greater than or equal to gross weight")
    void testRejectTareGreaterOrEqualToGross() {
        // Record gross 50.00 Qtl
        WeighmentDto grossReq = new WeighmentDto();
        grossReq.setGrossWeightQuintals(BigDecimal.valueOf(50.00));
        weighmentService.recordWeighment(testBooking.getId(), grossReq, "operator1");

        // Try tare 50.00 Qtl (equal)
        WeighmentDto equalTare = new WeighmentDto();
        equalTare.setTareWeightQuintals(BigDecimal.valueOf(50.00));
        BusinessException ex1 = assertThrows(BusinessException.class, () ->
                weighmentService.recordWeighment(testBooking.getId(), equalTare, "operator1"));
        assertEquals("INVALID_WEIGHT", ex1.getCode());

        // Try tare 55.00 Qtl (greater)
        WeighmentDto highTare = new WeighmentDto();
        highTare.setTareWeightQuintals(BigDecimal.valueOf(55.00));
        BusinessException ex2 = assertThrows(BusinessException.class, () ->
                weighmentService.recordWeighment(testBooking.getId(), highTare, "operator1"));
        assertEquals("INVALID_WEIGHT", ex2.getCode());
    }

    @Test
    @DisplayName("Should reject invalid moisture percentage (> 100 or < 0)")
    void testRejectInvalidMoisture() {
        WeighmentDto req = new WeighmentDto();
        req.setGrossWeightQuintals(BigDecimal.valueOf(60.00));
        req.setMoisturePct(BigDecimal.valueOf(105.00));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                weighmentService.recordWeighment(testBooking.getId(), req, "operator1"));
        assertEquals("INVALID_MOISTURE", ex.getCode());
    }

    @Test
    @DisplayName("Should calculate authoritative Net Weight (Gross - Tare), MSP payout, and transition to WEIGHMENT_COMPLETED")
    void testCompleteWeighmentLifecycle() {
        // 1. Start weighment
        weighmentService.startWeighment(testBooking.getId(), "Kanta Bay 1", "operator1");

        // 2. Record Gross Weight: 60.00 Quintals (6000 Kg)
        WeighmentDto grossReq = new WeighmentDto();
        grossReq.setGrossWeightQuintals(BigDecimal.valueOf(60.00));
        grossReq.setMoisturePct(BigDecimal.valueOf(11.50));
        WeighmentDto stage1 = weighmentService.recordWeighment(testBooking.getId(), grossReq, "operator1");

        assertEquals(0, BigDecimal.valueOf(60.00).compareTo(stage1.getGrossWeightQuintals()));
        assertEquals(0, BigDecimal.valueOf(6000).compareTo(stage1.getGrossWeightKg()));
        assertEquals(BookingStatus.WEIGHMENT_STAGE_1.name(), stage1.getBookingStatus());

        // 3. Record Tare Weight: 12.00 Quintals (1200 Kg)
        WeighmentDto tareReq = new WeighmentDto();
        tareReq.setTareWeightQuintals(BigDecimal.valueOf(12.00));
        WeighmentDto completed = weighmentService.recordWeighment(testBooking.getId(), tareReq, "operator1");

        // Authoritative Net = 60.00 - 12.00 = 48.00 Quintals (4800 Kg)
        assertEquals(0, BigDecimal.valueOf(48.00).compareTo(completed.getNetWeightQuintals()));
        assertEquals(0, BigDecimal.valueOf(4800).compareTo(completed.getNetWeightKg()));
        assertEquals(BookingStatus.WEIGHMENT_COMPLETED.name(), completed.getBookingStatus());

        // Total MSP rate = 2400.00 Rs/Qtl
        // Payout = 48.00 * 2400.00 = 115,200.00
        BigDecimal expectedPayout = BigDecimal.valueOf(115200.00).setScale(2);
        assertEquals(0, expectedPayout.compareTo(completed.getNetPayableAmount()));

        // Check Payment pre-population
        Payment payment = paymentRepository.findByBookingId(testBooking.getId()).orElseThrow();
        assertEquals("PENDING", payment.getPaymentStatus());
        assertEquals(0, expectedPayout.compareTo(payment.getNetPayableAmount()));
        assertEquals("4321", payment.getBankAccountLast4());
    }

    @Test
    @DisplayName("Should complete procurement, initiate DBT payment, and confirm bank credit")
    void testProcurementAndPaymentLifecycle() {
        // Record both weights
        WeighmentDto req = new WeighmentDto();
        req.setGrossWeightQuintals(BigDecimal.valueOf(50.00));
        req.setTareWeightQuintals(BigDecimal.valueOf(10.00));
        req.setMoisturePct(BigDecimal.valueOf(10.80));
        weighmentService.recordWeighment(testBooking.getId(), req, "operator1");

        // Complete Procurement
        BookingResponse procurementResp = weighmentService.completeProcurement(testBooking.getId(), "officer-sharma");
        assertEquals(BookingStatus.PROCUREMENT_COMPLETED.name(), procurementResp.getStatus());
        assertEquals(0, BigDecimal.valueOf(40.00).compareTo(procurementResp.getNetWeightQuintals()));

        // Initiate Payment
        PaymentDto initiated = paymentService.initiatePayment(testBooking.getId(), new PaymentDto());
        assertNotNull(initiated.getDbtReferenceNo());
        assertTrue(initiated.getDbtReferenceNo().startsWith("DBT-MP-"));
        assertEquals("PROCESSING", initiated.getPaymentStatus());

        Booking paymentProcessingBooking = bookingRepository.findById(testBooking.getId()).orElseThrow();
        assertEquals(BookingStatus.PAYMENT_PROCESSING, paymentProcessingBooking.getStatus());

        // Confirm DBT Credit
        PaymentDto credited = paymentService.confirmPaymentCredit(testBooking.getId(), initiated.getDbtReferenceNo());
        assertEquals("COMPLETED", credited.getPaymentStatus());
        assertNotNull(credited.getCreditedAt());

        Booking finalBooking = bookingRepository.findById(testBooking.getId()).orElseThrow();
        assertEquals(BookingStatus.COMPLETED, finalBooking.getStatus());
    }
}
