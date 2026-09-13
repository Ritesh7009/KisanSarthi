-- V13: Procurement Targets and Analytics Indexes
-- References MP State Agricultural Marketing Board (MPSAMB) annual MSP targets

CREATE TABLE procurement_targets (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    district VARCHAR(100) NOT NULL,
    hindi_district VARCHAR(100),
    crop_id VARCHAR(50) REFERENCES crops(id),
    season VARCHAR(20) NOT NULL, -- Rabi, Kharif, Zaid
    procurement_year VARCHAR(20) NOT NULL, -- e.g. '2025-26'
    target_quintals NUMERIC(12, 2) NOT NULL DEFAULT 0.0,
    warehouse_capacity_quintals NUMERIC(12, 2) NOT NULL DEFAULT 0.0,
    effective_start DATE NOT NULL,
    effective_end DATE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_target_district_crop_year UNIQUE (district, crop_id, season, procurement_year)
);

CREATE INDEX idx_targets_district ON procurement_targets(district);
CREATE INDEX idx_targets_year_season ON procurement_targets(procurement_year, season);

-- Analytical Composite Indexes for High-Performance State/District Aggregations
CREATE INDEX IF NOT EXISTS idx_bookings_status_date ON bookings(status, scheduled_date);
CREATE INDEX IF NOT EXISTS idx_bookings_mandi_status ON bookings(mandi_id, status);
CREATE INDEX IF NOT EXISTS idx_bookings_crop_status ON bookings(crop_id, status);
CREATE INDEX IF NOT EXISTS idx_bookings_created_at ON bookings(created_at);

CREATE INDEX IF NOT EXISTS idx_weighments_mandi ON weighments(mandi_id);
CREATE INDEX IF NOT EXISTS idx_weighments_moisture ON weighments(moisture_pct);
CREATE INDEX IF NOT EXISTS idx_weighments_net_weight ON weighments(net_weight_quintals);

CREATE INDEX IF NOT EXISTS idx_payments_mandi_status ON payments(mandi_id, payment_status);
CREATE INDEX IF NOT EXISTS idx_payments_credited_at ON payments(credited_at);
CREATE INDEX IF NOT EXISTS idx_payments_created_at ON payments(created_at);

-- Seed Controlled Targets for MP Reference Districts (Procurement Year 2025-26, Rabi Season)
INSERT INTO procurement_targets (district, hindi_district, crop_id, season, procurement_year, target_quintals, warehouse_capacity_quintals, effective_start, effective_end) VALUES
('Sehore', 'सीहोर', 'crop-wheat', 'Rabi', '2025-26', 950000.00, 1100000.00, '2025-10-01', '2026-06-30'),
('Harda', 'हरदा', 'crop-wheat', 'Rabi', '2025-26', 600000.00, 750000.00, '2025-10-01', '2026-06-30'),
('Ujjain', 'उज्जैन', 'crop-wheat', 'Rabi', '2025-26', 1100000.00, 1300000.00, '2025-10-01', '2026-06-30'),
('Bhopal', 'भोपाल', 'crop-wheat', 'Rabi', '2025-26', 450000.00, 600000.00, '2025-10-01', '2026-06-30'),
('Indore', 'इंदौर', 'crop-wheat', 'Rabi', '2025-26', 1250000.00, 1500000.00, '2025-10-01', '2026-06-30'),
('Vidisha', 'विदिशा', 'crop-wheat', 'Rabi', '2025-26', 700000.00, 850000.00, '2025-10-01', '2026-06-30');
