CREATE TABLE audit_log (
    id          BIGSERIAL PRIMARY KEY,
    action      VARCHAR(50)  NOT NULL,
    entity_type VARCHAR(50)  NOT NULL,
    entity_id   BIGINT,
    username    VARCHAR(100) NOT NULL,
    details     TEXT,
    ip_address  VARCHAR(45),
    created_at  TIMESTAMP    NOT NULL DEFAULT now()
);

CREATE INDEX idx_audit_log_action ON audit_log(action);
CREATE INDEX idx_audit_log_entity ON audit_log(entity_type, entity_id);
CREATE INDEX idx_audit_log_username ON audit_log(username);
CREATE INDEX idx_audit_log_created_at ON audit_log(created_at);
