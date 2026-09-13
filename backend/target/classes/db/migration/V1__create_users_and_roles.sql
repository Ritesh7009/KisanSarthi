-- V1: Users and Roles Schema
CREATE TABLE roles (
    id VARCHAR(50) PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(255)
);

INSERT INTO roles (id, name, description) VALUES
('ROLE_FARMER', 'ROLE_FARMER', 'Registered Agricultural Producer'),
('ROLE_MANDI_OPERATOR', 'ROLE_MANDI_OPERATOR', 'Weighbridge & Gate Operator'),
('ROLE_MANDI_MANAGER', 'ROLE_MANDI_MANAGER', 'Mandi Center Administrator'),
('ROLE_DISTRICT_OFFICER', 'ROLE_DISTRICT_OFFICER', 'District Agriculture Marketing Officer'),
('ROLE_ADMIN', 'ROLE_ADMIN', 'State System Super Administrator');

CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username VARCHAR(100) NOT NULL UNIQUE,
    phone VARCHAR(20) UNIQUE,
    password_hash VARCHAR(255),
    role VARCHAR(50) NOT NULL REFERENCES roles(name),
    mandi_id VARCHAR(50),
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_users_phone ON users(phone);
CREATE INDEX idx_users_username ON users(username);
