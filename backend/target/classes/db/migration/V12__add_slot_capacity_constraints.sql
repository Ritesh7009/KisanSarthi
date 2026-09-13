-- V12: Add slot capacity constraints and extend sms_logs
ALTER TABLE mandi_slots
    ADD CONSTRAINT chk_slot_booked_quintals_non_negative CHECK (booked_quintals >= 0),
    ADD CONSTRAINT chk_slot_booked_farmers_non_negative CHECK (booked_farmers >= 0),
    ADD CONSTRAINT chk_slot_max_capacity_non_negative CHECK (max_capacity_quintals >= 0),
    ADD CONSTRAINT chk_slot_max_farmers_non_negative CHECK (max_farmers >= 0);

ALTER TABLE sms_logs
    ADD COLUMN IF NOT EXISTS dlt_template_id VARCHAR(100),
    ADD COLUMN IF NOT EXISTS dispatched_by VARCHAR(150),
    ADD COLUMN IF NOT EXISTS channel VARCHAR(50),
    ADD COLUMN IF NOT EXISTS delivery_receipt_id VARCHAR(100);
