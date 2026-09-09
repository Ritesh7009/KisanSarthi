package com.kisansarthi.notification;

import com.kisansarthi.farmer.Farmer;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "notifications")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "farmer_id", nullable = false)
    private Farmer farmer;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(name = "hindi_title", length = 200)
    private String hindiTitle;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(name = "hindi_message", columnDefinition = "TEXT")
    private String hindiMessage;

    @Column(nullable = false, length = 50)
    private String category = "SYSTEM"; // TOKEN, GATE_CALL, WEIGHMENT, PAYMENT, SYSTEM

    @Column(name = "sender_tag", nullable = false, length = 50)
    private String senderTag = "VK-EUPARJAN";

    @Column(name = "is_read", nullable = false)
    private boolean isRead = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    public Notification() {}

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Farmer getFarmer() {
        return farmer;
    }

    public void setFarmer(Farmer farmer) {
        this.farmer = farmer;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getHindiTitle() {
        return hindiTitle;
    }

    public void setHindiTitle(String hindiTitle) {
        this.hindiTitle = hindiTitle;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getHindiMessage() {
        return hindiMessage;
    }

    public void setHindiMessage(String hindiMessage) {
        this.hindiMessage = hindiMessage;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getSenderTag() {
        return senderTag;
    }

    public void setSenderTag(String senderTag) {
        this.senderTag = senderTag;
    }

    public boolean isRead() {
        return isRead;
    }

    public void setRead(boolean read) {
        isRead = read;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
