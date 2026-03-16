-- LAWYER PROFILES
CREATE TABLE lawyer_profiles (
                                 id SERIAL PRIMARY KEY,
                                 user_id INTEGER REFERENCES users(id),
                                 bar_registration_no VARCHAR(100),
                                 specialization VARCHAR(255),
                                 experience_years INTEGER,
                                 city VARCHAR(100),
                                 bio TEXT,
                                 created_at TIMESTAMP DEFAULT NOW()
);

-- NGO PROFILES
CREATE TABLE ngo_profiles (
                              id SERIAL PRIMARY KEY,
                              user_id INTEGER REFERENCES users(id),
                              ngo_name VARCHAR(255),
                              registration_no VARCHAR(100),
                              city VARCHAR(100),
                              website VARCHAR(255),
                              description TEXT,
                              created_at TIMESTAMP DEFAULT NOW()
);

