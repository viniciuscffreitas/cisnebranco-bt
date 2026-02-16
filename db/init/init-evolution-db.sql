-- Creates the separate database used by Evolution API.
-- This script runs only on first container initialization (empty pgdata volume).
SELECT 'CREATE DATABASE evolution'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'evolution')\gexec
