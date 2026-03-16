CREATE TABLE IF NOT EXISTS matches (
    id SERIAL PRIMARY KEY,
    case_id INTEGER NOT NULL,
    provider_id INTEGER NOT NULL,
    score INTEGER NOT NULL,
    status VARCHAR(30) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_matches_case FOREIGN KEY (case_id)
        REFERENCES cases(id) ON DELETE CASCADE,
    CONSTRAINT fk_matches_provider FOREIGN KEY (provider_id)
        REFERENCES users(id) ON DELETE CASCADE
);
