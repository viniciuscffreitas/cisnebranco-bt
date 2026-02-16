-- Soft delete support for pets and service_types
ALTER TABLE pets ADD COLUMN active BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE service_types ADD COLUMN active BOOLEAN NOT NULL DEFAULT TRUE;

CREATE INDEX idx_pets_active ON pets(active);
CREATE INDEX idx_service_types_active ON service_types(active);
