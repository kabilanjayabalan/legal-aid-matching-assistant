-- Add case_number column to cases table
ALTER TABLE cases ADD COLUMN case_number VARCHAR(50) UNIQUE;

-- Add case_type column to cases table (WP, CS, CR, etc.)
ALTER TABLE cases ADD COLUMN case_type VARCHAR(10);

-- Create index on case_number for faster lookups
CREATE INDEX idx_cases_case_number ON cases(case_number);

-- Create a table to track sequential case numbers per type per year
CREATE TABLE case_number_sequence (
    id SERIAL PRIMARY KEY,
    case_type VARCHAR(10) NOT NULL,
    year INTEGER NOT NULL,
    last_number INTEGER NOT NULL DEFAULT 0,
    UNIQUE(case_type, year)
);

-- Create index for efficient lookups
CREATE INDEX idx_case_number_sequence_type_year ON case_number_sequence(case_type, year);

-- Update existing cases with generated case numbers (if any exist)
-- This will assign default case numbers to existing cases based on their ID and creation date
DO $$
DECLARE
    case_record RECORD;
    case_year INTEGER;
    case_num INTEGER := 0;
BEGIN
    FOR case_record IN SELECT id, created_at FROM cases ORDER BY created_at, id
    LOOP
        case_year := EXTRACT(YEAR FROM case_record.created_at);
        case_num := case_num + 1;

        -- Default to CS (Civil Suit) for existing cases
        UPDATE cases
        SET case_number = 'CS/' || LPAD(case_num::TEXT, 4, '0') || '/' || case_year,
            case_type = 'CS'
        WHERE id = case_record.id;
    END LOOP;
END $$;

