
-- Step 1: add column (nullable)
ALTER TABLE matches
ADD COLUMN provider_type VARCHAR(20);

-- Step 2: backfill existing rows
UPDATE matches
SET provider_type = 'LAWYER'
WHERE provider_type IS NULL;

-- Step 3: enforce NOT NULL
ALTER TABLE matches
ALTER COLUMN provider_type SET NOT NULL;

-- Step 4: drop old constraint if exists
ALTER TABLE matches
DROP CONSTRAINT IF EXISTS uq_case_provider;

-- Step 5: add correct unique constraint
ALTER TABLE matches
ADD CONSTRAINT uq_case_provider
UNIQUE (case_id, provider_type, provider_id);

