package com.kisansarthi.booking;

import com.kisansarthi.auth.SecurityAuthorizationService;
import com.kisansarthi.common.*;
import com.kisansarthi.crop.Crop;
import com.kisansarthi.crop.CropRepository;
import com.kisansarthi.farmer.Farmer;
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
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

@Service
public class BookingTransactionExecutor {

    private static final Logger log = LoggerFactory.getLogger(BookingTransactionExecutor.class);

    private final BookingRepository bookingRepository;
    private final MandiTokenSequenceRepository sequenceRepository;
    private final MandiRepository mandiRepository;
    private final CropRepository cropRepository;
    private final SmsService smsService;
    private final SlotService slotService;
    private final SlotRepository slotRepository;
    private final SlotEventPublisher eventPublisher;
    private final SecurityAuthorizationService authorizationService;

    public BookingTransactionExecutor(
            BookingRepository bookingRepository,
            MandiTokenSequenceRepository sequenceRepository,
            MandiRepository mandiRepository,
            CropRepository cropRepository,
            SmsService smsService,
            SlotService slotService,
            SlotRepository slotRepository,
            SlotEventPublisher eventPublisher,
            SecurityAuthorizationService authorizationService
    ) {
        this.bookingRepository = bookingRepository;
        this.sequenceRepository = sequenceRepository;
        this.mandiRepository = mandiRepository;
        this.cropRepository = cropRepository;
        this.smsService = smsService;
        this.slotService = slotService;
        this.slotRepository = slotRepository;
        this.eventPublisher = eventPublisher;
        this.authorizationService = authorizationService;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, isolation = Isolation.READ_COMMITTED)
    public BookingResponse executeBookingTransaction(
            CreateBookingRequest request,
            String idempotencyKey,
            Farmer farmer,
            LocalDate date,
            int requestedQty,
            BigDecimal yield
    ) {
        // Authorize Mandi Access
        authorizationService.verifyMandiAccess(request.getMandiId());

        // 1. Resolve Mandi and Crop with pessimistic write lock on Mandi row
        Mandi mandi = mandiRepository.findByIdForUpdate(request.getMandiId())
                .orElseThrow(() -> new ResourceNotFoundException("Mandi not found: " + request.getMandiId()));
        Crop crop = cropRepository.findById(request.getCropId())
                .orElseThrow(() -> new ResourceNotFoundException("Crop not found: " + request.getCropId()));

        // CRITICAL CHECK: After acquiring the Mandi lock, check if another concurrent thread
        // with the same idempotency key already committed while this thread was waiting on the lock
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            Optional<Booking> existing = bookingRepository.findByIdempotencyKey(idempotencyKey);
            if (existing.isPresent()) {
                log.info("Idempotent booking request matched after acquiring Mandi lock for key {}. Returning existing token {}",
                        idempotencyKey, existing.get().getTokenNumber());
                return BookingResponse.fromEntity(existing.get());
            }
        }

        // 2. Lock selected slot row and validate capacity
        MandiSlot slot = slotService.lockAndGetSlot(mandi.getId(), request.getSlotId(), request.getTimeSlot());

        if ("CLOSED".equalsIgnoreCase(slot.getStatus())) {
            throw new SlotClosedException("Selected slot [" + slot.getSlotLabel() + "] is currently closed for bookings.");
        }

        int availableQuintals = slot.getAvailableQuintals();
        if (requestedQty > availableQuintals) {
            throw new InsufficientSlotCapacityException(
                    String.format("Requested yield (%d Qtl) exceeds remaining slot capacity (%d Qtl).",
                            requestedQty, availableQuintals)
            );
        }

        if (slot.getBookedFarmers() >= slot.getMaxFarmers()) {
            throw new FarmerLimitReachedException(
                    String.format("Slot farmer capacity reached (max %d farmers).", slot.getMaxFarmers())
            );
        }

        // Reserve capacity on slot
        slot.setBookedQuintals(slot.getBookedQuintals() + requestedQty);
        slot.setBookedFarmers(slot.getBookedFarmers() + 1);
        slot.recalculateStatus();
        slot = slotRepository.saveAndFlush(slot);

        // 3. Lock sequence row for this Mandi and Date
        MandiTokenSequence sequence = sequenceRepository.findByMandiIdAndProcurementDateForUpdate(mandi.getId(), date)
                .orElseGet(() -> {
                    MandiTokenSequence newSeq = new MandiTokenSequence(mandi.getId(), date, 0);
                    return sequenceRepository.saveAndFlush(newSeq);
                });

        int nextSeq = sequence.getCurrentSequence() + 1;
        sequence.setCurrentSequence(nextSeq);
        sequenceRepository.saveAndFlush(sequence);

        // Format Official Token Number (e.g. MP-SEH-042)
        String distPrefix = (mandi.getDistrict() != null && mandi.getDistrict().trim().length() >= 3)
                ? mandi.getDistrict().trim().substring(0, 3).toUpperCase()
                : "MPM";
        String tokenNumber = String.format("MP-%s-%03d", distPrefix, nextSeq);

        // 4. Build and Save Booking Entity
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
        booking.setQrCodeData(String.format("TOKEN:%s|FARMER:%s|MANDI:%s|DATE:%s",
                tokenNumber, farmer.getKisanId(), mandi.getId(), date));

        Booking saved = bookingRepository.saveAndFlush(booking);

        // 5. Increment Daily Counters on Mandi
        mandiRepository.incrementTokenCounts(mandi.getId());

        // 6. Dispatch SMS Confirmation post-commit
        smsService.sendBookingConfirmation(
                farmer.getPhone(),
                farmer.getName(),
                tokenNumber,
                mandi.getName(),
                date.toString(),
                slot.getSlotLabel()
        );

        // 7. Publish real-time events post-commit
        eventPublisher.publishAfterCommit(mandi.getId(), "BOOKING_CREATED", BookingResponse.fromEntity(saved));
        eventPublisher.publishAfterCommit(mandi.getId(), "SLOT_CAPACITY_CHANGED", slot);

        log.info("Official Token {} successfully generated and committed for farmer {} at mandi {} in slot {}",
                tokenNumber, farmer.getPhone(), mandi.getId(), slot.getSlotLabel());

        return BookingResponse.fromEntity(saved);
    }
}
