package com.kisansarthi.weighment;

import com.kisansarthi.booking.Booking;
import com.kisansarthi.booking.BookingRepository;
import com.kisansarthi.booking.BookingStatus;
import com.kisansarthi.common.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Service
public class WeighmentService {

    private final WeighmentRepository weighmentRepository;
    private final BookingRepository bookingRepository;

    public WeighmentService(WeighmentRepository weighmentRepository, BookingRepository bookingRepository) {
        this.weighmentRepository = weighmentRepository;
        this.bookingRepository = bookingRepository;
    }

    @Transactional(readOnly = true)
    public WeighmentDto getWeighment(UUID bookingId) {
        Weighment w = weighmentRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Weighment not found for booking: " + bookingId));
        return toDto(w);
    }

    @Transactional
    public WeighmentDto recordWeighment(UUID bookingId, WeighmentDto req) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found: " + bookingId));

        Weighment weighment = weighmentRepository.findByBookingId(bookingId)
                .orElseGet(() -> {
                    Weighment w = new Weighment();
                    w.setBooking(booking);
                    w.setMandi(booking.getMandi());
                    return w;
                });

        if (req.getWeighbridgeBay() != null) {
            weighment.setWeighbridgeBay(req.getWeighbridgeBay());
        }
        if (req.getGrossWeightQuintals() != null) {
            weighment.setGrossWeightQuintals(req.getGrossWeightQuintals());
            weighment.setGrossWeighedAt(OffsetDateTime.now());
            booking.setStatus(BookingStatus.WEIGHMENT_STAGE_1);
        }
        if (req.getTareWeightQuintals() != null) {
            weighment.setTareWeightQuintals(req.getTareWeightQuintals());
            weighment.setTareWeighedAt(OffsetDateTime.now());
            if (weighment.getGrossWeightQuintals() != null) {
                BigDecimal net = weighment.getGrossWeightQuintals().subtract(req.getTareWeightQuintals());
                weighment.setNetWeightQuintals(net);
                booking.setNetWeightQuintals(net);
            }
            booking.setStatus(BookingStatus.WEIGHMENT_STAGE_2);
        }
        if (req.getMoisturePct() != null) {
            weighment.setMoisturePct(req.getMoisturePct());
            booking.setMoisturePercentage(req.getMoisturePct());
        }
        if (req.getForeignMatterPct() != null) {
            weighment.setForeignMatterPct(req.getForeignMatterPct());
        }

        weighment.setUpdatedAt(OffsetDateTime.now());
        Weighment saved = weighmentRepository.save(weighment);
        bookingRepository.save(booking);

        return toDto(saved);
    }

    private WeighmentDto toDto(Weighment w) {
        WeighmentDto dto = new WeighmentDto();
        dto.setId(w.getId());
        dto.setBookingId(w.getBooking() != null ? w.getBooking().getId() : null);
        dto.setMandiId(w.getMandi() != null ? w.getMandi().getId() : null);
        dto.setWeighbridgeBay(w.getWeighbridgeBay());
        dto.setGrossWeightQuintals(w.getGrossWeightQuintals());
        dto.setGrossWeighedAt(w.getGrossWeighedAt());
        dto.setTareWeightQuintals(w.getTareWeightQuintals());
        dto.setTareWeighedAt(w.getTareWeighedAt());
        dto.setNetWeightQuintals(w.getNetWeightQuintals());
        dto.setMoisturePct(w.getMoisturePct());
        dto.setForeignMatterPct(w.getForeignMatterPct());
        dto.setCreatedAt(w.getCreatedAt());
        return dto;
    }
}
