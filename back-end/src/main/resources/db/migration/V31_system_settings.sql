CREATE TABLE system_settings (
    id BIGSERIAL PRIMARY KEY,

    maintenance_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    maintenance_start TIMESTAMP,
    maintenance_end TIMESTAMP,
    maintenance_message TEXT,

    updated_by VARCHAR(100),
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
