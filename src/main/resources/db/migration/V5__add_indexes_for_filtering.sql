-- Optimize TechnicalOs filtering
CREATE INDEX idx_technical_os_created_at ON technical_os(created_at);
CREATE INDEX idx_technical_os_status_created ON technical_os(status, created_at);

-- Optimize Client filtering
CREATE INDEX idx_clients_name ON clients(name);
CREATE INDEX idx_clients_phone ON clients(phone);
CREATE INDEX idx_clients_created_at ON clients(created_at);

-- Service type reporting index
CREATE INDEX idx_os_service_items_service_type ON os_service_items(service_type_id);
