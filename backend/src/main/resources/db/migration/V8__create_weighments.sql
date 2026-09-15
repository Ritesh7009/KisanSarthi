-- V8: Weighments Table
CREATE TABLE weighments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    booking_id UUID NOT NULL UNIQUE REFERENCES bookings(id) ON DELETE CASCADE,
    mandi_id VARCHAR(50) NOT NULL REFERENCES mandis(id),
    weighbridge_bay VARCHAR(20) NOT NULL,
    gross_weight_quintals NUMERIC(8, 2) NOT NULL,
    gross_weighed_at TIMESTAMP WITH TIME ZONE NOT NULL,
    tare_weight_quintals NUMERIC(8, 2),
    tare_weighed_at TIMESTAMP WITH TIME ZONE,
    net_weight_quintals NUMERIC(8, 2),
    moisture_pct NUMERIC(5, 2),
    foreign_matter_pct NUMERIC(5, 2),
    weighbridge_operator_id UUID REFERENCES users(id),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);
