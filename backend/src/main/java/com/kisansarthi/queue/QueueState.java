package com.kisansarthi.queue;

import com.kisansarthi.booking.Booking;
import com.kisansarthi.mandi.Mandi;
import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "queue_state")
public class QueueState {

    @Id
    @Column(name = "mandi_id", length = 50)
    private String mandiId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mandi_id", insertable = false, updatable = false)
    private Mandi mandi;

    @Column(name = "current_serving_token", nullable = false)
    private int currentServingToken = 0;

    @Column(name = "total_tokens_generated", nullable = false)
    private int totalTokensGenerated = 0;

    @Column(name = "waiting_count", nullable = false)
    private int waitingCount = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "active_booking_id")
    private Booking activeBooking;

    @Column(name = "current_bay", length = 20)
    private String currentBay = "Kanta Bay 1";

    @Column(name = "last_called_at")
    private OffsetDateTime lastCalledAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt = OffsetDateTime.now();

    public QueueState() {}

    public QueueState(String mandiId) {
        this.mandiId = mandiId;
    }

    public String getMandiId() {
        return mandiId;
    }

    public void setMandiId(String mandiId) {
        this.mandiId = mandiId;
    }

    public Mandi getMandi() {
        return mandi;
    }

    public void setMandi(Mandi mandi) {
        this.mandi = mandi;
    }

    public int getCurrentServingToken() {
        return currentServingToken;
    }

    public void setCurrentServingToken(int currentServingToken) {
        this.currentServingToken = currentServingToken;
    }

    public int getTotalTokensGenerated() {
        return totalTokensGenerated;
    }

    public void setTotalTokensGenerated(int totalTokensGenerated) {
        this.totalTokensGenerated = totalTokensGenerated;
    }

    public int getWaitingCount() {
        return waitingCount;
    }

    public void setWaitingCount(int waitingCount) {
        this.waitingCount = waitingCount;
    }

    public Booking getActiveBooking() {
        return activeBooking;
    }

    public void setActiveBooking(Booking activeBooking) {
        this.activeBooking = activeBooking;
    }

    public String getCurrentBay() {
        return currentBay;
    }

    public void setCurrentBay(String currentBay) {
        this.currentBay = currentBay;
    }

    public OffsetDateTime getLastCalledAt() {
        return lastCalledAt;
    }

    public void setLastCalledAt(OffsetDateTime lastCalledAt) {
        this.lastCalledAt = lastCalledAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
