package com.kisansarthi.booking;

import com.kisansarthi.auth.User;
import com.kisansarthi.auth.UserRepository;
import com.kisansarthi.common.BusinessException;
import com.kisansarthi.common.InvalidStateTransitionException;
import com.kisansarthi.common.ResourceNotFoundException;
import com.kisansarthi.crop.Crop;
import com.kisansarthi.crop.CropRepository;
import com.kisansarthi.farmer.Farmer;
import com.kisansarthi.farmer.FarmerRepository;
import com.kisansarthi.mandi.Mandi;
import com.kisansarthi.mandi.MandiRepository;
import com.kisansarthi.sms.SmsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class BookingService {

    private static final Logger log = LoggerFactory.getLogger(BookingService.class);

    private final BookingRepository bookingRepository;
    private final MandiTokenSequenceRepository sequenceRepository;
    private final FarmerRepository farmerRepository;
    private final MandiRepository mandiRepository;
    private final CropRepository cropRepository;
    private final UserRepository userRepository;
    private final SmsService smsService;

    public BookingService(
            BookingRepository bookingRepository,
            MandiTokenSequenceRepository sequenceRepository,
            FarmerRepository farmerRepository,
            MandiRepository mandiRepository,
            CropRepository cropRepository,
            UserRepository userRepository,
            SmsService smsService) {
        this.bookingRepository = bookingRepository;
        this.sequenceRepository = sequenceRepository;
        this.farmerRepository = farmerRepository;
        this.mandiRepository = mandiRepository;
        this.cropRepository = cropRepository;
        this.userRepository = userRepository;
        this.smsService = smsService;
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public BookingResponse createBooking(CreateBookingRequest request, String idempotencyKey, String authenticatedUsername) {
        // 1. Idempotency Check: if request was already processed, return original
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            Optional<Booking> existing = bookingRepository.findByIdempotencyKey(idempotencyKey);
            if (existing.isPresent()) {
                log.info("Idempotent booking request matched for key {}. Returning existing token {}",
                        idempotencyKey, existing.get().getTokenNumber());
                return BookingResponse.fromEntity(existing.get());
            }
        }

        // 2. Resolve Farmer
        Farmer farmer;
        if (request.getFarmerId() != null) {
            farmer = farmerRepository.findById(request.getFarmerId())
                    .orElseThrow(() -> new ResourceNotFoundException("Farmer not found: " + request.getFarmerId()));
        } else {
            User user = userRepository.findByUsername(authenticatedUsername)
                    .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found: " + authenticatedUsername));
            farmer = farmerRepository.findByUserId(user.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Farmer profile not found for user: " + authenticatedUsername));
        }

        // 3. Resolve Mandi and Crop
        Mandi mandi = mandiRepository.findById(request.getMandiId())
                .orElseThrow(() -> new ResourceNotFoundException("Mandi not found: " + request.getMandiId()));
        Crop crop = cropRepository.findById(request.getCropId())
                .orElseThrow(() -> new ResourceNotFoundException("Crop not found: " + request.getCropId()));

        LocalDate date = request.getScheduledDate();

        // 4. Concurrency Protection: Lock sequence row for this Mandi and Date
        MandiTokenSequence sequence = sequenceRepository.findByMandiIdAndProcurementDateForUpdate(mandi.getId(), date)
                .orElseGet(() -> new MandiTokenSequence(mandi.getId(), date, 0));

        int nextSeq = sequence.getCurrentSequence() + 1;
        sequence.setCurrentSequence(nextSeq);
        sequenceRepository.save(sequence);

        // Format Official Token Number (e.g. MP-SEH-042)
        String distPrefix = (mandi.getDistrict() != null && mandi.getDistrict().trim().length() >= 3)
                ? mandi.getDistrict().trim().substring(0, 3).toUpperCase()
                : "MPM";
        String tokenNumber = String.format("MP-%s-%03d", distPrefix, nextSeq);

        // 5. Build and Save Booking Entity
        Booking booking = new Booking();
        booking.setIdempotencyKey(idempotencyKey);
        booking.setTokenNumber(tokenNumber);
        booking.setTokenSequence(nextSeq);
        booking.setFarmer(farmer);
        booking.setMandi(mandi);
        booking.setCrop(crop);
        booking.setSlotId(request.getSlotId());
        booking.setScheduledDate(date);
        booking.setTimeSlot(request.getTimeSlot());
        booking.setVehicleType(request.getVehicleType());
        booking.setVehicleNumber(request.getVehicleNumber());
        booking.setEstimatedYieldQuintals(request.getEstimatedYieldQuintals());
        booking.setAcreage(request.getAcreage());
        booking.setHarvestDate(request.getHarvestDate());
        booking.setStatus(BookingStatus.BOOKED);
        booking.setQrCodeData("https://euparjan.mp.gov.in/gate-pass?t=" + tokenNumber);

        Booking saved = bookingRepository.save(booking);

        // 6. Update Mandi total token stats
        mandi.setTotalTokensToday(mandi.getTotalTokensToday() + 1);
        mandi.setActiveTokensWaiting(mandi.getActiveTokensWaiting() + 1);
        mandiRepository.save(mandi);

        // 7. Send confirmation SMS asynchronously
        smsService.sendBookingConfirmation(
                farmer.getPhone(),
                farmer.getName(),
                tokenNumber,
                mandi.getName(),
                date.toString(),
                request.getTimeSlot()
        );

        log.info("Official Token {} successfully generated and committed for farmer {} at mandi {}",
                tokenNumber, farmer.getPhone(), mandi.getId());

        return BookingResponse.fromEntity(saved);
    }

    @Transactional
    public BookingResponse transitionStatus(UUID id, BookingStatus targetStatus) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found: " + id));

        BookingStatus current = booking.getStatus();
        if (!current.canTransitionTo(targetStatus)) {
            throw new InvalidStateTransitionException(current.name(), targetStatus.name());
        }

        booking.setStatus(targetStatus);
        Booking updated = bookingRepository.save(booking);

        // Update Mandi waiting counts when gate entered or completed
        Mandi mandi = booking.getMandi();
        if (targetStatus == BookingStatus.GATE_ENTERED || targetStatus == BookingStatus.COMPLETED) {
            if (mandi.getActiveTokensWaiting() > 0) {
                mandi.setActiveTokensWaiting(mandi.getActiveTokensWaiting() - 1);
                mandiRepository.save(mandi);
            }
        }

        // Notify farmer
        smsService.sendStatusUpdate(
                booking.getFarmer().getPhone(),
                booking.getFarmer().getName(),
                booking.getTokenNumber(),
                targetStatus.name()
        );

        return BookingResponse.fromEntity(updated);
    }

    @Transactional
    public BookingResponse cancelBooking(UUID id, String username) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found: " + id));

        if (booking.getStatus() == BookingStatus.COMPLETED || booking.getStatus() == BookingStatus.WEIGHMENT_COMPLETED) {
            throw new BusinessException("CANNOT_CANCEL", "Cannot cancel booking that has already progressed past procurement.");
        }

        return transitionStatus(id, BookingStatus.CANCELLED);
    }

    public List<BookingResponse> getBookingsByFarmer(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
        Farmer farmer = farmerRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Farmer profile not found"));

        return bookingRepository.findByFarmerIdOrderByCreatedAtDesc(farmer.getId()).stream()
                .map(BookingResponse::fromEntity)
                .collect(Collectors.toList());
    }

    public List<BookingResponse> getAllBookings(String mandiId) {
        List<Booking> list = (mandiId != null && !mandiId.isBlank())
                ? bookingRepository.findByMandiIdOrderByCreatedAtDesc(mandiId)
                : bookingRepository.findAll();
        return list.stream().map(BookingResponse::fromEntity).collect(Collectors.toList());
    }

    public BookingResponse getBookingById(UUID id) {
        return bookingRepository.findById(id)
                .map(BookingResponse::fromEntity)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found: " + id));
    }
}
