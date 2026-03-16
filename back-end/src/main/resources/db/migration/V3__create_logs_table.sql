-- LOGS TABLE
CREATE TABLE logs (
    id SERIAL PRIMARY KEY,
    log_timestamp TIMESTAMP NOT NULL DEFAULT NOW(),
    level VARCHAR(20) NOT NULL,
    logger VARCHAR(255),
    message TEXT NOT NULL
);


