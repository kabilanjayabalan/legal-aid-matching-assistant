-- Drop the old case_evidence_files table (file URLs)
DROP TABLE IF EXISTS case_evidence_files CASCADE;

-- Create new evidence_files table to store actual file data
CREATE TABLE evidence_files (
    id SERIAL PRIMARY KEY,
    file_name VARCHAR(255) NOT NULL,
    file_type VARCHAR(255),
    data BYTEA NOT NULL,
    case_id INTEGER NOT NULL REFERENCES cases(id) ON DELETE CASCADE
);

-- Create index on case_id for faster queries
CREATE INDEX idx_evidence_files_case_id ON evidence_files(case_id);

