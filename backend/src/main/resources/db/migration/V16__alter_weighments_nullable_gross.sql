-- V16: Allow staged weighment recording by making gross weight columns nullable
ALTER TABLE weighments
    ALTER COLUMN gross_weight_quintals DROP NOT NULL;

ALTER TABLE weighments
    ALTER COLUMN gross_weighed_at DROP NOT NULL;
