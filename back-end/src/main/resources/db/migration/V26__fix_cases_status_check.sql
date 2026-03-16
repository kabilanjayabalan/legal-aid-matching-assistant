-- Add check constraint for valid case statuses
-- Allows: SUBMITTED, OPEN, ASSIGNED, IN_PROGRESS, RESOLVED, CLOSED

ALTER TABLE cases
DROP CONSTRAINT IF EXISTS cases_status_check;

UPDATE cases
SET status = 'OPEN'
WHERE status = 'SUBMITTED';

UPDATE cases
SET status = 'IN_PROGRESS'
WHERE status = 'ASSIGNED';

ALTER TABLE cases
ADD CONSTRAINT cases_status_check
CHECK (status IN ('OPEN','IN_PROGRESS','CLOSED'));