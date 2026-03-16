-- V22_add_pending_status_to_appointments.sql
-- Add PENDING status to the appointments status check constraint

-- Drop the old constraint
ALTER TABLE appointments DROP CONSTRAINT IF EXISTS appointments_status_check;

-- Add the new constraint with PENDING included
ALTER TABLE appointments ADD CONSTRAINT appointments_status_check
    CHECK (status IN ('PENDING', 'CONFIRMED', 'CANCELLED', 'COMPLETED'));

