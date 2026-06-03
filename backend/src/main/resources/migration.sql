-- Migration script: Standardize audit columns and remove merchant_users surrogate key
-- Run this BEFORE starting the application after entity changes
-- This script drops all data — the DataSeeder will re-populate on startup

-- Drop junction tables first (FK dependencies)
DROP TABLE IF EXISTS merchant_users CASCADE;
DROP TABLE IF EXISTS user_roles CASCADE;
DROP TABLE IF EXISTS role_permissions CASCADE;

-- Drop main tables
DROP TABLE IF EXISTS analytics CASCADE;
DROP TABLE IF EXISTS refunds CASCADE;
DROP TABLE IF EXISTS transactions CASCADE;
DROP TABLE IF EXISTS settlements CASCADE;
DROP TABLE IF EXISTS credit_advices CASCADE;
DROP TABLE IF EXISTS merchants CASCADE;
DROP TABLE IF EXISTS users CASCADE;
DROP TABLE IF EXISTS roles CASCADE;
DROP TABLE IF EXISTS permissions CASCADE;

-- Tables will be auto-recreated by Hibernate ddl-auto=update on next startup
-- DataSeeder will re-populate all seed data

-- ============================================================
-- Migration: Standardize all currencies to MYR
-- Run against a LIVE database (no reseed needed)
-- ============================================================
UPDATE transactions    SET currency = 'MYR' WHERE currency IS DISTINCT FROM 'MYR';
UPDATE refunds         SET currency = 'MYR' WHERE currency IS DISTINCT FROM 'MYR';
UPDATE credit_advices  SET currency = 'MYR' WHERE currency IS DISTINCT FROM 'MYR';
UPDATE settlements     SET currency = 'MYR' WHERE currency IS DISTINCT FROM 'MYR';

-- ============================================================
-- Migration: Add MANAGE_CHILD_USERS permission
-- Allows managing only users created by yourself (child users)
-- ============================================================
INSERT INTO permissions (permission_name, description, module)
SELECT 'MANAGE_CHILD_USERS', 'Manage only users created by yourself (child users)', 'USER'
WHERE NOT EXISTS (
    SELECT 1 FROM permissions WHERE permission_name = 'MANAGE_CHILD_USERS'
);
