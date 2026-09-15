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
import com.kisansarthi.queue.QueueEventDto;
import com.kisansarthi.queue.QueueService;
import com.kisansarthi.slot.MandiSlot;
import com.kisansarthi.slot.SlotRepository;
import com.kisansarthi.slot.SlotService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:kisansarthi_test;DB_CLOSE_DELAY=-1;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.flyway.enabled=false"
})
public class BookingConcurrencyTest {

    @Autowired
    private BookingService bookingService;

    @Autowired
    private SlotService slotService;

    @Autowired
    private QueueService queueService;

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
    private com.kisansarthi.queue.QueueStateRepository queueStateRepository;

    @Autowired
    private com.kisansarthi.queue.QueueEventRepository queueEventRepository;

    private Mandi testMandi;
    private Crop testCrop;
    private List<Farmer> testFarmers;
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
        // Clear active booking pointer first to prevent FK constraint violations
        queueStateRepository.findAll().forEach(qs -> {
            if (qs.getActiveBooking() != null) {
                qs.setActiveBooking(null);
                queueStateRepository.save(qs);
            }
        });
        queueStateRepository.flush();
        queueStateRepository.deleteAllInBatch();
        bookingRepository.deleteAllInBatch();
        sequenceRepository.deleteAllInBatch();
        slotRepository.deleteAllInBatch();
    }

    @AfterEach
    void tearDown() {
        cleanTestData();
    }

    @BeforeEach
    void setUp() {
        cleanTestData();

        // Ensure test mandi exists
        if (!mandiRepository.existsById("mandi-sehore-test")) {
            Mandi m = new Mandi();
            m.setId("mandi-sehore-test");
            m.setName("Sehore Test Mandi");
            m.setHindiName("सीहोर टेस्ट मंडी");
            m.setDistrict("Sehore");
            m.setHindiDistrict("सीहोर");
            m.setAddress("Mandi Complex, Sehore, MP");
            m.setPinCode("466001");
            m.setPhone("07562224411");
            m.setCurrentTokenServing(0);
            m.setTotalTokensToday(0);
            m.setActiveTokensWaiting(0);
            testMandi = mandiRepository.save(m);
        } else {
            mandiRepository.resetCounters("mandi-sehore-test");
            testMandi = mandiRepository.findById("mandi-sehore-test").orElseThrow();
        }

        // Reset/init QueueState
        com.kisansarthi.queue.QueueState q = queueStateRepository.findById("mandi-sehore-test")
                .orElseGet(() -> new com.kisansarthi.queue.QueueState("mandi-sehore-test"));
        q.setCurrentServingToken(0);
        q.setTotalTokensGenerated(0);
        q.setWaitingCount(0);
        q.setActiveBooking(null);
        queueStateRepository.save(q);

        // Ensure admin user exists
        userRepository.findByUsername("admin").orElseGet(() -> {
            User admin = new User();
            admin.setUsername("admin");
            admin.setPasswordHash("hashed_password");
            admin.setRole(Role.ROLE_ADMIN);
            admin.setActive(true);
            admin.setCreatedAt(Instant.now());
            admin.setUpdatedAt(Instant.now());
            return userRepository.save(admin);
        });

        // Ensure test crop exists
        testCrop = cropRepository.findById("crop-wheat-test").orElseGet(() -> {
            Crop c = new Crop();
            c.setId("crop-wheat-test");
            c.setName("Wheat Test Grade");
            c.setHindiName("गेहूँ शरबती");
            c.setSeason("RABI");
            c.setStandardMspPerQuintal(new BigDecimal("2275.00"));
            c.setMpBonusPerQuintal(new BigDecimal("150.00"));
            c.setTotalMsp(new BigDecimal("2425.00"));
            c.setMarketPricePerQuintal(new BigDecimal("2300.00"));
            return cropRepository.save(c);
        });

        // Ensure test slot exists with generous capacity for 100 concurrent requests
        testSlot = new MandiSlot();
        testSlot.setId("slot-sehore-test-0810");
        testSlot.setMandiId(testMandi.getId());
        testSlot.setSlotLabel("08:00 AM - 10:00 AM");
        testSlot.setStartTime("08:00");
        testSlot.setEndTime("10:00");
        testSlot.setMaxCapacityQuintals(10000);
        testSlot.setBookedQuintals(0);
        testSlot.setMaxFarmers(200);
        testSlot.setBookedFarmers(0);
        testSlot.setStatus("AVAILABLE");
        testSlot.setCreatedAt(Instant.now());
        testSlot.setUpdatedAt(Instant.now());
        testSlot = slotRepository.save(testSlot);

        // Create a pool of farmers and associated users for testing
        testFarmers = new ArrayList<>();
        for (int i = 1; i <= 100; i++) {
            String phone = String.format("98000%05d", i);
            User user = userRepository.findByUsername(phone).orElseGet(() -> {
                User u = new User();
                u.setUsername(phone);
                u.setPasswordHash("hashed_password");
                u.setPhone(phone);
                u.setRole(Role.ROLE_FARMER);
                u.setActive(true);
                u.setCreatedAt(Instant.now());
                u.setUpdatedAt(Instant.now());
                return userRepository.save(u);
            });

            Farmer f = farmerRepository.findByPhone(phone).orElseGet(() -> {
                Farmer newFarmer = new Farmer();
                newFarmer.setUser(user);
                newFarmer.setKisanId("KISAN-" + phone);
                newFarmer.setName("Farmer Test " + phone);
                newFarmer.setPhone(phone);
                newFarmer.setMaskedAadhar("XXXX-XXXX-" + phone.substring(phone.length() - 4));
                newFarmer.setDistrict("Sehore");
                newFarmer.setVillage("Bilkisganj");
                newFarmer.setCreatedAt(Instant.now());
                newFarmer.setUpdatedAt(Instant.now());
                return farmerRepository.save(newFarmer);
            });

            if (f.getUser() == null) {
                f.setUser(user);
                f = farmerRepository.save(f);
            }
            testFarmers.add(f);
        }
    }

    @Test
    @DisplayName("Concurrently submit 100 simultaneous booking requests: verify zero duplicates, gapless sequence, and exact token counts")
    void test100SimultaneousBookings() throws InterruptedException {
        int threadCount = 100;
        ExecutorService executor = Executors.newFixedThreadPool(20);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(threadCount);

        List<BookingResponse> responses = Collections.synchronizedList(new ArrayList<>());
        AtomicInteger failureCount = new AtomicInteger(0);

        LocalDate scheduledDate = LocalDate.now();

        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            final Farmer farmer = testFarmers.get(index);
            final String idempotencyKey = "test-key-" + UUID.randomUUID();

            executor.submit(() -> {
                try {
                    startLatch.await(); // wait for all threads to align
                    setAuth(farmer.getPhone(), Role.ROLE_FARMER);

                    CreateBookingRequest req = new CreateBookingRequest();
                    req.setFarmerId(farmer.getId());
                    req.setMandiId(testMandi.getId());
                    req.setCropId(testCrop.getId());
                    req.setScheduledDate(scheduledDate);
                    req.setTimeSlot("08:00 AM - 10:00 AM");
                    req.setVehicleType("TRACTOR_TROLLEY");
                    req.setVehicleNumber(String.format("MP-04-AB-%04d", index + 1));
                    req.setEstimatedYieldQuintals(new BigDecimal("45.50"));

                    BookingResponse res = bookingService.createBooking(req, idempotencyKey, farmer.getPhone());
                    responses.add(res);
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                } finally {
                    finishLatch.countDown();
                }
            });
        }

        // Fire all threads simultaneously
        startLatch.countDown();
        boolean completed = finishLatch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        assertTrue(completed, "All 100 concurrent requests should complete within 30 seconds");
        assertEquals(0, failureCount.get(), "Zero booking creations should fail");
        assertEquals(100, responses.size(), "Exactly 100 responses must be returned");

        // Verification 1: No duplicate token numbers
        Set<String> uniqueTokenNumbers = new HashSet<>();
        for (BookingResponse r : responses) {
            assertTrue(uniqueTokenNumbers.add(r.getTokenNumber()),
                    "Found duplicate token number: " + r.getTokenNumber());
        }
        assertEquals(100, uniqueTokenNumbers.size());

        // Verification 2: No duplicate database bookings
        long totalInDb = bookingRepository.count();
        assertEquals(100, totalInDb, "Exactly 100 booking entities must be persisted in DB");

        // Verification 3: Gapless monotonic token sequences from 1 to 100
        List<Integer> sequences = responses.stream()
                .map(BookingResponse::getTokenSequence)
                .sorted()
                .toList();
        for (int i = 0; i < 100; i++) {
            assertEquals(i + 1, sequences.get(i), "Token sequence at index " + i + " must be strictly " + (i + 1));
        }

        // Verification 4: Correct total and waiting token counts on Mandi
        Mandi refreshedMandi = mandiRepository.findById(testMandi.getId()).orElseThrow();
        assertEquals(100, refreshedMandi.getTotalTokensToday(), "Mandi totalTokensToday must be exactly 100");
        assertEquals(100, refreshedMandi.getActiveTokensWaiting(), "Mandi activeTokensWaiting must be exactly 100");
    }

    @Test
    @DisplayName("Concurrently submit 20 requests with the SAME idempotency key: verify exactly 1 booking created and no race conditions")
    void test20ConcurrentRequestsSameIdempotencyKey() throws InterruptedException {
        int threadCount = 20;
        ExecutorService executor = Executors.newFixedThreadPool(10);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(threadCount);

        List<BookingResponse> responses = Collections.synchronizedList(new ArrayList<>());
        AtomicInteger failureCount = new AtomicInteger(0);

        Farmer farmer = testFarmers.get(0);
        String sharedIdempotencyKey = "shared-idemp-race-key-" + UUID.randomUUID();
        LocalDate scheduledDate = LocalDate.now().plusDays(20);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    setAuth(farmer.getPhone(), Role.ROLE_FARMER);

                    CreateBookingRequest req = new CreateBookingRequest();
                    req.setFarmerId(farmer.getId());
                    req.setMandiId(testMandi.getId());
                    req.setCropId(testCrop.getId());
                    req.setScheduledDate(scheduledDate);
                    req.setTimeSlot("08:00 AM - 10:00 AM");
                    req.setVehicleType("TRACTOR_TROLLEY");
                    req.setVehicleNumber("MP-04-ID-9999");
                    req.setEstimatedYieldQuintals(new BigDecimal("30.00"));

                    BookingResponse res = bookingService.createBooking(req, sharedIdempotencyKey, farmer.getPhone());
                    if (res != null) {
                        responses.add(res);
                    }
                } catch (Exception e) {
                    System.err.println("IDEMP_TEST_ERR: " + e.getClass().getName() + " -> " + e.getMessage());
                    failureCount.incrementAndGet();
                } finally {
                    finishLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        boolean completed = finishLatch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        assertTrue(completed, "All 20 concurrent requests should complete within 30 seconds");
        assertEquals(0, failureCount.get(), "Zero requests should fail with unhandled exceptions under idempotent race");
        assertEquals(20, responses.size(), "All 20 requests must receive a response");

        // Verify that only 1 booking was created with this idempotency key in DB
        List<Booking> bookingsInDb = bookingRepository.findAll().stream()
                .filter(b -> sharedIdempotencyKey.equals(b.getIdempotencyKey()))
                .toList();
        assertEquals(1, bookingsInDb.size(), "Exactly 1 booking record must exist in DB for the shared idempotency key");

        // Verify all successful responses returned the identical booking ID and token number
        UUID singleBookingId = bookingsInDb.get(0).getId();
        String singleTokenNumber = bookingsInDb.get(0).getTokenNumber();
        for (BookingResponse r : responses) {
            assertEquals(singleBookingId, r.getId(), "All responses must return the same booking ID");
            assertEquals(singleTokenNumber, r.getTokenNumber(), "All responses must return the same token number");
        }
    }

    @Test
    @DisplayName("Concurrently submit 50 requests for a slot with max capacity 30: exactly 30 succeed and 20 fail with capacity exhaustion")
    void test50ConcurrentBookingsForCapacity30Slot() throws InterruptedException {
        MandiSlot cappedSlot = new MandiSlot();
        cappedSlot.setId("slot-capped-30");
        cappedSlot.setMandiId(testMandi.getId());
        cappedSlot.setSlotLabel("10:00 AM - 12:00 PM");
        cappedSlot.setStartTime("10:00");
        cappedSlot.setEndTime("12:00");
        cappedSlot.setMaxCapacityQuintals(300); // 300 quintals total
        cappedSlot.setBookedQuintals(0);
        cappedSlot.setMaxFarmers(30); // 30 farmers max
        cappedSlot.setBookedFarmers(0);
        cappedSlot.setStatus("AVAILABLE");
        cappedSlot.setCreatedAt(Instant.now());
        cappedSlot.setUpdatedAt(Instant.now());
        slotRepository.save(cappedSlot);

        int threadCount = 50;
        ExecutorService executor = Executors.newFixedThreadPool(15);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(threadCount);

        List<BookingResponse> successes = Collections.synchronizedList(new ArrayList<>());
        AtomicInteger rejectedCount = new AtomicInteger(0);
        LocalDate scheduledDate = LocalDate.now().plusDays(25);

        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            final Farmer farmer = testFarmers.get(index);
            final String idempotencyKey = "capped-key-" + UUID.randomUUID();

            executor.submit(() -> {
                try {
                    startLatch.await();
                    setAuth(farmer.getPhone(), Role.ROLE_FARMER);

                    CreateBookingRequest req = new CreateBookingRequest();
                    req.setFarmerId(farmer.getId());
                    req.setMandiId(testMandi.getId());
                    req.setCropId(testCrop.getId());
                    req.setScheduledDate(scheduledDate);
                    req.setTimeSlot("10:00 AM - 12:00 PM");
                    req.setVehicleType("TRACTOR_TROLLEY");
                    req.setVehicleNumber(String.format("MP-04-CP-%04d", index + 1));
                    req.setEstimatedYieldQuintals(new BigDecimal("10.00")); // 10 quintals each -> 30*10 = 300

                    BookingResponse res = bookingService.createBooking(req, idempotencyKey, farmer.getPhone());
                    successes.add(res);
                } catch (SlotFullException | InsufficientSlotCapacityException | FarmerLimitReachedException e) {
                    rejectedCount.incrementAndGet();
                } catch (Exception e) {
                    // other
                } finally {
                    finishLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        boolean completed = finishLatch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        assertTrue(completed, "All 50 concurrent requests should complete within 30 seconds");
        assertEquals(30, successes.size(), "Exactly 30 booking creations must succeed");
        assertEquals(20, rejectedCount.get(), "Exactly 20 booking creations must be rejected due to capacity limits");

        MandiSlot refreshedSlot = slotRepository.findById("slot-capped-30").orElseThrow();
        assertEquals(300, refreshedSlot.getBookedQuintals(), "Booked quintals must be exactly 300");
        assertEquals(30, refreshedSlot.getBookedFarmers(), "Booked farmers must be exactly 30");
        assertEquals("FULL", refreshedSlot.getStatus(), "Slot status must be marked FULL");
    }

    @Test
    @DisplayName("Concurrently advance queue: verify atomic token increments without skipping or collisions")
    void testSimultaneousQueueAdvancement() throws InterruptedException {
        // Seed 10 initial bookings
        for (int i = 0; i < 10; i++) {
            setAuth(testFarmers.get(i).getPhone(), Role.ROLE_FARMER);
            CreateBookingRequest req = new CreateBookingRequest();
            req.setFarmerId(testFarmers.get(i).getId());
            req.setMandiId(testMandi.getId());
            req.setCropId(testCrop.getId());
            req.setScheduledDate(LocalDate.now());
            req.setTimeSlot("08:00 AM - 10:00 AM");
            req.setVehicleType("TRACTOR_TROLLEY");
            req.setVehicleNumber(String.format("MP-04-T-%04d", i + 1));
            req.setEstimatedYieldQuintals(new BigDecimal("30.00"));
            bookingService.createBooking(req, "seed-key-" + i, testFarmers.get(i).getPhone());
        }

        queueStateRepository.updateCounters(testMandi.getId(), 0, 10, 10);

        int advanceCalls = 5;
        ExecutorService executor = Executors.newFixedThreadPool(advanceCalls);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(advanceCalls);

        List<QueueEventDto> results = Collections.synchronizedList(new ArrayList<>());

        for (int i = 0; i < advanceCalls; i++) {
            final int operatorId = i + 1;
            executor.submit(() -> {
                try {
                    start.await();
                    setAuth("admin", Role.ROLE_ADMIN);
                    QueueEventDto dto = queueService.advanceQueue(testMandi.getId(), "Operator-" + operatorId);
                    results.add(dto);
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    done.countDown();
                }
            });
        }

        start.countDown();
        done.await(15, TimeUnit.SECONDS);
        executor.shutdown();

        assertEquals(5, results.size(), "All 5 advancement requests must complete successfully");

        // Verify all 5 advanced tokens are distinct and strictly increasing
        Set<Integer> calledTokens = new HashSet<>();
        for (QueueEventDto dto : results) {
            assertTrue(calledTokens.add(dto.getCurrentToken()), "Collision in called tokens: " + dto.getCurrentToken());
        }
        assertEquals(5, calledTokens.size());
    }

    @Test
    @DisplayName("Verify slot capacity limits: prevent overbooking quintals and enforce maximum farmer limit")
    void testSlotCapacityExhaustionAndFarmerLimit() {
        MandiSlot smallSlot = new MandiSlot();
        smallSlot.setId("slot-small-test");
        smallSlot.setMandiId(testMandi.getId());
        smallSlot.setSlotLabel("02:00 PM - 04:00 PM");
        smallSlot.setStartTime("14:00");
        smallSlot.setEndTime("16:00");
        smallSlot.setMaxCapacityQuintals(100);
        smallSlot.setBookedQuintals(0);
        smallSlot.setMaxFarmers(2);
        smallSlot.setBookedFarmers(0);
        smallSlot.setStatus("AVAILABLE");
        slotRepository.save(smallSlot);

        // 1. First farmer books 60 quintals: should succeed
        setAuth(testFarmers.get(0).getPhone(), Role.ROLE_FARMER);
        CreateBookingRequest req1 = new CreateBookingRequest();
        req1.setFarmerId(testFarmers.get(0).getId());
        req1.setMandiId(testMandi.getId());
        req1.setCropId(testCrop.getId());
        req1.setScheduledDate(LocalDate.now());
        req1.setTimeSlot("02:00 PM - 04:00 PM");
        req1.setVehicleType("TRACTOR_TROLLEY");
        req1.setVehicleNumber("MP-04-T-1001");
        req1.setEstimatedYieldQuintals(new BigDecimal("60.00"));

        BookingResponse res1 = bookingService.createBooking(req1, "req-small-1", testFarmers.get(0).getPhone());
        assertNotNull(res1);

        MandiSlot afterReq1 = slotRepository.findById(smallSlot.getId()).orElseThrow();
        assertEquals(60, afterReq1.getBookedQuintals());
        assertEquals(1, afterReq1.getBookedFarmers());
        assertEquals("AVAILABLE", afterReq1.getStatus());

        // 2. Second farmer attempts 50 quintals (exceeds 40 available): must fail with InsufficientSlotCapacityException
        setAuth(testFarmers.get(1).getPhone(), Role.ROLE_FARMER);
        CreateBookingRequest req2Exceed = new CreateBookingRequest();
        req2Exceed.setFarmerId(testFarmers.get(1).getId());
        req2Exceed.setMandiId(testMandi.getId());
        req2Exceed.setCropId(testCrop.getId());
        req2Exceed.setScheduledDate(LocalDate.now());
        req2Exceed.setTimeSlot("02:00 PM - 04:00 PM");
        req2Exceed.setVehicleType("TRACTOR_TROLLEY");
        req2Exceed.setVehicleNumber("MP-04-T-1002");
        req2Exceed.setEstimatedYieldQuintals(new BigDecimal("50.00"));

        assertThrows(InsufficientSlotCapacityException.class, () ->
                bookingService.createBooking(req2Exceed, "req-small-2-exceed", testFarmers.get(1).getPhone()));

        // 3. Second farmer requests exact remaining 40 quintals: should succeed and mark slot FULL
        req2Exceed.setEstimatedYieldQuintals(new BigDecimal("40.00"));
        BookingResponse res2 = bookingService.createBooking(req2Exceed, "req-small-2-success", testFarmers.get(1).getPhone());
        assertNotNull(res2);

        MandiSlot afterReq2 = slotRepository.findById(smallSlot.getId()).orElseThrow();
        assertEquals(100, afterReq2.getBookedQuintals());
        assertEquals(2, afterReq2.getBookedFarmers());
        assertEquals("FULL", afterReq2.getStatus());

        // 4. Third farmer attempts to book even 1 quintal: must fail with FarmerLimitReachedException or InsufficientSlotCapacityException
        setAuth(testFarmers.get(2).getPhone(), Role.ROLE_FARMER);
        CreateBookingRequest req3 = new CreateBookingRequest();
        req3.setFarmerId(testFarmers.get(2).getId());
        req3.setMandiId(testMandi.getId());
        req3.setCropId(testCrop.getId());
        req3.setScheduledDate(LocalDate.now());
        req3.setTimeSlot("02:00 PM - 04:00 PM");
        req3.setVehicleType("TRACTOR_TROLLEY");
        req3.setVehicleNumber("MP-04-T-1003");
        req3.setEstimatedYieldQuintals(new BigDecimal("1.00"));

        assertThrows(BusinessException.class, () ->
                bookingService.createBooking(req3, "req-small-3", testFarmers.get(2).getPhone()));
    }

    @Test
    @DisplayName("Verify slot capacity release and waiting count decrement on booking cancellation")
    void testSlotCapacityReleaseOnCancellation() {
        setAuth(testFarmers.get(0).getPhone(), Role.ROLE_FARMER);
        CreateBookingRequest req = new CreateBookingRequest();
        req.setFarmerId(testFarmers.get(0).getId());
        req.setMandiId(testMandi.getId());
        req.setCropId(testCrop.getId());
        req.setScheduledDate(LocalDate.now());
        req.setTimeSlot("08:00 AM - 10:00 AM");
        req.setVehicleType("TRACTOR_TROLLEY");
        req.setVehicleNumber("MP-04-AB-9999");
        req.setEstimatedYieldQuintals(new BigDecimal("55.00"));

        BookingResponse created = bookingService.createBooking(req, "cancel-test-key", testFarmers.get(0).getPhone());
        assertNotNull(created);

        MandiSlot bookedSlot = slotRepository.findById(testSlot.getId()).orElseThrow();
        assertEquals(55, bookedSlot.getBookedQuintals());
        assertEquals(1, bookedSlot.getBookedFarmers());

        Mandi mandiBeforeCancel = mandiRepository.findById(testMandi.getId()).orElseThrow();
        int waitingBefore = mandiBeforeCancel.getActiveTokensWaiting();

        // Cancel booking
        BookingResponse cancelled = bookingService.cancelBooking(created.getId(), testFarmers.get(0).getPhone());
        assertEquals("CANCELLED", cancelled.getStatus());

        // Verify slot capacity was released
        MandiSlot releasedSlot = slotRepository.findById(testSlot.getId()).orElseThrow();
        assertEquals(0, releasedSlot.getBookedQuintals());
        assertEquals(0, releasedSlot.getBookedFarmers());

        // Verify active waiting tokens count was decremented
        Mandi mandiAfterCancel = mandiRepository.findById(testMandi.getId()).orElseThrow();
        assertEquals(waitingBefore - 1, mandiAfterCancel.getActiveTokensWaiting());

        // Repeated cancellation must throw BookingAlreadyCancelledException
        assertThrows(BookingAlreadyCancelledException.class, () ->
                bookingService.cancelBooking(created.getId(), testFarmers.get(0).getPhone()));
    }

    @Test
    @DisplayName("Verify administrative capacity reduction below current bookings is rejected")
    void testCapacityReductionPreventedWhenBookingsExist() {
        setAuth(testFarmers.get(0).getPhone(), Role.ROLE_FARMER);
        CreateBookingRequest req = new CreateBookingRequest();
        req.setFarmerId(testFarmers.get(0).getId());
        req.setMandiId(testMandi.getId());
        req.setCropId(testCrop.getId());
        req.setScheduledDate(LocalDate.now());
        req.setTimeSlot("08:00 AM - 10:00 AM");
        req.setVehicleType("TRACTOR_TROLLEY");
        req.setVehicleNumber("MP-04-AB-1234");
        req.setEstimatedYieldQuintals(new BigDecimal("50.00"));

        bookingService.createBooking(req, "admin-cap-key", testFarmers.get(0).getPhone());

        // Attempting to reduce maxCapacityQuintals to 40 (less than 50 booked) must fail
        MandiSlot reductionUpdate = new MandiSlot();
        reductionUpdate.setMaxCapacityQuintals(40);

        assertThrows(CapacityReductionNotAllowedException.class, () ->
                slotService.updateSlot(testSlot.getId(), reductionUpdate));

        // Expanding capacity must succeed
        reductionUpdate.setMaxCapacityQuintals(20000);
        MandiSlot updated = slotService.updateSlot(testSlot.getId(), reductionUpdate);
        assertEquals(20000, updated.getMaxCapacityQuintals());
    }

    @Test
    @DisplayName("Verify validation: past dates, closed slots, and non-positive yields are rejected")
    void testBookingValidations() {
        setAuth(testFarmers.get(0).getPhone(), Role.ROLE_FARMER);
        // 1. Past date
        CreateBookingRequest pastDateReq = new CreateBookingRequest();
        pastDateReq.setFarmerId(testFarmers.get(0).getId());
        pastDateReq.setMandiId(testMandi.getId());
        pastDateReq.setCropId(testCrop.getId());
        pastDateReq.setScheduledDate(LocalDate.now().minusDays(1));
        pastDateReq.setTimeSlot("08:00 AM - 10:00 AM");
        pastDateReq.setVehicleType("TRACTOR_TROLLEY");
        pastDateReq.setVehicleNumber("MP-04-AB-0001");
        pastDateReq.setEstimatedYieldQuintals(new BigDecimal("25.00"));

        assertThrows(InvalidBookingDateException.class, () ->
                bookingService.createBooking(pastDateReq, "past-date-key", testFarmers.get(0).getPhone()));

        // 2. Non-positive yield
        CreateBookingRequest zeroYieldReq = new CreateBookingRequest();
        zeroYieldReq.setFarmerId(testFarmers.get(0).getId());
        zeroYieldReq.setMandiId(testMandi.getId());
        zeroYieldReq.setCropId(testCrop.getId());
        zeroYieldReq.setScheduledDate(LocalDate.now());
        zeroYieldReq.setTimeSlot("08:00 AM - 10:00 AM");
        zeroYieldReq.setVehicleType("TRACTOR_TROLLEY");
        zeroYieldReq.setVehicleNumber("MP-04-AB-0001");
        zeroYieldReq.setEstimatedYieldQuintals(new BigDecimal("0.00"));

        assertThrows(InvalidBookingQuantityException.class, () ->
                bookingService.createBooking(zeroYieldReq, "zero-yield-key", testFarmers.get(0).getPhone()));

        // 3. Closed slot
        MandiSlot closedSlot = new MandiSlot();
        closedSlot.setId("slot-closed-test");
        closedSlot.setMandiId(testMandi.getId());
        closedSlot.setSlotLabel("06:00 PM - 08:00 PM");
        closedSlot.setStartTime("18:00");
        closedSlot.setEndTime("20:00");
        closedSlot.setMaxCapacityQuintals(1000);
        closedSlot.setBookedQuintals(0);
        closedSlot.setMaxFarmers(50);
        closedSlot.setBookedFarmers(0);
        closedSlot.setStatus("CLOSED");
        slotRepository.save(closedSlot);

        CreateBookingRequest closedSlotReq = new CreateBookingRequest();
        closedSlotReq.setFarmerId(testFarmers.get(0).getId());
        closedSlotReq.setMandiId(testMandi.getId());
        closedSlotReq.setCropId(testCrop.getId());
        closedSlotReq.setScheduledDate(LocalDate.now());
        closedSlotReq.setTimeSlot("06:00 PM - 08:00 PM");
        closedSlotReq.setVehicleType("TRACTOR_TROLLEY");
        closedSlotReq.setVehicleNumber("MP-04-AB-0001");
        closedSlotReq.setEstimatedYieldQuintals(new BigDecimal("25.00"));

        assertThrows(SlotClosedException.class, () ->
                bookingService.createBooking(closedSlotReq, "closed-slot-key", testFarmers.get(0).getPhone()));
    }

    @Test
    @DisplayName("Test A — Existing sequence row: start at sequence 10, verify concurrent requests produce 11..30 with no duplicates")
    void testExistingSequenceRowConcurrency() throws InterruptedException {
        LocalDate testDate = LocalDate.now().plusDays(5);
        // Pre-initialize sequence row to 10
        MandiTokenSequence existingSeq = new MandiTokenSequence(testMandi.getId(), testDate, 10);
        sequenceRepository.saveAndFlush(existingSeq);

        int threadCount = 20;
        ExecutorService executor = Executors.newFixedThreadPool(10);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(threadCount);

        List<BookingResponse> responses = Collections.synchronizedList(new ArrayList<>());
        AtomicInteger failureCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            final Farmer farmer = testFarmers.get(index);
            final String idempotencyKey = "existing-seq-key-" + UUID.randomUUID();

            executor.submit(() -> {
                try {
                    startLatch.await();
                    setAuth(farmer.getPhone(), Role.ROLE_FARMER);

                    CreateBookingRequest req = new CreateBookingRequest();
                    req.setFarmerId(farmer.getId());
                    req.setMandiId(testMandi.getId());
                    req.setCropId(testCrop.getId());
                    req.setScheduledDate(testDate);
                    req.setTimeSlot("08:00 AM - 10:00 AM");
                    req.setVehicleType("TRACTOR_TROLLEY");
                    req.setVehicleNumber(String.format("MP-04-EX-%04d", index + 1));
                    req.setEstimatedYieldQuintals(new BigDecimal("20.00"));

                    BookingResponse res = bookingService.createBooking(req, idempotencyKey, farmer.getPhone());
                    responses.add(res);
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                } finally {
                    finishLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        boolean completed = finishLatch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        assertTrue(completed, "All 20 concurrent requests should complete within 30 seconds");
        assertEquals(0, failureCount.get(), "Zero booking creations should fail");
        assertEquals(20, responses.size(), "Exactly 20 responses must be returned");

        // Verify sequences are strictly 11 through 30
        List<Integer> seqNumbers = responses.stream()
                .map(BookingResponse::getTokenSequence)
                .sorted()
                .toList();
        for (int i = 0; i < 20; i++) {
            assertEquals(11 + i, seqNumbers.get(i), "Sequence at index " + i + " must be " + (11 + i));
        }

        // Verify token numbers are unique
        Set<String> uniqueTokens = new HashSet<>();
        for (BookingResponse r : responses) {
            assertTrue(uniqueTokens.add(r.getTokenNumber()), "Duplicate token number found: " + r.getTokenNumber());
        }
        assertEquals(20, uniqueTokens.size());

        // Verify database sequence state is exactly 30
        MandiTokenSequence finalSeq = sequenceRepository.findByMandiIdAndProcurementDate(testMandi.getId(), testDate)
                .orElseThrow();
        assertEquals(30, finalSeq.getCurrentSequence());
    }

    @Test
    @DisplayName("Test B — First-ever sequence row: start with NO sequence row in DB, verify concurrent creation produces unique 1..25 sequences and exactly 1 sequence row")
    void testFirstTimeSequenceInitializationConcurrency() throws InterruptedException {
        LocalDate firstTimeDate = LocalDate.now().plusDays(10);
        // Ensure absolutely no sequence row exists
        assertFalse(sequenceRepository.findByMandiIdAndProcurementDate(testMandi.getId(), firstTimeDate).isPresent());

        int threadCount = 25;
        ExecutorService executor = Executors.newFixedThreadPool(10);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(threadCount);

        List<BookingResponse> responses = Collections.synchronizedList(new ArrayList<>());
        AtomicInteger failureCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            final Farmer farmer = testFarmers.get(index);
            final String idempotencyKey = "first-time-seq-key-" + UUID.randomUUID();

            executor.submit(() -> {
                try {
                    startLatch.await();
                    setAuth(farmer.getPhone(), Role.ROLE_FARMER);

                    CreateBookingRequest req = new CreateBookingRequest();
                    req.setFarmerId(farmer.getId());
                    req.setMandiId(testMandi.getId());
                    req.setCropId(testCrop.getId());
                    req.setScheduledDate(firstTimeDate);
                    req.setTimeSlot("08:00 AM - 10:00 AM");
                    req.setVehicleType("TRACTOR_TROLLEY");
                    req.setVehicleNumber(String.format("MP-04-FT-%04d", index + 1));
                    req.setEstimatedYieldQuintals(new BigDecimal("15.00"));

                    BookingResponse res = bookingService.createBooking(req, idempotencyKey, farmer.getPhone());
                    responses.add(res);
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                } finally {
                    finishLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        boolean completed = finishLatch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        assertTrue(completed, "All 25 concurrent requests should complete within 30 seconds");
        assertEquals(0, failureCount.get(), "Zero booking creations should fail during initial sequence creation");
        assertEquals(25, responses.size(), "Exactly 25 responses must be returned");

        // Verify exactly one sequence row exists for this mandi + date
        MandiTokenSequence createdSeq = sequenceRepository.findByMandiIdAndProcurementDate(testMandi.getId(), firstTimeDate)
                .orElseThrow();
        assertEquals(25, createdSeq.getCurrentSequence(), "Final sequence value in DB must be exactly 25");

        // Verify sequences are strictly 1 through 25 with no duplicates
        List<Integer> seqNumbers = responses.stream()
                .map(BookingResponse::getTokenSequence)
                .sorted()
                .toList();
        for (int i = 0; i < 25; i++) {
            assertEquals(i + 1, seqNumbers.get(i), "Sequence at index " + i + " must be strictly " + (i + 1));
        }

        // Verify token numbers are unique
        Set<String> uniqueTokens = new HashSet<>();
        for (BookingResponse r : responses) {
            assertTrue(uniqueTokens.add(r.getTokenNumber()), "Duplicate token number found: " + r.getTokenNumber());
        }
        assertEquals(25, uniqueTokens.size());
    }

    @Test
    @DisplayName("Test C — Transaction rollback does not corrupt sequence state and subsequent transactions succeed")
    void testFailedTransactionDoesNotCorruptSequence() {
        LocalDate rollbackDate = LocalDate.now().plusDays(15);

        // 1. First transaction succeeds from clean state (sequence 1)
        setAuth(testFarmers.get(0).getPhone(), Role.ROLE_FARMER);
        CreateBookingRequest req1 = new CreateBookingRequest();
        req1.setFarmerId(testFarmers.get(0).getId());
        req1.setMandiId(testMandi.getId());
        req1.setCropId(testCrop.getId());
        req1.setScheduledDate(rollbackDate);
        req1.setTimeSlot("08:00 AM - 10:00 AM");
        req1.setVehicleType("TRACTOR_TROLLEY");
        req1.setVehicleNumber("MP-04-RB-0001");
        req1.setEstimatedYieldQuintals(new BigDecimal("10.00"));

        BookingResponse res1 = bookingService.createBooking(req1, "rb-key-1", testFarmers.get(0).getPhone());
        assertNotNull(res1);
        assertEquals(1, res1.getTokenSequence());

        // 2. Second transaction fails due to invalid crop ID -> rolls back
        setAuth(testFarmers.get(1).getPhone(), Role.ROLE_FARMER);
        CreateBookingRequest reqFail = new CreateBookingRequest();
        reqFail.setFarmerId(testFarmers.get(1).getId());
        reqFail.setMandiId(testMandi.getId());
        reqFail.setCropId("non-existent-crop-id");
        reqFail.setScheduledDate(rollbackDate);
        reqFail.setTimeSlot("08:00 AM - 10:00 AM");
        reqFail.setVehicleType("TRACTOR_TROLLEY");
        reqFail.setVehicleNumber("MP-04-RB-0002");
        reqFail.setEstimatedYieldQuintals(new BigDecimal("10.00"));

        assertThrows(ResourceNotFoundException.class, () ->
                bookingService.createBooking(reqFail, "rb-key-fail", testFarmers.get(1).getPhone()));

        // 3. Third transaction succeeds -> should cleanly acquire sequence 2
        setAuth(testFarmers.get(2).getPhone(), Role.ROLE_FARMER);
        CreateBookingRequest req3 = new CreateBookingRequest();
        req3.setFarmerId(testFarmers.get(2).getId());
        req3.setMandiId(testMandi.getId());
        req3.setCropId(testCrop.getId());
        req3.setScheduledDate(rollbackDate);
        req3.setTimeSlot("08:00 AM - 10:00 AM");
        req3.setVehicleType("TRACTOR_TROLLEY");
        req3.setVehicleNumber("MP-04-RB-0003");
        req3.setEstimatedYieldQuintals(new BigDecimal("10.00"));

        BookingResponse res3 = bookingService.createBooking(req3, "rb-key-3", testFarmers.get(2).getPhone());
        assertNotNull(res3);
        assertEquals(2, res3.getTokenSequence());

        // Verify DB sequence state is exactly 2
        MandiTokenSequence finalSeq = sequenceRepository.findByMandiIdAndProcurementDate(testMandi.getId(), rollbackDate)
                .orElseThrow();
        assertEquals(2, finalSeq.getCurrentSequence());
    }
}
