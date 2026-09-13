-- V4: Crops Table
CREATE TABLE crops (
    id VARCHAR(50) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    hindi_name VARCHAR(100) NOT NULL,
    malwi_name VARCHAR(100),
    season VARCHAR(20) NOT NULL,
    standard_msp_per_quintal NUMERIC(10, 2) NOT NULL,
    mp_bonus_per_quintal NUMERIC(10, 2) NOT NULL DEFAULT 0.0,
    total_msp NUMERIC(10, 2) NOT NULL,
    market_price_per_quintal NUMERIC(10, 2) NOT NULL,
    typical_cost_per_acre NUMERIC(10, 2) NOT NULL DEFAULT 0.0,
    average_yield_per_acre_quintal NUMERIC(8, 2) NOT NULL DEFAULT 0.0,
    moisture_limit_pct NUMERIC(5, 2) NOT NULL DEFAULT 12.0,
    grade_specs TEXT,
    icon VARCHAR(50),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);
