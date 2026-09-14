package com.kisansarthi.weighment;

import com.kisansarthi.auth.SecurityAuthorizationService;
import com.kisansarthi.auth.User;
import com.kisansarthi.auth.UserRepository;
import com.kisansarthi.booking.Booking;
import com.kisansarthi.booking.BookingRepository;
import com.kisansarthi.booking.BookingResponse;
import com.kisansarthi.booking.BookingStatus;
import com.kisansarthi.common.BusinessException;
import com.kisansarthi.common.ResourceNotFoundException;
import com.kisansarthi.crop.Crop;
import com.kisansarthi.payment.Payment;
import com.kisansarthi.payment.PaymentRepository;
import com.kisansarthi.queue.QueueEvent;
import com.kisansarthi.queue.QueueEventRepository;
import com.kisansarthi.slot.SlotEventPublisher;
import com.kisansarthi.sms.SmsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.UUID;

@Service
public class WeighmentService {

    private static final Logger log = LoggerFactory.getLogger(WeighmentService.class);

    private final WeighmentRepository weighmentRepository;
    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;
    private final QueueEventRepository queueEventRepository;
    private final UserRepository userRepository;
    private final SmsService smsService;
    private final SlotEventPublisher eventPublisher;
    private final SecurityAuthorizationService authorizationService;

    public WeighmentService(
            WeighmentRepository weighmentRepository,
            BookingRepository bookingRepository,
            PaymentRepository paymentRepository,
            QueueEventRepository queueEventRepository,
            UserRepository userRepository,
            SmsService smsService,
            SlotEventPublisher eventPublisher,
            SecurityAuthorizationService authorizationService
    ) {
        this.weighmentRepository = weighmentRepository;
        this.bookingRepository = bookingRepository;
        this.paymentRepository = paymentRepository;
        this.queueEventRepository = queueEventRepository;
        this.userRepository = userRepository;
        this.smsService = smsService;
        this.eventPublisher = eventPublisher;
        this.authorizationService = authorizationService;
    }

    @Transactional(readOnly = true)
    public WeighmentDto getWeighment(UUID bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found: " + bookingId));

        if (booking.getMandi() != null) {
            authorizationService.verifyDistrictAccess(booking.getMandi().getDistrict());
        }

        return weighmentRepository.findByBookingId(bookingId)
                .map(w -> toDto(w, booking))
                .orElseGet(() -> createDraftDto(booking));
    }

    @Transactional
    public WeighmentDto startWeighment(UUID bookingId, String weighbridgeBay, String operatorUsername) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found: " + bookingId));

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new BusinessException("BOOKING_CANCELLED", "Cannot start weighment for cancelled booking: " + bookingId);
        }
        if (booking.getStatus() == BookingStatus.COMPLETED || booking.getStatus() == BookingStatus.PROCUREMENT_COMPLETED) {
            throw new BusinessException("ALREADY_COMPLETED", "Procurement or weighment is already completed for booking: " + bookingId);
        }

        // Transition booking to WEIGHING if not already in weighing workflow
        BookingStatus status = booking.getStatus();
        if (status == BookingStatus.BOOKED || status == BookingStatus.GATE_CALLED) {
            booking.setStatus(BookingStatus.GATE_ENTERED);
        }
        if (booking.getStatus() == BookingStatus.GATE_ENTERED) {
            booking.setStatus(BookingStatus.WEIGHING);
        }

        Weighment weighment = weighmentRepository.findByBookingId(bookingId)
                .orElseGet(() -> {
                    Weighment w = new Weighment();
                    w.setBooking(booking);
                    w.setMandi(booking.getMandi());
                    w.setCreatedAt(OffsetDateTime.now());
                    return w;
                });

        if (weighbridgeBay != null && !weighbridgeBay.isBlank()) {
            weighment.setWeighbridgeBay(weighbridgeBay);
        } else if (weighment.getWeighbridgeBay() == null) {
            weighment.setWeighbridgeBay("Kanta Bay 1");
        }
        if (operatorUsername != null && !operatorUsername.isBlank()) {
            userRepository.findByUsername(operatorUsername).ifPresent(weighment::setWeighbridgeOperator);
        }
        weighment.setUpdatedAt(OffsetDateTime.now());
        weighmentRepository.save(weighment);

        Booking savedBooking = bookingRepository.save(booking);

        // Record Queue Audit Event
        QueueEvent event = new QueueEvent();
        event.setMandi(booking.getMandi());
        event.setBooking(booking);
        event.setTokenNumber(booking.getTokenNumber());
        event.setTokenSequence(booking.getTokenSequence());
        event.setEventType("WEIGHMENT_STARTED");
        event.setNotes("Vehicle admitted to weighbridge bay: " + (weighbridgeBay != null ? weighbridgeBay : "Kanta Bay 1"));
        event.setCreatedAt(OffsetDateTime.now());
        queueEventRepository.save(event);

        WeighmentDto responseDto = getWeighment(bookingId);
        // Broadcast post-commit
        eventPublisher.publishAfterCommit(booking.getMandi().getId(), "WEIGHMENT_STARTED", responseDto);

        return responseDto;
    }

    @Transactional
    public WeighmentDto recordWeighment(UUID bookingId, WeighmentDto req, String operatorUsername) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found: " + bookingId));

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new BusinessException("BOOKING_CANCELLED", "Cannot record weighment for a cancelled booking");
        }

        Weighment weighment = weighmentRepository.findByBookingId(bookingId)
                .orElseGet(() -> {
                    Weighment w = new Weighment();
                    w.setBooking(booking);
                    w.setMandi(booking.getMandi());
                    return w;
                });

        if (operatorUsername != null && !operatorUsername.isBlank()) {
            userRepository.findByUsername(operatorUsername).ifPresent(weighment::setWeighbridgeOperator);
        }

        if (req.getWeighbridgeBay() != null && !req.getWeighbridgeBay().isBlank()) {
            weighment.setWeighbridgeBay(req.getWeighbridgeBay());
        }

        // 1. Normalize Gross Weight (Accept either Kg or Quintals)
        BigDecimal grossQtl = null;
        if (req.getGrossWeightKg() != null && req.getGrossWeightKg().compareTo(BigDecimal.ZERO) > 0) {
            grossQtl = req.getGrossWeightKg().divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        } else if (req.getGrossWeightQuintals() != null) {
            grossQtl = req.getGrossWeightQuintals().setScale(2, RoundingMode.HALF_UP);
        }

        if (grossQtl != null) {
            if (grossQtl.compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusinessException("INVALID_WEIGHT", "Gross weight must be strictly greater than zero");
            }
            weighment.setGrossWeightQuintals(grossQtl);
            weighment.setGrossWeighedAt(OffsetDateTime.now());
            if (booking.getStatus() == BookingStatus.BOOKED || booking.getStatus() == BookingStatus.GATE_ENTERED || booking.getStatus() == BookingStatus.WEIGHING) {
                booking.setStatus(BookingStatus.WEIGHMENT_STAGE_1);
            }
        }

        // 2. Normalize Tare Weight (Accept either Kg or Quintals)
        BigDecimal tareQtl = null;
        if (req.getTareWeightKg() != null && req.getTareWeightKg().compareTo(BigDecimal.ZERO) >= 0) {
            tareQtl = req.getTareWeightKg().divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        } else if (req.getTareWeightQuintals() != null) {
            tareQtl = req.getTareWeightQuintals().setScale(2, RoundingMode.HALF_UP);
        }

        if (tareQtl != null) {
            if (tareQtl.compareTo(BigDecimal.ZERO) < 0) {
                throw new BusinessException("INVALID_WEIGHT", "Tare weight cannot be negative");
            }
            BigDecimal currentGross = weighment.getGrossWeightQuintals();
            if (currentGross == null || currentGross.compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusinessException("INVALID_WEIGHT", "Gross loaded weight must be captured before tare weight");
            }
            if (tareQtl.compareTo(currentGross) >= 0) {
                throw new BusinessException("INVALID_WEIGHT", "Empty tare weight (" + tareQtl + " Qtl) cannot equal or exceed gross weight (" + currentGross + " Qtl)");
            }
            weighment.setTareWeightQuintals(tareQtl);
            weighment.setTareWeighedAt(OffsetDateTime.now());
        }

        // 3. Quality Parameters: Moisture & Foreign Matter
        if (req.getMoisturePct() != null) {
            if (req.getMoisturePct().compareTo(BigDecimal.ZERO) < 0 || req.getMoisturePct().compareTo(BigDecimal.valueOf(100)) > 0) {
                throw new BusinessException("INVALID_MOISTURE", "Moisture content percentage must be between 0.0% and 100.0%");
            }
            weighment.setMoisturePct(req.getMoisturePct());
            booking.setMoisturePercentage(req.getMoisturePct());
        }

        if (req.getForeignMatterPct() != null) {
            if (req.getForeignMatterPct().compareTo(BigDecimal.ZERO) < 0 || req.getForeignMatterPct().compareTo(BigDecimal.valueOf(100)) > 0) {
                throw new BusinessException("INVALID_FOREIGN_MATTER", "Foreign matter percentage must be between 0.0% and 100.0%");
            }
            weighment.setForeignMatterPct(req.getForeignMatterPct());
        }

        // 4. Authoritative Net Weight Calculation (Always computed on backend: Net = Gross - Tare)
        boolean hasGrossAndTare = weighment.getGrossWeightQuintals() != null && weighment.getTareWeightQuintals() != null;
        if (hasGrossAndTare) {
            BigDecimal netQtl = weighment.getGrossWeightQuintals().subtract(weighment.getTareWeightQuintals()).setScale(2, RoundingMode.HALF_UP);
            if (netQtl.compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusinessException("INVALID_WEIGHT", "Authoritative net yield must be greater than zero");
            }
            weighment.setNetWeightQuintals(netQtl);
            booking.setNetWeightQuintals(netQtl);

            // Calculate MSP Rate & Settlement Amount
            Crop crop = booking.getCrop();
            BigDecimal mspRate = (crop != null && crop.getTotalMsp() != null && crop.getTotalMsp().compareTo(BigDecimal.ZERO) > 0)
                    ? crop.getTotalMsp()
                    : (crop != null && crop.getStandardMspPerQuintal() != null ? crop.getStandardMspPerQuintal() : BigDecimal.valueOf(2400));

            BigDecimal grossPayout = mspRate.multiply(netQtl).setScale(2, RoundingMode.HALF_UP);
            BigDecimal deductions = BigDecimal.ZERO;
            BigDecimal netPayable = grossPayout.subtract(deductions).setScale(2, RoundingMode.HALF_UP);

            booking.setSettlementAmount(netPayable);
            booking.setStatus(BookingStatus.WEIGHMENT_COMPLETED);

            // Pre-populate or synchronize Payment entity in PENDING status
            Payment payment = paymentRepository.findByBookingId(booking.getId()).orElseGet(() -> {
                Payment p = new Payment();
                p.setBooking(booking);
                p.setFarmer(booking.getFarmer());
                p.setMandi(booking.getMandi());
                return p;
            });
            payment.setGrossAmount(grossPayout);
            payment.setDeductions(deductions);
            payment.setNetPayableAmount(netPayable);
            payment.setBankAccountLast4(booking.getFarmer() != null && booking.getFarmer().getBankAccountLast4() != null ? booking.getFarmer().getBankAccountLast4() : "5421");
            payment.setIfscCode(booking.getFarmer() != null && booking.getFarmer().getIfscCode() != null ? booking.getFarmer().getIfscCode() : "SBIN0001234");
            if (payment.getPaymentStatus() == null || payment.getPaymentStatus().isBlank()) {
                payment.setPaymentStatus("PENDING");
            }
            paymentRepository.save(payment);

            // Audit Queue Event
            QueueEvent event = new QueueEvent();
            event.setMandi(booking.getMandi());
            event.setBooking(booking);
            event.setTokenNumber(booking.getTokenNumber());
            event.setTokenSequence(booking.getTokenSequence());
            event.setEventType("WEIGHMENT_COMPLETED");
            event.setNotes(String.format("Gross: %s Qtl, Tare: %s Qtl, Net: %s Qtl, Payout: Rs %s",
                    weighment.getGrossWeightQuintals(), weighment.getTareWeightQuintals(), netQtl, netPayable));
            event.setCreatedAt(OffsetDateTime.now());
            queueEventRepository.save(event);

            // Send farmer SMS
            if (booking.getFarmer() != null) {
                String msg = String.format(
                        "[MP-EUPARJAN] Token %s Kanta certification completed. Net weight: %s Qtl. MSP Payout: Rs %s. Receipt/J-Form ready.",
                        booking.getTokenNumber(),
                        netQtl.toPlainString(),
                        netPayable.toPlainString()
                );
                smsService.sendSms(booking.getFarmer().getPhone(), booking.getFarmer().getName(), msg);
            }
        } else {
            // Gross recorded, awaiting tare unloading
            QueueEvent event = new QueueEvent();
            event.setMandi(booking.getMandi());
            event.setBooking(booking);
            event.setTokenNumber(booking.getTokenNumber());
            event.setTokenSequence(booking.getTokenSequence());
            event.setEventType("WEIGHMENT_GROSS_RECORDED");
            event.setNotes("Gross loaded weight recorded: " + weighment.getGrossWeightQuintals() + " Qtl. Proceeding to unloading shed.");
            event.setCreatedAt(OffsetDateTime.now());
            queueEventRepository.save(event);
        }

        weighment.setUpdatedAt(OffsetDateTime.now());
        Weighment savedWeighment = weighmentRepository.save(weighment);
        Booking savedBooking = bookingRepository.save(booking);

        // Broadcast real-time update
        eventPublisher.publishAfterCommit(booking.getMandi().getId(), "WEIGHMENT_RECORDED", toDto(savedWeighment, savedBooking));

        return toDto(savedWeighment, savedBooking);
    }

    @Transactional
    public BookingResponse completeProcurement(UUID bookingId, String operatorUsername) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found: " + bookingId));

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new BusinessException("BOOKING_CANCELLED", "Cannot complete procurement for a cancelled booking");
        }

        if (booking.getNetWeightQuintals() == null || booking.getNetWeightQuintals().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("WEIGHMENT_INCOMPLETE", "Cannot complete procurement without certified net weight");
        }

        // Transition: WEIGHMENT_COMPLETED -> PROCUREMENT_COMPLETED -> PAYMENT_PENDING
        booking.setStatus(BookingStatus.PROCUREMENT_COMPLETED);

        // Ensure settlement amount is calculated
        if (booking.getSettlementAmount() == null || booking.getSettlementAmount().compareTo(BigDecimal.ZERO) <= 0) {
            Crop crop = booking.getCrop();
            BigDecimal mspRate = (crop != null && crop.getTotalMsp() != null) ? crop.getTotalMsp() : BigDecimal.valueOf(2400);
            booking.setSettlementAmount(mspRate.multiply(booking.getNetWeightQuintals()).setScale(2, RoundingMode.HALF_UP));
        }

        // Ensure Payment record is initialized in PENDING state
        Payment payment = paymentRepository.findByBookingId(booking.getId()).orElseGet(() -> {
            Payment p = new Payment();
            p.setBooking(booking);
            p.setFarmer(booking.getFarmer());
            p.setMandi(booking.getMandi());
            return p;
        });
        payment.setGrossAmount(booking.getSettlementAmount());
        payment.setDeductions(BigDecimal.ZERO);
        payment.setNetPayableAmount(booking.getSettlementAmount());
        payment.setBankAccountLast4(booking.getFarmer() != null && booking.getFarmer().getBankAccountLast4() != null ? booking.getFarmer().getBankAccountLast4() : "5421");
        payment.setIfscCode(booking.getFarmer() != null && booking.getFarmer().getIfscCode() != null ? booking.getFarmer().getIfscCode() : "SBIN0001234");
        payment.setPaymentStatus("PENDING");
        paymentRepository.save(payment);

        Booking saved = bookingRepository.save(booking);

        // Log Queue Event
        QueueEvent event = new QueueEvent();
        event.setMandi(booking.getMandi());
        event.setBooking(booking);
        event.setTokenNumber(booking.getTokenNumber());
        event.setTokenSequence(booking.getTokenSequence());
        event.setEventType("PROCUREMENT_COMPLETED");
        event.setNotes("Procurement finalized and certified by " + (operatorUsername != null ? operatorUsername : "APMC Officer"));
        event.setCreatedAt(OffsetDateTime.now());
        queueEventRepository.save(event);

        // Send SMS confirmation to farmer
        if (booking.getFarmer() != null) {
            String msg = String.format(
                    "[MP-EUPARJAN] Procurement finalized for Token %s. Certified Net Yield: %s Qtl (%s). Net Payable: Rs %s. DBT settlement queued.",
                    booking.getTokenNumber(),
                    booking.getNetWeightQuintals().toPlainString(),
                    booking.getCrop() != null ? booking.getCrop().getName() : "Produce",
                    booking.getSettlementAmount().toPlainString()
            );
            smsService.sendSms(booking.getFarmer().getPhone(), booking.getFarmer().getName(), msg);
        }

        // Publish real-time events post-commit
        eventPublisher.publishAfterCommit(booking.getMandi().getId(), "PROCUREMENT_COMPLETED", BookingResponse.fromEntity(saved));

        return BookingResponse.fromEntity(saved);
    }

    private WeighmentDto toDto(Weighment w, Booking b) {
        WeighmentDto dto = new WeighmentDto();
        dto.setId(w.getId());
        dto.setBookingId(b.getId());
        dto.setMandiId(b.getMandi() != null ? b.getMandi().getId() : null);
        dto.setWeighbridgeBay(w.getWeighbridgeBay());
        dto.setGrossWeightQuintals(w.getGrossWeightQuintals());
        dto.setGrossWeighedAt(w.getGrossWeighedAt());
        dto.setTareWeightQuintals(w.getTareWeightQuintals());
        dto.setTareWeighedAt(w.getTareWeighedAt());
        dto.setNetWeightQuintals(w.getNetWeightQuintals());
        dto.setMoisturePct(w.getMoisturePct());
        dto.setForeignMatterPct(w.getForeignMatterPct());
        dto.setCreatedAt(w.getCreatedAt());

        // Derived Kg values
        if (w.getGrossWeightQuintals() != null) {
            dto.setGrossWeightKg(w.getGrossWeightQuintals().multiply(BigDecimal.valueOf(100)).setScale(0, RoundingMode.HALF_UP));
        }
        if (w.getTareWeightQuintals() != null) {
            dto.setTareWeightKg(w.getTareWeightQuintals().multiply(BigDecimal.valueOf(100)).setScale(0, RoundingMode.HALF_UP));
        }
        if (w.getNetWeightQuintals() != null) {
            dto.setNetWeightKg(w.getNetWeightQuintals().multiply(BigDecimal.valueOf(100)).setScale(0, RoundingMode.HALF_UP));
        }

        // Quality & Booking context
        dto.setBookingStatus(b.getStatus().name());
        dto.setTokenNumber(b.getTokenNumber());
        dto.setFarmerName(b.getFarmer() != null ? b.getFarmer().getName() : null);
        dto.setCropName(b.getCrop() != null ? b.getCrop().getName() : null);
        dto.setVehicleNumber(b.getVehicleNumber());

        // Crop moisture specification
        if (b.getCrop() != null) {
            dto.setMoistureLimitPct(b.getCrop().getMoistureLimitPct());
            BigDecimal mspRate = b.getCrop().getTotalMsp() != null ? b.getCrop().getTotalMsp() : b.getCrop().getStandardMspPerQuintal();
            dto.setMspRatePerQuintal(mspRate);

            if (w.getNetWeightQuintals() != null && mspRate != null) {
                BigDecimal grossPayout = mspRate.multiply(w.getNetWeightQuintals()).setScale(2, RoundingMode.HALF_UP);
                dto.setGrossPayableAmount(grossPayout);
                dto.setDeductions(BigDecimal.ZERO);
                dto.setNetPayableAmount(grossPayout);
            }
        }

        // Yield variance (Actual Net - Booked Estimated)
        if (w.getNetWeightQuintals() != null && b.getEstimatedYieldQuintals() != null) {
            dto.setVarianceQuintals(w.getNetWeightQuintals().subtract(b.getEstimatedYieldQuintals()).setScale(2, RoundingMode.HALF_UP));
        }

        // Quality Grade determination
        if (w.getMoisturePct() != null) {
            BigDecimal limit = (b.getCrop() != null && b.getCrop().getMoistureLimitPct() != null)
                    ? b.getCrop().getMoistureLimitPct()
                    : BigDecimal.valueOf(12.0);
            if (w.getMoisturePct().compareTo(limit) <= 0) {
                dto.setQualityGrade("Grade A (FAQ Standard)");
            } else {
                dto.setQualityGrade("Grade B (Moisture Dockage)");
            }
        } else {
            dto.setQualityGrade("Grade A (FAQ Standard)");
        }

        return dto;
    }

    private WeighmentDto createDraftDto(Booking b) {
        WeighmentDto dto = new WeighmentDto();
        dto.setBookingId(b.getId());
        dto.setMandiId(b.getMandi() != null ? b.getMandi().getId() : null);
        dto.setWeighbridgeBay("Kanta Bay 1");
        dto.setBookingStatus(b.getStatus().name());
        dto.setTokenNumber(b.getTokenNumber());
        dto.setFarmerName(b.getFarmer() != null ? b.getFarmer().getName() : null);
        dto.setCropName(b.getCrop() != null ? b.getCrop().getName() : null);
        dto.setVehicleNumber(b.getVehicleNumber());

        if (b.getCrop() != null) {
            dto.setMoistureLimitPct(b.getCrop().getMoistureLimitPct());
            BigDecimal mspRate = b.getCrop().getTotalMsp() != null ? b.getCrop().getTotalMsp() : b.getCrop().getStandardMspPerQuintal();
            dto.setMspRatePerQuintal(mspRate);
            if (b.getEstimatedYieldQuintals() != null && mspRate != null) {
                BigDecimal estimatedPayout = mspRate.multiply(b.getEstimatedYieldQuintals()).setScale(2, RoundingMode.HALF_UP);
                dto.setGrossPayableAmount(estimatedPayout);
                dto.setNetPayableAmount(estimatedPayout);
            }
        }
        dto.setQualityGrade("Grade A (FAQ Standard)");
        return dto;
    }
}
