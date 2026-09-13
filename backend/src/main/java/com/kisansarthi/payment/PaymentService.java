package com.kisansarthi.payment;

import com.kisansarthi.booking.Booking;
import com.kisansarthi.booking.BookingRepository;
import com.kisansarthi.booking.BookingStatus;
import com.kisansarthi.common.BusinessException;
import com.kisansarthi.common.ResourceNotFoundException;
import com.kisansarthi.queue.QueueEvent;
import com.kisansarthi.queue.QueueEventRepository;
import com.kisansarthi.slot.SlotEventPublisher;
import com.kisansarthi.sms.SmsService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final BookingRepository bookingRepository;
    private final QueueEventRepository queueEventRepository;
    private final SmsService smsService;
    private final SlotEventPublisher eventPublisher;

    public PaymentService(
            PaymentRepository paymentRepository,
            BookingRepository bookingRepository,
            QueueEventRepository queueEventRepository,
            SmsService smsService,
            SlotEventPublisher eventPublisher
    ) {
        this.paymentRepository = paymentRepository;
        this.bookingRepository = bookingRepository;
        this.queueEventRepository = queueEventRepository;
        this.smsService = smsService;
        this.eventPublisher = eventPublisher;
    }

    @Transactional(readOnly = true)
    public PaymentDto getPayment(UUID bookingId) {
        Payment p = paymentRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found for booking: " + bookingId));
        return toDto(p);
    }

    @Transactional
    public PaymentDto initiatePayment(UUID bookingId, PaymentDto req) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found: " + bookingId));

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new BusinessException("BOOKING_CANCELLED", "Cannot initiate payment for cancelled booking: " + bookingId);
        }

        Payment payment = paymentRepository.findByBookingId(bookingId)
                .orElseGet(() -> {
                    Payment p = new Payment();
                    p.setBooking(booking);
                    p.setFarmer(booking.getFarmer());
                    p.setMandi(booking.getMandi());
                    return p;
                });

        BigDecimal gross = (req != null && req.getGrossAmount() != null && req.getGrossAmount().compareTo(BigDecimal.ZERO) > 0)
                ? req.getGrossAmount()
                : (booking.getSettlementAmount() != null ? booking.getSettlementAmount() : BigDecimal.valueOf(2400));

        BigDecimal deductions = (req != null && req.getDeductions() != null) ? req.getDeductions() : BigDecimal.ZERO;
        BigDecimal net = gross.subtract(deductions);

        String bankLast4 = (req != null && req.getBankAccountLast4() != null && !req.getBankAccountLast4().isBlank())
                ? req.getBankAccountLast4()
                : (booking.getFarmer() != null && booking.getFarmer().getBankAccountLast4() != null ? booking.getFarmer().getBankAccountLast4() : "5421");

        String ifsc = (req != null && req.getIfscCode() != null && !req.getIfscCode().isBlank())
                ? req.getIfscCode()
                : (booking.getFarmer() != null && booking.getFarmer().getIfscCode() != null ? booking.getFarmer().getIfscCode() : "SBIN0001234");

        payment.setGrossAmount(gross);
        payment.setDeductions(deductions);
        payment.setNetPayableAmount(net);
        payment.setBankAccountLast4(bankLast4);
        payment.setIfscCode(ifsc);
        payment.setPaymentStatus("PROCESSING");
        if (payment.getDbtReferenceNo() == null || payment.getDbtReferenceNo().isBlank()) {
            payment.setDbtReferenceNo("DBT-MP-" + System.currentTimeMillis() + "-" + (int)(1000 + Math.random() * 9000));
        }
        payment.setInitiatedAt(OffsetDateTime.now());
        payment.setUpdatedAt(OffsetDateTime.now());

        booking.setStatus(BookingStatus.PAYMENT_PROCESSING);
        booking.setSettlementAmount(net);
        bookingRepository.save(booking);

        Payment saved = paymentRepository.save(payment);

        // Queue Audit Event
        QueueEvent event = new QueueEvent();
        event.setMandi(booking.getMandi());
        event.setBooking(booking);
        event.setTokenNumber(booking.getTokenNumber());
        event.setTokenSequence(booking.getTokenSequence());
        event.setEventType("DBT_PAYMENT_INITIATED");
        event.setNotes("DBT payment file generated: " + payment.getDbtReferenceNo() + " for Rs " + net);
        event.setCreatedAt(OffsetDateTime.now());
        queueEventRepository.save(event);

        // Send SMS confirmation
        if (booking.getFarmer() != null) {
            String msg = String.format(
                    "[MP-EUPARJAN] DBT Settlement initiated for ₹%s (Net amount) against Token %s. Ref: %s.",
                    net.toPlainString(),
                    booking.getTokenNumber(),
                    payment.getDbtReferenceNo()
            );
            smsService.sendSms(booking.getFarmer().getPhone(), booking.getFarmer().getName(), msg);
        }

        eventPublisher.publishAfterCommit(booking.getMandi().getId(), "PAYMENT_INITIATED", toDto(saved));

        return toDto(saved);
    }

    @Transactional
    public PaymentDto confirmPaymentCredit(UUID bookingId, String dbtRef) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found: " + bookingId));

        Payment payment = paymentRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found for booking: " + bookingId));

        payment.setPaymentStatus("COMPLETED");
        payment.setCreditedAt(OffsetDateTime.now());
        payment.setUpdatedAt(OffsetDateTime.now());
        if (dbtRef != null && !dbtRef.isBlank()) {
            payment.setDbtReferenceNo(dbtRef);
        }

        booking.setStatus(BookingStatus.COMPLETED);
        bookingRepository.save(booking);

        Payment saved = paymentRepository.save(payment);

        // Audit Event
        QueueEvent event = new QueueEvent();
        event.setMandi(booking.getMandi());
        event.setBooking(booking);
        event.setTokenNumber(booking.getTokenNumber());
        event.setTokenSequence(booking.getTokenSequence());
        event.setEventType("DBT_PAYMENT_CREDITED");
        event.setNotes("Payment credited successfully via DBT. Ref: " + saved.getDbtReferenceNo());
        event.setCreatedAt(OffsetDateTime.now());
        queueEventRepository.save(event);

        // Send confirmation SMS
        if (booking.getFarmer() != null) {
            String msg = String.format(
                    "[MP-EUPARJAN] ₹%s has been successfully credited via DBT into your bank account ending in %s for Token %s. Ref: %s.",
                    saved.getNetPayableAmount().toPlainString(),
                    saved.getBankAccountLast4(),
                    booking.getTokenNumber(),
                    saved.getDbtReferenceNo()
            );
            smsService.sendSms(booking.getFarmer().getPhone(), booking.getFarmer().getName(), msg);
        }

        eventPublisher.publishAfterCommit(booking.getMandi().getId(), "PAYMENT_CREDITED", toDto(saved));

        return toDto(saved);
    }

    private PaymentDto toDto(Payment p) {
        PaymentDto dto = new PaymentDto();
        dto.setId(p.getId());
        dto.setBookingId(p.getBooking() != null ? p.getBooking().getId() : null);
        dto.setFarmerId(p.getFarmer() != null ? p.getFarmer().getId() : null);
        dto.setMandiId(p.getMandi() != null ? p.getMandi().getId() : null);
        dto.setGrossAmount(p.getGrossAmount());
        dto.setDeductions(p.getDeductions());
        dto.setNetPayableAmount(p.getNetPayableAmount());
        dto.setBankAccountLast4(p.getBankAccountLast4());
        dto.setIfscCode(p.getIfscCode());
        dto.setPaymentStatus(p.getPaymentStatus());
        dto.setDbtReferenceNo(p.getDbtReferenceNo());
        dto.setInitiatedAt(p.getInitiatedAt());
        dto.setCreditedAt(p.getCreditedAt());
        dto.setCreatedAt(p.getCreatedAt());
        return dto;
    }
}
