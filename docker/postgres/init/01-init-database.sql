-- ATMConnect PostgreSQL Database Initialization Script
-- This script sets up the initial database configuration for ATMConnect

-- Create additional schemas if needed
CREATE SCHEMA IF NOT EXISTS audit;
CREATE SCHEMA IF NOT EXISTS reporting;

-- Grant permissions to the atmconnect user
GRANT USAGE ON SCHEMA public TO atmconnect;
GRANT USAGE ON SCHEMA audit TO atmconnect;
GRANT USAGE ON SCHEMA reporting TO atmconnect;

GRANT CREATE ON SCHEMA public TO atmconnect;
GRANT CREATE ON SCHEMA audit TO atmconnect;
GRANT CREATE ON SCHEMA reporting TO atmconnect;

-- Create extension for UUID generation
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Create extension for cryptographic functions
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- Create audit table for tracking all table changes
CREATE TABLE IF NOT EXISTS audit.audit_log (
    id SERIAL PRIMARY KEY,
    table_name VARCHAR(100) NOT NULL,
    operation VARCHAR(10) NOT NULL,
    old_data JSONB,
    new_data JSONB,
    changed_by VARCHAR(100),
    changed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    transaction_id BIGINT
);

-- Create audit trigger function
CREATE OR REPLACE FUNCTION audit.audit_trigger_function()
RETURNS TRIGGER AS $$
BEGIN
    IF TG_OP = 'DELETE' THEN
        INSERT INTO audit.audit_log (table_name, operation, old_data, changed_by, transaction_id)
        VALUES (TG_TABLE_NAME, TG_OP, row_to_json(OLD), session_user, txid_current());
        RETURN OLD;
    ELSIF TG_OP = 'UPDATE' THEN
        INSERT INTO audit.audit_log (table_name, operation, old_data, new_data, changed_by, transaction_id)
        VALUES (TG_TABLE_NAME, TG_OP, row_to_json(OLD), row_to_json(NEW), session_user, txid_current());
        RETURN NEW;
    ELSIF TG_OP = 'INSERT' THEN
        INSERT INTO audit.audit_log (table_name, operation, new_data, changed_by, transaction_id)
        VALUES (TG_TABLE_NAME, TG_OP, row_to_json(NEW), session_user, txid_current());
        RETURN NEW;
    END IF;
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

-- Grant permissions on audit schema
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA audit TO atmconnect;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA audit TO atmconnect;

-- Create function to generate secure random strings
CREATE OR REPLACE FUNCTION generate_secure_random(length INTEGER)
RETURNS TEXT AS $$
BEGIN
    RETURN encode(gen_random_bytes(length), 'hex');
END;
$$ LANGUAGE plpgsql;

-- Create function to hash passwords with salt
CREATE OR REPLACE FUNCTION hash_password(password TEXT, salt TEXT)
RETURNS TEXT AS $$
BEGIN
    RETURN crypt(password, salt);
END;
$$ LANGUAGE plpgsql;

-- Create function to verify passwords
CREATE OR REPLACE FUNCTION verify_password(password TEXT, hash TEXT)
RETURNS BOOLEAN AS $$
BEGIN
    RETURN hash = crypt(password, hash);
END;
$$ LANGUAGE plpgsql;

-- Grant execute permissions on functions
GRANT EXECUTE ON FUNCTION generate_secure_random(INTEGER) TO atmconnect;
GRANT EXECUTE ON FUNCTION hash_password(TEXT, TEXT) TO atmconnect;
GRANT EXECUTE ON FUNCTION verify_password(TEXT, TEXT) TO atmconnect;

-- Create a cleanup job function for expired tokens
CREATE OR REPLACE FUNCTION cleanup_expired_tokens()
RETURNS INTEGER AS $$
DECLARE
    deleted_count INTEGER;
BEGIN
    DELETE FROM jwt_blacklist WHERE expires_at < NOW();
    GET DIAGNOSTICS deleted_count = ROW_COUNT;
    
    INSERT INTO audit.audit_log (table_name, operation, new_data, changed_by)
    VALUES ('jwt_blacklist', 'CLEANUP', json_build_object('deleted_count', deleted_count), 'system');
    
    RETURN deleted_count;
END;
$$ LANGUAGE plpgsql;

GRANT EXECUTE ON FUNCTION cleanup_expired_tokens() TO atmconnect;

-- Log successful initialization
INSERT INTO audit.audit_log (table_name, operation, new_data, changed_by)
VALUES ('system', 'INIT', json_build_object('message', 'Database initialized successfully'), 'system');