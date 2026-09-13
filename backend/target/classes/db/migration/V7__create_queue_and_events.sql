-- V7: Real-Time Queue State and Queue Events Schema
CREATE TABLE queue_state (
    mandi_id VARCHAR(50) PRIMARY KEY REFERENCES mandis(id) ON DELETE CASCADE,
    current_serving_token INT NOT NULL DEFAULT 0,
    total_tokens_generated INT NOT NULL DEFAULT 0,
    waiting_count INT NOT NULL DEFAULT 0,
    active_booking_id UUID REFERENCES bookings(id),
    current_bay VARCHAR(20) DEFAULT 'Kanta Bay 1',
    last_called_at TIMESTAMP WITH TIME ZONE,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE queue_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    mandi_id VARCHAR(50) NOT NULL REFERENCES mandis(id),
    booking_id UUID REFERENCES bookings(id),
    token_number VARCHAR(50) NOT NULL,
    token_sequence INT NOT NULL,
    event_type VARCHAR(50) NOT NULL, -- CALL_NEXT, ENTER_GATE, WEIGH_GROSS, WEIGH_TARE, COMPLETE
    operator_user_id UUID REFERENCES users(id),
    notes TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_queue_events_mandi ON queue_events(mandi_id, created_at DESC);
