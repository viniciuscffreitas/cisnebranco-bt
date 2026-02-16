-- Payment events audit trail
CREATE TABLE payment_events (
    id               BIGSERIAL PRIMARY KEY,
    technical_os_id  BIGINT         NOT NULL REFERENCES technical_os(id),
    amount           NUMERIC(10, 2) NOT NULL,
    method           VARCHAR(20)    NOT NULL CHECK (method IN ('PIX', 'CREDIT_CARD', 'DEBIT_CARD', 'CASH')),
    transaction_ref  VARCHAR(100),
    notes            TEXT,
    refund_of_id     BIGINT         REFERENCES payment_events(id),
    created_by       BIGINT         NOT NULL REFERENCES app_users(id),
    created_at       TIMESTAMP      NOT NULL DEFAULT now(),
    updated_at       TIMESTAMP      NOT NULL DEFAULT now()
);

CREATE INDEX idx_payment_events_os_id ON payment_events(technical_os_id);
CREATE UNIQUE INDEX idx_payment_events_refund_unique ON payment_events(refund_of_id) WHERE refund_of_id IS NOT NULL;

-- Add payment tracking columns to technical_os
ALTER TABLE technical_os
    ADD COLUMN payment_status VARCHAR(20) NOT NULL DEFAULT 'PENDING'
        CHECK (payment_status IN ('PENDING', 'PAID', 'PARTIALLY_PAID', 'REFUNDED', 'CANCELLED')),
    ADD COLUMN total_paid NUMERIC(10, 2) NOT NULL DEFAULT 0
        CHECK (total_paid >= 0);

-- payment_balance as generated column
ALTER TABLE technical_os
    ADD COLUMN payment_balance NUMERIC(10, 2) GENERATED ALWAYS AS (total_price - total_paid) STORED;

CREATE INDEX idx_technical_os_payment_status ON technical_os(payment_status);

-- Trigger: auto-update payment_status when total_paid changes
CREATE OR REPLACE FUNCTION update_payment_status()
RETURNS TRIGGER AS $$
BEGIN
    -- Detect refund: total_paid decreased from a positive value to zero
    IF NEW.total_paid <= 0 AND OLD.total_paid > 0 THEN
        NEW.payment_status := 'REFUNDED';
    ELSIF NEW.total_paid <= 0 THEN
        NEW.payment_status := 'PENDING';
    ELSIF NEW.total_paid >= NEW.total_price AND NEW.total_price > 0 THEN
        NEW.payment_status := 'PAID';
    ELSE
        NEW.payment_status := 'PARTIALLY_PAID';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_update_payment_status
    BEFORE UPDATE OF total_paid ON technical_os
    FOR EACH ROW
    EXECUTE FUNCTION update_payment_status();
