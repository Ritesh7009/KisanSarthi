package com.kisansarthi.slot;

import com.kisansarthi.common.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class SlotService {

    private final SlotRepository slotRepository;

    public SlotService(SlotRepository slotRepository) {
        this.slotRepository = slotRepository;
    }

    public List<MandiSlot> getSlotsByMandiId(String mandiId) {
        return slotRepository.findByMandiIdOrderByStartTimeAsc(mandiId);
    }

    @Transactional
    public MandiSlot createSlot(String mandiId, MandiSlot slot) {
        slot.setId("slot-" + mandiId + "-" + System.currentTimeMillis());
        slot.setMandiId(mandiId);
        slot.setCreatedAt(Instant.now());
        slot.setUpdatedAt(Instant.now());
        return slotRepository.save(slot);
    }

    @Transactional
    public MandiSlot updateSlot(String id, MandiSlot update) {
        MandiSlot existing = slotRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Slot not found: " + id));

        if (update.getSlotLabel() != null) existing.setSlotLabel(update.getSlotLabel());
        if (update.getStartTime() != null) existing.setStartTime(update.getStartTime());
        if (update.getEndTime() != null) existing.setEndTime(update.getEndTime());
        if (update.getMaxCapacityQuintals() > 0) existing.setMaxCapacityQuintals(update.getMaxCapacityQuintals());
        if (update.getMaxFarmers() > 0) existing.setMaxFarmers(update.getMaxFarmers());
        if (update.getStatus() != null) existing.setStatus(update.getStatus());
        existing.setUpdatedAt(Instant.now());

        return slotRepository.save(existing);
    }
}
