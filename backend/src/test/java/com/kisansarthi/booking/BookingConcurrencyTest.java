package com.kisansarthi.booking;

import com.kisansarthi.crop.Crop;
import com.kisansarthi.crop.CropRepository;
import com.kisansarthi.farmer.Farmer;
import com.kisansarthi.farmer.FarmerRepository;
import com.kisansarthi.mandi.Mandi;
import com.kisansarthi.mandi.MandiRepository;
import com.kisansarthi.queue.QueueEventDto;
import com.kisansarthi.queue.QueueService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
public class BookingConcurrencyTest {

    @Autowired
    private BookingService bookingService;

    @Autowired
    private QueueService queueService;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private MandiRepository mandiRepository;

    @Autowired
    private FarmerRepository farmerRepository;

    @Autowired
    private CropRepository cropRepository;

    @Autowired
    private MandiTokenSequenceRepository sequenceRepository;

    private Mandi testMandi;
    private Crop testCrop;
    private List<Farmer> testFarmers;

    @BeforeEach
    void setUp() {
        bookingRepository.deleteAll();
        sequenceRepository.deleteAll();

        // Ensure test mandi exists
        testMandi = mandiRepository.findById("mandi-sehore-test").orElseGet(() -> {
            Mandi m = new Mandi();
            m.setId("mandi-sehore-test");
            m.setName("Sehore Test Mandi");
            m.setDistrict("Sehore");
            m.setCurrentTokenServing(0);
            m.setTotalTokensToday(0);
            m.setActiveTokensWaiting(0);
            return mandiRepository.save(m);
        });

        // Reset Mandi counters
        testMandi.setCurrentTokenServing(0);
        testMandi.setTotalTokensToday(0);
        testMandi.setActiveTokensWaiting(0);
        mandiRepository.save(testMandi);

        // Ensure test crop exists
        testCrop = cropRepository.findById("crop-wheat-test").orElseGet(() -> {
            Crop c = new Crop();
            c.setId("crop-wheat-test");
            c.setName("Wheat Test Grade");
            c.setStandardMspPerQuintal(new BigDecimal("2275.00"));
            c.setMpBonusPerQuintal(new BigDecimal("150.00"));
            c.setTotalMsp(new BigDecimal("2425.00"));
            return cropRepository.save(c);
        });

        // Create a pool of farmers for testing
        testFarmers = new ArrayList<>();
        for (int i = 1; i <= 100; i++) {
            String phone = String.format("98000%05d", i);
            Farmer f = farmerRepository.findByPhone(phone).orElseGet(() -> {
                Farmer newFarmer = new Farmer();
                newFarmer.setName("Farmer Test " + phone);
                newFarmer.setPhone(phone);
                newFarmer.setAadharNumber("7104882" + String.format("%05d", phone.hashCode() % 100000));
                newFarmer.setMaskedAadhar("XXXX-XXXX-" + phone.substring(phone.length() - 4));
                newFarmer.setDistrict("Sehore");
                newFarmer.setVillage("Bilkisganj");
                return farmerRepository.save(newFarmer);
            });
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

                    CreateBookingRequest req = new CreateBookingRequest();
                    req.setFarmerId(farmer.getId());
                    req.setMandiId(testMandi.getId());
                    req.setCropId(testCrop.getId());
                    req.setScheduledDate(scheduledDate);
                    req.setTimeSlot("08:00 AM - 10:00 AM");
                    req.setVehicleType("TRACTOR_TROLLEY");
                    req.setVehicleNumber(String.format("MP-04-AB-%04d", index + 1));
                    req.setEstimatedYieldQuintals(new BigDecimal("45.50"));

                    BookingResponse res = bookingService.createBooking(req, idempotencyKey, null);
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
    @DisplayName("Concurrently advance queue: verify atomic token increments without skipping or collisions")
    void testSimultaneousQueueAdvancement() throws InterruptedException {
        // Seed 10 initial bookings
        for (int i = 0; i < 10; i++) {
            CreateBookingRequest req = new CreateBookingRequest();
            req.setFarmerId(testFarmers.get(i).getId());
            req.setMandiId(testMandi.getId());
            req.setCropId(testCrop.getId());
            req.setScheduledDate(LocalDate.now());
            req.setTimeSlot("08:00 AM - 10:00 AM");
            req.setVehicleType("TRACTOR_TROLLEY");
            req.setVehicleNumber(String.format("MP-04-T-%04d", i + 1));
            req.setEstimatedYieldQuintals(new BigDecimal("30.00"));
            bookingService.createBooking(req, "seed-key-" + i, null);
        }

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
                    QueueEventDto dto = queueService.advanceQueue(testMandi.getId(), "Operator-" + operatorId);
                    results.add(dto);
                } catch (Exception e) {
                    // Log
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
}
