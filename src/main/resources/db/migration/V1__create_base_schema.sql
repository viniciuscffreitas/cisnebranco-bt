-- Breeds
CREATE TABLE breeds (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    species     VARCHAR(10)  NOT NULL CHECK (species IN ('DOG', 'CAT')),
    created_at  TIMESTAMP    NOT NULL DEFAULT now(),
    updated_at  TIMESTAMP    NOT NULL DEFAULT now()
);

-- Service types
CREATE TABLE service_types (
    id              BIGSERIAL PRIMARY KEY,
    code            VARCHAR(30)    NOT NULL UNIQUE,
    name            VARCHAR(100)   NOT NULL,
    commission_rate NUMERIC(5, 2)  NOT NULL,
    created_at      TIMESTAMP      NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP      NOT NULL DEFAULT now()
);

-- Groomers
CREATE TABLE groomers (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(150) NOT NULL,
    phone       VARCHAR(20)  NOT NULL,
    active      BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP    NOT NULL DEFAULT now(),
    updated_at  TIMESTAMP    NOT NULL DEFAULT now()
);

-- App users (login)
CREATE TABLE app_users (
    id          BIGSERIAL PRIMARY KEY,
    username    VARCHAR(100)  NOT NULL UNIQUE,
    password    VARCHAR(255)  NOT NULL,
    role        VARCHAR(20)   NOT NULL CHECK (role IN ('ADMIN', 'GROOMER')),
    groomer_id  BIGINT        REFERENCES groomers(id),
    active      BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP     NOT NULL DEFAULT now(),
    updated_at  TIMESTAMP     NOT NULL DEFAULT now()
);

-- Clients
CREATE TABLE clients (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(200) NOT NULL,
    phone       VARCHAR(20)  NOT NULL,
    email       VARCHAR(200),
    address     VARCHAR(500),
    created_at  TIMESTAMP    NOT NULL DEFAULT now(),
    updated_at  TIMESTAMP    NOT NULL DEFAULT now()
);

-- Pets
CREATE TABLE pets (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(100)  NOT NULL,
    species     VARCHAR(10)   NOT NULL CHECK (species IN ('DOG', 'CAT')),
    breed_id    BIGINT        REFERENCES breeds(id),
    size        VARCHAR(10)   NOT NULL CHECK (size IN ('SMALL', 'MEDIUM', 'LARGE')),
    notes       TEXT,
    client_id   BIGINT        NOT NULL REFERENCES clients(id),
    created_at  TIMESTAMP     NOT NULL DEFAULT now(),
    updated_at  TIMESTAMP     NOT NULL DEFAULT now()
);

CREATE INDEX idx_pets_client_id ON pets(client_id);

-- Pricing matrix (service × species × size → price)
CREATE TABLE pricing_matrix (
    id              BIGSERIAL PRIMARY KEY,
    service_type_id BIGINT         NOT NULL REFERENCES service_types(id),
    species         VARCHAR(10)    NOT NULL CHECK (species IN ('DOG', 'CAT')),
    pet_size        VARCHAR(10)    NOT NULL CHECK (pet_size IN ('SMALL', 'MEDIUM', 'LARGE')),
    price           NUMERIC(10, 2) NOT NULL,
    created_at      TIMESTAMP      NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP      NOT NULL DEFAULT now(),
    UNIQUE (service_type_id, species, pet_size)
);

-- Technical OS (ordem de servico)
CREATE TABLE technical_os (
    id               BIGSERIAL PRIMARY KEY,
    pet_id           BIGINT          NOT NULL REFERENCES pets(id),
    groomer_id       BIGINT          REFERENCES groomers(id),
    status           VARCHAR(20)     NOT NULL DEFAULT 'SCHEDULED'
                                     CHECK (status IN ('SCHEDULED', 'WAITING', 'IN_PROGRESS', 'READY', 'DELIVERED')),
    total_price      NUMERIC(10, 2)  NOT NULL DEFAULT 0,
    total_commission NUMERIC(10, 2)  NOT NULL DEFAULT 0,
    balance          NUMERIC(10, 2)  GENERATED ALWAYS AS (total_price - total_commission) STORED,
    started_at       TIMESTAMP,
    finished_at      TIMESTAMP,
    delivered_at     TIMESTAMP,
    notes            TEXT,
    created_at       TIMESTAMP       NOT NULL DEFAULT now(),
    updated_at       TIMESTAMP       NOT NULL DEFAULT now()
);

CREATE INDEX idx_technical_os_groomer_id ON technical_os(groomer_id);
CREATE INDEX idx_technical_os_status     ON technical_os(status);
CREATE INDEX idx_technical_os_delivered  ON technical_os(groomer_id, delivered_at);

-- OS service items (price snapshot at check-in)
CREATE TABLE os_service_items (
    id                     BIGSERIAL PRIMARY KEY,
    technical_os_id        BIGINT          NOT NULL REFERENCES technical_os(id) ON DELETE CASCADE,
    service_type_id        BIGINT          NOT NULL REFERENCES service_types(id),
    locked_price           NUMERIC(10, 2)  NOT NULL,
    locked_commission_rate NUMERIC(5, 2)   NOT NULL,
    commission_value       NUMERIC(10, 2)  NOT NULL,
    created_at             TIMESTAMP       NOT NULL DEFAULT now(),
    updated_at             TIMESTAMP       NOT NULL DEFAULT now()
);

CREATE INDEX idx_os_service_items_os_id ON os_service_items(technical_os_id);

-- Inspection photos
CREATE TABLE inspection_photos (
    id              BIGSERIAL PRIMARY KEY,
    technical_os_id BIGINT       NOT NULL REFERENCES technical_os(id) ON DELETE CASCADE,
    file_path       VARCHAR(500) NOT NULL,
    caption         VARCHAR(300),
    created_at      TIMESTAMP    NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP    NOT NULL DEFAULT now()
);

CREATE INDEX idx_inspection_photos_os_id ON inspection_photos(technical_os_id);

-- Health checklists (one per OS)
CREATE TABLE health_checklists (
    id              BIGSERIAL PRIMARY KEY,
    technical_os_id BIGINT       NOT NULL UNIQUE REFERENCES technical_os(id) ON DELETE CASCADE,
    skin_condition  VARCHAR(200),
    coat_condition  VARCHAR(200),
    has_fleas       BOOLEAN      NOT NULL DEFAULT FALSE,
    has_ticks       BOOLEAN      NOT NULL DEFAULT FALSE,
    has_wounds      BOOLEAN      NOT NULL DEFAULT FALSE,
    ear_condition   VARCHAR(200),
    nail_condition  VARCHAR(200),
    observations    TEXT,
    created_at      TIMESTAMP    NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP    NOT NULL DEFAULT now()
);

-- Weekly commissions
CREATE TABLE weekly_commissions (
    id               BIGSERIAL PRIMARY KEY,
    groomer_id       BIGINT          NOT NULL REFERENCES groomers(id),
    week_start       DATE            NOT NULL,
    week_end         DATE            NOT NULL,
    total_services   INT             NOT NULL DEFAULT 0,
    total_revenue    NUMERIC(10, 2)  NOT NULL DEFAULT 0,
    total_commission NUMERIC(10, 2)  NOT NULL DEFAULT 0,
    created_at       TIMESTAMP       NOT NULL DEFAULT now(),
    updated_at       TIMESTAMP       NOT NULL DEFAULT now(),
    UNIQUE (groomer_id, week_start)
);
