-- Add assigned timestamp
ALTER TABLE cases
ADD COLUMN assigned_at TIMESTAMP;

-- Add closed timestamp
ALTER TABLE cases
ADD COLUMN closed_at TIMESTAMP;

-- Add closed_by (FK to users.id)
ALTER TABLE cases
ADD COLUMN closed_by INTEGER;

-- Add closure reason
ALTER TABLE cases
ADD COLUMN closure_reason VARCHAR(500);

-- Foreign key constraint for closed_by
ALTER TABLE cases
ADD CONSTRAINT fk_cases_closed_by
FOREIGN KEY (closed_by)
REFERENCES users(id)
ON DELETE SET NULL;

--update old status
UPDATE cases
SET assigned_at = updated_at
WHERE status = 'IN_PROGRESS'
  AND assigned_at IS NULL;