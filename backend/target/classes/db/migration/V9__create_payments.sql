-- V9: Payments Table
CREATE TABLE payments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    booking_id UUID NOT NULL UNIQUE REFERENCES bookings(id) ON DELETE CASCADE,
    farmer_id UUID NOT NULL REFERENCES farmers(id),
    mandi_id VARCHAR(50) NOT NULL REFERENCES mandis(id),
    gross_amount NUMERIC(12, 2) NOT NULL,
    deductions NUMERIC(12, 2) NOT NULL DEFAULT 0.0,
    net_payable_amount NUMERIC(12, 2) NOT NULL,
    bank_account_last4 VARCHAR(10) NOT NULL,
    ifsc_code VARCHAR(20) NOT NULL,
    payment_status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    dbt_reference_no VARCHAR(100) UNIQUE,
    initiated_at TIMESTAMP WITH TIME ZONE,
    credited_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_payments_farmer ON payments(farmer_id);
CREATE INDEX idx_payments_status ON payments(payment_status);
