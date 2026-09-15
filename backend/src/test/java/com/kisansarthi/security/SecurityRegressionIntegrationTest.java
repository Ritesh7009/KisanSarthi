package com.kisansarthi.security;

import com.kisansarthi.auth.Role;
import com.kisansarthi.auth.SecurityAuthorizationService;
import com.kisansarthi.auth.User;
import com.kisansarthi.auth.UserRepository;
import com.kisansarthi.booking.*;
import com.kisansarthi.common.*;
import com.kisansarthi.crop.Crop;
import com.kisansarthi.crop.CropRepository;
import com.kisansarthi.farmer.Farmer;
import com.kisansarthi.farmer.FarmerRepository;
import com.kisansarthi.mandi.Mandi;
import com.kisansarthi.mandi.MandiRepository;
import com.kisansarthi.payment.PaymentService;
import com.kisansarthi.queue.QueueService;
import com.kisansarthi.slot.MandiSlot;
import com.kisansarthi.slot.SlotRepository;
import com.kisansarthi.slot.SlotService;
import com.kisansarthi.weighment.WeighmentService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:kisansarthi_security_reg_test;DB_CLOSE_DELAY=-1;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.flyway.enabled=false"
})
public class SecurityRegressionIntegrationTest {

    @Autowired
    private BookingService bookingService;

    @Autowired
    private SlotService slotService;

    @Autowired
    private QueueService queueService;

    @Autowired
    private WeighmentService weighmentService;

    @Autowired
    private PaymentService paymentService;

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
    private SlotRepository slotRepository;

    @Autowired
    private com.kisansarthi.queue.QueueEventRepository queueEventRepository;

    @Autowired
    private com.kisansarthi.queue.QueueStateRepository queueStateRepository;

    @Autowired
    private SecurityAuthorizationService authorizationService;

    private User userFarmerA;
    private User userFarmerB;
    private User userOperatorMandiA;
    private User userManagerMandiA;
    private User userOfficerDistrictSehore;

    private Farmer farmerA;
    private Farmer farmerB;

    private Mandi mandiA;
    private Mandi mandiB;

    private MandiSlot slotA;
    private MandiSlot slotB;

    private Crop crop;

    @BeforeEach
    public void setup() {
        cleanData();

        // 1. Mandis
        mandiA = new Mandi();
        mandiA.setId("mandi-sehore-sec-" + UUID.randomUUID().toString().substring(0, 6));
        mandiA.setName("Sehore Krishi Upaj Mandi");
        mandiA.setHindiName("सीहोर कृषि उपज मंडी");
        mandiA.setDistrict("Sehore");
        mandiA.setHindiDistrict("सीहोर");
        mandiA.setAddress("Mandi Road, Sehore, MP");
        mandiA.setPinCode("466001");
        mandiA.setPhone("07562224455");
        mandiA.setTotalTokensToday(0);
        mandiA.setCurrentTokenServing(0);
        mandiA.setActiveTokensWaiting(0);
        mandiA.setCreatedAt(Instant.now());
        mandiA.setUpdatedAt(Instant.now());
        mandiA = mandiRepository.save(mandiA);

        mandiB = new Mandi();
        mandiB.setId("mandi-indore-sec-" + UUID.randomUUID().toString().substring(0, 6));
        mandiB.setName("Indore Laxmibai Nagar Mandi");
        mandiB.setHindiName("इंदौर लक्ष्मीबाई नगर मंडी");
        mandiB.setDistrict("Indore");
        mandiB.setHindiDistrict("इंदौर");
        mandiB.setAddress("Laxmibai Nagar, Indore, MP");
        mandiB.setPinCode("452006");
        mandiB.setPhone("07312224455");
        mandiB.setTotalTokensToday(0);
        mandiB.setCurrentTokenServing(0);
        mandiB.setActiveTokensWaiting(0);
        mandiB.setCreatedAt(Instant.now());
        mandiB.setUpdatedAt(Instant.now());
        mandiB = mandiRepository.save(mandiB);

        // 2. Slots
        slotA = new MandiSlot();
        slotA.setId("slot-a-" + UUID.randomUUID().toString().substring(0, 6));
        slotA.setMandiId(mandiA.getId());
        slotA.setSlotLabel("08:00 AM - 10:00 AM");
        slotA.setStartTime("08:00");
        slotA.setEndTime("10:00");
        slotA.setMaxCapacityQuintals(1000);
        slotA.setBookedQuintals(0);
        slotA.setMaxFarmers(50);
        slotA.setBookedFarmers(0);
        slotA.setStatus("AVAILABLE");
        slotA.setCreatedAt(Instant.now());
        slotA.setUpdatedAt(Instant.now());
        slotA = slotRepository.save(slotA);

        slotB = new MandiSlot();
        slotB.setId("slot-b-" + UUID.randomUUID().toString().substring(0, 6));
        slotB.setMandiId(mandiB.getId());
        slotB.setSlotLabel("10:00 AM - 12:00 PM");
        slotB.setStartTime("10:00");
        slotB.setEndTime("12:00");
        slotB.setMaxCapacityQuintals(1000);
        slotB.setBookedQuintals(0);
        slotB.setMaxFarmers(50);
        slotB.setBookedFarmers(0);
        slotB.setStatus("AVAILABLE");
        slotB.setCreatedAt(Instant.now());
        slotB.setUpdatedAt(Instant.now());
        slotB = slotRepository.save(slotB);

        // 3. Crop
        crop = new Crop();
        crop.setId("crop-wheat-" + UUID.randomUUID().toString().substring(0, 6));
        crop.setName("Sharbati Wheat");
        crop.setHindiName("शरबती गेहूं");
        crop.setSeason("RABI");
        crop.setStandardMspPerQuintal(BigDecimal.valueOf(2425));
        crop.setMpBonusPerQuintal(BigDecimal.valueOf(125));
        crop.setTotalMsp(BigDecimal.valueOf(2550));
        crop.setMarketPricePerQuintal(BigDecimal.valueOf(2350));
        crop.setMoistureLimitPct(BigDecimal.valueOf(12.0));
        crop.setCreatedAt(Instant.now());
        crop.setUpdatedAt(Instant.now());
        crop = cropRepository.save(crop);

        // 4. Users
        userFarmerA = new User();
        userFarmerA.setUsername("9826011111");
        userFarmerA.setPasswordHash("hashed_pass");
        userFarmerA.setPhone("9826011111");
        userFarmerA.setRole(Role.ROLE_FARMER);
        userFarmerA.setActive(true);
        userFarmerA.setCreatedAt(Instant.now());
        userFarmerA.setUpdatedAt(Instant.now());
        userFarmerA = userRepository.save(userFarmerA);

        userFarmerB = new User();
        userFarmerB.setUsername("9826022222");
        userFarmerB.setPasswordHash("hashed_pass");
        userFarmerB.setPhone("9826022222");
        userFarmerB.setRole(Role.ROLE_FARMER);
        userFarmerB.setActive(true);
        userFarmerB.setCreatedAt(Instant.now());
        userFarmerB.setUpdatedAt(Instant.now());
        userFarmerB = userRepository.save(userFarmerB);

        userOperatorMandiA = new User();
        userOperatorMandiA.setUsername("op_sehore");
        userOperatorMandiA.setPasswordHash("hashed_pass");
        userOperatorMandiA.setPhone("9826033333");
        userOperatorMandiA.setRole(Role.ROLE_MANDI_OPERATOR);
        userOperatorMandiA.setMandiId(mandiA.getId());
        userOperatorMandiA.setActive(true);
        userOperatorMandiA.setCreatedAt(Instant.now());
        userOperatorMandiA.setUpdatedAt(Instant.now());
        userOperatorMandiA = userRepository.save(userOperatorMandiA);

        userManagerMandiA = new User();
        userManagerMandiA.setUsername("mgr_sehore");
        userManagerMandiA.setPasswordHash("hashed_pass");
        userManagerMandiA.setPhone("9826044444");
        userManagerMandiA.setRole(Role.ROLE_MANDI_MANAGER);
        userManagerMandiA.setMandiId(mandiA.getId());
        userManagerMandiA.setActive(true);
        userManagerMandiA.setCreatedAt(Instant.now());
        userManagerMandiA.setUpdatedAt(Instant.now());
        userManagerMandiA = userRepository.save(userManagerMandiA);

        userOfficerDistrictSehore = new User();
        userOfficerDistrictSehore.setUsername("do_sehore");
        userOfficerDistrictSehore.setPasswordHash("hashed_pass");
        userOfficerDistrictSehore.setPhone("9826055555");
        userOfficerDistrictSehore.setRole(Role.ROLE_DISTRICT_OFFICER);
        userOfficerDistrictSehore.setMandiId(mandiA.getId());
        userOfficerDistrictSehore.setActive(true);
        userOfficerDistrictSehore.setCreatedAt(Instant.now());
        userOfficerDistrictSehore.setUpdatedAt(Instant.now());
        userOfficerDistrictSehore = userRepository.save(userOfficerDistrictSehore);

        // 5. Farmers
        farmerA = new Farmer();
        farmerA.setKisanId("KISAN-9826011111");
        farmerA.setUser(userFarmerA);
        farmerA.setName("Ramesh Patel");
        farmerA.setPhone("9826011111");
        farmerA.setDistrict("Sehore");
        farmerA.setVillage("Ashta");
        farmerA.setMaskedAadhar("XXXX-XXXX-1111");
        farmerA.setBankAccountLast4("1111");
        farmerA.setIfscCode("SBIN0001111");
        farmerA.setCreatedAt(Instant.now());
        farmerA.setUpdatedAt(Instant.now());
        farmerA = farmerRepository.save(farmerA);

        farmerB = new Farmer();
        farmerB.setKisanId("KISAN-9826022222");
        farmerB.setUser(userFarmerB);
        farmerB.setName("Suresh Sharma");
        farmerB.setPhone("9826022222");
        farmerB.setDistrict("Indore");
        farmerB.setVillage("Sanwer");
        farmerB.setMaskedAadhar("XXXX-XXXX-2222");
        farmerB.setBankAccountLast4("2222");
        farmerB.setIfscCode("SBIN0002222");
        farmerB.setCreatedAt(Instant.now());
        farmerB.setUpdatedAt(Instant.now());
        farmerB = farmerRepository.save(farmerB);
    }

    @AfterEach
    public void tearDown() {
        SecurityContextHolder.clearContext();
        cleanData();
    }

    private void cleanData() {
        queueEventRepository.deleteAllInBatch();
        queueStateRepository.deleteAllInBatch();
        bookingRepository.deleteAllInBatch();
        slotRepository.deleteAllInBatch();
        cropRepository.deleteAllInBatch();
        farmerRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();
        mandiRepository.deleteAllInBatch();
    }

    private void setAuth(User user) {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                user.getUsername(),
                "N/A",
                List.of(new SimpleGrantedAuthority(user.getRole().name()))
        );
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    @DisplayName("Reg-1: Farmer A cannot cancel Farmer B's booking (IDOR/BOLA Protection)")
    public void testFarmerCannotCancelOtherFarmerBooking() {
        // Create booking for Farmer B
        setAuth(userFarmerB);
        CreateBookingRequest request = new CreateBookingRequest();
        request.setMandiId(mandiB.getId());
        request.setCropId(crop.getId());
        request.setSlotId(slotB.getId());
        request.setTimeSlot(slotB.getSlotLabel());
        request.setScheduledDate(LocalDate.now());
        request.setEstimatedYieldQuintals(BigDecimal.valueOf(40.0));
        request.setVehicleType("TRACTOR_TROLLEY");
        request.setVehicleNumber("MP-09-AB-1234");
        BookingResponse bookingB = bookingService.createBooking(request, "idemp-b-1", userFarmerB.getUsername());

        // Now Farmer A attempts to cancel Farmer B's booking
        setAuth(userFarmerA);
        assertThrows(AccessDeniedException.class, () -> {
            bookingService.cancelBooking(bookingB.getId(), userFarmerA.getUsername());
        });

        // Verify booking B status remains BOOKED
        Booking verified = bookingRepository.findById(bookingB.getId()).orElseThrow();
        assertEquals(BookingStatus.BOOKED, verified.getStatus());
    }

    @Test
    @DisplayName("Reg-2: Farmer A cannot retrieve Farmer B's booking details (IDOR Read Protection)")
    public void testFarmerCannotRetrieveOtherFarmerBooking() {
        // Create booking for Farmer B
        setAuth(userFarmerB);
        CreateBookingRequest request = new CreateBookingRequest();
        request.setMandiId(mandiB.getId());
        request.setCropId(crop.getId());
        request.setSlotId(slotB.getId());
        request.setTimeSlot(slotB.getSlotLabel());
        request.setScheduledDate(LocalDate.now());
        request.setEstimatedYieldQuintals(BigDecimal.valueOf(30.0));
        request.setVehicleType("TRACTOR_TROLLEY");
        request.setVehicleNumber("MP-09-AB-5678");
        BookingResponse bookingB = bookingService.createBooking(request, "idemp-b-2", userFarmerB.getUsername());

        // Farmer A attempts to read booking B details
        setAuth(userFarmerA);
        assertThrows(AccessDeniedException.class, () -> {
            bookingService.getBookingById(bookingB.getId());
        });
    }

    @Test
    @DisplayName("Reg-3: Farmer A cannot create booking specifying Farmer B's farmerId (Impersonation Protection)")
    public void testFarmerCannotImpersonateOtherFarmer() {
        setAuth(userFarmerA);
        CreateBookingRequest request = new CreateBookingRequest();
        request.setFarmerId(farmerB.getId()); // Attacker supplies Victim's farmerId
        request.setMandiId(mandiA.getId());
        request.setCropId(crop.getId());
        request.setSlotId(slotA.getId());
        request.setTimeSlot(slotA.getSlotLabel());
        request.setScheduledDate(LocalDate.now());
        request.setEstimatedYieldQuintals(BigDecimal.valueOf(25.0));
        request.setVehicleType("TRACTOR_TROLLEY");
        request.setVehicleNumber("MP-04-AB-9999");

        assertThrows(AccessDeniedException.class, () -> {
            bookingService.createBooking(request, "idemp-impersonate", userFarmerA.getUsername());
        });
    }

    @Test
    @DisplayName("Reg-4: Mandi Operator A cannot modify Mandi B's booking status (Cross-Mandi Scoping)")
    public void testOperatorCannotModifyOtherMandiBooking() {
        // Create booking in Mandi B
        setAuth(userFarmerB);
        CreateBookingRequest request = new CreateBookingRequest();
        request.setMandiId(mandiB.getId());
        request.setCropId(crop.getId());
        request.setSlotId(slotB.getId());
        request.setTimeSlot(slotB.getSlotLabel());
        request.setScheduledDate(LocalDate.now());
        request.setEstimatedYieldQuintals(BigDecimal.valueOf(50.0));
        request.setVehicleType("TRACTOR_TROLLEY");
        request.setVehicleNumber("MP-09-AB-7777");
        BookingResponse bookingB = bookingService.createBooking(request, "idemp-b-3", userFarmerB.getUsername());

        // Mandi Operator of Mandi A attempts to transition Mandi B booking
        setAuth(userOperatorMandiA);
        assertThrows(AccessDeniedException.class, () -> {
            bookingService.transitionStatus(bookingB.getId(), BookingStatus.GATE_ENTERED);
        });
    }

    @Test
    @DisplayName("Reg-5: Mandi Manager A cannot create or alter slots in Mandi B (Cross-Mandi Slot Protection)")
    public void testManagerCannotModifyOtherMandiSlot() {
        setAuth(userManagerMandiA);

        MandiSlot newSlot = new MandiSlot();
        newSlot.setSlotLabel("02:00 PM - 04:00 PM");
        newSlot.setStartTime("14:00");
        newSlot.setEndTime("16:00");
        newSlot.setMaxCapacityQuintals(500);
        newSlot.setMaxFarmers(25);

        assertThrows(AccessDeniedException.class, () -> {
            slotService.createSlot(mandiB.getId(), newSlot);
        });

        assertThrows(AccessDeniedException.class, () -> {
            slotService.updateSlot(slotB.getId(), newSlot);
        });
    }

    @Test
    @DisplayName("Reg-6: District Officer Sehore cannot access Indore bookings or queue")
    public void testDistrictOfficerCannotAccessOtherDistrict() {
        setAuth(userOfficerDistrictSehore);

        assertThrows(AccessDeniedException.class, () -> {
            queueService.advanceQueue(mandiB.getId(), userOfficerDistrictSehore.getUsername());
        });
    }

    @Test
    @DisplayName("Reg-7: Invalid booking status transitions fail safely")
    public void testInvalidStateTransitionsFail() {
        setAuth(userFarmerA);
        CreateBookingRequest request = new CreateBookingRequest();
        request.setMandiId(mandiA.getId());
        request.setCropId(crop.getId());
        request.setSlotId(slotA.getId());
        request.setTimeSlot(slotA.getSlotLabel());
        request.setScheduledDate(LocalDate.now());
        request.setEstimatedYieldQuintals(BigDecimal.valueOf(35.0));
        request.setVehicleType("TRACTOR_TROLLEY");
        request.setVehicleNumber("MP-04-AB-1111");
        BookingResponse bookingA = bookingService.createBooking(request, "idemp-a-1", userFarmerA.getUsername());

        // Cancel booking
        bookingService.cancelBooking(bookingA.getId(), userFarmerA.getUsername());

        // Attempt illegal transition CANCELLED -> GATE_ENTERED
        setAuth(userOperatorMandiA);
        assertThrows(InvalidStateTransitionException.class, () -> {
            bookingService.transitionStatus(bookingA.getId(), BookingStatus.GATE_ENTERED);
        });
    }

    @Test
    @DisplayName("Reg-8: Repeated cancellation fails with BookingAlreadyCancelledException")
    public void testRepeatedCancellationFailsSafely() {
        setAuth(userFarmerA);
        CreateBookingRequest request = new CreateBookingRequest();
        request.setMandiId(mandiA.getId());
        request.setCropId(crop.getId());
        request.setSlotId(slotA.getId());
        request.setTimeSlot(slotA.getSlotLabel());
        request.setScheduledDate(LocalDate.now());
        request.setEstimatedYieldQuintals(BigDecimal.valueOf(20.0));
        request.setVehicleType("TRACTOR_TROLLEY");
        request.setVehicleNumber("MP-04-AB-2222");
        BookingResponse bookingA = bookingService.createBooking(request, "idemp-a-2", userFarmerA.getUsername());

        // First cancellation succeeds
        bookingService.cancelBooking(bookingA.getId(), userFarmerA.getUsername());

        // Second cancellation fails safely
        assertThrows(BookingAlreadyCancelledException.class, () -> {
            bookingService.cancelBooking(bookingA.getId(), userFarmerA.getUsername());
        });
    }

    @Test
    @DisplayName("FailClosed-1: verifyBookingAccess with no security context throws AccessDeniedException")
    public void testVerifyBookingAccessNoSecurityContextThrows() {
        SecurityContextHolder.clearContext();
        Booking booking = new Booking();
        booking.setFarmer(farmerA);
        booking.setMandi(mandiA);

        assertThrows(AccessDeniedException.class, () -> {
            authorizationService.verifyBookingAccess(booking);
        });
    }

    @Test
    @DisplayName("FailClosed-2: verifyBookingAccess with non-existent user throws AccessDeniedException")
    public void testVerifyBookingAccessNonExistentUserThrows() {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                "non_existent_user_12345",
                "N/A",
                List.of(new SimpleGrantedAuthority("ROLE_FARMER"))
        );
        SecurityContextHolder.getContext().setAuthentication(auth);

        Booking booking = new Booking();
        booking.setFarmer(farmerA);
        booking.setMandi(mandiA);

        assertThrows(AccessDeniedException.class, () -> {
            authorizationService.verifyBookingAccess(booking);
        });
    }

    @Test
    @DisplayName("FailClosed-3: verifyBookingAccess with farmer user lacking farmer profile throws AccessDeniedException")
    public void testVerifyBookingAccessFarmerLackingProfileThrows() {
        User orphanFarmerUser = new User();
        orphanFarmerUser.setUsername("orphan_farmer_user");
        orphanFarmerUser.setPasswordHash("hashed_pass");
        orphanFarmerUser.setPhone("9826099999");
        orphanFarmerUser.setRole(Role.ROLE_FARMER);
        orphanFarmerUser.setActive(true);
        orphanFarmerUser.setCreatedAt(Instant.now());
        orphanFarmerUser.setUpdatedAt(Instant.now());
        orphanFarmerUser = userRepository.save(orphanFarmerUser);

        setAuth(orphanFarmerUser);

        Booking booking = new Booking();
        booking.setFarmer(farmerA);
        booking.setMandi(mandiA);

        assertThrows(AccessDeniedException.class, () -> {
            authorizationService.verifyBookingAccess(booking);
        });
    }

    @Test
    @DisplayName("FailClosed-4: verifyBookingCancellation with no security context and null username throws AccessDeniedException")
    public void testVerifyBookingCancellationNoContextNullUsernameThrows() {
        SecurityContextHolder.clearContext();
        Booking booking = new Booking();
        booking.setFarmer(farmerA);
        booking.setMandi(mandiA);

        assertThrows(AccessDeniedException.class, () -> {
            authorizationService.verifyBookingCancellation(booking, null);
        });
    }

    @Test
    @DisplayName("FailClosed-5: verifyMandiAccess with unassigned mandi operator throws AccessDeniedException")
    public void testVerifyMandiAccessUnassignedOperatorThrows() {
        User unassignedOperator = new User();
        unassignedOperator.setUsername("unassigned_op");
        unassignedOperator.setPasswordHash("hashed_pass");
        unassignedOperator.setPhone("9826088888");
        unassignedOperator.setRole(Role.ROLE_MANDI_OPERATOR);
        unassignedOperator.setMandiId(null); // Unassigned!
        unassignedOperator.setActive(true);
        unassignedOperator.setCreatedAt(Instant.now());
        unassignedOperator.setUpdatedAt(Instant.now());
        unassignedOperator = userRepository.save(unassignedOperator);

        setAuth(unassignedOperator);

        assertThrows(AccessDeniedException.class, () -> {
            authorizationService.verifyMandiAccess(mandiA.getId());
        });
    }

    @Test
    @DisplayName("FailClosed-6: verifyDistrictAccess with unassigned district officer throws AccessDeniedException")
    public void testVerifyDistrictAccessUnassignedDistrictOfficerThrows() {
        User unassignedDO = new User();
        unassignedDO.setUsername("unassigned_do");
        unassignedDO.setPasswordHash("hashed_pass");
        unassignedDO.setPhone("9826077777");
        unassignedDO.setRole(Role.ROLE_DISTRICT_OFFICER);
        unassignedDO.setMandiId(null); // Unassigned district!
        unassignedDO.setActive(true);
        unassignedDO.setCreatedAt(Instant.now());
        unassignedDO.setUpdatedAt(Instant.now());
        unassignedDO = userRepository.save(unassignedDO);

        setAuth(unassignedDO);

        assertThrows(AccessDeniedException.class, () -> {
            authorizationService.verifyDistrictAccess("Sehore");
        });
    }

    @Test
    @DisplayName("FailClosed-7: resolveAndAuthorizeDistrict with no auth throws AccessDeniedException")
    public void testResolveAndAuthorizeDistrictNoAuthThrows() {
        SecurityContextHolder.clearContext();

        assertThrows(AccessDeniedException.class, () -> {
            authorizationService.resolveAndAuthorizeDistrict("Sehore");
        });

        assertThrows(AccessDeniedException.class, () -> {
            authorizationService.resolveAndAuthorizeDistrict(null);
        });
    }

    @Test
    @DisplayName("FailClosed-8: resolveAndAuthorizeDistrict with unassigned district officer throws AccessDeniedException")
    public void testResolveAndAuthorizeDistrictUnassignedDistrictOfficerThrows() {
        User unassignedDO = new User();
        unassignedDO.setUsername("unassigned_do_2");
        unassignedDO.setPasswordHash("hashed_pass");
        unassignedDO.setPhone("9826066666");
        unassignedDO.setRole(Role.ROLE_DISTRICT_OFFICER);
        unassignedDO.setMandiId(null); // Unassigned
        unassignedDO.setActive(true);
        unassignedDO.setCreatedAt(Instant.now());
        unassignedDO.setUpdatedAt(Instant.now());
        unassignedDO = userRepository.save(unassignedDO);

        setAuth(unassignedDO);

        assertThrows(AccessDeniedException.class, () -> {
            authorizationService.resolveAndAuthorizeDistrict("Sehore");
        });

        assertThrows(AccessDeniedException.class, () -> {
            authorizationService.resolveAndAuthorizeDistrict(null);
        });
    }
}
