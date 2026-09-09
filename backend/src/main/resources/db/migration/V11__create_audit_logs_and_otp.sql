-- V11: Audit Logs, OTP Requests, and Production Seed Data
CREATE TABLE audit_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID,
    username VARCHAR(100),
    action VARCHAR(100) NOT NULL,
    entity VARCHAR(100) NOT NULL,
    entity_id VARCHAR(100),
    ip_address VARCHAR(50),
    details TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_audit_logs_action ON audit_logs(action, created_at DESC);
CREATE INDEX idx_audit_logs_entity ON audit_logs(entity, entity_id);

CREATE TABLE otp_requests (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    phone VARCHAR(20) NOT NULL,
    otp_hash VARCHAR(100) NOT NULL,
    attempts INT NOT NULL DEFAULT 0,
    is_used BOOLEAN NOT NULL DEFAULT false,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_otp_requests_phone ON otp_requests(phone, expires_at);

-- Seed Initial APMC Mandis
INSERT INTO mandis (id, name, hindi_name, district, hindi_district, address, pin_code, open_time, close_time, weighbridges_count, daily_capacity_quintals, current_token_serving, total_tokens_today, active_tokens_waiting, average_processing_mins, gate_status, phone, commodities_handled, lat, lng) VALUES
('mandi-sehore', 'Krishi Upaj Mandi Samiti, Sehore', 'कृषि उपज मंडी समिति, सीहोर', 'Sehore', 'सीहोर', 'Mandi Road, Near Bus Stand, Sehore', '466001', '08:00 AM', '06:00 PM', 6, 3500, 38, 120, 14, 12, 'OPEN', '07562-224580', ARRAY['Wheat', 'Soybean', 'Chana', 'Mustard']::TEXT[], 23.2031, 77.0844),
('mandi-harda', 'Krishi Upaj Mandi Samiti, Harda', 'कृषि उपज मंडी समिति, हरदा', 'Harda', 'हरदा', 'Timarni Road, Harda Industrial Area', '461331', '08:30 AM', '06:30 PM', 5, 2800, 24, 85, 9, 14, 'OPEN', '07577-222145', ARRAY['Wheat', 'Soybean', 'Moong', 'Chana']::TEXT[], 22.3395, 77.0952),
('mandi-ujjain', 'Ujjain Krishi Upaj Mandi (Chimanganj)', 'कृषि उपज मंडी (चिमनगंज), उज्जैन', 'Ujjain', 'उज्जैन', 'Chimanganj Mandi, Agar Road, Ujjain', '456006', '08:00 AM', '07:00 PM', 8, 4500, 52, 160, 22, 10, 'OPEN', '0734-2581200', ARRAY['Wheat', 'Soybean', 'Chana', 'Garlic', 'Mustard']::TEXT[], 23.1872, 75.7725),
('mandi-bhopal-karond', 'Karond Krishi Upaj Mandi, Bhopal', 'करोंद कृषि उपज मंडी, भोपाल', 'Bhopal', 'भोपाल', 'Berasia Road, Karond, Bhopal', '462038', '08:00 AM', '06:00 PM', 4, 2200, 19, 64, 7, 15, 'OPEN', '0755-2741122', ARRAY['Wheat', 'Soybean', 'Chana']::TEXT[], 23.2985, 77.4055),
('mandi-indore', 'Devi Ahilya Bai Holkar Mandi, Indore', 'देवी अहिल्या बाई होलकर मंडी, इंदौर', 'Indore', 'इंदौर', 'Laxmibai Nagar, Sanwer Road, Indore', '452006', '08:00 AM', '07:30 PM', 10, 6000, 74, 210, 28, 10, 'OPEN', '0731-2415800', ARRAY['Soybean', 'Wheat', 'Chana', 'Garlic', 'Cotton']::TEXT[], 22.7533, 75.8637),
('mandi-vidisha', 'Krishi Upaj Mandi Samiti, Vidisha', 'कृषि उपज मंडी समिति, विदिशा', 'Vidisha', 'विदिशा', 'Sagar Road, Vidisha', '464001', '08:30 AM', '06:00 PM', 4, 2000, 16, 55, 6, 16, 'OPEN', '07592-232145', ARRAY['Wheat', 'Chana', 'Soybean', 'Lentil']::TEXT[], 23.5251, 77.8081);

-- Initialize Queue State for each Mandi
INSERT INTO queue_state (mandi_id, current_serving_token, total_tokens_generated, waiting_count, current_bay) VALUES
('mandi-sehore', 38, 120, 14, 'Kanta Bay 1'),
('mandi-harda', 24, 85, 9, 'Kanta Bay 2'),
('mandi-ujjain', 52, 160, 22, 'Kanta Bay 1'),
('mandi-bhopal-karond', 19, 64, 7, 'Kanta Bay 3'),
('mandi-indore', 74, 210, 28, 'Kanta Bay 1'),
('mandi-vidisha', 16, 55, 6, 'Kanta Bay 2');

-- Seed Crops
INSERT INTO crops (id, name, hindi_name, malwi_name, season, standard_msp_per_quintal, mp_bonus_per_quintal, total_msp, market_price_per_quintal, typical_cost_per_acre, average_yield_per_acre_quintal, moisture_limit_pct, grade_specs, icon) VALUES
('crop-wheat', 'Wheat (Gehun - Sharbati)', 'गेहूं (शरबती / सीहोर गोल्ड)', 'गेहूँ', 'Rabi', 2275.00, 150.00, 2425.00, 2650.00, 14500.00, 18.5, 12.0, 'Grade A, Max moisture 12%, Foreign matter < 0.75%', 'Wheat'),
('crop-soybean', 'Soybean (Pili Sona)', 'सोयाबीन (पीली सोना - JS 9560)', 'सोयाबीन', 'Kharif', 4892.00, 0.00, 4892.00, 4750.00, 16200.00, 8.5, 10.0, 'Yellow sound beans, Moisture < 10%, Foreign matter < 1%', 'Sparkles'),
('crop-mustard', 'Mustard (Sarson / Rai)', 'सरसों (राई / कालो सरसो)', 'सरसों', 'Rabi', 5650.00, 0.00, 5650.00, 5820.00, 11800.00, 7.2, 8.0, 'Oil content > 38%, Moisture < 8%', 'Sparkles'),
('crop-chana', 'Gram / Chickpea (Desi Chana)', 'चना (देसी / काबुली)', 'चणा', 'Rabi', 5440.00, 0.00, 5440.00, 5600.00, 12500.00, 9.0, 9.5, 'Sound clean grains, Moisture < 9.5%', 'Sparkles'),
('crop-moong', 'Green Gram (Moong)', 'मूंग (ग्रीन ग्राम)', 'मूंग', 'Zaid', 8558.00, 0.00, 8558.00, 8700.00, 13000.00, 5.5, 9.0, 'Sound dry pulses, Moisture < 9%', 'Sparkles');

-- Seed Standard Mandi Slots
INSERT INTO mandi_slots (id, mandi_id, slot_label, start_time, end_time, max_capacity_quintals, booked_quintals, max_farmers, booked_farmers, status) VALUES
('slot-sehore-08-10', 'mandi-sehore', 'Morning Slot 1 (08:00 - 10:00 AM)', '08:00 AM', '10:00 AM', 800, 650, 20, 16, 'AVAILABLE'),
('slot-sehore-10-12', 'mandi-sehore', 'Morning Slot 2 (10:00 - 12:00 PM)', '10:00 AM', '12:00 PM', 800, 800, 20, 20, 'FULL'),
('slot-sehore-12-02', 'mandi-sehore', 'Midday Slot (12:00 - 02:00 PM)', '12:00 PM', '02:00 PM', 700, 420, 18, 11, 'AVAILABLE'),
('slot-sehore-02-04', 'mandi-sehore', 'Afternoon Slot (02:00 - 04:00 PM)', '02:00 PM', '04:00 PM', 700, 300, 18, 8, 'AVAILABLE'),
('slot-sehore-04-06', 'mandi-sehore', 'Evening Slot (04:00 - 06:00 PM)', '04:00 PM', '06:00 PM', 500, 150, 15, 4, 'AVAILABLE');

-- Seed Official Admin User
INSERT INTO users (id, username, phone, role, mandi_id, is_active) VALUES
('a0000000-0000-0000-0000-000000000001', 'MP-AGRI-ADMIN-701', '9826000001', 'ROLE_ADMIN', 'mandi-sehore', true);
