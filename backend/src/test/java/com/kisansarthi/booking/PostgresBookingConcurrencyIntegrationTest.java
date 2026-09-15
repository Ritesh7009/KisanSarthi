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

/**
 * Authoritative PostgreSQL Concurrency Integration Tests.
 * Directly executes against local PostgreSQL 15 database instance (kisansarthi_pg_test).
 */
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5432}/${DB_NAME:kisansarthi_pg_test}",
        "spring.datasource.driver-class-name=org.postgresql.Driver",
        "spring.datasource.username=${DB_USER:postgres}",
        "spring.datasource.password=${DB_PASSWORD:}",
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.flyway.enabled=true",
        "spring.flyway.baseline-on-migrate=true",
        "spring.flyway.locations=classpath:db/migration"
})
public class PostgresBookingConcurrencyIntegrationTest {

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
    private com.kisansarthi.queue.QueueStateRepository queueStateRepository;

    @Autowired
    private com.kisansarthi.queue.QueueEventRepository queueEventRepository;

    @Autowired
    private com.kisansarthi.weighment.WeighmentRepository weighmentRepository;

    @Autowired
    private com.kisansarthi.payment.PaymentRepository paymentRepository;

    private Mandi mandiSehore;
    private Mandi mandiBhopal;
    private Crop testCrop;
    private List<Farmer> testFarmers;
    private MandiSlot testSlotSehore;
    private MandiSlot testSlotBhopal;

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
        paymentRepository.deleteAllInBatch();
        weighmentRepository.deleteAllInBatch();
        queueEventRepository.deleteAllInBatch();

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

        // 1. Mandi Sehore
        if (!mandiRepository.existsById("mandi-pg-sehore")) {
            Mandi m = new Mandi();
            m.setId("mandi-pg-sehore");
            m.setName("Sehore PG APMC Mandi");
            m.setHindiName("सीहोर पीजी मंडी");
            m.setDistrict("Sehore");
            m.setHindiDistrict("सीहोर");
            m.setAddress("Mandi Complex, Sehore, MP");
            m.setPinCode("466001");
            m.setPhone("07562224411");
            m.setCurrentTokenServing(0);
            m.setTotalTokensToday(0);
            m.setActiveTokensWaiting(0);
            mandiSehore = mandiRepository.save(m);
        } else {
            mandiRepository.resetCounters("mandi-pg-sehore");
            mandiSehore = mandiRepository.findById("mandi-pg-sehore").orElseThrow();
        }

        // 2. Mandi Bhopal
        if (!mandiRepository.existsById("mandi-pg-bhopal")) {
            Mandi m = new Mandi();
            m.setId("mandi-pg-bhopal");
            m.setName("Bhopal PG APMC Mandi");
            m.setHindiName("भोपाल पीजी मंडी");
            m.setDistrict("Bhopal");
            m.setHindiDistrict("भोपाल");
            m.setAddress("Karond Mandi, Bhopal, MP");
            m.setPinCode("462038");
            m.setPhone("07552741122");
            m.setCurrentTokenServing(0);
            m.setTotalTokensToday(0);
            m.setActiveTokensWaiting(0);
            mandiBhopal = mandiRepository.save(m);
        } else {
            mandiRepository.resetCounters("mandi-pg-bhopal");
            mandiBhopal = mandiRepository.findById("mandi-pg-bhopal").orElseThrow();
        }

        // Admin User
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

        // Test Crop
        testCrop = cropRepository.findById("crop-pg-wheat").orElseGet(() -> {
            Crop c = new Crop();
            c.setId("crop-pg-wheat");
            c.setName("Wheat Sharbati PG");
            c.setHindiName("गेहूँ शरबती पीजी");
            c.setSeason("RABI");
            c.setStandardMspPerQuintal(new BigDecimal("2275.00"));
            c.setMpBonusPerQuintal(new BigDecimal("150.00"));
            c.setTotalMsp(new BigDecimal("2425.00"));
            c.setMarketPricePerQuintal(new BigDecimal("2300.00"));
            return cropRepository.save(c);
        });

        // Slots
        testSlotSehore = new MandiSlot();
        testSlotSehore.setId("slot-pg-sehore-0810");
        testSlotSehore.setMandiId(mandiSehore.getId());
        testSlotSehore.setSlotLabel("08:00 AM - 10:00 AM");
        testSlotSehore.setStartTime("08:00");
        testSlotSehore.setEndTime("10:00");
        testSlotSehore.setMaxCapacityQuintals(10000);
        testSlotSehore.setBookedQuintals(0);
        testSlotSehore.setMaxFarmers(200);
        testSlotSehore.setBookedFarmers(0);
        testSlotSehore.setStatus("AVAILABLE");
        testSlotSehore.setCreatedAt(Instant.now());
        testSlotSehore.setUpdatedAt(Instant.now());
        testSlotSehore = slotRepository.save(testSlotSehore);

        testSlotBhopal = new MandiSlot();
        testSlotBhopal.setId("slot-pg-bhopal-0810");
        testSlotBhopal.setMandiId(mandiBhopal.getId());
        testSlotBhopal.setSlotLabel("08:00 AM - 10:00 AM");
        testSlotBhopal.setStartTime("08:00");
        testSlotBhopal.setEndTime("10:00");
        testSlotBhopal.setMaxCapacityQuintals(10000);
        testSlotBhopal.setBookedQuintals(0);
        testSlotBhopal.setMaxFarmers(200);
        testSlotBhopal.setBookedFarmers(0);
        testSlotBhopal.setStatus("AVAILABLE");
        testSlotBhopal.setCreatedAt(Instant.now());
        testSlotBhopal.setUpdatedAt(Instant.now());
        testSlotBhopal = slotRepository.save(testSlotBhopal);

        // Pool of 100 Farmers
        testFarmers = new ArrayList<>();
        for (int i = 1; i <= 100; i++) {
            String phone = String.format("98760%05d", i);
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
                newFarmer.setKisanId("KISAN-PG-" + phone);
                newFarmer.setName("PG Farmer " + phone);
                newFarmer.setPhone(phone);
                newFarmer.setMaskedAadhar("XXXX-XXXX-" + phone.substring(phone.length() - 4));
                newFarmer.setDistrict("Sehore");
                newFarmer.setVillage("Shampur");
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

    /**
     * Requirement 4.A: Same Idempotency Key under PostgreSQL Concurrency
     * 20 concurrent threads submit booking with the exact same idempotency key.
     */
    @Test
    @DisplayName("PostgreSQL Concurrency 4.A: 20 concurrent requests with identical idempotency key -> exactly 1 created")
    void testPostgres20ConcurrentRequestsSameIdempotencyKey() throws InterruptedException {
        int threadCount = 20;
        ExecutorService executor = Executors.newFixedThreadPool(10);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(threadCount);

        List<BookingResponse> responses = Collections.synchronizedList(new ArrayList<>());
        AtomicInteger failureCount = new AtomicInteger(0);

        Farmer farmer = testFarmers.get(0);
        String sharedIdempotencyKey = "pg-idemp-key-" + UUID.randomUUID();
        LocalDate scheduledDate = LocalDate.now().plusDays(10);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    setAuth(farmer.getPhone(), Role.ROLE_FARMER);

                    CreateBookingRequest req = new CreateBookingRequest();
                    req.setFarmerId(farmer.getId());
                    req.setMandiId(mandiSehore.getId());
                    req.setCropId(testCrop.getId());
                    req.setScheduledDate(scheduledDate);
                    req.setTimeSlot("08:00 AM - 10:00 AM");
                    req.setVehicleType("TRACTOR_TROLLEY");
                    req.setVehicleNumber("MP-04-PG-1111");
                    req.setEstimatedYieldQuintals(new BigDecimal("25.00"));

                    BookingResponse res = bookingService.createBooking(req, sharedIdempotencyKey, farmer.getPhone());
                    if (res != null) {
                        responses.add(res);
                    }
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

        assertTrue(completed, "All 20 concurrent requests should complete within 30s");
        assertEquals(0, failureCount.get(), "Zero requests should fail under idempotent race");
        assertEquals(20, responses.size(), "All 20 callers must receive a valid response");

        // Exactly 1 booking in database
        List<Booking> bookingsInDb = bookingRepository.findAll().stream()
                .filter(b -> sharedIdempotencyKey.equals(b.getIdempotencyKey()))
                .toList();
        assertEquals(1, bookingsInDb.size(), "Exactly 1 booking record must exist in PostgreSQL for shared idempotency key");

        // All 20 responses return the exact same token number and ID
        UUID winnerId = bookingsInDb.get(0).getId();
        String winnerToken = bookingsInDb.get(0).getTokenNumber();
        for (BookingResponse r : responses) {
            assertEquals(winnerId, r.getId(), "All callers must receive the winner booking ID");
            assertEquals(winnerToken, r.getTokenNumber(), "All callers must receive the winner token number");
        }

        // Mandi counters incremented by exactly 1
        Mandi refreshedMandi = mandiRepository.findById(mandiSehore.getId()).orElseThrow();
        assertEquals(1, refreshedMandi.getTotalTokensToday(), "Mandi totalTokensToday must be exactly 1");
        assertEquals(1, refreshedMandi.getActiveTokensWaiting(), "Mandi activeTokensWaiting must be exactly 1");

        // Slot booked count is exactly 1 (25 Qtl)
        MandiSlot slot = slotRepository.findById(testSlotSehore.getId()).orElseThrow();
        assertEquals(25, slot.getBookedQuintals(), "Slot bookedQuintals must be exactly 25");
        assertEquals(1, slot.getBookedFarmers(), "Slot bookedFarmers must be exactly 1");
    }

    /**
     * Requirement 4.B: 100 Simultaneous Distinct Bookings under PostgreSQL Concurrency
     */
    @Test
    @DisplayName("PostgreSQL Concurrency 4.B: 100 simultaneous distinct bookings -> 100 unique tokens, gapless sequence")
    void testPostgres100SimultaneousDistinctBookings() throws InterruptedException {
        int threadCount = 100;
        ExecutorService executor = Executors.newFixedThreadPool(25);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(threadCount);

        List<BookingResponse> responses = Collections.synchronizedList(new ArrayList<>());
        AtomicInteger failureCount = new AtomicInteger(0);

        LocalDate scheduledDate = LocalDate.now().plusDays(15);

        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            final Farmer farmer = testFarmers.get(index);
            final String idempotencyKey = "pg-distinct-key-" + UUID.randomUUID();

            executor.submit(() -> {
                try {
                    startLatch.await();
                    setAuth(farmer.getPhone(), Role.ROLE_FARMER);

                    CreateBookingRequest req = new CreateBookingRequest();
                    req.setFarmerId(farmer.getId());
                    req.setMandiId(mandiSehore.getId());
                    req.setCropId(testCrop.getId());
                    req.setScheduledDate(scheduledDate);
                    req.setTimeSlot("08:00 AM - 10:00 AM");
                    req.setVehicleType("TRACTOR_TROLLEY");
                    req.setVehicleNumber(String.format("MP-04-PG-%04d", index + 1));
                    req.setEstimatedYieldQuintals(new BigDecimal("35.00"));

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
        boolean completed = finishLatch.await(45, TimeUnit.SECONDS);
        executor.shutdown();

        assertTrue(completed, "All 100 requests must complete within 45s");
        assertEquals(0, failureCount.get(), "Zero requests should fail");
        assertEquals(100, responses.size(), "Exactly 100 responses must be returned");

        // Unique tokens check
        Set<String> uniqueTokens = new HashSet<>();
        for (BookingResponse r : responses) {
            assertTrue(uniqueTokens.add(r.getTokenNumber()), "Duplicate token number detected: " + r.getTokenNumber());
        }
        assertEquals(100, uniqueTokens.size(), "All 100 token numbers must be unique");

        // Monotonic gapless sequence 1 to 100
        List<Integer> sequences = responses.stream()
                .map(BookingResponse::getTokenSequence)
                .sorted()
                .toList();
        for (int i = 0; i < 100; i++) {
            assertEquals(i + 1, sequences.get(i), "Sequence at index " + i + " must be " + (i + 1));
        }

        // Database count check
        long countInDb = bookingRepository.count();
        assertEquals(100, countInDb, "Exactly 100 bookings persisted in PostgreSQL");

        // Mandi counters check
        Mandi refreshedMandi = mandiRepository.findById(mandiSehore.getId()).orElseThrow();
        assertEquals(100, refreshedMandi.getTotalTokensToday());
        assertEquals(100, refreshedMandi.getActiveTokensWaiting());

        // Slot capacity check: 100 * 35 = 3500 Qtl
        MandiSlot slot = slotRepository.findById(testSlotSehore.getId()).orElseThrow();
        assertEquals(3500, slot.getBookedQuintals());
        assertEquals(100, slot.getBookedFarmers());
    }

    /**
     * Requirement 4.C: Capacity Enforcement under PostgreSQL Concurrency
     * Capacity = 30 (300 Quintals). 50 concurrent requests for 10 Qtl each. Exactly 30 succeed, 20 rejected.
     */
    @Test
    @DisplayName("PostgreSQL Concurrency 4.C: Capacity enforcement race (max 30) -> 30 accepted, 20 rejected")
    void testPostgresCapacityEnforcementRace() throws InterruptedException {
        MandiSlot cappedSlot = new MandiSlot();
        cappedSlot.setId("slot-pg-capped-30");
        cappedSlot.setMandiId(mandiSehore.getId());
        cappedSlot.setSlotLabel("11:00 AM - 01:00 PM");
        cappedSlot.setStartTime("11:00");
        cappedSlot.setEndTime("13:00");
        cappedSlot.setMaxCapacityQuintals(300); // 300 Qtl
        cappedSlot.setBookedQuintals(0);
        cappedSlot.setMaxFarmers(30); // 30 farmers
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
        LocalDate scheduledDate = LocalDate.now().plusDays(18);

        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            final Farmer farmer = testFarmers.get(index);
            final String idempotencyKey = "pg-cap-key-" + UUID.randomUUID();

            executor.submit(() -> {
                try {
                    startLatch.await();
                    setAuth(farmer.getPhone(), Role.ROLE_FARMER);

                    CreateBookingRequest req = new CreateBookingRequest();
                    req.setFarmerId(farmer.getId());
                    req.setMandiId(mandiSehore.getId());
                    req.setCropId(testCrop.getId());
                    req.setScheduledDate(scheduledDate);
                    req.setTimeSlot("11:00 AM - 01:00 PM");
                    req.setVehicleType("TRACTOR_TROLLEY");
                    req.setVehicleNumber(String.format("MP-04-CAP-%04d", index + 1));
                    req.setEstimatedYieldQuintals(new BigDecimal("10.00")); // 10 Qtl

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

        assertTrue(completed, "All 50 requests must finish within 30s");
        assertEquals(30, successes.size(), "Exactly 30 requests must succeed");
        assertEquals(20, rejectedCount.get(), "Exactly 20 requests must be rejected due to capacity limits");

        MandiSlot refreshedSlot = slotRepository.findById("slot-pg-capped-30").orElseThrow();
        assertEquals(300, refreshedSlot.getBookedQuintals(), "Booked quintals must be exactly 300");
        assertEquals(30, refreshedSlot.getBookedFarmers(), "Booked farmers must be exactly 30");
        assertEquals("FULL", refreshedSlot.getStatus(), "Slot status must transition to FULL");
    }

    /**
     * Requirement 4.D: Multiple Mandis Concurrency Isolation
     * 25 concurrent requests to Mandi Sehore + 25 concurrent requests to Mandi Bhopal (50 total).
     * Verify complete isolation between mandis.
     */
    @Test
    @DisplayName("PostgreSQL Concurrency 4.D: Multi-mandi concurrency isolation (25 on Mandi A, 25 on Mandi B)")
    void testPostgresMultiMandiConcurrencyIsolation() throws InterruptedException {
        int countPerMandi = 25;
        int totalThreads = countPerMandi * 2;
        ExecutorService executor = Executors.newFixedThreadPool(20);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(totalThreads);

        List<BookingResponse> sehoreResponses = Collections.synchronizedList(new ArrayList<>());
        List<BookingResponse> bhopalResponses = Collections.synchronizedList(new ArrayList<>());
        AtomicInteger failures = new AtomicInteger(0);
        LocalDate scheduledDate = LocalDate.now().plusDays(22);

        for (int i = 0; i < countPerMandi; i++) {
            final int index = i;
            final Farmer farmer1 = testFarmers.get(index);
            final Farmer farmer2 = testFarmers.get(index + 50);

            // Mandi Sehore thread
            executor.submit(() -> {
                try {
                    startLatch.await();
                    setAuth(farmer1.getPhone(), Role.ROLE_FARMER);

                    CreateBookingRequest req = new CreateBookingRequest();
                    req.setFarmerId(farmer1.getId());
                    req.setMandiId(mandiSehore.getId());
                    req.setCropId(testCrop.getId());
                    req.setScheduledDate(scheduledDate);
                    req.setTimeSlot("08:00 AM - 10:00 AM");
                    req.setVehicleType("TRACTOR_TROLLEY");
                    req.setVehicleNumber(String.format("MP-04-SEH-%04d", index + 1));
                    req.setEstimatedYieldQuintals(new BigDecimal("20.00"));

                    BookingResponse res = bookingService.createBooking(req, "pg-seh-" + UUID.randomUUID(), farmer1.getPhone());
                    sehoreResponses.add(res);
                } catch (Exception e) {
                    failures.incrementAndGet();
                } finally {
                    finishLatch.countDown();
                }
            });

            // Mandi Bhopal thread
            executor.submit(() -> {
                try {
                    startLatch.await();
                    setAuth(farmer2.getPhone(), Role.ROLE_FARMER);

                    CreateBookingRequest req = new CreateBookingRequest();
                    req.setFarmerId(farmer2.getId());
                    req.setMandiId(mandiBhopal.getId());
                    req.setCropId(testCrop.getId());
                    req.setScheduledDate(scheduledDate);
                    req.setTimeSlot("08:00 AM - 10:00 AM");
                    req.setVehicleType("PICKUP_TRUCK");
                    req.setVehicleNumber(String.format("MP-04-BHP-%04d", index + 1));
                    req.setEstimatedYieldQuintals(new BigDecimal("20.00"));

                    BookingResponse res = bookingService.createBooking(req, "pg-bhp-" + UUID.randomUUID(), farmer2.getPhone());
                    bhopalResponses.add(res);
                } catch (Exception e) {
                    failures.incrementAndGet();
                } finally {
                    finishLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        boolean completed = finishLatch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        assertTrue(completed);
        assertEquals(0, failures.get());
        assertEquals(25, sehoreResponses.size(), "Mandi Sehore must process exactly 25 bookings");
        assertEquals(25, bhopalResponses.size(), "Mandi Bhopal must process exactly 25 bookings");

        // Verify prefixes
        assertTrue(sehoreResponses.stream().allMatch(r -> r.getTokenNumber().startsWith("MP-SEH-")));
        assertTrue(bhopalResponses.stream().allMatch(r -> r.getTokenNumber().startsWith("MP-BHO-") || r.getTokenNumber().startsWith("MP-BHP-")));

        // Verify sequences 1..25 for each mandi
        List<Integer> sehoreSeqs = sehoreResponses.stream().map(BookingResponse::getTokenSequence).sorted().toList();
        List<Integer> bhopalSeqs = bhopalResponses.stream().map(BookingResponse::getTokenSequence).sorted().toList();
        for (int i = 0; i < 25; i++) {
            assertEquals(i + 1, sehoreSeqs.get(i));
            assertEquals(i + 1, bhopalSeqs.get(i));
        }

        // Verify Mandi counters in PostgreSQL
        Mandi refSehore = mandiRepository.findById(mandiSehore.getId()).orElseThrow();
        Mandi refBhopal = mandiRepository.findById(mandiBhopal.getId()).orElseThrow();
        assertEquals(25, refSehore.getTotalTokensToday());
        assertEquals(25, refBhopal.getTotalTokensToday());
    }

    /**
     * Requirement 4.E: Transaction Rollback Integrity under PostgreSQL
     * Tests that validation failures or errors rollback slot capacity and sequence without leaving partial state.
     */
    @Test
    @DisplayName("PostgreSQL Concurrency 4.E: Transaction rollback restores slot capacity and leaves no corrupt state")
    void testPostgresTransactionRollbackSafety() {
        Farmer farmer = testFarmers.get(0);
        setAuth(farmer.getPhone(), Role.ROLE_FARMER);

        // 1. Initial slot state
        MandiSlot slotBefore = slotRepository.findById(testSlotSehore.getId()).orElseThrow();
        assertEquals(0, slotBefore.getBookedQuintals());
        assertEquals(0, slotBefore.getBookedFarmers());

        // 2. Attempt invalid booking with date in the past
        CreateBookingRequest invalidDateReq = new CreateBookingRequest();
        invalidDateReq.setFarmerId(farmer.getId());
        invalidDateReq.setMandiId(mandiSehore.getId());
        invalidDateReq.setCropId(testCrop.getId());
        invalidDateReq.setScheduledDate(LocalDate.now().minusDays(1)); // Invalid: past date
        invalidDateReq.setTimeSlot("08:00 AM - 10:00 AM");
        invalidDateReq.setEstimatedYieldQuintals(new BigDecimal("50.00"));

        assertThrows(InvalidBookingDateException.class, () ->
                bookingService.createBooking(invalidDateReq, "rollback-key-1", farmer.getPhone())
        );

        // 3. Verify zero state changes in DB
        MandiSlot slotAfter = slotRepository.findById(testSlotSehore.getId()).orElseThrow();
        assertEquals(0, slotAfter.getBookedQuintals(), "Slot capacity must remain 0 after rollback");
        assertEquals(0, slotAfter.getBookedFarmers(), "Slot booked farmers must remain 0 after rollback");
        assertEquals(0, bookingRepository.count(), "Zero bookings must exist in DB");

        // 4. Attempt booking with zero yield
        CreateBookingRequest invalidQtyReq = new CreateBookingRequest();
        invalidQtyReq.setFarmerId(farmer.getId());
        invalidQtyReq.setMandiId(mandiSehore.getId());
        invalidQtyReq.setCropId(testCrop.getId());
        invalidQtyReq.setScheduledDate(LocalDate.now().plusDays(5));
        invalidQtyReq.setTimeSlot("08:00 AM - 10:00 AM");
        invalidQtyReq.setEstimatedYieldQuintals(BigDecimal.ZERO); // Invalid: zero yield

        assertThrows(InvalidBookingQuantityException.class, () ->
                bookingService.createBooking(invalidQtyReq, "rollback-key-2", farmer.getPhone())
        );

        // Verify slot unchanged
        MandiSlot slotAfterQty = slotRepository.findById(testSlotSehore.getId()).orElseThrow();
        assertEquals(0, slotAfterQty.getBookedQuintals());
        assertEquals(0, slotAfterQty.getBookedFarmers());
    }
}
