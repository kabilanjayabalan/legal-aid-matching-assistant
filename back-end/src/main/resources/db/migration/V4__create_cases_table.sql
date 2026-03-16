-- Create cases table
CREATE TABLE cases (
    id SERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    status VARCHAR(50) DEFAULT 'SUBMITTED',
    priority VARCHAR(50) DEFAULT 'MEDIUM',
    created_by INTEGER REFERENCES users(id),
    assigned_to INTEGER REFERENCES users(id),
    location VARCHAR(255),
    is_urgent BOOLEAN DEFAULT FALSE,
    contact_info TEXT,
    category VARCHAR(255),
    preferred_language VARCHAR(255),
    parties TEXT,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP
);

CREATE INDEX idx_cases_created_by ON cases(created_by);
CREATE INDEX idx_cases_assigned_to ON cases(assigned_to);

-- Create table for expertise tags
CREATE TABLE case_expertise_tags (
    case_id INTEGER NOT NULL REFERENCES cases(id) ON DELETE CASCADE,
    tag VARCHAR(255) NOT NULL
);

CREATE INDEX idx_case_expertise_tags_case_id ON case_expertise_tags(case_id);

-- Create table for evidence files
CREATE TABLE case_evidence_files (
    case_id INTEGER NOT NULL REFERENCES cases(id) ON DELETE CASCADE,
    file_url VARCHAR(255) NOT NULL
);

CREATE INDEX idx_case_evidence_files_case_id ON case_evidence_files(case_id);