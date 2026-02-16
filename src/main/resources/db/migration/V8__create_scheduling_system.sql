-- Add default duration to service types
ALTER TABLE service_types
    ADD COLUMN default_duration_minutes INT NOT NULL DEFAULT 30;

-- Groomer availability windows (weekly schedule)
CREATE TABLE availability_windows (
    id          BIGSERIAL PRIMARY KEY,
    groomer_id  BIGINT   NOT NULL REFERENCES groomers(id),
    day_of_week INT      NOT NULL CHECK (day_of_week BETWEEN 1 AND 7),
    start_time  TIME     NOT NULL,
    end_time    TIME     NOT NULL,
    is_active   BOOLEAN  NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP NOT NULL DEFAULT now(),
    updated_at  TIMESTAMP NOT NULL DEFAULT now(),
    UNIQUE (groomer_id, day_of_week, start_time),
    CHECK (end_time > start_time)
);

CREATE INDEX idx_availability_groomer ON availability_windows(groomer_id);

-- Appointments table
CREATE TABLE appointments (
    id                  BIGSERIAL PRIMARY KEY,
    client_id           BIGINT      NOT NULL REFERENCES clients(id),
    pet_id              BIGINT      NOT NULL REFERENCES pets(id),
    groomer_id          BIGINT      NOT NULL REFERENCES groomers(id),
    service_type_id     BIGINT      NOT NULL REFERENCES service_types(id),
    scheduled_start     TIMESTAMP   NOT NULL,
    scheduled_end       TIMESTAMP   NOT NULL,
    status              VARCHAR(20) NOT NULL DEFAULT 'SCHEDULED'
                                    CHECK (status IN ('SCHEDULED', 'CONFIRMED', 'CANCELLED', 'NO_SHOW', 'COMPLETED')),
    notes               TEXT,
    technical_os_id     BIGINT      UNIQUE REFERENCES technical_os(id),
    cancelled_at        TIMESTAMP,
    cancellation_reason TEXT,
    created_at          TIMESTAMP   NOT NULL DEFAULT now(),
    updated_at          TIMESTAMP   NOT NULL DEFAULT now(),
    CHECK (scheduled_end > scheduled_start)
);

CREATE INDEX idx_appointments_groomer ON appointments(groomer_id, scheduled_start);
CREATE INDEX idx_appointments_client ON appointments(client_id);
CREATE INDEX idx_appointments_status ON appointments(status);

-- Prevent overlapping active appointments for the same groomer
-- This uses a unique functional index to detect conflicts at the DB level
CREATE OR REPLACE FUNCTION check_appointment_conflict()
RETURNS TRIGGER AS $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM appointments a
        WHERE a.groomer_id = NEW.groomer_id
          AND a.id != COALESCE(NEW.id, 0)
          AND a.status NOT IN ('CANCELLED', 'NO_SHOW')
          AND a.scheduled_start < NEW.scheduled_end
          AND a.scheduled_end > NEW.scheduled_start
    ) THEN
        RAISE EXCEPTION 'Appointment conflicts with an existing appointment for this groomer';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_check_appointment_conflict
    BEFORE INSERT OR UPDATE ON appointments
    FOR EACH ROW
    WHEN (NEW.status NOT IN ('CANCELLED', 'NO_SHOW'))
    EXECUTE FUNCTION check_appointment_conflict();
