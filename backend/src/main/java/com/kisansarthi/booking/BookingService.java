package com.kisansarthi.booking;

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
import com.kisansarthi.slot.SlotEventPublisher;
import com.kisansarthi.slot.SlotRepository;
import com.kisansarthi.slot.SlotService;
import com.kisansarthi.sms.SmsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
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
    private final SlotService slotService;
    private final SlotRepository slotRepository;
    private final SlotEventPublisher eventPublisher;

    public BookingService(
            BookingRepository bookingRepository,
            MandiTokenSequenceRepository sequenceRepository,
            FarmerRepository farmerRepository,
            MandiRepository mandiRepository,
            CropRepository cropRepository,
            UserRepository userRepository,
            SmsService smsService,
            SlotService slotService,
            SlotRepository slotRepository,
            SlotEventPublisher eventPublisher) {
        this.bookingRepository = bookingRepository;
        this.sequenceRepository = sequenceRepository;
        this.farmerRepository = farmerRepository;
        this.mandiRepository = mandiRepository;
        this.cropRepository = cropRepository;
        this.userRepository = userRepository;
        this.smsService = smsService;
        this.slotService = slotService;
        this.slotRepository = slotRepository;
        this.eventPublisher = eventPublisher;
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

        // 2. Validate Scheduled Date (cannot be in the past)
        LocalDate date = request.getScheduledDate();
        if (date == null || date.isBefore(LocalDate.now())) {
            throw new InvalidBookingDateException("Scheduled booking date cannot be in the past: " + date);
        }

        // 3. Validate Yield Quantity (must be positive)
        BigDecimal yield = request.getEstimatedYieldQuintals();
        if (yield == null || yield.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidBookingQuantityException("Estimated yield quantity must be greater than zero.");
        }
        int requestedQty = yield.setScale(0, RoundingMode.CEILING).intValue();

        // 4. Resolve Farmer
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

        // 5. Resolve Mandi and Crop
        Mandi mandi = mandiRepository.findById(request.getMandiId())
                .orElseThrow(() -> new ResourceNotFoundException("Mandi not found: " + request.getMandiId()));
        Crop crop = cropRepository.findById(request.getCropId())
                .orElseThrow(() -> new ResourceNotFoundException("Crop not found: " + request.getCropId()));

        // 6. Concurrency Protection on Slot: Lock selected slot row and validate capacity
        MandiSlot slot = slotService.lockAndGetSlot(mandi.getId(), request.getSlotId(), request.getTimeSlot());

        if ("CLOSED".equalsIgnoreCase(slot.getStatus())) {
            throw new SlotClosedException("Selected slot [" + slot.getSlotLabel() + "] is currently closed for bookings.");
        }

        int availableQuintals = slot.getMaxCapacityQuintals() - slot.getBookedQuintals();
        int availableFarmers = slot.getMaxFarmers() - slot.getBookedFarmers();

        if (requestedQty > availableQuintals) {
            throw new InsufficientSlotCapacityException(String.format(
                    "Insufficient capacity in slot [%s]. Requested: %d Qtl, Available: %d Qtl",
                    slot.getSlotLabel(), requestedQty, Math.max(0, availableQuintals)));
        }

        if (availableFarmers <= 0 || slot.getBookedFarmers() >= slot.getMaxFarmers()) {
            throw new FarmerLimitReachedException(String.format(
                    "Farmer limit reached for slot [%s]. Max farmers: %d, currently booked: %d",
                    slot.getSlotLabel(), slot.getMaxFarmers(), slot.getBookedFarmers()));
        }

        // Reserve capacity on slot
        slot.setBookedQuintals(slot.getBookedQuintals() + requestedQty);
        slot.setBookedFarmers(slot.getBookedFarmers() + 1);
        slot.recalculateStatus();
        slot = slotRepository.saveAndFlush(slot);
        log.info("SLOT DEBUG: slotId={}, bookedQuintals={}, bookedFarmers={}", slot.getId(), slot.getBookedQuintals(), slot.getBookedFarmers());

        // 7. Concurrency Protection on Sequence: Lock sequence row for this Mandi and Date
        int nextSeq;
        synchronized (this) {
            MandiTokenSequence sequence = sequenceRepository.findByMandiIdAndProcurementDateForUpdate(mandi.getId(), date)
                    .orElseGet(() -> sequenceRepository.saveAndFlush(new MandiTokenSequence(mandi.getId(), date, 0)));

            nextSeq = sequence.getCurrentSequence() + 1;
            sequence.setCurrentSequence(nextSeq);
            sequenceRepository.saveAndFlush(sequence);
        }

        // Format Official Token Number (e.g. MP-SEH-042)
        String distPrefix = (mandi.getDistrict() != null && mandi.getDistrict().trim().length() >= 3)
                ? mandi.getDistrict().trim().substring(0, 3).toUpperCase()
                : "MPM";
        String tokenNumber = String.format("MP-%s-%03d", distPrefix, nextSeq);

        // 8. Build and Save Booking Entity
        Booking booking = new Booking();
        booking.setIdempotencyKey(idempotencyKey);
        booking.setTokenNumber(tokenNumber);
        booking.setTokenSequence(nextSeq);
        booking.setFarmer(farmer);
        booking.setMandi(mandi);
        booking.setCrop(crop);
        booking.setSlotId(slot.getId());
        booking.setScheduledDate(date);
        booking.setTimeSlot(slot.getSlotLabel());
        booking.setVehicleType(request.getVehicleType());
        booking.setVehicleNumber(request.getVehicleNumber());
        booking.setEstimatedYieldQuintals(yield);
        booking.setAcreage(request.getAcreage());
        booking.setHarvestDate(request.getHarvestDate());
        booking.setStatus(BookingStatus.BOOKED);
        booking.setQrCodeData("https://euparjan.mp.gov.in/gate-pass?t=" + tokenNumber);

        Booking saved = bookingRepository.saveAndFlush(booking);

        // 9. Update Mandi total token stats atomically
        mandiRepository.incrementTokenCounts(mandi.getId());

        // 10. Send confirmation SMS asynchronously
        smsService.sendBookingConfirmation(
                farmer.getPhone(),
                farmer.getName(),
                tokenNumber,
                mandi.getName(),
                date.toString(),
                slot.getSlotLabel()
        );

        // 11. Publish real-time events post-commit
        eventPublisher.publishAfterCommit(mandi.getId(), "BOOKING_CREATED", BookingResponse.fromEntity(saved));
        eventPublisher.publishAfterCommit(mandi.getId(), "SLOT_CAPACITY_CHANGED", slot);

        log.info("Official Token {} successfully generated and committed for farmer {} at mandi {} in slot {}",
                tokenNumber, farmer.getPhone(), mandi.getId(), slot.getSlotLabel());

        return BookingResponse.fromEntity(saved);
    }

    @Transactional
    public BookingResponse transitionStatus(UUID id, BookingStatus targetStatus) {
        if (targetStatus == BookingStatus.CANCELLED) {
            return cancelBooking(id, null);
        }

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
            mandiRepository.decrementActiveWaiting(mandi.getId());
        }

        // Notify farmer
        smsService.sendStatusUpdate(
                booking.getFarmer().getPhone(),
                booking.getFarmer().getName(),
                booking.getTokenNumber(),
                targetStatus.name()
        );

        // Broadcast status transition post-commit
        eventPublisher.publishAfterCommit(mandi.getId(), "BOOKING_STATUS_CHANGED", BookingResponse.fromEntity(updated));

        return BookingResponse.fromEntity(updated);
    }

    @Transactional
    public BookingResponse cancelBooking(UUID id, String username) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found: " + id));

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new BookingAlreadyCancelledException("Booking has already been cancelled: " + id);
        }

        if (!booking.getStatus().canTransitionTo(BookingStatus.CANCELLED)) {
            throw new BusinessException("CANNOT_CANCEL",
                    "Cannot cancel booking that has already progressed past procurement status: " + booking.getStatus());
        }

        // 1. Release reserved capacity on slot
        if (booking.getSlotId() != null || booking.getTimeSlot() != null) {
            try {
                MandiSlot slot = slotService.lockAndGetSlot(booking.getMandi().getId(), booking.getSlotId(), booking.getTimeSlot());
                int reservedQty = (booking.getEstimatedYieldQuintals() != null)
                        ? booking.getEstimatedYieldQuintals().setScale(0, RoundingMode.CEILING).intValue()
                        : 0;
                slot.setBookedQuintals(Math.max(0, slot.getBookedQuintals() - reservedQty));
                slot.setBookedFarmers(Math.max(0, slot.getBookedFarmers() - 1));
                slot.recalculateStatus();
                slot = slotRepository.saveAndFlush(slot);
                eventPublisher.publishAfterCommit(slot.getMandiId(), "SLOT_CAPACITY_CHANGED", slot);
            } catch (Exception e) {
                log.warn("Unable to release slot capacity for booking {}: {}", id, e.getMessage());
            }
        }

        // 2. Decrement active waiting count if booking was waiting (do NOT decrement totalTokensToday)
        Mandi mandi = booking.getMandi();
        if (booking.getStatus() == BookingStatus.BOOKED || booking.getStatus() == BookingStatus.GATE_CALLED) {
            mandiRepository.decrementActiveWaiting(mandi.getId());
        }

        // 3. Mark CANCELLED
        booking.setStatus(BookingStatus.CANCELLED);
        Booking saved = bookingRepository.save(booking);

        // 4. Send cancellation notification
        smsService.sendStatusUpdate(
                booking.getFarmer().getPhone(),
                booking.getFarmer().getName(),
                booking.getTokenNumber(),
                "CANCELLED"
        );

        // 5. Publish real-time events post-commit
        eventPublisher.publishAfterCommit(mandi.getId(), "BOOKING_CANCELLED", BookingResponse.fromEntity(saved));

        return BookingResponse.fromEntity(saved);
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
