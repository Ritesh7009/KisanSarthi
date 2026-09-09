-- V6: Bookings and Token Sequences Schema
CREATE TABLE mandi_token_sequences (
    mandi_id VARCHAR(50) NOT NULL REFERENCES mandis(id),
    procurement_date DATE NOT NULL,
    current_sequence INT NOT NULL DEFAULT 0,
    PRIMARY KEY (mandi_id, procurement_date)
);

CREATE TABLE bookings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    idempotency_key VARCHAR(100) UNIQUE,
    token_number VARCHAR(50) NOT NULL UNIQUE,
    token_sequence INT NOT NULL,
    farmer_id UUID NOT NULL REFERENCES farmers(id),
    mandi_id VARCHAR(50) NOT NULL REFERENCES mandis(id),
    crop_id VARCHAR(50) NOT NULL REFERENCES crops(id),
    slot_id VARCHAR(100),
    scheduled_date DATE NOT NULL,
    time_slot VARCHAR(50) NOT NULL,
    vehicle_type VARCHAR(50) NOT NULL,
    vehicle_number VARCHAR(50) NOT NULL,
    estimated_yield_quintals NUMERIC(8, 2) NOT NULL,
    acreage NUMERIC(8, 2),
    harvest_date DATE,
    status VARCHAR(30) NOT NULL DEFAULT 'BOOKED',
    qr_code_data TEXT NOT NULL,
    net_weight_quintals NUMERIC(8, 2),
    moisture_percentage NUMERIC(5, 2),
    settlement_amount NUMERIC(12, 2),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_bookings_farmer_id ON bookings(farmer_id);
CREATE INDEX idx_bookings_mandi_date ON bookings(mandi_id, scheduled_date);
CREATE INDEX idx_bookings_status ON bookings(status);
CREATE INDEX idx_bookings_token_num ON bookings(token_number);
