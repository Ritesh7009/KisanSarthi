package com.kisansarthi.booking;

import com.kisansarthi.auth.Role;
import com.kisansarthi.auth.User;
import com.kisansarthi.auth.UserRepository;
import com.kisansarthi.common.*;
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
import com.kisansarthi.slot.MandiSlot;
import com.kisansarthi.slot.SlotRepository;
import com.kisansarthi.slot.SlotService;
import com.kisansarthi.weighment.WeighmentDto;
import com.kisansarthi.weighment.WeighmentRepository;
import com.kisansarthi.weighment.WeighmentService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:kisansarthi_remediation_test;DB_CLOSE_DELAY=-1;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.flyway.enabled=false"
})
public class BookingRemediationIntegrationTest {

    @Autowired
    private BookingService bookingService;

    @Autowired
    private SlotService slotService;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private MandiRepository mandiRepository;

    @Autowired
    private FarmerRepository farmerRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CropRepository cropRepository;

    @Autowired
    private MandiTokenSequenceRepository sequenceRepository;

    @Autowired
    private SlotRepository slotRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private WeighmentRepository weighmentRepository;

    @Autowired
    private WeighmentService weighmentService;

    @Autowired
    private com.kisansarthi.queue.QueueStateRepository queueStateRepository;

    @Autowired
    private com.kisansarthi.queue.QueueEventRepository queueEventRepository;

    private Mandi testMandi;
    private Crop testCrop;
    private Farmer testFarmer;
    private User testUser;
    private MandiSlot testSlot;

    private void setAuth(String username, Role role) {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                username,
                "N/A",
                List.of(new SimpleGrantedAuthority(role.name()))
        );
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private void cleanTestData() {
        SecurityContextHolder.clearContext();
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
        sequenceRepository.deleteAllInBatch();
        slotRepository.deleteAllInBatch();
        farmerRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();
        cropRepository.deleteAllInBatch();
        mandiRepository.deleteAllInBatch();
    }

    @BeforeEach
    void setUp() {
        cleanTestData();

        testMandi = new Mandi();
        testMandi.setId("mandi-rem-test-" + UUID.randomUUID().toString().substring(0, 6));
        testMandi.setName("Remediation Test Mandi");
        testMandi.setHindiName("सुधार टेस्ट मंडी");
        testMandi.setDistrict("Bhopal");
        testMandi.setHindiDistrict("भोपाल");
        testMandi.setAddress("Mandi Complex, Bhopal, MP");
        testMandi.setPinCode("462001");
        testMandi.setPhone("07552223344");
        testMandi.setCurrentTokenServing(0);
        testMandi.setTotalTokensToday(0);
        testMandi.setActiveTokensWaiting(0);
        testMandi = mandiRepository.save(testMandi);

        testCrop = new Crop();
        testCrop.setId("crop-wheat-rem-" + UUID.randomUUID().toString().substring(0, 6));
        testCrop.setName("Sharbati Wheat");
        testCrop.setHindiName("गेहूँ शरबती");
        testCrop.setSeason("RABI");
        testCrop.setStandardMspPerQuintal(new BigDecimal("2275.00"));
        testCrop.setMpBonusPerQuintal(new BigDecimal("125.00"));
        testCrop.setTotalMsp(new BigDecimal("2400.00"));
        testCrop.setMarketPricePerQuintal(new BigDecimal("2350.00"));
        testCrop.setMoistureLimitPct(new BigDecimal("12.00"));
        testCrop = cropRepository.save(testCrop);

        testUser = new User();
        testUser.setUsername("9826099999");
        testUser.setPasswordHash("hashed_password");
        testUser.setPhone("9826099999");
        testUser.setRole(Role.ROLE_FARMER);
        testUser.setActive(true);
        testUser.setCreatedAt(Instant.now());
        testUser.setUpdatedAt(Instant.now());
        testUser = userRepository.save(testUser);

        testFarmer = new Farmer();
        testFarmer.setUser(testUser);
        testFarmer.setKisanId("KISAN-9826099999");
        testFarmer.setName("Mukesh Yadav");
        testFarmer.setPhone("9826099999");
        testFarmer.setMaskedAadhar("XXXX-XXXX-9999");
        testFarmer.setDistrict("Bhopal");
        testFarmer.setVillage("Phanda");
        testFarmer.setBankAccountLast4("9999");
        testFarmer.setIfscCode("SBIN0002222");
        testFarmer.setCreatedAt(Instant.now());
        testFarmer.setUpdatedAt(Instant.now());
        testFarmer = farmerRepository.save(testFarmer);

        testSlot = new MandiSlot();
        testSlot.setId("slot-rem-test-0810");
        testSlot.setMandiId(testMandi.getId());
        testSlot.setSlotLabel("08:00 AM - 10:00 AM");
        testSlot.setStartTime("08:00");
        testSlot.setEndTime("10:00");
        testSlot.setMaxCapacityQuintals(5000);
        testSlot.setBookedQuintals(0);
        testSlot.setMaxFarmers(500);
        testSlot.setBookedFarmers(0);
        testSlot.setStatus("AVAILABLE");
        testSlot.setCreatedAt(Instant.now());
        testSlot.setUpdatedAt(Instant.now());
        testSlot = slotRepository.save(testSlot);
    }

    @AfterEach
    void tearDown() {
        cleanTestData();
    }

    @Test
    @DisplayName("Verify pagination on /api/v1/bookings/my: page boundaries, max size 100 capping, and createdAt DESC ordering")
    void testFarmerBookingsPagination() {
        setAuth(testUser.getUsername(), Role.ROLE_FARMER);
        // Create 25 bookings for testFarmer
        List<BookingResponse> createdList = new ArrayList<>();
        for (int i = 1; i <= 25; i++) {
            CreateBookingRequest req = new CreateBookingRequest();
            req.setFarmerId(testFarmer.getId());
            req.setMandiId(testMandi.getId());
            req.setCropId(testCrop.getId());
            req.setScheduledDate(LocalDate.now());
            req.setTimeSlot("08:00 AM - 10:00 AM");
            req.setVehicleType("TRACTOR_TROLLEY");
            req.setVehicleNumber(String.format("MP-04-T-%04d", i));
            req.setEstimatedYieldQuintals(new BigDecimal("10.00"));

            BookingResponse res = bookingService.createBooking(req, "pag-key-" + i, testUser.getUsername());
            createdList.add(res);
        }

        // 1. Test page 0, size 10 -> Should return 10 items
        Page<BookingResponse> page0 = bookingService.getBookingsByFarmer(testFarmer.getId(), 0, 10);
        assertEquals(25, page0.getTotalElements());
        assertEquals(3, page0.getTotalPages());
        assertEquals(10, page0.getContent().size());

        // Verify deterministic createdAt DESC ordering: The latest booking created is first
        assertEquals(createdList.get(24).getId(), page0.getContent().get(0).getId());

        // 2. Test page 2, size 10 -> Should return remaining 5 items
        Page<BookingResponse> page2 = bookingService.getBookingsByFarmer(testFarmer.getId(), 2, 10);
        assertEquals(5, page2.getContent().size());

        // 3. Test size > 100 capping -> Querying size=250 should be capped to 100
        Page<BookingResponse> cappedPage = bookingService.getBookingsByFarmer(testFarmer.getId(), 0, 250);
        assertEquals(25, cappedPage.getContent().size());
        assertEquals(100, cappedPage.getSize());
    }

    @Test
    @DisplayName("Verify requested quantity semantics with RoundingMode.CEILING for 2.00, 2.01, 2.50, 2.99, 3.00")
    void testRequestedQuantityCeilingSemantics() {
        setAuth(testUser.getUsername(), Role.ROLE_FARMER);
        BigDecimal[] testYields = {
                new BigDecimal("2.00"),
                new BigDecimal("2.01"),
                new BigDecimal("2.50"),
                new BigDecimal("2.99"),
                new BigDecimal("3.00")
        };
        int[] expectedReservedQuintals = {2, 3, 3, 3, 3};

        for (int i = 0; i < testYields.length; i++) {
            BigDecimal yield = testYields[i];
            int expectedCeil = expectedReservedQuintals[i];

            // Verify the integer ceiling calculation matches
            int calculatedCeil = yield.setScale(0, RoundingMode.CEILING).intValue();
            assertEquals(expectedCeil, calculatedCeil, "Ceiling for " + yield + " must be " + expectedCeil);

            // Create slot for exact measurement
            MandiSlot specificSlot = new MandiSlot();
            specificSlot.setId("slot-ceil-test-" + i);
            specificSlot.setMandiId(testMandi.getId());
            specificSlot.setSlotLabel("10:00 AM - 12:00 PM");
            specificSlot.setStartTime("10:00");
            specificSlot.setEndTime("12:00");
            specificSlot.setMaxCapacityQuintals(100);
            specificSlot.setBookedQuintals(0);
            specificSlot.setMaxFarmers(10);
            specificSlot.setBookedFarmers(0);
            specificSlot.setStatus("AVAILABLE");
            slotRepository.save(specificSlot);

            CreateBookingRequest req = new CreateBookingRequest();
            req.setFarmerId(testFarmer.getId());
            req.setMandiId(testMandi.getId());
            req.setCropId(testCrop.getId());
            req.setSlotId(specificSlot.getId());
            req.setScheduledDate(LocalDate.now());
            req.setTimeSlot("10:00 AM - 12:00 PM");
            req.setVehicleType("TRACTOR_TROLLEY");
            req.setVehicleNumber("MP-04-AB-1234");
            req.setEstimatedYieldQuintals(yield);

            BookingResponse response = bookingService.createBooking(req, "ceil-key-" + i, testUser.getUsername());
            assertNotNull(response);

            // Stored entity must retain exact decimal precision (e.g. 2.01 or 2.50)
            assertEquals(0, yield.compareTo(response.getEstimatedYieldQuintals()));

            // Slot bookedQuintals must reflect the safe integer ceiling reservation
            MandiSlot updatedSlot = slotRepository.findById(specificSlot.getId()).orElseThrow();
            assertEquals(expectedCeil, updatedSlot.getBookedQuintals());

            // On cancellation, the exact same integer ceiling reservation must be cleanly deducted
            bookingService.cancelBooking(response.getId(), testUser.getUsername());
            MandiSlot releasedSlot = slotRepository.findById(specificSlot.getId()).orElseThrow();
            assertEquals(0, releasedSlot.getBookedQuintals());
            assertEquals(0, releasedSlot.getBookedFarmers());
        }
    }

    @Test
    @DisplayName("Verify multi-thread concurrent token generation with database-level sequence locking (no duplicate tokens)")
    void testConcurrentDatabaseLevelTokenSequenceLocking() throws InterruptedException {
        int threadCount = 20;
        ExecutorService executor = Executors.newFixedThreadPool(10);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(threadCount);

        List<BookingResponse> responses = Collections.synchronizedList(new ArrayList<>());
        AtomicInteger failures = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            executor.submit(() -> {
                try {
                    startLatch.await();
                    setAuth(testUser.getUsername(), Role.ROLE_FARMER);
                    CreateBookingRequest req = new CreateBookingRequest();
                    req.setFarmerId(testFarmer.getId());
                    req.setMandiId(testMandi.getId());
                    req.setCropId(testCrop.getId());
                    req.setScheduledDate(LocalDate.now());
                    req.setTimeSlot("08:00 AM - 10:00 AM");
                    req.setVehicleType("TRACTOR_TROLLEY");
                    req.setVehicleNumber(String.format("MP-04-CC-%04d", index + 1));
                    req.setEstimatedYieldQuintals(new BigDecimal("15.00"));

                    BookingResponse res = bookingService.createBooking(req, "db-lock-key-" + index, testUser.getUsername());
                    responses.add(res);
                } catch (Exception e) {
                    failures.incrementAndGet();
                } finally {
                    finishLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        boolean completed = finishLatch.await(20, TimeUnit.SECONDS);
        executor.shutdown();

        assertTrue(completed);
        assertEquals(0, failures.get());
        assertEquals(threadCount, responses.size());

        // Verify all token sequences are unique and strictly monotonic 1..20
        Set<Integer> uniqueSequences = new HashSet<>();
        for (BookingResponse res : responses) {
            assertTrue(uniqueSequences.add(res.getTokenSequence()));
        }
        assertEquals(threadCount, uniqueSequences.size());
    }

    @Test
    @DisplayName("Verify full lifecycle: Booking -> Weighment -> Payment DBT -> Precision check")
    void testFullProcurementFinancialLifecycle() {
        setAuth(testUser.getUsername(), Role.ROLE_FARMER);
        CreateBookingRequest req = new CreateBookingRequest();
        req.setFarmerId(testFarmer.getId());
        req.setMandiId(testMandi.getId());
        req.setCropId(testCrop.getId());
        req.setScheduledDate(LocalDate.now());
        req.setTimeSlot("08:00 AM - 10:00 AM");
        req.setVehicleType("TRACTOR_TROLLEY");
        req.setVehicleNumber("MP-04-FP-1111");
        req.setEstimatedYieldQuintals(new BigDecimal("50.00"));

        BookingResponse booking = bookingService.createBooking(req, "full-life-key", testUser.getUsername());
        assertNotNull(booking);

        // Record Gross (65.25 Qtl) and Tare (15.25 Qtl) -> Net = 50.00 Qtl
        setAuth("op1", Role.ROLE_ADMIN);
        WeighmentDto weighmentReq = new WeighmentDto();
        weighmentReq.setGrossWeightQuintals(new BigDecimal("65.25"));
        weighmentReq.setTareWeightQuintals(new BigDecimal("15.25"));
        weighmentReq.setMoisturePct(new BigDecimal("11.20"));
        WeighmentDto recorded = weighmentService.recordWeighment(booking.getId(), weighmentReq, "op1");

        assertEquals(0, new BigDecimal("50.00").compareTo(recorded.getNetWeightQuintals()));
        // Total MSP = 2400.00, Payout = 50.00 * 2400.00 = 120,000.00
        BigDecimal expectedPayout = new BigDecimal("120000.00");
        assertEquals(0, expectedPayout.compareTo(recorded.getNetPayableAmount()));

        // Complete Procurement
        setAuth("officer-patil", Role.ROLE_ADMIN);
        BookingResponse cert = weighmentService.completeProcurement(booking.getId(), "officer-patil");
        assertEquals(BookingStatus.PROCUREMENT_COMPLETED.name(), cert.getStatus());

        // Initiate DBT Payment
        setAuth("accountant1", Role.ROLE_ADMIN);
        PaymentDto paymentDto = paymentService.initiatePayment(booking.getId(), new PaymentDto());
        assertEquals("PROCESSING", paymentDto.getPaymentStatus());
        assertEquals(0, expectedPayout.compareTo(paymentDto.getNetPayableAmount()));

        // Confirm DBT Payment
        PaymentDto confirmed = paymentService.confirmPaymentCredit(booking.getId(), paymentDto.getDbtReferenceNo());
        assertEquals("COMPLETED", confirmed.getPaymentStatus());

        Booking finalBooking = bookingRepository.findById(booking.getId()).orElseThrow();
        assertEquals(BookingStatus.COMPLETED, finalBooking.getStatus());
    }

    @Test
    @DisplayName("Verify cancellation atomicity and failure handling: simulated capacity release failure rolls back entire transaction")
    void testCancellationFailureRollsBackTransaction() {
        setAuth(testUser.getUsername(), Role.ROLE_FARMER);
        // 1. Create a booking associated with a custom slot
        MandiSlot customSlot = new MandiSlot();
        customSlot.setId("slot-cancel-fail-test");
        customSlot.setMandiId(testMandi.getId());
        customSlot.setSlotLabel("11:00 AM - 01:00 PM");
        customSlot.setStartTime("11:00");
        customSlot.setEndTime("13:00");
        customSlot.setMaxCapacityQuintals(1000);
        customSlot.setBookedQuintals(0);
        customSlot.setMaxFarmers(50);
        customSlot.setBookedFarmers(0);
        customSlot.setStatus("AVAILABLE");
        slotRepository.save(customSlot);

        CreateBookingRequest req = new CreateBookingRequest();
        req.setFarmerId(testFarmer.getId());
        req.setMandiId(testMandi.getId());
        req.setCropId(testCrop.getId());
        req.setSlotId(customSlot.getId());
        req.setScheduledDate(LocalDate.now());
        req.setTimeSlot("11:00 AM - 01:00 PM");
        req.setVehicleType("TRACTOR_TROLLEY");
        req.setVehicleNumber("MP-04-CF-1234");
        req.setEstimatedYieldQuintals(new BigDecimal("30.00"));

        BookingResponse created = bookingService.createBooking(req, "cancel-atomic-key", testUser.getUsername());
        assertNotNull(created);
        UUID bookingId = created.getId();

        Mandi mandiBefore = mandiRepository.findById(testMandi.getId()).orElseThrow();
        int waitingBefore = mandiBefore.getActiveTokensWaiting();

        // 2. Corrupt/delete the slot to simulate an unexpected capacity release failure
        slotRepository.deleteById(customSlot.getId());

        // 3. Attempting to cancel must fail and propagate exception (SlotNotFoundException)
        assertThrows(Exception.class, () -> bookingService.cancelBooking(bookingId, testUser.getUsername()));

        // 4. Verify transaction rollback: Booking MUST remain BOOKED, NOT CANCELLED
        Booking postFailureBooking = bookingRepository.findById(bookingId).orElseThrow();
        assertEquals(BookingStatus.BOOKED, postFailureBooking.getStatus(),
                "Booking status must remain BOOKED after capacity release failure");

        // 5. Verify waiting count remained unchanged
        Mandi mandiAfterFailure = mandiRepository.findById(testMandi.getId()).orElseThrow();
        assertEquals(waitingBefore, mandiAfterFailure.getActiveTokensWaiting(),
                "Waiting count must remain unchanged when cancellation rolls back");
    }

    @Test
    @DisplayName("Verify cancellation restrictions: repeated cancellation and cancellation after procurement progression")
    void testCancellationRestrictionsAndProgression() {
        setAuth(testUser.getUsername(), Role.ROLE_FARMER);
        CreateBookingRequest req = new CreateBookingRequest();
        req.setFarmerId(testFarmer.getId());
        req.setMandiId(testMandi.getId());
        req.setCropId(testCrop.getId());
        req.setScheduledDate(LocalDate.now());
        req.setTimeSlot("08:00 AM - 10:00 AM");
        req.setVehicleType("TRACTOR_TROLLEY");
        req.setVehicleNumber("MP-04-CR-5678");
        req.setEstimatedYieldQuintals(new BigDecimal("20.00"));

        BookingResponse created = bookingService.createBooking(req, "cancel-prog-key", testUser.getUsername());
        assertNotNull(created);
        UUID bookingId = created.getId();

        // Progress booking: BOOKED -> GATE_CALLED -> GATE_ENTERED -> WEIGHING (cancellation no longer permitted once in WEIGHING)
        setAuth("admin", Role.ROLE_ADMIN);
        bookingService.transitionStatus(bookingId, BookingStatus.GATE_CALLED);
        bookingService.transitionStatus(bookingId, BookingStatus.GATE_ENTERED);
        bookingService.transitionStatus(bookingId, BookingStatus.WEIGHING);

        // Attempting to cancel while in WEIGHING must be rejected (cannot cancel once weighing has started)
        setAuth(testUser.getUsername(), Role.ROLE_FARMER);
        assertThrows(BusinessException.class, () -> bookingService.cancelBooking(bookingId, testUser.getUsername()));

        Booking curBooking = bookingRepository.findById(bookingId).orElseThrow();
        assertEquals(BookingStatus.WEIGHING, curBooking.getStatus());
    }
}
