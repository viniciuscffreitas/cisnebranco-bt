-- Creates the separate database and user for Evolution API.
-- This script runs only on first container initialization (empty pgdata volume).
SELECT 'CREATE DATABASE evolution'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'evolution')\gexec

DO $$
BEGIN
  IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'evolution') THEN
    CREATE ROLE evolution WITH LOGIN PASSWORD 'change-me-evolution-pass';
  END IF;
END$$;

GRANT ALL PRIVILEGES ON DATABASE evolution TO evolution;
