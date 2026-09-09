package com.kisansarthi.queue;

import com.kisansarthi.auth.User;
import com.kisansarthi.booking.Booking;
import com.kisansarthi.mandi.Mandi;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "queue_events")
public class QueueEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "mandi_id", nullable = false)
    private Mandi mandi;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id")
    private Booking booking;

    @Column(name = "token_number", nullable = false, length = 50)
    private String tokenNumber;

    @Column(name = "token_sequence", nullable = false)
    private int tokenSequence;

    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType; // TOKEN_CALLED, GATE_ENTERED, WEIGH_GROSS, WEIGH_TARE, COMPLETE

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "operator_user_id")
    private User operatorUser;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    public QueueEvent() {}

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Mandi getMandi() {
        return mandi;
    }

    public void setMandi(Mandi mandi) {
        this.mandi = mandi;
    }

    public Booking getBooking() {
        return booking;
    }

    public void setBooking(Booking booking) {
        this.booking = booking;
    }

    public String getTokenNumber() {
        return tokenNumber;
    }

    public void setTokenNumber(String tokenNumber) {
        this.tokenNumber = tokenNumber;
    }

    public int getTokenSequence() {
        return tokenSequence;
    }

    public void setTokenSequence(int tokenSequence) {
        this.tokenSequence = tokenSequence;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public User getOperatorUser() {
        return operatorUser;
    }

    public void setOperatorUser(User operatorUser) {
        this.operatorUser = operatorUser;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
