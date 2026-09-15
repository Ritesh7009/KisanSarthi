package com.kisansarthi.booking;

import com.kisansarthi.auth.SecurityAuthorizationService;
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
    private final SecurityAuthorizationService authorizationService;
    private final BookingTransactionExecutor transactionExecutor;

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
            SlotEventPublisher eventPublisher,
            SecurityAuthorizationService authorizationService,
            BookingTransactionExecutor transactionExecutor) {
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
        this.authorizationService = authorizationService;
        this.transactionExecutor = transactionExecutor;
    }

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

        // 4. Resolve Farmer with Impersonation Protection
        Farmer farmer = resolveFarmer(request, authenticatedUsername);

        // 5. Execute within dedicated isolated transaction boundary
        try {
            return transactionExecutor.executeBookingTransaction(
                    request, idempotencyKey, farmer, date, requestedQty, yield
            );
        } catch (org.springframework.dao.DataIntegrityViolationException ex) {
            if (idempotencyKey != null && !idempotencyKey.isBlank() && isIdempotencyKeyViolation(ex)) {
                log.info("Idempotency unique constraint race caught for key {}. Fetching winning transaction booking.", idempotencyKey);
                for (int i = 0; i < 20; i++) {
                    Optional<Booking> winner = bookingRepository.findByIdempotencyKey(idempotencyKey);
                    if (winner.isPresent()) {
                        return BookingResponse.fromEntity(winner.get());
                    }
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
                Optional<Booking> winner = bookingRepository.findByIdempotencyKey(idempotencyKey);
                if (winner.isPresent()) {
                    return BookingResponse.fromEntity(winner.get());
                }
            }
            throw ex;
        }
    }

    private boolean isIdempotencyKeyViolation(org.springframework.dao.DataIntegrityViolationException ex) {
        String msg = ex.getMessage();
        if (msg != null && (msg.toLowerCase().contains("idempotency_key") || msg.toLowerCase().contains("uk_bookings_idempotency_key"))) {
            return true;
        }
        Throwable cause = ex.getCause();
        while (cause != null) {
            String causeMsg = cause.getMessage();
            if (causeMsg != null && (causeMsg.toLowerCase().contains("idempotency_key") || causeMsg.toLowerCase().contains("uk_bookings_idempotency_key"))) {
                return true;
            }
            cause = cause.getCause();
        }
        return false;
    }

    private Farmer resolveFarmer(CreateBookingRequest request, String authenticatedUsername) {
        if (authenticatedUsername != null && !authenticatedUsername.isBlank()) {
            User user = userRepository.findByUsername(authenticatedUsername).orElse(null);
            if (user != null && user.getRole() == com.kisansarthi.auth.Role.ROLE_FARMER) {
                Farmer farmer = farmerRepository.findByUserId(user.getId())
                        .orElseThrow(() -> new ResourceNotFoundException("Farmer profile not found for user: " + authenticatedUsername));
                if (request.getFarmerId() != null && !farmer.getId().equals(request.getFarmerId())) {
                    throw new org.springframework.security.access.AccessDeniedException("Access denied: You are only authorized to create bookings for your own farmer profile");
                }
                return farmer;
            } else if (request.getFarmerId() != null) {
                return farmerRepository.findById(request.getFarmerId())
                        .orElseThrow(() -> new ResourceNotFoundException("Farmer not found: " + request.getFarmerId()));
            } else if (user != null) {
                return farmerRepository.findByUserId(user.getId())
                        .orElseThrow(() -> new ResourceNotFoundException("Farmer profile not found for user: " + authenticatedUsername));
            } else {
                throw new ResourceNotFoundException("Farmer not found");
            }
        } else if (request.getFarmerId() != null) {
            return farmerRepository.findById(request.getFarmerId())
                    .orElseThrow(() -> new ResourceNotFoundException("Farmer not found: " + request.getFarmerId()));
        } else {
            throw new BusinessException("FARMER_REQUIRED", "Farmer ID or authenticated session is required to create a booking");
        }
    }

    @Transactional
    public BookingResponse transitionStatus(UUID id, BookingStatus targetStatus) {
        if (targetStatus == BookingStatus.CANCELLED) {
            return cancelBooking(id, null);
        }

        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found: " + id));

        authorizationService.verifyBookingAccess(booking);

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

        authorizationService.verifyBookingCancellation(booking, username);

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new BookingAlreadyCancelledException("Booking has already been cancelled: " + id);
        }

        if (!booking.getStatus().canTransitionTo(BookingStatus.CANCELLED)) {
            throw new BusinessException("CANNOT_CANCEL",
                    "Cannot cancel booking that has already progressed past procurement status: " + booking.getStatus());
        }

        // 1. Release reserved capacity on slot (Failure MUST propagate and rollback the transaction)
        if (booking.getSlotId() != null || booking.getTimeSlot() != null) {
            MandiSlot slot = slotService.lockAndGetSlot(booking.getMandi().getId(), booking.getSlotId(), booking.getTimeSlot());
            int reservedQty = (booking.getEstimatedYieldQuintals() != null)
                    ? booking.getEstimatedYieldQuintals().setScale(0, RoundingMode.CEILING).intValue()
                    : 0;
            slot.setBookedQuintals(Math.max(0, slot.getBookedQuintals() - reservedQty));
            slot.setBookedFarmers(Math.max(0, slot.getBookedFarmers() - 1));
            slot.recalculateStatus();
            slot = slotRepository.saveAndFlush(slot);
            eventPublisher.publishAfterCommit(slot.getMandiId(), "SLOT_CAPACITY_CHANGED", slot);
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

    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<BookingResponse> getBookingsByFarmer(UUID farmerId, int page, int size) {
        int safePage = Math.max(0, page);
        int safeSize = Math.max(1, Math.min(size, 100)); // Maximum size 100
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(
                safePage,
                safeSize,
                org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "createdAt")
                        .and(org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "id"))
        );

        return bookingRepository.findByFarmerId(farmerId, pageable)
                .map(BookingResponse::fromEntity);
    }

    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<BookingResponse> getBookingsByFarmer(String username, int page, int size) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
        Farmer farmer = farmerRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Farmer profile not found"));

        return getBookingsByFarmer(farmer.getId(), page, size);
    }

    @Transactional(readOnly = true)
    public List<BookingResponse> getBookingsByFarmer(String username) {
        return getBookingsByFarmer(username, 0, 100).getContent();
    }

    @Transactional(readOnly = true)
    public List<BookingResponse> getAllBookings(String mandiId) {
        return getBookingsPaginated(mandiId, 0, 100).getContent();
    }

    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<BookingResponse> getBookingsPaginated(String mandiId, int page, int size) {
        String authoritativeDistrict = authorizationService.resolveAndAuthorizeDistrict(null);
        if (mandiId != null && !mandiId.isBlank()) {
            authorizationService.verifyMandiAccess(mandiId.trim());
        }

        int safePage = Math.max(0, page);
        int safeSize = Math.max(1, Math.min(size, 100)); // Enforce maximum page size of 100
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(
                safePage, safeSize, org.springframework.data.domain.Sort.by("createdAt").descending().and(org.springframework.data.domain.Sort.by("id").descending())
        );

        org.springframework.data.domain.Page<Booking> pageResult;
        if (mandiId != null && !mandiId.isBlank()) {
            pageResult = bookingRepository.findByMandiId(mandiId.trim(), pageable);
        } else if (authoritativeDistrict != null && !authoritativeDistrict.isBlank()) {
            pageResult = bookingRepository.findByMandiDistrictIgnoreCase(authoritativeDistrict.trim(), pageable);
        } else {
            pageResult = bookingRepository.findAll(pageable);
        }

        return pageResult.map(BookingResponse::fromEntity);
    }

    @Transactional(readOnly = true)
    public BookingResponse getBookingById(UUID id) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found: " + id));
        authorizationService.verifyBookingAccess(booking);
        return BookingResponse.fromEntity(booking);
    }
}
