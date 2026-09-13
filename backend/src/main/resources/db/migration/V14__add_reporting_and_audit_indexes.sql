-- V14: Reporting Query Performance and Composite Indexes
-- Safe, forward-only PostgreSQL composite indexes on existing tables and columns

CREATE INDEX IF NOT EXISTS idx_bookings_status_sched_date ON bookings(status, scheduled_date);
CREATE INDEX IF NOT EXISTS idx_bookings_mandi_sched_date ON bookings(mandi_id, scheduled_date);
CREATE INDEX IF NOT EXISTS idx_bookings_crop_sched_date ON bookings(crop_id, scheduled_date);
CREATE INDEX IF NOT EXISTS idx_bookings_farmer_status ON bookings(farmer_id, status);

CREATE INDEX IF NOT EXISTS idx_weighments_booking_id ON weighments(booking_id);
CREATE INDEX IF NOT EXISTS idx_weighments_mandi_created ON weighments(mandi_id, created_at);

CREATE INDEX IF NOT EXISTS idx_payments_booking_id ON payments(booking_id);
CREATE INDEX IF NOT EXISTS idx_payments_status_initiated ON payments(payment_status, initiated_at);
CREATE INDEX IF NOT EXISTS idx_payments_mandi_status_cred ON payments(mandi_id, payment_status, credited_at);
