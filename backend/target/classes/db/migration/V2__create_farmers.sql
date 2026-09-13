-- V2: Farmers Table
CREATE TABLE farmers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID UNIQUE REFERENCES users(id) ON DELETE SET NULL,
    kisan_id VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(150) NOT NULL,
    hindi_name VARCHAR(150),
    phone VARCHAR(20) NOT NULL UNIQUE,
    masked_aadhar VARCHAR(20) NOT NULL,
    aadhar_hash VARCHAR(64),
    district VARCHAR(100) NOT NULL,
    village VARCHAR(100) NOT NULL,
    land_size_acres NUMERIC(8, 2) NOT NULL DEFAULT 0.0,
    bank_account_last4 VARCHAR(10),
    ifsc_code VARCHAR(20),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_farmers_phone ON farmers(phone);
CREATE INDEX idx_farmers_kisan_id ON farmers(kisan_id);
CREATE INDEX idx_farmers_district ON farmers(district);
