-- V14: Reporting query indexes
-- These indexes support database-backed reporting filters and queue/slot lookups.

CREATE INDEX IF NOT EXISTS idx_bookings_mandi_scheduled_status
    ON bookings(mandi_id, scheduled_date, status);

CREATE INDEX IF NOT EXISTS idx_bookings_farmer_status
    ON bookings(farmer_id, status);

CREATE INDEX IF NOT EXISTS idx_bookings_scheduled_status
    ON bookings(scheduled_date, status);

CREATE INDEX IF NOT EXISTS idx_mandi_slots_mandi_date
    ON mandi_slots(mandi_id, slot_date);

CREATE INDEX IF NOT EXISTS idx_mandi_slots_date_status
    ON mandi_slots(slot_date, status);

CREATE INDEX IF NOT EXISTS idx_queue_events_mandi_created
    ON queue_events(mandi_id, created_at);

CREATE INDEX IF NOT EXISTS idx_queue_events_booking_created
    ON queue_events(booking_id, created_at);

CREATE INDEX IF NOT EXISTS idx_payments_booking_status
    ON payments(booking_id, payment_status);

CREATE INDEX IF NOT EXISTS idx_payments_initiated_status
    ON payments(initiated_at, payment_status);

CREATE INDEX IF NOT EXISTS idx_weighments_booking_created
    ON weighments(booking_id, created_at);

CREATE INDEX IF NOT EXISTS idx_targets_district_crop
    ON procurement_targets(district, crop_id);
