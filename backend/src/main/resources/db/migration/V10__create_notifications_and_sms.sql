-- V10: Notifications and SMS Logs Schema
CREATE TABLE notifications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    farmer_id UUID NOT NULL REFERENCES farmers(id) ON DELETE CASCADE,
    title VARCHAR(200) NOT NULL,
    hindi_title VARCHAR(200),
    message TEXT NOT NULL,
    hindi_message TEXT,
    category VARCHAR(50) NOT NULL DEFAULT 'SYSTEM',
    sender_tag VARCHAR(50) NOT NULL DEFAULT 'VK-EUPARJAN',
    is_read BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE sms_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    recipient_phone VARCHAR(20) NOT NULL,
    farmer_name VARCHAR(150),
    masked_aadhar VARCHAR(20),
    message TEXT NOT NULL,
    sender_header VARCHAR(20) NOT NULL DEFAULT 'VK-EUPARJAN',
    status VARCHAR(30) NOT NULL DEFAULT 'QUEUED', -- QUEUED, SENDING, SENT, DELIVERED, FAILED, RETRYING
    delivery_report TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_notifications_farmer ON notifications(farmer_id, is_read);
CREATE INDEX idx_sms_phone ON sms_logs(recipient_phone, created_at DESC);
