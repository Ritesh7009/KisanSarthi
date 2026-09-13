-- V5: Mandi Slots Table
CREATE TABLE mandi_slots (
    id VARCHAR(100) PRIMARY KEY,
    mandi_id VARCHAR(50) NOT NULL REFERENCES mandis(id) ON DELETE CASCADE,
    slot_label VARCHAR(50) NOT NULL,
    start_time VARCHAR(20) NOT NULL,
    end_time VARCHAR(20) NOT NULL,
    max_capacity_quintals INT NOT NULL DEFAULT 450,
    booked_quintals INT NOT NULL DEFAULT 0,
    max_farmers INT NOT NULL DEFAULT 15,
    booked_farmers INT NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_mandi_slot UNIQUE(mandi_id, slot_label)
);
