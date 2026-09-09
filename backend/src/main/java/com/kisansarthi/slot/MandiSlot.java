package com.kisansarthi.slot;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "mandi_slots")
public class MandiSlot {

    @Id
    @Column(length = 100)
    private String id;

    @Column(name = "mandi_id", nullable = false, length = 50)
    private String mandiId;

    @Column(name = "slot_label", nullable = false, length = 50)
    private String slotLabel;

    @Column(name = "start_time", nullable = false, length = 20)
    private String startTime;

    @Column(name = "end_time", nullable = false, length = 20)
    private String endTime;

    @Column(name = "max_capacity_quintals", nullable = false)
    private int maxCapacityQuintals = 450;

    @Column(name = "booked_quintals", nullable = false)
    private int bookedQuintals = 0;

    @Column(name = "max_farmers", nullable = false)
    private int maxFarmers = 15;

    @Column(name = "booked_farmers", nullable = false)
    private int bookedFarmers = 0;

    @Column(nullable = false, length = 20)
    private String status = "AVAILABLE"; // AVAILABLE, FULL, CLOSED

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public MandiSlot() {}

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getMandiId() { return mandiId; }
    public void setMandiId(String mandiId) { this.mandiId = mandiId; }
    public String getSlotLabel() { return slotLabel; }
    public void setSlotLabel(String slotLabel) { this.slotLabel = slotLabel; }
    public String getStartTime() { return startTime; }
    public void setStartTime(String startTime) { this.startTime = startTime; }
    public String getEndTime() { return endTime; }
    public void setEndTime(String endTime) { this.endTime = endTime; }
    public int getMaxCapacityQuintals() { return maxCapacityQuintals; }
    public void setMaxCapacityQuintals(int maxCapacityQuintals) { this.maxCapacityQuintals = maxCapacityQuintals; }
    public int getBookedQuintals() { return bookedQuintals; }
    public void setBookedQuintals(int bookedQuintals) { this.bookedQuintals = bookedQuintals; }
    public int getMaxFarmers() { return maxFarmers; }
    public void setMaxFarmers(int maxFarmers) { this.maxFarmers = maxFarmers; }
    public int getBookedFarmers() { return bookedFarmers; }
    public void setBookedFarmers(int bookedFarmers) { this.bookedFarmers = bookedFarmers; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
