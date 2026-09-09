package com.kisansarthi.payment;

import com.kisansarthi.booking.Booking;
import com.kisansarthi.booking.BookingRepository;
import com.kisansarthi.booking.BookingStatus;
import com.kisansarthi.common.ResourceNotFoundException;
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
    private final SmsService smsService;

    public PaymentService(PaymentRepository paymentRepository, BookingRepository bookingRepository, SmsService smsService) {
        this.paymentRepository = paymentRepository;
        this.bookingRepository = bookingRepository;
        this.smsService = smsService;
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

        Payment payment = paymentRepository.findByBookingId(bookingId)
                .orElseGet(() -> {
                    Payment p = new Payment();
                    p.setBooking(booking);
                    p.setFarmer(booking.getFarmer());
                    p.setMandi(booking.getMandi());
                    return p;
                });

        BigDecimal gross = req.getGrossAmount() != null ? req.getGrossAmount() : BigDecimal.ZERO;
        BigDecimal deductions = req.getDeductions() != null ? req.getDeductions() : BigDecimal.ZERO;
        BigDecimal net = gross.subtract(deductions);

        payment.setGrossAmount(gross);
        payment.setDeductions(deductions);
        payment.setNetPayableAmount(net);
        payment.setBankAccountLast4(req.getBankAccountLast4() != null ? req.getBankAccountLast4() : "5421");
        payment.setIfscCode(req.getIfscCode() != null ? req.getIfscCode() : "SBIN0001234");
        payment.setPaymentStatus("PROCESSING");
        payment.setDbtReferenceNo("DBT-MP-" + System.currentTimeMillis() + "-" + (int)(1000 + Math.random() * 9000));
        payment.setInitiatedAt(OffsetDateTime.now());
        payment.setUpdatedAt(OffsetDateTime.now());

        booking.setStatus(BookingStatus.PAYMENT_PROCESSING);
        booking.setSettlementAmount(net);
        bookingRepository.save(booking);

        Payment saved = paymentRepository.save(payment);

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
