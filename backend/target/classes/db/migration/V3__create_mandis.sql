-- V3: Mandis Table
CREATE TABLE mandis (
    id VARCHAR(50) PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    hindi_name VARCHAR(150) NOT NULL,
    district VARCHAR(100) NOT NULL,
    hindi_district VARCHAR(100) NOT NULL,
    address TEXT NOT NULL,
    pin_code VARCHAR(10) NOT NULL,
    open_time VARCHAR(20) NOT NULL DEFAULT '08:00 AM',
    close_time VARCHAR(20) NOT NULL DEFAULT '06:00 PM',
    weighbridges_count INT NOT NULL DEFAULT 4,
    daily_capacity_quintals INT NOT NULL DEFAULT 2000,
    current_token_serving INT NOT NULL DEFAULT 0,
    total_tokens_today INT NOT NULL DEFAULT 0,
    active_tokens_waiting INT NOT NULL DEFAULT 0,
    average_processing_mins INT NOT NULL DEFAULT 15,
    gate_status VARCHAR(30) NOT NULL DEFAULT 'OPEN',
    phone VARCHAR(20),
    commodities_handled TEXT[] NOT NULL DEFAULT ARRAY[]::TEXT[],
    lat DOUBLE PRECISION NOT NULL,
    lng DOUBLE PRECISION NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_mandis_district ON mandis(district);
