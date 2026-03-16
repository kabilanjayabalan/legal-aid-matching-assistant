-- V21_create_appointments_table.sql

CREATE TABLE appointments (
    id BIGSERIAL PRIMARY KEY,

    -- Business relationship
    match_id VARCHAR(50) NOT NULL,
    requester_id VARCHAR(50) NOT NULL,
    receiver_id VARCHAR(50) NOT NULL,

    -- Appointment details
    appointment_date DATE NOT NULL,
    time_zone VARCHAR(100) NOT NULL,
    time_slot VARCHAR(20) NOT NULL,
    duration_minutes INT NOT NULL,

    -- Reminder options
    remind_15_min BOOLEAN DEFAULT FALSE,
    remind_1_hour BOOLEAN DEFAULT FALSE,

    -- Status management
    status VARCHAR(20) NOT NULL
        CHECK (status IN ('PENDING', 'CONFIRMED', 'CANCELLED', 'COMPLETED')),

    -- Audit
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
