-- Clamp any out-of-range rows before adding the constraint so the migration
-- does not fail if data was inserted directly (bypassing API validation).
UPDATE service_types
   SET commission_rate = LEAST(GREATEST(commission_rate, 0), 1)
 WHERE commission_rate < 0 OR commission_rate > 1;

ALTER TABLE service_types
    ADD CONSTRAINT chk_commission_rate CHECK (commission_rate BETWEEN 0 AND 1);
