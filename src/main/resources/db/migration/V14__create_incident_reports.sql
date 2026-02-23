-- Incident reports for service orders
CREATE TABLE incident_reports (
    id              BIGSERIAL PRIMARY KEY,
    technical_os_id BIGINT       NOT NULL REFERENCES technical_os(id),
    category        VARCHAR(50)  NOT NULL,
    description     TEXT         NOT NULL,
    reported_by     VARCHAR(100),
    created_at      TIMESTAMP    NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP    NOT NULL DEFAULT now()
);

CREATE INDEX idx_incident_reports_os_id ON incident_reports(technical_os_id);

-- Media attachments for incident reports (photos and videos)
CREATE TABLE incident_media (
    id                 BIGSERIAL PRIMARY KEY,
    incident_report_id BIGINT       NOT NULL REFERENCES incident_reports(id) ON DELETE CASCADE,
    file_path          VARCHAR(500) NOT NULL,
    content_type       VARCHAR(100) NOT NULL,
    created_at         TIMESTAMP    NOT NULL DEFAULT now(),
    updated_at         TIMESTAMP    NOT NULL DEFAULT now()
);

CREATE INDEX idx_incident_media_report_id ON incident_media(incident_report_id);
