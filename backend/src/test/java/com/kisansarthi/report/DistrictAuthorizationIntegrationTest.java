package com.kisansarthi.report;

import com.kisansarthi.auth.JwtTokenProvider;
import com.kisansarthi.auth.Role;
import com.kisansarthi.auth.User;
import com.kisansarthi.auth.UserRepository;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:kisansarthi_auth_test;DB_CLOSE_DELAY=-1;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.flyway.enabled=false"
})
public class DistrictAuthorizationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MandiRepository mandiRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private FarmerRepository farmerRepository;

    @Autowired
    private CropRepository cropRepository;

    @Autowired
    private SlotRepository slotRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private ProcurementTargetRepository targetRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private String doSehoreToken;
    private String adminToken;

    private Mandi sehoreMandi;
    private Mandi ujjainMandi;

    private Booking sehoreBooking;
    private Booking ujjainBooking;

    @BeforeEach
    void setUp() {
        String uid = UUID.randomUUID().toString().substring(0, 8);

        // 1. Setup Mandis in different districts
        sehoreMandi = new Mandi();
        sehoreMandi.setId("mandi-sehore-" + uid);
        sehoreMandi.setName("Sehore Krishi Mandi " + uid);
        sehoreMandi.setHindiName("सीहोर मंडी");
        sehoreMandi.setDistrict("Sehore");
        sehoreMandi.setHindiDistrict("सीहोर");
        sehoreMandi.setAddress("Sehore Bypass");
        sehoreMandi.setPinCode("466001");
        sehoreMandi.setDailyCapacityQuintals(2000);
        sehoreMandi.setActiveTokensWaiting(5);
        sehoreMandi.setGateStatus("OPEN");
        sehoreMandi = mandiRepository.save(sehoreMandi);

        ujjainMandi = new Mandi();
        ujjainMandi.setId("mandi-ujjain-" + uid);
        ujjainMandi.setName("Ujjain Krishi Mandi " + uid);
        ujjainMandi.setHindiName("उज्जैन मंडी");
        ujjainMandi.setDistrict("Ujjain");
        ujjainMandi.setHindiDistrict("उज्जैन");
        ujjainMandi.setAddress("Ujjain Road");
        ujjainMandi.setPinCode("456001");
        ujjainMandi.setDailyCapacityQuintals(3000);
        ujjainMandi.setActiveTokensWaiting(8);
        ujjainMandi.setGateStatus("OPEN");
        ujjainMandi = mandiRepository.save(ujjainMandi);

        // 2. Setup Procurement Targets
        ProcurementTarget sehoreTarget = new ProcurementTarget();
        sehoreTarget.setDistrict("Sehore");
        sehoreTarget.setSeason("Rabi 2025");
        sehoreTarget.setProcurementYear("2025-26");
        sehoreTarget.setEffectiveStart(LocalDate.now().minusMonths(1));
        sehoreTarget.setEffectiveEnd(LocalDate.now().plusMonths(5));
        sehoreTarget.setTargetQuintals(new BigDecimal("500000.00"));
        sehoreTarget.setWarehouseCapacityQuintals(new BigDecimal("600000.00"));
        targetRepository.save(sehoreTarget);

        ProcurementTarget ujjainTarget = new ProcurementTarget();
        ujjainTarget.setDistrict("Ujjain");
        ujjainTarget.setSeason("Rabi 2025");
        ujjainTarget.setProcurementYear("2025-26");
        ujjainTarget.setEffectiveStart(LocalDate.now().minusMonths(1));
        ujjainTarget.setEffectiveEnd(LocalDate.now().plusMonths(5));
        ujjainTarget.setTargetQuintals(new BigDecimal("700000.00"));
        ujjainTarget.setWarehouseCapacityQuintals(new BigDecimal("850000.00"));
        targetRepository.save(ujjainTarget);

        // 3. Setup Farmers
        Farmer sehoreFarmer = new Farmer();
        sehoreFarmer.setName("Sehore Kisan " + uid);
        sehoreFarmer.setPhone("9826" + (System.currentTimeMillis() % 1000000));
        sehoreFarmer.setKisanId("MP-SEHORE-" + uid);
        sehoreFarmer.setDistrict("Sehore");
        sehoreFarmer.setVillage("Ashta");
        sehoreFarmer.setLandSizeAcres(new BigDecimal("10.0"));
        sehoreFarmer.setMaskedAadhar("XXXX-XXXX-1111");
        sehoreFarmer = farmerRepository.save(sehoreFarmer);

        Farmer ujjainFarmer = new Farmer();
        ujjainFarmer.setName("Ujjain Kisan " + uid);
        ujjainFarmer.setPhone("9827" + (System.currentTimeMillis() % 1000000));
        ujjainFarmer.setKisanId("MP-UJJAIN-" + uid);
        ujjainFarmer.setDistrict("Ujjain");
        ujjainFarmer.setVillage("Nagda");
        ujjainFarmer.setLandSizeAcres(new BigDecimal("15.0"));
        ujjainFarmer.setMaskedAadhar("XXXX-XXXX-2222");
        ujjainFarmer = farmerRepository.save(ujjainFarmer);

        // 4. Setup Crop
        Crop testCrop = new Crop();
        testCrop.setId("crop-wheat-" + uid);
        testCrop.setName("Wheat " + uid);
        testCrop.setHindiName("गेहूं");
        testCrop.setSeason("Rabi");
        testCrop.setStandardMspPerQuintal(new BigDecimal("2275.00"));
        testCrop.setMpBonusPerQuintal(new BigDecimal("125.00"));
        testCrop.setTotalMsp(new BigDecimal("2400.00"));
        testCrop.setMarketPricePerQuintal(new BigDecimal("2350.00"));
        testCrop.setTypicalCostPerAcre(new BigDecimal("12000.00"));
        testCrop.setAverageYieldPerAcreQuintal(new BigDecimal("18.00"));
        testCrop.setMoistureLimitPct(new BigDecimal("12.00"));
        testCrop = cropRepository.save(testCrop);

        // 5. Setup Bookings
        sehoreBooking = new Booking();
        sehoreBooking.setTokenNumber("SHR-" + uid);
        sehoreBooking.setTokenSequence(1);
        sehoreBooking.setScheduledDate(LocalDate.now());
        sehoreBooking.setTimeSlot("09:00 AM - 11:00 AM");
        sehoreBooking.setVehicleType("TRACTOR");
        sehoreBooking.setVehicleNumber("MP-04-AB-1234");
        sehoreBooking.setQrCodeData("QR-SHR-" + uid);
        sehoreBooking.setMandi(sehoreMandi);
        sehoreBooking.setFarmer(sehoreFarmer);
        sehoreBooking.setCrop(testCrop);
        sehoreBooking.setStatus(BookingStatus.PROCUREMENT_COMPLETED);
        sehoreBooking.setEstimatedYieldQuintals(new BigDecimal("50.00"));
        sehoreBooking.setNetWeightQuintals(new BigDecimal("50.00"));
        sehoreBooking.setSettlementAmount(new BigDecimal("113750.00"));
        sehoreBooking = bookingRepository.save(sehoreBooking);

        ujjainBooking = new Booking();
        ujjainBooking.setTokenNumber("UJN-" + uid);
        ujjainBooking.setTokenSequence(1);
        ujjainBooking.setScheduledDate(LocalDate.now());
        ujjainBooking.setTimeSlot("09:00 AM - 11:00 AM");
        ujjainBooking.setVehicleType("TRACTOR");
        ujjainBooking.setVehicleNumber("MP-13-CD-5678");
        ujjainBooking.setQrCodeData("QR-UJN-" + uid);
        ujjainBooking.setMandi(ujjainMandi);
        ujjainBooking.setFarmer(ujjainFarmer);
        ujjainBooking.setCrop(testCrop);
        ujjainBooking.setStatus(BookingStatus.PROCUREMENT_COMPLETED);
        ujjainBooking.setEstimatedYieldQuintals(new BigDecimal("80.00"));
        ujjainBooking.setNetWeightQuintals(new BigDecimal("80.00"));
        ujjainBooking.setSettlementAmount(new BigDecimal("182000.00"));
        ujjainBooking = bookingRepository.save(ujjainBooking);

        // 6. Setup District Officer User assigned to Sehore (via sehoreMandi)
        String doUsername = "do_sehore_" + uid;
        User doUser = new User();
        doUser.setUsername(doUsername);
        doUser.setPhone("98930" + (System.currentTimeMillis() % 100000));
        doUser.setRole(Role.ROLE_DISTRICT_OFFICER);
        doUser.setMandiId(sehoreMandi.getId());
        userRepository.save(doUser);

        doSehoreToken = "Bearer " + jwtTokenProvider.generateAccessToken(doUser);

        // 7. Setup Admin User
        String adminUsername = "admin_" + uid;
        User adminUser = new User();
        adminUser.setUsername(adminUsername);
        adminUser.setPhone("98931" + (System.currentTimeMillis() % 100000));
        adminUser.setRole(Role.ROLE_ADMIN);
        userRepository.save(adminUser);

        adminToken = "Bearer " + jwtTokenProvider.generateAccessToken(adminUser);
    }

    // =========================================================================
    // Requirement 5: EXPLICIT AUTHORIZATION TESTS
    // =========================================================================

    @Test
    @DisplayName("District Officer → own district → HTTP 200 (mandi-performance)")
    void testDistrictOfficer_ownDistrict_returns200() throws Exception {
        mockMvc.perform(get("/api/v1/reports/mandi-performance")
                        .param("district", "Sehore")
                        .header("Authorization", doSehoreToken)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.data[?(@.district == 'Ujjain')]").doesNotExist());
    }

    @Test
    @DisplayName("District Officer → another district → HTTP 403 (mandi-performance)")
    void testDistrictOfficer_anotherDistrict_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/reports/mandi-performance")
                        .param("district", "Ujjain")
                        .header("Authorization", doSehoreToken)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
    }

    @Test
    @DisplayName("District Officer → omitted district → returns only assigned district (mandi-performance)")
    void testDistrictOfficer_omittedDistrict_returnsOnlyAssignedDistrict() throws Exception {
        mockMvc.perform(get("/api/v1/reports/mandi-performance")
                        .header("Authorization", doSehoreToken)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[*].district", everyItem(equalToIgnoringCase("Sehore"))));
    }

    @Test
    @DisplayName("Admin → own / any district → HTTP 200")
    void testAdmin_anyDistrict_returns200() throws Exception {
        // Admin querying specific district
        mockMvc.perform(get("/api/v1/reports/mandi-performance")
                        .param("district", "Ujjain")
                        .header("Authorization", adminToken)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[*].district", everyItem(equalToIgnoringCase("Ujjain"))));

        // Admin querying all districts (omitted parameter)
        mockMvc.perform(get("/api/v1/reports/mandi-performance")
                        .header("Authorization", adminToken)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    // =========================================================================
    // Requirement 3: DISTRICT-STATS, BOTTLENECKS, PROCUREMENT REGISTER & CSV
    // =========================================================================

    @Test
    @DisplayName("District Officer → district-stats → scoped strictly to assigned district")
    void testDistrictOfficer_districtStats_scopedToAssignedDistrict() throws Exception {
        mockMvc.perform(get("/api/v1/reports/district-stats")
                        .header("Authorization", doSehoreToken)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.stats", hasSize(1)))
                .andExpect(jsonPath("$.data.stats[0].district").value("Sehore"));
    }

    @Test
    @DisplayName("District Officer → bottlenecks → scoped strictly to assigned district mandis")
    void testDistrictOfficer_bottlenecks_scopedToAssignedDistrict() throws Exception {
        mockMvc.perform(get("/api/v1/reports/bottlenecks")
                        .header("Authorization", doSehoreToken)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[?(@.district == 'Ujjain')]").doesNotExist());
    }

    @Test
    @DisplayName("District Officer → procurement-register → own district 200, foreign district 403")
    void testDistrictOfficer_procurementRegister_districtIsolation() throws Exception {
        // Own district -> 200
        mockMvc.perform(get("/api/v1/reports/procurement-register")
                        .param("district", "Sehore")
                        .header("Authorization", doSehoreToken)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // Foreign district -> 403
        mockMvc.perform(get("/api/v1/reports/procurement-register")
                        .param("district", "Ujjain")
                        .header("Authorization", doSehoreToken)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("District Officer → procurement CSV export → own district 200, foreign district 403")
    void testDistrictOfficer_procurementCsv_districtIsolation() throws Exception {
        // Own district -> 200
        mockMvc.perform(get("/api/v1/reports/export/csv")
                        .param("district", "Sehore")
                        .header("Authorization", doSehoreToken))
                .andExpect(status().isOk())
                .andExpect(content().contentType("text/csv;charset=UTF-8"));

        // Foreign district -> 403
        mockMvc.perform(get("/api/v1/reports/export/csv")
                        .param("district", "Ujjain")
                        .header("Authorization", doSehoreToken))
                .andExpect(status().isForbidden());
    }

    // =========================================================================
    // Requirement 4: RESOURCE ISOLATION AUDIT (FARMERS, BOOKINGS)
    // =========================================================================

    @Test
    @DisplayName("District Officer → farmers list → foreign district 403, own district 200")
    void testDistrictOfficer_farmers_districtIsolation() throws Exception {
        // Foreign district -> 403
        mockMvc.perform(get("/api/v1/farmers")
                        .param("district", "Ujjain")
                        .header("Authorization", doSehoreToken)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());

        // Own district -> 200
        mockMvc.perform(get("/api/v1/farmers")
                        .param("district", "Sehore")
                        .header("Authorization", doSehoreToken)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("District Officer → get booking by ID → foreign district booking 403, own district 200")
    void testDistrictOfficer_bookingById_districtIsolation() throws Exception {
        // Own district booking -> 200
        mockMvc.perform(get("/api/v1/bookings/" + sehoreBooking.getId())
                        .header("Authorization", doSehoreToken)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.tokenNumber").value(sehoreBooking.getTokenNumber()));

        // Foreign district booking -> 403
        mockMvc.perform(get("/api/v1/bookings/" + ujjainBooking.getId())
                        .header("Authorization", doSehoreToken)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }
}
