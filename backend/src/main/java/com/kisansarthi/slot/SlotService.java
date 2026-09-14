package com.kisansarthi.slot;

import com.kisansarthi.auth.SecurityAuthorizationService;
import com.kisansarthi.common.CapacityReductionNotAllowedException;
import com.kisansarthi.common.SlotNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
public class SlotService {

    private static final Logger log = LoggerFactory.getLogger(SlotService.class);

    private final SlotRepository slotRepository;
    private final SlotEventPublisher eventPublisher;
    private final SecurityAuthorizationService authorizationService;

    public SlotService(
            SlotRepository slotRepository,
            SlotEventPublisher eventPublisher,
            SecurityAuthorizationService authorizationService
    ) {
        this.slotRepository = slotRepository;
        this.eventPublisher = eventPublisher;
        this.authorizationService = authorizationService;
    }

    public List<MandiSlot> getSlotsByMandiId(String mandiId) {
        return slotRepository.findByMandiIdOrderByStartTimeAsc(mandiId);
    }

    public MandiSlot getSlotById(String id) {
        return slotRepository.findById(id)
                .orElseThrow(() -> new SlotNotFoundException("Slot not found: " + id));
    }

    @Transactional
    public MandiSlot lockAndGetSlot(String mandiId, String slotId, String timeSlot) {
        if (slotId != null && !slotId.isBlank()) {
            Optional<MandiSlot> byId = slotRepository.findByIdForUpdate(slotId);
            if (byId.isPresent()) {
                return byId.get();
            }
        }

        String labelToSearch = (timeSlot != null && !timeSlot.isBlank()) ? timeSlot : slotId;
        if (labelToSearch != null && !labelToSearch.isBlank()) {
            List<MandiSlot> matched = slotRepository.findByMandiIdAndSlotIdentifierForUpdate(mandiId, labelToSearch);
            if (!matched.isEmpty()) {
                return matched.get(0);
            }
        }

        // Check if any slot exists for this mandi; if none exists at all (e.g. fresh test mandi), initialize defaults
        List<MandiSlot> existing = slotRepository.findByMandiIdOrderByStartTimeAsc(mandiId);
        if (existing.isEmpty()) {
            log.info("No slots found for mandi [{}]. Auto-initializing default slots.", mandiId);
            MandiSlot defaultSlot = new MandiSlot();
            defaultSlot.setId("slot-" + mandiId + "-default");
            defaultSlot.setMandiId(mandiId);
            defaultSlot.setSlotLabel(labelToSearch != null && !labelToSearch.isBlank() ? labelToSearch : "08:00 AM - 10:00 AM");
            defaultSlot.setStartTime("08:00");
            defaultSlot.setEndTime("10:00");
            defaultSlot.setMaxCapacityQuintals(10000);
            defaultSlot.setBookedQuintals(0);
            defaultSlot.setMaxFarmers(200);
            defaultSlot.setBookedFarmers(0);
            defaultSlot.setStatus("AVAILABLE");
            defaultSlot.setCreatedAt(Instant.now());
            defaultSlot.setUpdatedAt(Instant.now());
            MandiSlot saved = slotRepository.save(defaultSlot);
            return slotRepository.findByIdForUpdate(saved.getId()).orElse(saved);
        }

        throw new SlotNotFoundException("Time slot not found for mandi [" + mandiId + "]: " + labelToSearch);
    }

    @Transactional
    public MandiSlot createSlot(String mandiId, MandiSlot slot) {
        authorizationService.verifyMandiAccess(mandiId);
        if (slot.getId() == null || slot.getId().isBlank()) {
            slot.setId("slot-" + mandiId + "-" + System.currentTimeMillis());
        }
        slot.setMandiId(mandiId);
        if (slot.getStatus() == null || slot.getStatus().isBlank()) {
            slot.setStatus("AVAILABLE");
        }
        slot.setCreatedAt(Instant.now());
        slot.setUpdatedAt(Instant.now());
        MandiSlot saved = slotRepository.save(slot);
        eventPublisher.publishAfterCommit(mandiId, "SLOT_CAPACITY_CHANGED", saved);
        return saved;
    }

    @Transactional
    public MandiSlot updateSlot(String id, MandiSlot update) {
        MandiSlot existing = slotRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new SlotNotFoundException("Slot not found: " + id));

        authorizationService.verifyMandiAccess(existing.getMandiId());

        // Capacity reduction check: cannot reduce max below currently booked amounts
        if (update.getMaxCapacityQuintals() > 0 && update.getMaxCapacityQuintals() < existing.getBookedQuintals()) {
            throw new CapacityReductionNotAllowedException(
                    String.format("Cannot reduce slot capacity below current booked quantity. Booked: %d Quintals, Requested max: %d Quintals",
                            existing.getBookedQuintals(), update.getMaxCapacityQuintals())
            );
        }

        if (update.getMaxFarmers() > 0 && update.getMaxFarmers() < existing.getBookedFarmers()) {
            throw new CapacityReductionNotAllowedException(
                    String.format("Cannot reduce slot max farmers below currently booked farmers. Booked: %d farmers, Requested max: %d farmers",
                            existing.getBookedFarmers(), update.getMaxFarmers())
            );
        }

        if (update.getSlotLabel() != null) existing.setSlotLabel(update.getSlotLabel());
        if (update.getStartTime() != null) existing.setStartTime(update.getStartTime());
        if (update.getEndTime() != null) existing.setEndTime(update.getEndTime());
        if (update.getMaxCapacityQuintals() > 0) existing.setMaxCapacityQuintals(update.getMaxCapacityQuintals());
        if (update.getMaxFarmers() > 0) existing.setMaxFarmers(update.getMaxFarmers());

        if ("CLOSED".equalsIgnoreCase(update.getStatus())) {
            existing.setStatus("CLOSED");
        } else if ("AVAILABLE".equalsIgnoreCase(update.getStatus())) {
            existing.setStatus("AVAILABLE");
            existing.recalculateStatus();
        } else if (update.getStatus() != null) {
            existing.setStatus(update.getStatus());
            existing.recalculateStatus();
        } else {
            existing.recalculateStatus();
        }

        existing.setUpdatedAt(Instant.now());
        MandiSlot saved = slotRepository.save(existing);
        eventPublisher.publishAfterCommit(saved.getMandiId(), "SLOT_CAPACITY_CHANGED", saved);
        return saved;
    }
}
