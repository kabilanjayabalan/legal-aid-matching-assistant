-- V29_add_last_modified_by_to_appointments.sql
-- Add lastModifiedBy column to track who last changed the appointment

ALTER TABLE appointments ADD COLUMN IF NOT EXISTS last_modified_by VARCHAR(50);

-- Set initial value for existing appointments to requesterId (they created it)
UPDATE appointments SET last_modified_by = requester_id WHERE last_modified_by IS NULL;

