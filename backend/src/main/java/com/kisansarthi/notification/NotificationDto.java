package com.kisansarthi.notification;

import java.time.OffsetDateTime;
import java.util.UUID;

public class NotificationDto {
    private UUID id;
    private String title;
    private String hindiTitle;
    private String message;
    private String hindiMessage;
    private String category;
    private String senderTag;
    private boolean isRead;
    private OffsetDateTime createdAt;

    public NotificationDto() {}

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
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
