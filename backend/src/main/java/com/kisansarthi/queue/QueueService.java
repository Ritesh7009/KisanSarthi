package com.kisansarthi.queue;

import com.kisansarthi.booking.Booking;
import com.kisansarthi.booking.BookingRepository;
import com.kisansarthi.booking.BookingStatus;
import com.kisansarthi.common.ResourceNotFoundException;
import com.kisansarthi.mandi.Mandi;
import com.kisansarthi.mandi.MandiRepository;
import com.kisansarthi.sms.SmsService;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;

@Service
public class QueueService {

    private final QueueStateRepository queueStateRepository;
    private final QueueEventRepository queueEventRepository;
    private final MandiRepository mandiRepository;
    private final BookingRepository bookingRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final SmsService smsService;

    public QueueService(
            QueueStateRepository queueStateRepository,
            QueueEventRepository queueEventRepository,
            MandiRepository mandiRepository,
            BookingRepository bookingRepository,
            SimpMessagingTemplate messagingTemplate,
            SmsService smsService
    ) {
        this.queueStateRepository = queueStateRepository;
        this.queueEventRepository = queueEventRepository;
        this.mandiRepository = mandiRepository;
        this.bookingRepository = bookingRepository;
        this.messagingTemplate = messagingTemplate;
        this.smsService = smsService;
    }

    @Transactional(readOnly = true)
    public QueueState getQueueState(String mandiId) {
        return queueStateRepository.findById(mandiId)
                .orElseThrow(() -> new ResourceNotFoundException("Queue state not found for mandi: " + mandiId));
    }

    @Transactional(readOnly = true)
    public List<QueueEvent> getRecentEvents(String mandiId) {
        return queueEventRepository.findByMandiIdOrderByCreatedAtDesc(mandiId);
    }

    @Transactional
    public QueueEventDto advanceQueue(String mandiId, String operatorUsername) {
        QueueState queueState = queueStateRepository.findByIdForUpdate(mandiId)
                .orElseGet(() -> {
                    Mandi mandi = mandiRepository.findById(mandiId)
                            .orElseThrow(() -> new ResourceNotFoundException("Mandi not found: " + mandiId));
                    QueueState qs = new QueueState(mandiId);
                    qs.setMandi(mandi);
                    qs.setCurrentServingToken(mandi.getCurrentTokenServing());
                    qs.setTotalTokensGenerated(mandi.getTotalTokensToday());
                    qs.setWaitingCount(mandi.getActiveTokensWaiting());
                    return queueStateRepository.save(qs);
                });

        Mandi mandi = queueState.getMandi();
        if (mandi == null) {
            mandi = mandiRepository.findById(mandiId)
                    .orElseThrow(() -> new ResourceNotFoundException("Mandi not found: " + mandiId));
        }

        int nextToken = queueState.getCurrentServingToken() + 1;
        queueState.setCurrentServingToken(nextToken);

        if (queueState.getWaitingCount() > 0) {
            queueState.setWaitingCount(queueState.getWaitingCount() - 1);
        }

        LocalDate today = LocalDate.now();
        List<Booking> todaysBookings = bookingRepository.findByMandiIdAndScheduledDateOrderByTokenSequenceAsc(mandiId, today);
        Booking matchedBooking = todaysBookings.stream()
                .filter(b -> b.getTokenSequence() == nextToken)
                .findFirst()
                .orElse(null);

        String distPrefix = (mandi.getDistrict() != null && mandi.getDistrict().length() >= 3)
                ? mandi.getDistrict().substring(0, 3).toUpperCase()
                : "MPM";
        String tokenNumber = matchedBooking != null
                ? matchedBooking.getTokenNumber()
                : String.format("MP-%s-%03d", distPrefix, nextToken);

        if (matchedBooking != null) {
            matchedBooking.setStatus(BookingStatus.GATE_CALLED);
            matchedBooking.setUpdatedAt(Instant.now());
            bookingRepository.save(matchedBooking);
            queueState.setActiveBooking(matchedBooking);

            if (matchedBooking.getFarmer() != null) {
                String farmerPhone = matchedBooking.getFarmer().getPhone();
                String farmerName = matchedBooking.getFarmer().getName();
                String smsText = String.format(
                        "[MP-EUPARJAN] Token %s: Aapko Mandi Gate par bulaya gaya hai. Kripya vehicle %s ke sath Kanta Bay 1 par pravesh karein.",
                        tokenNumber,
                        matchedBooking.getVehicleNumber()
                );
                smsService.sendSms(farmerPhone, farmerName, smsText);
            }
        }

        queueState.setLastCalledAt(OffsetDateTime.now());
        queueState.setUpdatedAt(OffsetDateTime.now());
        queueStateRepository.save(queueState);

        mandi.setCurrentTokenServing(nextToken);
        mandi.setActiveTokensWaiting(queueState.getWaitingCount());
        mandiRepository.save(mandi);

        QueueEvent event = new QueueEvent();
        event.setMandi(mandi);
        event.setBooking(matchedBooking);
        event.setTokenNumber(tokenNumber);
        event.setTokenSequence(nextToken);
        event.setEventType("TOKEN_CALLED");
        event.setNotes("Token advanced by " + (operatorUsername != null ? operatorUsername : "Mandi Operator"));
        event.setCreatedAt(OffsetDateTime.now());
        queueEventRepository.save(event);

        QueueEventDto dto = new QueueEventDto();
        dto.setEvent("TOKEN_CALLED");
        dto.setMandiId(mandiId);
        dto.setBookingId(matchedBooking != null ? matchedBooking.getId() : null);
        dto.setTokenNumber(tokenNumber);
        dto.setCurrentToken(nextToken);
        dto.setWaitingCount(queueState.getWaitingCount());
        dto.setStatus("GATE_CALLED");
        dto.setBay(queueState.getCurrentBay());
        dto.setTimestamp(OffsetDateTime.now());
        dto.setNotes(event.getNotes());

        try {
            messagingTemplate.convertAndSend("/topic/mandi/" + mandiId + "/queue", dto);
            messagingTemplate.convertAndSend("/topic/mandi/" + mandiId + "/status", dto);

            if (matchedBooking != null && matchedBooking.getFarmer() != null) {
                messagingTemplate.convertAndSend(
                        "/topic/farmer/" + matchedBooking.getFarmer().getId() + "/notifications",
                        dto
                );
            }
        } catch (Exception e) {
            // Non-fatal logging for WebSocket broadcast in tests or standalone
        }

        return dto;
    }
}
