-- V15: Remove redundant booking composite index and add index for non-completed payments
-- idx_bookings_status_sched_date in V14 is identical to idx_bookings_status_date in V13 (status, scheduled_date)
-- We safely drop idx_bookings_status_sched_date to eliminate redundant write overhead.

DROP INDEX IF EXISTS idx_bookings_status_sched_date;

-- Add index on payments for SLA delay calculation and non-completed payments
CREATE INDEX IF NOT EXISTS idx_payments_status_initiated_created ON payments(payment_status, initiated_at, created_at);
