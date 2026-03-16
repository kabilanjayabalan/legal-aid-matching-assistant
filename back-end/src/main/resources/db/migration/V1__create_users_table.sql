-- USERS TABLE
CREATE TABLE users (
                       id SERIAL PRIMARY KEY,
                       full_name VARCHAR(255),
                       email VARCHAR(255) NOT NULL UNIQUE,
                       profile_id INTEGER ,
                       created_at TIMESTAMP DEFAULT NOW(),
                       updated_at TIMESTAMP DEFAULT NOW(),
                       password VARCHAR(255),
                       role VARCHAR(50),
                       username VARCHAR(255)
);
CREATE TABLE IF NOT EXISTS user_profiles (
    id SERIAL PRIMARY KEY,

    user_id INTEGER NOT NULL UNIQUE,

    location VARCHAR(255),
    contact_info TEXT,

    latitude DOUBLE PRECISION,
    longitude DOUBLE PRECISION,

    CONSTRAINT fk_profiles_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE CASCADE
);