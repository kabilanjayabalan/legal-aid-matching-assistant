-- Add check constraint for valid match statuses
-- Allows: PENDING, CITIZEN_ACCEPTED, PROVIDER_CONFIRMED, REJECTED

ALTER TABLE matches
DROP CONSTRAINT IF EXISTS matches_status_check;

ALTER TABLE matches
ADD CONSTRAINT matches_status_check
CHECK (status IN ('PENDING', 'CITIZEN_ACCEPTED', 'PROVIDER_CONFIRMED', 'REJECTED'));

