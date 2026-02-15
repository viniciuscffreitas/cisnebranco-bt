-- Add SCHEDULED status to the technical_os state machine
ALTER TABLE technical_os DROP CONSTRAINT IF EXISTS technical_os_status_check;
ALTER TABLE technical_os ADD CONSTRAINT technical_os_status_check
    CHECK (status IN ('SCHEDULED', 'WAITING', 'IN_PROGRESS', 'READY', 'DELIVERED'));
ALTER TABLE technical_os ALTER COLUMN status SET DEFAULT 'SCHEDULED';
