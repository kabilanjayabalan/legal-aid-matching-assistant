-- Create directory_lawyers table for external imports (Bar Council data, etc.)
CREATE TABLE directory_lawyers (
    id SERIAL PRIMARY KEY,
    full_name VARCHAR(255) NOT NULL,
    bar_registration_id VARCHAR(255) NOT NULL UNIQUE,
    specialization VARCHAR(255),
    city VARCHAR(255),
    contact_number VARCHAR(50),
    email VARCHAR(255),
    source VARCHAR(100) DEFAULT 'EXTERNAL',
    verified BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_directory_lawyers_city ON directory_lawyers(city);
CREATE INDEX idx_directory_lawyers_specialization ON directory_lawyers(specialization);

-- Create directory_ngos table for external imports (NGO Darpan data, etc.)
CREATE TABLE directory_ngos (
    id SERIAL PRIMARY KEY,
    org_name VARCHAR(255) NOT NULL,
    registration_number VARCHAR(255) NOT NULL UNIQUE,
    focus_area VARCHAR(255),
    city VARCHAR(255),
    contact_number VARCHAR(50),
    email VARCHAR(255),
    website VARCHAR(255),
    source VARCHAR(100) DEFAULT 'EXTERNAL',
    verified BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_directory_ngos_city ON directory_ngos(city);
CREATE INDEX idx_directory_ngos_focus_area ON directory_ngos(focus_area);

